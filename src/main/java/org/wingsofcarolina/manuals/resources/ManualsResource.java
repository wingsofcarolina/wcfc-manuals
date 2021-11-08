package org.wingsofcarolina.manuals.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

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
import org.wingsofcarolina.manuals.authentication.AuthUtils;
import org.wingsofcarolina.manuals.common.APIException;

/**
 * @author dwight
 *
 */
@Path("/")	// Note that this is actually accessed as /api due to the setUrPattern() call in OralService
public class ManualsResource {
	private static final Logger LOG = LoggerFactory.getLogger(ManualsResource.class);
	
	private static ManualsConfiguration config;
	private static String versionOverride = null;
	private DateTimeFormatter dateFormatGmt;

    // Slack credentials
    private static final String CLIENT_ID = "REDACTED";
    private static final String CLIENT_SECRET = "REDACTED";
	private SlackAuthService slackAuth;

	private AuthUtils authUtils;
	private boolean authEnabled = false;
	
	private static User mockUser = null; // When we are developing and don't want to authenticate with Slack
	
	private static String root;
	
	private ObjectMapper mapper;
	
	private static final String AIRCRAFT_JSON = "Aircraft.json";
	private static final String EQUIPMENT_JSON = "Equipment.json";
	
	private List<Equipment> equipmentCache = new ArrayList<Equipment>();
	private List<Aircraft> aircraftCache = new ArrayList<Aircraft>();
	private AircraftComparator aircraft_compare = new AircraftComparator();
	private ManualTypeComparator manual_compare = new ManualTypeComparator();

	@SuppressWarnings("static-access")
	public ManualsResource(ManualsConfiguration config) throws IOException, ListFolderErrorException, DbxException {
		this.config = config;
		
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
		// Load the aircraft store first ....
	    aircraftCache = Arrays.asList(mapper.readValue(Paths.get(root + "/" + AIRCRAFT_JSON).toFile(), Aircraft[].class));
	    
	    // then the equipment store last
	    equipmentCache = Arrays.asList(mapper.readValue(Paths.get(root + "/" + EQUIPMENT_JSON).toFile(), Equipment[].class));
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
        Map<String, Object> reply = new HashMap<String, Object>();

        if (authEnabled) {
        	user = authUtils.getUserFromCookie(cookie);
		} else {
			if ( mockUser == null) {
				user = new User("Anonymous", "nobody@wingsofcarolina.org");
				anonymous = true;
			} else {
				user = new User("Dwight Frye", "dwight@openweave.org");
				anonymous = false;
			}
		}

        if (user != null) {
	        reply.put("name", user.getName());
	        reply.put("email", user.getEmail());
	        reply.put("admin", user.getAdmin());
	        reply.put("anonymous", false);

			NewCookie newCookie = authUtils.generateCookie(user);
	        return Response.ok().entity(reply).cookie(newCookie).build();
        } else {
        	return Response.status(404).cookie(AuthUtils.instance().removeCookie()).build();
        }
	}
	
	@GET
	@Path("fetch/{uuid}")
	@Produces("application/pdf")
	public Response fetchFile(@PathParam("uuid") String uuid) throws IOException, DbxException {
		
		File file = new File(root + "/" + uuid + ".pdf");
		if (file.exists()) {
	        InputStream inputStream = new FileInputStream(file);
	        return Response.ok().type("application/pdf").entity(inputStream).build();
		} else {
			return Response.status(404).build();
		}
	}
	
