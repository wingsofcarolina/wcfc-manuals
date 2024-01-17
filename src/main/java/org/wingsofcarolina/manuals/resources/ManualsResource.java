package org.wingsofcarolina.manuals.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.CharacterIterator;
import java.text.ParseException;
import java.text.StringCharacterIterator;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import com.palantir.roboslack.api.MessageRequest;
import com.palantir.roboslack.api.attachments.Attachment;
import com.palantir.roboslack.api.attachments.Attachment.Builder;
import com.palantir.roboslack.api.attachments.components.Author;
import com.palantir.roboslack.api.attachments.components.Color;
import com.palantir.roboslack.api.attachments.components.Field;
import com.palantir.roboslack.api.attachments.components.Footer;
import com.palantir.roboslack.api.attachments.components.Title;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.exceptions.CsvException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.model.Aircraft;
import org.wingsofcarolina.manuals.model.AircraftType;
import org.wingsofcarolina.manuals.model.Equipment;
import org.wingsofcarolina.manuals.model.EquipmentType;
import org.wingsofcarolina.manuals.model.User;
import org.wingsofcarolina.manuals.slack.Slack;
import org.wingsofcarolina.manuals.slack.SlackAuthService;
import org.wingsofcarolina.manuals.ManualsConfiguration;
import org.wingsofcarolina.manuals.SimpleLogger;
import org.wingsofcarolina.manuals.authentication.AuthUtils;

/**
 * @author dwight
 *
 */
@Path("/")	// Note that this is actually accessed as /api due to the setUrPattern() call in parent service
public class ManualsResource {
	private static final Logger LOG = LoggerFactory.getLogger(ManualsResource.class);
	
	private static SimpleLogger accessLog;

	private static ManualsConfiguration config;
	private static String versionOverride = null;
	private DateTimeFormatter dateFormatGmt;
	private DateTimeFormatter dateFormatArchive;

    // Slack credentials
    private static final String CLIENT_ID = "REDACTED";
    private static final String CLIENT_SECRET = "REDACTED";
	private SlackAuthService slackAuth;

	private AuthUtils authUtils;
	
	private Integer accessCount = 0;
	
	private static User mockUser = null; // When we are developing and don't want to authenticate with Slack
	
	private static String root;
	
	private ObjectMapper mapper;
	
	public static enum ManualType { AIRCRAFT, EQUIPMENT };

	private static final String AIRCRAFT_JSON = "Aircraft.json";
	private static final String EQUIPMENT_JSON = "Equipment.json";
	
	private final ReentrantLock lock = new ReentrantLock();;

	private List<Equipment> equipmentCache = new ArrayList<Equipment>();
	private List<Aircraft> aircraftCache = new ArrayList<Aircraft>();
	private AircraftComparator aircraft_compare = new AircraftComparator();
	private ManualTypeComparator manual_compare = new ManualTypeComparator();

	@SuppressWarnings("static-access")
	public ManualsResource(ManualsConfiguration config) throws IOException, ListFolderErrorException, DbxException {
		this.config = config;
		
		// Create authentication and access logs
		accessLog = new SimpleLogger("access", config);
		
		// See if we have a mock user
		if (config.getMockUser() != null) {
			mockUser = User.userFromMock(config.getMockUser());
		}
		
		// Get authorization utils object instance
		authUtils = AuthUtils.instance();
		
		// Get the Manual document root
		root = config.getRoot();
		
		// For JSON serialization/deserialization
		mapper = new ObjectMapper();
		
		// Get the startup date/time format in GMT and for the archive
		dateFormatGmt = DateTimeFormatter.ofPattern("yyyy/MM/dd - HH:mm:ss z");
		dateFormatArchive = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		
        // Create Slack authentication API service object
        slackAuth = new SlackAuthService(CLIENT_ID, CLIENT_SECRET);
        
        // Load data store (from JSON files, simplistic!)
        loadDataStore();
	}
	
	private void loadDataStore() throws JsonParseException, JsonMappingException, IOException {
		try {
			lock.lock();
			
			// Load the aircraft store first ....
		    aircraftCache = Arrays.asList(mapper.readValue(Paths.get(root + "/" + AIRCRAFT_JSON).toFile(), Aircraft[].class));
		    
		    // then the equipment store last
		    equipmentCache = Arrays.asList(mapper.readValue(Paths.get(root + "/" + EQUIPMENT_JSON).toFile(), Equipment[].class));
		} finally {
			lock.unlock();
		}

	}

	@GET
	@Path("version")
	@Produces(MediaType.APPLICATION_JSON)
	public Response version() {
		Map<String, String> version = getBuildMetadata();
		if (version == null) {
			return Response.status(404).build();
		} else {
			return Response.ok().entity(version).build();
		}
	}
	
