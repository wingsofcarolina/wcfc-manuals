package org.wingsofcarolina.manuals.resources;

import java.io.BufferedOutputStream;
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
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.palantir.roboslack.api.MessageRequest;
import com.palantir.roboslack.api.attachments.Attachment;
import com.palantir.roboslack.api.attachments.Attachment.Builder;
import com.palantir.roboslack.api.attachments.components.Author;
import com.palantir.roboslack.api.attachments.components.Color;
import com.palantir.roboslack.api.attachments.components.Field;
import com.palantir.roboslack.api.attachments.components.Footer;
import com.palantir.roboslack.api.attachments.components.Title;
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
import org.wingsofcarolina.manuals.common.APIException;

/**
 * @author dwight
 *
 */
@Path("/")	// Note that this is actually accessed as /api due to the setUrPattern() call in parent service
public class ManualsResource {
	private static final Logger LOG = LoggerFactory.getLogger(ManualsResource.class);
	
	private static SimpleLogger authLog;
	private static SimpleLogger accessLog;

	private static ManualsConfiguration config;
	private static String versionOverride = null;
	private DateTimeFormatter dateFormatGmt;

    // Slack credentials
    private static final String CLIENT_ID = "REDACTED";
    private static final String CLIENT_SECRET = "REDACTED";
	private SlackAuthService slackAuth;

	private AuthUtils authUtils;
	private boolean authEnabled = false;
	
	private Integer authCount = 0;
	private Integer accessCount = 0;
	
	private static User mockUser = null; // When we are developing and don't want to authenticate with Slack
	
	private static String root;
	
	private ObjectMapper mapper;
	
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
		authLog = new SimpleLogger("authentication", config);
		accessLog = new SimpleLogger("access", config);
		
		// See if we have turned auth on
		authEnabled = config.getAuth();
		
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
		
		// Get the startup date/time format in GMT
		dateFormatGmt = DateTimeFormatter.ofPattern("yyyy/MM/dd - HH:mm:ss z");
		
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
	@Path("status")
	@Produces(MediaType.APPLICATION_JSON)
	public Response stats(@CookieParam("wcfc.manuals.token") Cookie cookie) {
		User user = AuthUtils.instance().getUserFromCookie(cookie);

		if (user.getEmail().equals("dwight@openweave.org")) {
			Map<String, Integer> reply = new HashMap<String, Integer>();
			
			reply.put("authCount", authCount);
			reply.put("accessCount", accessCount);
		
		return Response.ok().entity(reply).build();
		} else {
			return Response.status(401).build();
		}
	}
	