	@GET
	@Path("aircraft")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAircraft(@CookieParam("wcfc.manuals.token") Cookie cookie) {
		User user = AuthUtils.instance().getUserFromCookie(cookie);
		
		if (user != null) {
			// Always return aircraft in type sorted order
			aircraftCache.sort(aircraft_compare);
			
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
	
	private void makeAircraft() {
		aircraftCache.add(new Aircraft("N4640B", AircraftType.C152));
		aircraftCache.add(new Aircraft("N5389M", AircraftType.C152));
		aircraftCache.add(new Aircraft("N69012", AircraftType.C152));
		aircraftCache.add(new Aircraft("N89333", AircraftType.C152));
		aircraftCache.add(new Aircraft("N89433", AircraftType.C152));

		aircraftCache.add(new Aircraft("N972WW", AircraftType.C172));
		aircraftCache.add(new Aircraft("N53587", AircraftType.C172));
		aircraftCache.add(new Aircraft("N72675", AircraftType.C172));

		aircraftCache.add(new Aircraft("N2114F", AircraftType.PA28));
		aircraftCache.add(new Aircraft("N64TZ", AircraftType.PA28));
		aircraftCache.add(new Aircraft("N8080A", AircraftType.PA28));
		aircraftCache.add(new Aircraft("N8116J", AircraftType.PA28));

		aircraftCache.add(new Aircraft("N1068X", AircraftType.M20J));
		aircraftCache.add(new Aircraft("N5760R", AircraftType.M20J));
		
		writeJson("Aircraft", aircraftCache);
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
			
			Aircraft aircraft = getAircraftByRegistration(registration);
			Equipment equipment = getEquipmentByUuid(uuid);
			if (aircraft != null && equipment != null) {
				aircraft.addEquipment(equipment);
			}
			writeJson("Aircraft", aircraftCache);
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
			
			Aircraft aircraft = getAircraftByRegistration(registration);
			Equipment equipment = getEquipmentByUuid(uuid);
			if (aircraft != null && equipment != null) {
				aircraft.removeEquipment(equipment);
			}
			writeJson("Aircraft", aircraftCache);
		} else {
			return Response.status(404).build();
		}
		
		return Response.ok().build();
	}
	
	@GET
	@Path("equipment")
	@Produces(MediaType.APPLICATION_JSON)
	public Response equipment(@CookieParam("wcfc.manuals.token") Cookie cookie) {
		User user = null;

		// Always return aircraft in type sorted order
		equipmentCache.sort(manual_compare);
		
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
	
	private void makeManuals() {
		
		equipmentCache.add(new Equipment("AEM AA80 InterVox",EquipmentType.INTERCOM));
		equipmentCache.add(new Equipment("PS Engineering PM1000",EquipmentType.INTERCOM));
		equipmentCache.add(new Equipment("PS Engineering PM1000II",EquipmentType.INTERCOM));
		equipmentCache.add(new Equipment("PS Engineering PM3000",EquipmentType.INTERCOM));
		equipmentCache.add(new Equipment("Sigtronics SPA-400",EquipmentType.INTERCOM));

		equipmentCache.add(new Equipment("Bendix/King KMA 24",EquipmentType.AUDIOPNL));
		equipmentCache.add(new Equipment("Bendix/King KMA 28",EquipmentType.AUDIOPNL));
		equipmentCache.add(new Equipment("Garmin GMA 345",EquipmentType.AUDIOPNL));
		equipmentCache.add(new Equipment("PS Engineering PMA6000B",EquipmentType.AUDIOPNL));

		equipmentCache.add(new Equipment("Bendix/King KX 155",EquipmentType.NAVCOM));
		equipmentCache.add(new Equipment("Bendix/King KX 155A",EquipmentType.NAVCOM));
		equipmentCache.add(new Equipment("Bendix/King KX 165",EquipmentType.NAVCOM));
		equipmentCache.add(new Equipment("Garmin GNC 255",EquipmentType.NAVCOM));
		equipmentCache.add(new Equipment("Narco MK 12D TSO",EquipmentType.NAVCOM));

		equipmentCache.add(new Equipment("Appareo Stratus",EquipmentType.TRANSPONDER));
		equipmentCache.add(new Equipment("Bendix/King KT76A",EquipmentType.TRANSPONDER));
		equipmentCache.add(new Equipment("Garmin GTX 327",EquipmentType.TRANSPONDER));
		equipmentCache.add(new Equipment("Garmin GTX 345",EquipmentType.TRANSPONDER));

		equipmentCache.add(new Equipment("GTN 650 / Pilot's Guide",EquipmentType.GPS));
		equipmentCache.add(new Equipment("GTN 650 / Cockpit Reference Guide",EquipmentType.GPS));
		equipmentCache.add(new Equipment("GTN 750 / Pilot's Guide",EquipmentType.GPS));
		equipmentCache.add(new Equipment("GTN 750 / Cockpit Reference Guide",EquipmentType.GPS));

		equipmentCache.add(new Equipment("Bendix/King KAP 140",EquipmentType.AUTOPILOT));
		equipmentCache.add(new Equipment("Bendix/King KAP 150",EquipmentType.AUTOPILOT));
		equipmentCache.add(new Equipment("Garmin GFC 500",EquipmentType.AUTOPILOT));
		equipmentCache.add(new Equipment("GFC 500 / G5 w/ GFC 500 Pilot Guide",EquipmentType.AUTOPILOT));
		equipmentCache.add(new Equipment("GFC 500 / AFMS PA-28",EquipmentType.AUTOPILOT));
		equipmentCache.add(new Equipment("S-TEC System 50",EquipmentType.AUTOPILOT));

		equipmentCache.add(new Equipment("JPI Fuel Scan 450",EquipmentType.TOTALIZER));
		equipmentCache.add(new Equipment("Shadin MiniFlo-L",EquipmentType.TOTALIZER));

		equipmentCache.add(new Equipment("EI CGR-30P",EquipmentType.ENGMONITOR));
		equipmentCache.add(new Equipment("EI SA-8A",EquipmentType.ENGMONITOR));
		equipmentCache.add(new Equipment("EI SA-8A Operating Manual",EquipmentType.ENGMONITOR));
		equipmentCache.add(new Equipment("QEI SA-8A uick Reference",EquipmentType.ENGMONITOR));
		equipmentCache.add(new Equipment("Insight G2",EquipmentType.ENGMONITOR));

		equipmentCache.add(new Equipment("Bendix/King KN 64",EquipmentType.DME));

		equipmentCache.add(new Equipment("Bendix/King KMD 550 Multi-Function Display",EquipmentType.OTHER));
		equipmentCache.add(new Equipment("BFG WX-900 Storm Scope",EquipmentType.OTHER));
		equipmentCache.add(new Equipment("BFG WX-950 Storm Scope",EquipmentType.OTHER));
		equipmentCache.add(new Equipment("Garmin G5",EquipmentType.OTHER));
		equipmentCache.add(new Equipment("Precise Flight Pulselite",EquipmentType.OTHER));
		
		writeJson("Equipment", equipmentCache);
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
		public Boolean getOpen() {
			return open;
		}
		public void setOpen(Boolean open) {
			this.open = open;
		}
		public String getLabel() {
			return label;
		}
		public String getLink() {
			return link;
		}
		public ArrayList<TreeEntry> getChildren() {
			return children;
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
		
	    for(AircraftType t : EnumSet.allOf(AircraftType.class)) {
	         response.add(new TypeEntry(typeCount(t), t));
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
	public Response manualTypes(@CookieParam("wcfc.manuals.token") Cookie cookie) {
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
	@Path("contact")
	@Produces(MediaType.APPLICATION_JSON)
	public Response contact(@CookieParam("wcfc.manuals.token") Cookie cookie,
			Map<String, String> request) {
		User user = authUtils.getUserFromCookie(cookie);
		if (user != null) {
			Slack.instance().sendMessage(Slack.Channel.NOTIFY, contactMessage(user, request));
		} else {
			LOG.info("An unknown user tried to send a contact message!");
		}
		return Response.ok().build();
	}
	
	private MessageRequest contactMessage(User user, Map<String, String> request) {
		String name = user.getName();
		String email = user.getEmail();
		String message = request.getOrDefault("message", "NONE");

		ZoneId zoneId = ZoneId.of("US/Eastern");
		LOG.info("Zone : {}", zoneId);
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
		LOG.info("Code : {}", code);
		
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
			LOG.info("Authenticated user : {}", user);
			//Slack.instance().sendString(Slack.Channel.NOTIFY, "Authenticated user : " + user);
			
			// User authenticated and identified. Save the info.
			NewCookie cookie = authUtils.generateCookie(user);
			
			return Response.seeOther(new URI("/equipment")).cookie(cookie).build();
		} else {
			return Response.seeOther(new URI("/")).cookie(authUtils.removeCookie()).build();
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
	@Path("mock")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response mock() throws URISyntaxException {

		User user = mockUser();
		
		// User authenticated and identified. Save the info.
		NewCookie cookie = authUtils.generateCookie(user);
		
        return Response.seeOther(new URI("/")).cookie(cookie).build();
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