	@GET
	@Path("user")
	@Produces(MediaType.APPLICATION_JSON)
	public Response user(@CookieParam("wcfc.manuals.token") Cookie cookie) {
		User user = null;
		Boolean anonymous = false;
		Boolean badcookie = false;
        Map<String, Object> reply = new HashMap<String, Object>();
        
        if (config.getAuth() == true) {
        	user = authUtils.getUserFromCookie(cookie);
		} else {
			if ( mockUser == null) {
				user = new User("Anonymous", "nobody@wingsofcarolina.org");
				anonymous = true;
			} else {
				user = new User("Dwight Frye", "dwight@openweave.org");
			}
		}

        if (cookie != null) {
	        Jws<Claims> claims = authUtils.decodeCookie(cookie);
			if (claims.getBody().get("version") == null && user.getEmail().equals("dwight@openweave.org")) {
				badcookie = true;
			}
        } else {
        	badcookie = true;
        }
        if (config.getAuth() == false || ( user != null && badcookie == false ) ) {
	        reply.put("name", user.getName());
	        reply.put("email", user.getEmail());
	        reply.put("admin", user.getAdmin());
	        reply.put("anonymous", anonymous);

			NewCookie newCookie = authUtils.generateCookie(user);
	        return Response.ok().entity(reply).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
        } else {
        	NewCookie newCookie = AuthUtils.instance().removeCookie();
        	return Response.status(404).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
        }
	}
	
	@GET
	@Path("fetch/{uuid}")
	@Produces("application/pdf")
	public Response fetchFile(@CookieParam("wcfc.manuals.token") Cookie cookie,
			@PathParam("uuid") String uuid) throws IOException, DbxException {

		User user = AuthUtils.instance().getUserFromCookie(cookie);
		if (user != null) {
			File file = new File(root + "/" + uuid + ".pdf");
			if (file.exists()) {
				accessLog.logAccess(user, documentName(uuid));
				accessCount++;

				InputStream inputStream = new FileInputStream(file);
				return Response.ok().type("application/pdf").entity(inputStream).build();
			} else {
				return Response.status(404).build();
			}
		} else {
			return Response.status(401).build();
		}
	}
	
	private String documentName(String uuid) {
		for (Equipment e : equipmentCache) {
			if (e.getUuid().equals(uuid)) {
				return e.getName();
			}
		}
		for (Aircraft a : aircraftCache) {
			if (a.getUuid().equals(uuid)) {
				return a.getRegistration() + " POH";
			}
		}
		return "Not Found";
	}
	
	@GET
	@Path("aircraft")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAircraft(@CookieParam("wcfc.manuals.token") Cookie cookie) {
		User user = AuthUtils.instance().getUserFromCookie(cookie);
		
		if (user != null) {
			try {
				lock.lock();

				// Always return aircraft in type sorted order
				aircraftCache.sort(aircraft_compare);
			} finally {
				lock.unlock();
			}
			
	        return Response.ok().entity(aircraftCache).build();
		} else {
			return Response.status(401).entity("Not authorized.").build();
		}
	}
	
	class AircraftComparator implements Comparator<Aircraft>
	{
	    public int compare(Aircraft a, Aircraft b)
	    {
	        return a.getType().compareTo(b.getType());
	    }
	}
	
	@PATCH
	@Path("aircraft/add")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response addEquipment(@CookieParam("wcfc.manuals.token") Cookie cookie,
			Map<String, String>request) {
		
		if (request != null) {
			String registration = request.get("aircraft");
			String uuid = request.get("uuid");
			
			try {
				// We need to ensure that nobody does anything while we are updating
				lock.lock();

				Aircraft aircraft = getAircraftByRegistration(registration);
				Equipment equipment = getEquipmentByUuid(uuid);
				if (aircraft != null && equipment != null) {
					aircraft.addEquipment(equipment);
				}
				writeJson("Aircraft", aircraftCache);
			} finally {
				lock.unlock();
			}
		} else {
			return Response.status(404).build();
		}
		
		return Response.ok().build();
	}
	
	@PATCH
	@Path("aircraft/remove")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response removeEquipment(@CookieParam("wcfc.manuals.token") Cookie cookie,
			Map<String, String>request) {
		
		if (request != null) {
			String registration = request.get("aircraft");
			String uuid = request.get("uuid");
			
			try {
				// We need to ensure that nobody does anything while we are updating
				lock.lock();

				// TODO: This needs work!
				Aircraft aircraft = getAircraftByRegistration(registration);
				Equipment equipment = getEquipmentByUuid(uuid);
				if (aircraft != null && equipment != null) {
					aircraft.removeEquipment(equipment);
				}
				writeJson("Aircraft", aircraftCache);
			} finally {
				lock.unlock();
			}
		} else {
			return Response.status(404).build();
		}
		
		return Response.ok().build();
	}
	
	@GET
	@Path("equipment")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEquipment(@CookieParam("wcfc.manuals.token") Cookie cookie) {
		User user = null;

		try {
			lock.lock();
			
			// Always return aircraft in type sorted order
			equipmentCache.sort(manual_compare);
		} finally {
			lock.unlock();
		}
		
        return Response.ok().entity(equipmentCache).build();
	}
	
