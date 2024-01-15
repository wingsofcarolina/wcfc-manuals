package org.wingsofcarolina.manuals.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.model.Aircraft;
import org.wingsofcarolina.manuals.model.Equipment;
import org.wingsofcarolina.manuals.model.User;
import org.wingsofcarolina.manuals.resources.ManualsResource.ManualType;
import org.wingsofcarolina.manuals.slack.Slack;
import org.wingsofcarolina.manuals.domain.Admin;
import org.wingsofcarolina.manuals.domain.Person;
import org.wingsofcarolina.manuals.domain.Member;
import org.wingsofcarolina.manuals.domain.VerificationCode;
import org.wingsofcarolina.manuals.email.EmailLogin;
import org.wingsofcarolina.manuals.members.MemberListXLS;
import org.wingsofcarolina.manuals.ManualsConfiguration;
import org.wingsofcarolina.manuals.authentication.AuthUtils;
import org.wingsofcarolina.manuals.common.APIException;

/**
 * @author dwight
 *
 */
@Path("/member")	// Note that this is actually accessed as /api/member due to the setUrPattern() call in parent service
public class MembersResource {
	private static final Logger LOG = LoggerFactory.getLogger(MembersResource.class);
	
	private static ManualsConfiguration config;

	private User mockUser;
	private AuthUtils authUtils;

	@SuppressWarnings("static-access")
	public MembersResource(ManualsConfiguration config) throws IOException, ListFolderErrorException, DbxException {
		this.config = config;
		
		// See if we have a mock user
		if (config.getMockUser() != null) {
			mockUser = User.userFromMock(config.getMockUser());
		}
		
		// Get authorization utils object instance
		authUtils = AuthUtils.instance();
	}

	@GET
	@Path("email/{email}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response email(@PathParam("email") String email) {
		Member member = Member.getByEmail(email.toLowerCase());
		if (member != null) {
			new EmailLogin().emailTo(email, member.getUUID());
			return Response.ok().build();
		} else {
			Admin admin = Admin.getByEmail(email.toLowerCase());
			if (admin !=null) {
				new EmailLogin().emailTo(email, admin.getUUID());
				return Response.ok().build();
			}
		}
		LOG.info("Authentication for {}, person not found", email);
		return Response.status(404).build();
	}
	
	@GET
	@Path("verify/{uuid}/{code}")
	@Produces(MediaType.TEXT_HTML)
	public Response verify(@CookieParam("wcfc.manuals.token") Cookie cookie,
			@PathParam("uuid") String uuid,
			@PathParam("code") Integer code) throws URISyntaxException, APIException {


		User user = AuthUtils.instance().getUserFromCookie(cookie);
		if (user == null) {
			NewCookie newcookie = null;
			
			LOG.info("Code : {}   UUID: {}", code, uuid);
			VerificationCode verify = VerificationCode.getByPersonUUID(uuid);
			if (verify != null) {
				Person person = Person.getPerson(uuid);
				LOG.info("Authenticated user {}, admin == {}", person.getName(), person.isAdmin());
				newcookie = authUtils.generateCookie(new User(person));
				verify.setVerified(true);
				verify.save();
			}

			// User authenticated and identified. Save the info.
			if (newcookie != null) {
				return Response.seeOther(new URI("/equipment")).header("Set-Cookie", AuthUtils.sameSite(newcookie)).build();
			} else {
				LOG.info("Failed to verify {} with code {}", uuid, code);
				return Response.seeOther(new URI("/failure")).build();
			}
		} else {
			LOG.info("{} clicked on the URL again!", user);
			return Response.seeOther(new URI("/")).build();
		}
	}
	
	@GET
	@Path("logout")
	@Produces(MediaType.APPLICATION_JSON)
	public Response logout(@CookieParam("wcfc.manuals.token") Cookie cookie) throws URISyntaxException {
		User user = AuthUtils.instance().getUserFromCookie(cookie);
		if (user != null ) {
			LOG.info("User {} / {} logged out.", user.getName(), user.getEmail());
		}
		return Response.seeOther(new URI("/")).header("Set-Cookie", AuthUtils.instance().removeCookie()).build();
	}
	
	@POST
	@Path("populate")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public Response upload(@CookieParam("wcfc.manuals.token") Cookie cookie,
			@FormDataParam("members") InputStream uploadedInputStream,
			@FormDataParam("members") FormDataContentDisposition fileDetails)
			throws Exception {
		
		User user = authUtils.getUserFromCookie(cookie);
		
		MemberListXLS members = new MemberListXLS(uploadedInputStream);
		members.clean();
		
		Iterator<Entry<Integer, Member>> iterator = members.members().entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, Member> entry = iterator.next();
			Member member = entry.getValue();
			member.save();
		}

		return Response.ok().build();
	}

	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response createMember(@CookieParam("wcfc.manuals.token") Cookie cookie,
			Map<String, String> request) {
		String name = request.get("name");
		String email = request.get("email");
		Integer level = Integer.parseInt(request.get("email"));
		Integer id = Integer.parseInt(request.get("id"));
		
		LOG.info("Trying to create {}, {}, {}, {}", name, email, id, level);
		Member member = new Member(id, name, email, level);
		member.save();
		
		return Response.ok().build();
	}
}