	@GET
	@Path("user")
	@Produces(MediaType.APPLICATION_JSON)
	public Response user(@CookieParam("wcfc.manuals.token") Cookie cookie) {
		User user = null;
		Boolean anonymous = false;
        Map<String, Object> reply = new HashMap<String, Object>();

        if (authEnabled) {
        	user = authUtils.getUserFromCookie(cookie);
		} else {
			if ( mockUser == null) {
				user = new User("Anonymous", "nobody@wingsofcarolina.org");
				anonymous = true;
			} else {
				user = new User("Dwight Frye", "dwight@openweave.org");
			}
		}

        if (user != null) {
	        reply.put("name", user.getName());
	        reply.put("email", user.getEmail());
	        reply.put("admin", user.getAdmin());
	        reply.put("anonymous", anonymous);

			NewCookie newCookie = authUtils.generateCookie(user);
			// The following header hack is due to (a) Chrome demanding SameSite be set
			// and (b) NewCookie having no way to freaking do that. WTF people?
	        return Response.ok().entity(reply).header("Set-Cookie", newCookie.toString() + ";SameSite=none").build();
        } else {
        	NewCookie newCookie = AuthUtils.instance().removeCookie();
    		// The following header hack is due to (a) Chrome demanding SameSite be set
    		// and (b) NewCookie having no way to freaking do that. WTF people?
        	return Response.status(404).header("Set-Cookie", newCookie.toString() + ";SameSite=none").build();
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
	@Path("auth")
	@Produces(MediaType.TEXT_HTML)
	@SuppressWarnings("unchecked")
	public Response auth(@QueryParam("code") String code) throws URISyntaxException, APIException {
		if (code != null) {
			Map<String, Object> details = slackAuth.authenticate(code);
			Map<String, Object> user_details = (Map<String, Object>)details.get("authed_user");
			
			String user_id = (String) user_details.get("id");
			String access_token = (String) user_details.get("access_token");
			String team_id = (String) ((Map<String, Object>)details.get("team")).get("id");
	
			Map<String, Object> identity = slackAuth.identity(access_token);
			Map<String, String>userMap = (Map<String, String>) identity.get("user");
			String name = userMap.get("name");
			String email = userMap.get("email");
			
			User user = new User(name, email, user_id, team_id, access_token);
			authLog.logUser(user);
			authCount++;
			
			// User authenticated and identified. Save the info.
			NewCookie cookie = authUtils.generateCookie(user);
			// The following header hack is due to (a) Chrome demanding SameSite be set
			// and (b) NewCookie having no way to freaking do that. WTF people?
			return Response.seeOther(new URI("/equipment")).header("Set-Cookie", cookie.toString() + ";SameSite=none").build();
		} else {
			NewCookie cookie = authUtils.removeCookie();
			// The following header hack is due to (a) Chrome demanding SameSite be set
			// and (b) NewCookie having no way to freaking do that. WTF people?
			return Response.seeOther(new URI("/")).header("Set-Cookie", cookie.toString() + ";SameSite=none").build();
		}
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
	public Response upload(@FormDataParam("identifier") String identifier,
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetails)
			throws IOException, CsvException, ParseException {
		
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
				LOG.info("Creating : {}", newname);
				
				// Update appropriate data object and corresponding JSON file
				if (updateAircraftStore(identifier)) {
					LOG.info("Aircraft store updated.");
				} if (updateEquipmentStore(identifier)) {
					LOG.info("Equipment store updated.");
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
	
	private boolean updateAircraftStore(String uuid) {
		Aircraft aircraft = getAircraftByUuid(uuid);
		if (aircraft != null) {
			aircraft.setHasDocument(true);
			writeJson("Aircraft", aircraftCache);
			return true;
		} else {
			return false;
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
	
	private boolean updateEquipmentStore(String uuid) {
		Equipment equipment = getEquipmentByUuid(uuid);
		if (equipment != null) {
			equipment.setHasDocument(true);
			writeJson("Equipment", equipmentCache);
			return true;
		} else {
			return false;
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
	@Path("archive")
	@Produces(MediaType.TEXT_PLAIN)
	public Response archive(@CookieParam("wcfc.manuals.token") Cookie cookie) {
		User user = authUtils.getUserFromCookie(cookie);
		if (user != null) {
			
			String now = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());

			byte[] guidePage = generateGuidePage().toString().getBytes();
			
			StreamingOutput streamingOutput = outputStream -> {
				ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(outputStream));
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

				// Wrap up the Zip file
				zipOut.close();
				outputStream.flush();
				outputStream.close();
			};
			
			return Response.ok(streamingOutput).type(MediaType.TEXT_PLAIN)
					.header("Content-Disposition", "attachment; filename=\"wcfc-manuals-" + now + ".zip\"").build();
		} else {
			return Response.status(401).entity("Are you logged in??").build();
		}
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

		// This filter will only include files ending with .py
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File f, String name) {
				return name.endsWith(".pdf");
			}
		};
		
		pathnames = f.list(filter);
		
		for (String fileName : pathnames) {
			String name = zipOutputName(fileName);
			System.out.println("===> " + name);
			try (FileInputStream fis = new FileInputStream(new File(root + "/" + fileName))) {

				zipOut.putNextEntry(new ZipEntry("data/" + name));

                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                	zipOut.write(buffer, 0, len);
                }

                zipOut.closeEntry();
            } catch (IOException e) {
                e.printStackTrace();
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
	
	private StringBuffer generateGuidePage() {
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("<div class='container'><div class='header'><img src='img/WCFC-logo.jpg'><span style='vertical-align: top'>WCFC Flight Manuals</span></div><hr>");
		
		// Output the aircraft type headers
		sb.append("<table id='equipment'><tr><th class='blank'>&nbsp;</th>");
	    for(AircraftType t : EnumSet.allOf(AircraftType.class)) {
	         sb.append("<th class='type' colspan=" + typeCount(t) + "><span class='label'>" + t.getLabel() + "</span></th>");
	    }
	    sb.append("</tr>");
	    
	    // Output the registration/aircraft headers
	    sb.append("<tr><th>Equipment</th>");
	    for (Aircraft acft : aircraftCache) {
	    	sb.append("<th class='reg'><a href='data/" + zipOutputName(acft.getUuid()) + "' target=_blank>" + acft.getRegistration() + "</a></th>");
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
		
		// The following header hack is due to (a) Chrome demanding SameSite be set
		// and (b) NewCookie having no way to freaking do that. WTF people?
        return Response.seeOther(new URI("/")).header("Set-Cookie", cookie.toString() + ";SameSite=none").build();
	}
	
	private User mockUser() {
		return new User("Dwight Frye", "dwight@openweave.org", "REDACTED", "REDACTED", "REDACTED");
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