	class ManualTypeComparator implements Comparator<Equipment>
	{
	    // Used for sorting in ascending order of
	    // roll number
	    public int compare(Equipment a, Equipment b)
	    {
	        return a.getType().ordinal() - b.getType().ordinal();
	    }
	}
	

	@GET
	@Path("reload")
	@Produces(MediaType.APPLICATION_JSON)
	public Response reload(@CookieParam("wcfc.manuals.token") Cookie cookie) throws JsonParseException, JsonMappingException, IOException {
		User user = authUtils.getUserFromCookie(cookie);
		
		if (user != null && user.getAdmin()) {
			loadDataStore();
			LOG.info("Data stores reloaded from JSON");
		} else {
			LOG.info("Non-admin user attempted reloading ... ignored.");
		}
			
		return Response.ok().build();
	}
	
	@GET
	@Path("tree")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTree(@CookieParam("wcfc.manuals.token") Cookie cookie) {
		// Create the head entry
		TreeEntry head = new TreeEntry("WCFC Flight Manuals", null);
		head.setOpen(true);
	    
		try {
			lock.lock();
			
		    for(AircraftType t : EnumSet.allOf(AircraftType.class)) {
		    	TreeEntry tEntry = new TreeEntry(t.getLabel(), null);
		    	head.addChild(tEntry);
		         for (Aircraft a : allAircraftOfType(t)) {
		        	 TreeEntry aEntry = new TreeEntry(a.getRegistration(), a.getUuid());
		        	 tEntry.addChild(aEntry);
		        	 List<String> eidList = a.getEquipment();
		        	 if (eidList != null) {
		        		 for (String eid: eidList) {
		        			 Equipment e = getEquipmentByUuid(eid);
		        			 if (e != null) {
		        				 aEntry.addChild(new TreeEntry(e.getName(), eid));
		        			 } else {
		        				 LOG.info("Could not find entry with EID : {}", eid);
		        			 }
		        		 }
		        	 }
		         }
		    }
		} finally {
			lock.unlock();
		}
	    
		return Response.ok().entity(head).build();
	}
	
	private List<Aircraft> allAircraftOfType(AircraftType t) {
		List<Aircraft> result = new ArrayList<Aircraft>();
		for (Aircraft a : aircraftCache) {
			if (a.getType() == t) {
				result.add(a);
			}
		}
		return result;
	}
	
	private class TreeEntry {
		private String label;
		private String link;
		private Boolean open = false;
		private ArrayList<TreeEntry> children = null;
		
		public TreeEntry(String label, String link) {
			super();
			this.label = label;
			this.link = link;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public String getLink() {
			return link;
		}

		public void setLink(String link) {
			this.link = link;
		}

		public Boolean getOpen() {
			return open;
		}

		public void setOpen(Boolean open) {
			this.open = open;
		}

		public ArrayList<TreeEntry> getChildren() {
			return children;
		}

		public void setChildren(ArrayList<TreeEntry> children) {
			this.children = children;
		}

		public void addChild(TreeEntry child) {
			if (children == null) {
				children = new ArrayList<TreeEntry>();
			}
			children.add(child);
		}
	}
	
	@GET
	@Path("aircraft/types")
	@Produces(MediaType.APPLICATION_JSON)
	public Response aircraftTypes(@CookieParam("wcfc.manuals.token") Cookie cookie) {
		User user = null;
		List<TypeEntry> response = new ArrayList<TypeEntry>();
		
		try {
			lock.lock();
			
		    for(AircraftType t : EnumSet.allOf(AircraftType.class)) {
		         response.add(new TypeEntry(typeCount(t), t));
		    }
		} finally {
			lock.unlock();
		}

		return Response.ok().entity(response).build();
	}
	
	private Integer typeCount(AircraftType t) {
		Integer count = 0;
		for (Aircraft aircraft : aircraftCache) {
			if (aircraft.getType() == t) {
				count++;
			}
		}
		return count;
	}
	
	class TypeEntry implements Comparable<AircraftType>{
		Integer count;
		String label;
		AircraftType type;
		
		public TypeEntry(Integer count, AircraftType type) {
			this.count = count;
			this.label = type.getLabel();
			this.type = type;
		}
		
		public Integer getCount() {
			return count;
		}
		public String getLabel() {
			return label;
		}
		public AircraftType getType() {
			return type;
		}

		@Override
		public int compareTo(AircraftType other) {
			return type.ordinal() - other.ordinal();
		}
	}
	
	@GET
	@Path("equipment/types")
	@Produces(MediaType.APPLICATION_JSON)
	public Response equipmentTypes(@CookieParam("wcfc.manuals.token") Cookie cookie) {
		User user = null;
		
        return Response.ok().entity(EquipmentType.getTypes()).build();
	}
	
	private void writeJson(String name, List entities) {
		try {
			String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(entities);

			try (FileOutputStream fos = new FileOutputStream(root + "/" + name + ".json");
					FileChannel channel = fos.getChannel();
					FileLock lock = channel.lock()) {
				fos.write(json.getBytes());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@POST
	@Path("equipment/register")
	@Produces(MediaType.APPLICATION_JSON)
	public Response registerEquipment(@CookieParam("wcfc.manuals.token") Cookie cookie,
			Map<String, String> request) {
		User user = authUtils.getUserFromCookie(cookie);
		if (user != null) {
			if (user.getAdmin()) {
				String name = request.get("name");
				String type = request.get("type");
				
				try {
					lock.lock();
				
					// Add to the in-memory cache
					addNewEquipment(type, name);
					
					// Persist the change
					writeJson("Equipment", equipmentCache);
				} finally {
					lock.unlock();
				}
				
				return Response.ok().build();
			} else {
				return Response.status(401).entity("Not authorized.").build();
			}
		}
		return Response.status(404).entity("Are you logged in??").build();
	}
	
	@DELETE
	@Path("/equipment/{uuid}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteEquipment(@CookieParam("wcfc.manuals.token") Cookie cookie,
	@PathParam("uuid") String uuid) {
		User user = authUtils.getUserFromCookie(cookie);
		if (user != null) {
			if (user.getAdmin()) {
				LOG.info("Deleting document {} from the system.", uuid);
				try {
					lock.lock();
					
					// Remove from the aircraft store first ....
				    Iterator<Aircraft> a_it = aircraftCache.iterator();
				    while (a_it.hasNext()) {
				    	Aircraft aircraft = a_it.next();
				    	List<String> equipmentList = aircraft.getEquipment();
				    	if (equipmentList != null) {
					    	Iterator<String> d_it = equipmentList.listIterator();
					    	while (d_it.hasNext()) {
					    		String document = d_it.next();
					    		if (document.equalsIgnoreCase(uuid)) {
					    			LOG.debug("Removing {} from {}", uuid, aircraft.getLabel());
					    			d_it.remove();
					    		}
					    	}
				    	}
				    }
				    
				    // then the equipment store last
				    List<Equipment> newCache = new ArrayList<Equipment>();

				    Iterator<Equipment> e_it = equipmentCache.listIterator();
				    while (e_it.hasNext()) {
				    	Equipment equipment = e_it.next();
				    	if ( ! equipment.getUuid().equalsIgnoreCase(uuid)) {
				    		newCache.add(equipment);
				    	} else {
			    			LOG.info("Removing {} from equipment list", equipment.getName());
				    	}
				    }
				    equipmentCache = newCache;
				    
				    // Physically remove the file
					File file = new File(root + "/" + uuid + ".pdf");
					file.delete();
				    
					// Persist the change
					writeJson("Equipment", equipmentCache);
					writeJson("Aircraft", aircraftCache);
				} finally {
					lock.unlock();
				}
			}
		}
		return Response.ok().build();
	}
	
	@POST
	@Path("aircraft/register")
	@Produces(MediaType.APPLICATION_JSON)
	public Response registerAircraft(@CookieParam("wcfc.manuals.token") Cookie cookie,
			Map<String, String> request) {
		User user = authUtils.getUserFromCookie(cookie);
		if (user != null) {
			if (user.getAdmin()) {
				String registration = request.get("registration");
				String type = request.get("type");
				
				try {
					lock.lock();
					
					// Add to the in-memory cache
					addNewAircraft(type, registration);
					
					// Persist the change
					writeJson("Aircraft", aircraftCache);
				} finally {
					lock.unlock();
				}
				
				return Response.ok().build();
			} else {
				return Response.status(401).entity("Not authorized.").build();
			}
		}
		return Response.status(404).entity("Are you logged in??").build();
	}
	
	private void addNewEquipment(String type, String name) {
		EquipmentType eType = EquipmentType.valueOf(type);
		if (eType != null) {
			Equipment equipment = new Equipment(name, eType);
			if (equipmentCache != null && equipment != null) {
				equipmentCache = new ArrayList<Equipment>(equipmentCache);
				equipmentCache.add(equipment);
			} else {
				LOG.info("WTF? Why is the equipment cache OR the newly created equipment null??");
			}
		}
	}

	private void addNewAircraft(String type, String registration) {
		AircraftType aType = AircraftType.valueOf(type);
		if (aType != null) {
			Aircraft aircraft = new Aircraft(registration, aType);
			if (aircraftCache != null && aircraft != null) {
				aircraftCache = new ArrayList<Aircraft>(aircraftCache);
				aircraftCache.add(aircraft);
			} else {
				LOG.info("WTF? Why is the aircraft cache OR the newly created aircraft null??");
			}
		}
	}
	

	@POST
	@Path("contact")
	@Produces(MediaType.APPLICATION_JSON)
	public Response contact(@CookieParam("wcfc.manuals.token") Cookie cookie,
			Map<String, String> request) {

		String name = request.getOrDefault("name", "NONE");
		String email = request.getOrDefault("email", "NONE");
		String message = request.getOrDefault("message", "NONE");
		
		if (name != null && email != null && message != null) {
			Slack.instance().sendMessage(Slack.Channel.NOTIFY, contactMessage(name, email, message));
		} else {
			LOG.info("Someone without all the input values tried to send a contact message!");
		}
		return Response.ok().build();
	}
	
	private MessageRequest contactMessage(String name, String email, String message) {
		ZoneId zoneId = ZoneId.of("US/Eastern");
		ZonedDateTime now = LocalDateTime.now().atZone(zoneId);
		
		Builder ab = Attachment.builder()
				.fallback("Manuals : " + message)
				.author(Author.of(name))
				.color(Color.good())
				.title(Title.builder()
				.text(email)
				.link(url("mailto:" + email))
				.build())
				.footer(Footer.builder().text("Generated By WCFC Manuals Server")
						.icon(url("https://platform.slack-edge.com/img/default_application_icon.png"))
						.timestamp(now.toEpochSecond()).build());
		ab.addFields(Field.builder().isShort(false).title("Message").value(message).build());
		
		MessageRequest msg = MessageRequest.builder().username("Manuals Contact")
				.channel("contact")
				.text("*Manuals contact sent at : " + dateFormatGmt.format(now) + "*") // + SlackMarkdown.EMOJI.decorate("new"))
				.addAttachments(ab.build())
				.build();

		return msg;
	}
	
	@GET
	@Path("freespace")
	@Produces(MediaType.APPLICATION_JSON)
	public Response freespace() {
		File fs = new File("tmp");
		Map<String, Long> myMap = new HashMap<String, Long>() {{
	        put("space", fs.getFreeSpace());
	    }};
	    
	    return Response.ok().entity(myMap).build();
	}
	
	@POST
	@Path("upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public Response upload(@CookieParam("wcfc.manuals.token") Cookie cookie,
			@FormDataParam("identifier") String identifier,
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetails)
			throws IOException, CsvException, ParseException {
		
		User user = authUtils.getUserFromCookie(cookie);
		
		String path = identifier + ".pdf";
	    File targetFile = new File(root + identifier + ".tmp");
	    OutputStream outStream = new FileOutputStream(targetFile);

	    byte[] buffer = new byte[8 * 1024];
	    int bytesRead;
	    while ((bytesRead = uploadedInputStream.read(buffer)) != -1) {
	        outStream.write(buffer, 0, bytesRead);
	    }
	    uploadedInputStream.close();
	    outStream.close();

	    // Check out the type 
	    String fileType = getFileTypeByTika(targetFile);
	    if (fileType.equals("application/pdf")) {
			String newname = root + "/" + path;
			File newfile = new File(newname);
			if (newfile.exists()) {
				FileUtils.deleteQuietly(newfile);
			}
			FileUtils.moveFile(targetFile, newfile);
			if (newfile.exists()) {
				Aircraft aircraft = null;
				Equipment equipment = null;
				LOG.info("Creating : {}", newname);
				
				// Update appropriate data object and corresponding JSON file
				if ((aircraft = updateAircraftStore(identifier)) != null) {
					LOG.info("Aircraft store updated.");

					// Let the world know a new file was uploaded for an aircraft
			        Slack.instance().sendMessage(Slack.Channel.MANUALS, uploadMessage(user, ManualType.AIRCRAFT, aircraft.getRegistration()));
				}
				if ((equipment = updateEquipmentStore(identifier)) != null) {
					LOG.info("Equipment store updated.");

					// Let the world know a new file was uploaded
			        Slack.instance().sendMessage(Slack.Channel.MANUALS, uploadMessage(user, ManualType.EQUIPMENT, equipment.getName()));
				}

				return Response.ok().build();
		    } else {
		    	LOG.error("Renaming of temp upload file failed : {}", path);
				return Response.status(500).build();
		    }
	    } else {
	    	LOG.error("File was not a PDF, rejected");
	    	targetFile.delete();
			return Response.status(400).build();
	    }
	}
	
	private MessageRequest uploadMessage(User user, ManualType type, String identifier) {
		ZoneId zoneId = ZoneId.of("US/Eastern");
		ZonedDateTime now = LocalDateTime.now().atZone(zoneId);
		
		String message = "none";
		switch (type) {
		case AIRCRAFT  : message = "A new POH for aircraft '" + identifier + "' has been uploaded by " + user.getName() + "."; break;
		case EQUIPMENT : message = "A new equipment manual for '" + identifier + "' has been uploaded by " + user.getName() + "."; break;
		}
		
		Builder ab = Attachment.builder()
				.fallback("New manual for " + identifier + " has been uploaded.")
				.author(Author.of(user.getName()))
				.color(Color.good())
				.title(Title.builder()
						.text("New Manual Uploaded")
					.build())
				.text(message)
				.footer(Footer.builder().text("Generated By WCFC Manuals Server")
						.icon(url("https://platform.slack-edge.com/img/default_application_icon.png"))
						.timestamp(now.toEpochSecond()).build());

		MessageRequest msg = MessageRequest.builder().username("WCFC Manuals Server")
				.channel("manuals")
				.text("*WCFC Manuals Server notification sent at : " + dateFormatGmt.format(now) + "*") // + SlackMarkdown.EMOJI.decorate("new"))
				.addAttachments(ab.build())
				.build();

		return msg;
	}

	private Aircraft updateAircraftStore(String uuid) {
		Aircraft aircraft = getAircraftByUuid(uuid);
		if (aircraft != null) {
			aircraft.setHasDocument(true);
			writeJson("Aircraft", aircraftCache);
			return aircraft;
		} else {
			return null;
		}
	}

	private Aircraft getAircraftByUuid(String uuid) {
		for (Aircraft aircraft : aircraftCache) {
			if (uuid.equals(aircraft.getUuid())) {
				return aircraft;
			}
		}
		return null;
	}
	
	private Aircraft getAircraftByRegistration(String registration) {
		for (Aircraft aircraft : aircraftCache) {
			if (registration.equals(aircraft.getRegistration())) {
				return aircraft;
			}
		}
		return null;
	}
	
	private Equipment updateEquipmentStore(String uuid) {
		Equipment equipment = getEquipmentByUuid(uuid);
		if (equipment != null) {
			equipment.setHasDocument(true);
			writeJson("Equipment", equipmentCache);
			return equipment;
		} else {
			return null;
		}
	}
	
	private Equipment getEquipmentByUuid(String uuid) {
		for (Equipment equipment : equipmentCache) {
			if (uuid.equals(equipment.getUuid())) {
				return equipment;
			}
		}
		return null;
	}
	
	public static String getFileTypeByTika(File file) {        
	    final Tika tika = new Tika();       
	    String fileTypeDefault ="";
	    try {
	        fileTypeDefault = tika.detect(file);
	    } catch (IOException e) {
	        LOG.error("Error while detecting file type from File");
	        LOG.error("*Error message: {}", e.getMessage());
	        e.printStackTrace();
	    }
	    return fileTypeDefault;
	}

	@GET
	@Path("archive/details")
	@Produces(MediaType.APPLICATION_JSON)
	public Response archiveDetails(@CookieParam("wcfc.manuals.token") Cookie cookie) throws IOException {
		List<String> files = new ArrayList<>();
		files.addAll(listFiles("dynamic"));
		if (!files.isEmpty()) {
			Map<String, String> result = new HashMap<String, String>();
			// Provide the name ...
			String filename = files.get(0);
			result.put("name", filename);
			java.nio.file.Path path = Paths.get("dynamic/" + filename);

			// ... the creation time ...
			BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
			long cTime = attr.creationTime().toMillis();
			ZonedDateTime t = Instant.ofEpochMilli(cTime).atZone(ZoneId.of("UTC"));
			result.put("created", dateFormatArchive.format(t));
			// ... and the size.
            long bytes = Files.size(path);
            result.put("size", humanReadableByteCountBin(bytes));
            
			return Response.ok().entity(result).build();
		} else {
			return Response.status(404).build();
		}
	}

	public static String humanReadableByteCountBin(long bytes) {
	    long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
	    if (absB < 1024) {
	        return bytes + " B";
	    }
	    long value = absB;
	    CharacterIterator ci = new StringCharacterIterator("KMGTPE");
	    for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
	        value >>= 10;
	        ci.next();
	    }
	    value *= Long.signum(bytes);
	    return String.format("%.1f %cb", value / 1024.0, ci.current());
	}
	
	@GET
	@Path("archive")
	public Response archive(@CookieParam("wcfc.manuals.token") Cookie cookie) throws IOException {
		String filename = "none";
		String fullpath = "none";
		ZonedDateTime now = LocalDateTime.now().atZone(ZoneId.of("US/Eastern"));
		User user = authUtils.getUserFromCookie(cookie);
		if (user != null) {
			LOG.info("Starting new archive generation.");
			
			// Remove all the old files in the 'dynamic' directory (should only be one)
			Set<String> files = listFiles("dynamic");
			for (String name : files) {
				File file = new File("dynamic/" + name);
				file.delete();
			}
			
			ZipOutputStream zipOut = null;
			try {
				filename = "wcfc-manuals-" + dateFormatArchive.format(now) + ".zip";
				fullpath = "dynamic/" + filename;
				FileOutputStream fout = new FileOutputStream(new File(fullpath));
				zipOut = new ZipOutputStream(fout);
				
				byte[] guidePage = generateGuidePage(filename).toString().getBytes();
				ZipEntry zipEntry = new ZipEntry("index.html");
				zipOut.putNextEntry(zipEntry);
				zipOut.write(guidePage, 0, guidePage.length);
				zipOut.flush();
				
				// Stash images
				addImage(zipOut, "checkmark.png");
				addImage(zipOut, "unchecked.png");
				addImage(zipOut, "WCFC-logo.jpg");
				
				// Now, write all the data files
				addDataFiles(zipOut);
			} catch (IOException ex) {
				LOG.info("IOException during archive generation : {}", ex.getMessage());
			} finally {
				// 	Wrap up the Zip file
				zipOut.close();
			}
			LOG.info("New archive generation completed.");
			
			// Let the world know a new archive was generated
            long bytes = Files.size(Paths.get(fullpath));
	        Slack.instance().sendMessage(Slack.Channel.MANUALS, archiveMessage(user, filename, humanReadableByteCountBin(bytes)));

			return Response.ok().build();
		} else {
			return Response.status(401).entity("Are you logged in??").build();
		}
	}
	
	private MessageRequest archiveMessage(User user, String filename, String size) {
		ZoneId zoneId = ZoneId.of("US/Eastern");
		ZonedDateTime now = LocalDateTime.now().atZone(zoneId);
		
		Builder ab = Attachment.builder()
				.fallback("New manuals archive created.")
				.author(Author.of(user.getName()))
				.color(Color.good())
				.title(Title.builder()
					.text("Archive Updated")
					.build())
				.text("A new WCFC Manuals ZIP archive has been generated by " + user.getName() + ". The archive name is '" + filename + "' and it is " + size + " in size.")
				.footer(Footer.builder().text("Generated By WCFC Manuals Server")
						.icon(url("https://platform.slack-edge.com/img/default_application_icon.png"))
						.timestamp(now.toEpochSecond()).build());

		MessageRequest msg = MessageRequest.builder().username("WCFC Manuals Server")
				.channel("manuals")
				.text("*WCFC Manuals Server notification sent at : " + dateFormatGmt.format(now) + "*") // + SlackMarkdown.EMOJI.decorate("new"))
				.addAttachments(ab.build())
				.build();

		return msg;
	}

	public Set<String> listFiles(String dir) {
	    return Stream.of(new File(dir).listFiles())
	      .filter(file -> !file.isDirectory())
	      .map(File::getName)
	      .collect(Collectors.toSet());
	}
	
	private void addImage(ZipOutputStream zipOut, String filename) {
		try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("assets/" + filename)) {

			zipOut.putNextEntry(new ZipEntry("img/" + filename));

            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) > 0) {
            	zipOut.write(buffer, 0, len);
            }

            zipOut.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	private void addDataFiles(ZipOutputStream zipOut) {
		String[] pathnames;

		File f = new File(root);

		// This filter will only include files ending with .pdf
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File f, String name) {
				return name.endsWith(".pdf");
			}
		};
		
		pathnames = f.list(filter);
		
		for (String fileName : pathnames) {
			String name = zipOutputName(fileName);
			try (FileInputStream fis = new FileInputStream(new File(root + "/" + fileName))) {

				zipOut.putNextEntry(new ZipEntry("data/" + name));

                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                	zipOut.write(buffer, 0, len);
                }

                zipOut.closeEntry();
            } catch (IOException e) {
                LOG.info("IOException in addDataFiles() : {}", e.getCause().getMessage());
                break;
            }
		}
	}
	
	private String zipOutputName(String uuid) {
		String zipName = null;
		String zipDirectory = null;
		
		uuid = FilenameUtils.removeExtension(uuid);
		
		for (Equipment e : equipmentCache) {
			if (e.getUuid().equals(uuid)) {
				zipDirectory = e.getType().getLabel();
				zipName = e.getName();
				break;
			}
		}
		if (zipName == null) {
			for (Aircraft a : aircraftCache) {
				if (a.getUuid().equals(uuid)) {
					zipDirectory = a.getType().getLabel();
					zipName = a.getRegistration() + " POH";
					break;
				}
			}
		}
		
		if (zipDirectory != null && zipName != null) {
			return zipDirectory.replace(' ', '_').replace('/', '_') + "/" +
					zipName.replace(' ', '_').replace('/', '-').replace('\'', '^') + ".pdf";
		} else {
			System.out.println("Cound not find a name for ==> " + uuid);
			return "unknown.pdf";
		}
	}
	
	private StringBuffer generateGuidePage(String filename) {
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("<div class='container'><div class='header'><img src='img/WCFC-logo.jpg'>\n");
		sb.append("<span style='vertical-align: top'>WCFC Flight Manuals</span></div>\n");
		sb.append("<div class='header'><span style='font-size: 0.6em;'>Source : " + filename + "</span></div><hr>");
		
		// Output the aircraft type headers
		sb.append("<table id='equipment'><tr><th class='blank'>&nbsp;</th>\n");
	    for(AircraftType t : EnumSet.allOf(AircraftType.class)) {
	         sb.append("<th class='type' colspan=" + typeCount(t) + "><span class='label'>" + t.getLabel() + "</span></th>\n");
	    }
	    sb.append("</tr>");
	    
	    // Output the registration/aircraft headers
	    sb.append("<tr><th>Equipment</th>\n");
	    for (Aircraft acft : aircraftCache) {
	    	sb.append("<th class='reg'><a href='data/" + zipOutputName(acft.getUuid()) + "' target=_blank>" + acft.getRegistration() + "</a></th>\n");
	    }
	    sb.append("</tr>");
	    
	    // Output the equipment types
	    for (Map type : EquipmentType.getTypes()) {
	    	EquipmentType t = (EquipmentType) type.get("mtype");
	    	sb.append("<tr class='label'><td>" + t.getLabel() + "</td>");
	    	// Output all the equipment in the current equipment type
	    	for (Equipment e : equipmentCache) {
	    		if (e.getType().equals(t)) {
	    			sb.append("<tr class='detail'><td class='equipment'><a href='data/" + zipOutputName(e.getUuid()) + "' target=_blank>" + e.getName() + "</a></td>");
	    			// Output all the "checkmarks"
	    			for (Aircraft acft : aircraftCache) {
			        	 if (equipmentInstalled(acft, e))
			        		 sb.append("<td><img src='img/checkmark.png' alt='X'></td>");
			        	 else
			        		 sb.append("<td><img src='img/unchecked.png' alt='-'></td>");
	    			}
	    			sb.append("</tr>");
	    		}
	    	}
	    	sb.append("</tr>");
	    }
	    
	    sb.append("</table>");
	    sb.append("</div>");
	    
		sb.append("<style>");
		sb.append("body {\n" + 
				"        margin: 0;\n" + 
				"        font-family: 'Poppins', sans-serif;\n" + 
				"        font-size: 14px;\n" + 
				"        line-height: 1.5;\n" + 
				"        color: #333;\n" + 
				"}\n");
		sb.append(".container { margin:75; }\n");
		sb.append(".header {font-size: 28px; font-weight: 300; }\n");
		sb.append("#equipment .blank { background-color:rgba(0, 0, 0, 0); width: 30%; }\n");
		sb.append("#equipment .detail { text-align: center; }\n");
		sb.append("#equipment .detail a { text-decoration: none; color: black; }\n");
		sb.append("#equipment .type { text-align: -internal-center; }\n");
		sb.append("#equipment th {\n" + 
				"  padding-top: 5px;\n" + 
				"  padding-bottom: 5px;\n" + 
				"  background-color: #7887a2;\n" + 
				"  color: white;\n" + 
				"}\n");
		sb.append("#equipment .reg { width: 30px;\n" + 
				"	-webkit-writing-mode: vertical-lr;\n" + 
				"	writing-mode: vertical-lr;\n" + 
				"	-webkit-text-orientation: sideways;\n" + 
				"	text-orientation: sideways;\n" + 
				"	text-align: end;\n" + 
				"	padding: 12px; }\n");
		sb.append("#equipment .reg a { text-decoration: none; color: white }\n");
		sb.append("#equipment .label { font-weight: bold; font-size: 1.2em }\n");
		sb.append("#equipment .equipment { text-align: right; float: right; }\n" + 
				"#equipment .link { cursor: pointer; font-size: 1.0em }");
		
		return sb;
	}

	private boolean equipmentInstalled(Aircraft acft, Equipment e) {
		List<String> eidList = acft.getEquipment();
		if (eidList == null) return false;
	   	 for (String eid : eidList) {
	   		 if (eid.equals(e.getUuid()))
	   			 return true;
	   	 }
	   	 return false;
	}
	
	@GET
	@Path("mock")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response mock() throws URISyntaxException {

		User user = mockUser();
		
		// User authenticated and identified. Save the info.
		NewCookie cookie = authUtils.generateCookie(user);
        return Response.seeOther(new URI("/")).header("Set-Cookie", AuthUtils.sameSite(cookie)).build();
	}
	
	private User mockUser() {
		return new User("Dwight Frye", "dwight@openweave.org");
	}
	
    private static URL url(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getLocalizedMessage(), e);
        }
    }

	public static Map<String, String> getBuildMetadata() {
		Map<String, String> version = new HashMap<String, String>();
		
		if (config.getMode().equals("DEV")) {
            if (versionOverride != null) {
            	version.put("version", versionOverride);
            } else {
            	version.put("version", "DEV");
            }
            version.put("build", "DEV");
            return version;
		} else {
		    Enumeration<URL> resEnum;
		    try {
		        resEnum = ManualsResource.class.getClassLoader().getResources(JarFile.MANIFEST_NAME);
		        while (resEnum.hasMoreElements()) {
		            URL url = resEnum.nextElement();
		            InputStream is = url.openStream();
		            if (is != null) {
		                Manifest manifest = new Manifest(is);
		                Attributes mainAttribs = manifest.getMainAttributes();
		                version.put("version", mainAttribs.getValue("Git-Build-Version"));
		                version.put("build", mainAttribs.getValue("Git-Commit-Id"));
		                if (version != null) {
		                    return version;
		                }
		            }
		        }
		    } catch (IOException e1) {
		        // Silently ignore wrong manifests on classpath?
		    	LOG.info("IOException during manifest retrieval : {}", e1);
		    }
		    return null; 
		}
	}
}
