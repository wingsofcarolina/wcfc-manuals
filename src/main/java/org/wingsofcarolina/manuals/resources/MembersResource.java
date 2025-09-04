package org.wingsofcarolina.manuals.resources;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.ListFolderErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.ManualsConfiguration;
import org.wingsofcarolina.manuals.SimpleLogger;
import org.wingsofcarolina.manuals.authentication.AuthUtils;
import org.wingsofcarolina.manuals.domain.Admin;
import org.wingsofcarolina.manuals.domain.Member;
import org.wingsofcarolina.manuals.domain.VerificationCode;
import org.wingsofcarolina.manuals.email.EmailLogin;
import org.wingsofcarolina.manuals.members.MemberListXLS;
import org.wingsofcarolina.manuals.model.User;

/**
 * @author dwight
 *
 */
@Path("/member") // Note that this is actually accessed as /api/member due to the setUrPattern() call in parent service
public class MembersResource {

  private static final Logger LOG = LoggerFactory.getLogger(MembersResource.class);
  private static final String WCFC_TOKEN = "adfasd58df57a8adf68dsafd";

  @SuppressWarnings("unused")
  private static ManualsConfiguration config;

  private static SimpleLogger authLog;

  private User mockUser;
  private AuthUtils authUtils;

  private Integer authCount = 0;

  @SuppressWarnings("static-access")
  public MembersResource(ManualsConfiguration config)
    throws IOException, ListFolderErrorException, DbxException {
    this.config = config;

    // See if we have a mock user
    if (config.getMockUser() != null) {
      mockUser = User.userFromMock(config.getMockUser());
    }

    // Get authorization utils object instance
    authUtils = AuthUtils.instance();

    // Create auth logger
    authLog = new SimpleLogger("authentication", config);
  }

  @GET
  @Path("email/{email}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response email(@PathParam("email") String email) {
    if (email != null) {
      Member member = Member.getByEmail(email.toLowerCase());
      if (member != null) {
        new EmailLogin().emailTo(email, member.getUUID());
        return Response.ok().build();
      } else {
        Admin admin = Admin.getByEmail(email.toLowerCase());
        if (admin != null) {
          new EmailLogin().emailTo(email, admin.getUUID());
          return Response.ok().build();
        }
      }
      LOG.info("Authentication for {}, person not found", email);
    } else {
      LOG.info("Somehow an empty/null email address was provided, failure.");
    }
    return Response.status(404).build();
  }

  @GET
  @Path("verify/{code}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response verify(@PathParam("code") Integer code) {
    VerificationCode vc = VerificationCode.getByCode(code);
    if (vc != null) {
      Member member = Member.getByUUID(vc.getUUID());
      if (member != null) {
        User user = new User(member.getName(), member.getEmail());
        authLog.logUser(user);
        authCount++;

        // Remove used verification codes
        vc.delete();

        // User authenticated and identified. Save the info.
        NewCookie cookie = authUtils.generateCookie(user);
        return Response.ok().header("Set-Cookie", AuthUtils.sameSite(cookie)).build();
      }
    } else {
      LOG.info("Verification of code {} failed, not found.", code);
    }

    return Response.status(404).build();
  }

  @GET
  @Path("logout")
  @Produces(MediaType.APPLICATION_JSON)
  public Response logout(@CookieParam("wcfc.manuals.token") Cookie cookie)
    throws URISyntaxException {
    User user = AuthUtils.instance().getUserFromCookie(cookie);
    if (user != null) {
      LOG.info("User {} / {} logged out.", user.getName(), user.getEmail());
    }
    return Response
      .seeOther(new URI("/"))
      .header("Set-Cookie", AuthUtils.instance().removeCookie())
      .build();
  }

  @POST
  @Path("populate")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.TEXT_PLAIN)
  public Response populate(
    @Context HttpHeaders httpHeaders,
    @FormDataParam("members") InputStream uploadedInputStream,
    @FormDataParam("members") FormDataContentDisposition fileDetails
  ) throws Exception {
    String secret = httpHeaders.getHeaderString("X-WCFC-TOKEN");
    if (secret.compareTo(WCFC_TOKEN) == 0) {
      MemberListXLS members = new MemberListXLS(uploadedInputStream);
      members.clean();

      Iterator<Entry<Integer, Member>> iterator = members.members().entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<Integer, Member> entry = iterator.next();
        Member member = entry.getValue();
        member.save();
      }
    }

    return Response.ok().build();
  }

  @POST
  @Path("add")
  @Produces(MediaType.APPLICATION_JSON)
  public Response addMember(
    @Context HttpHeaders httpHeaders,
    Map<String, String> request
  ) {
    String secret = httpHeaders.getHeaderString("X-WCFC-TOKEN");
    if (secret.compareTo(WCFC_TOKEN) == 0) {
      String name = request.get("name");
      String email = request.get("email");
      Integer level = Integer.parseInt(request.get("level"));
      Integer id = Integer.parseInt(request.get("id"));

      Member member = Member.getByEmail(email);
      if (member == null) {
        LOG.info("Creating : {}, {}, {}, {}", name, email, id, level);
        member = new Member(id, name, email, level);
        member.save();

        return Response.ok().entity(member).build();
      } else {
        Map<String, String> response = new HashMap<String, String>();
        response.put("message", "Bad request, user already exists");
        return Response.status(400).entity(response).build();
      }
    } else {
      Map<String, String> response = new HashMap<String, String>();
      response.put("message", "Not authorized");
      return Response.status(401).entity(response).build();
    }
  }

  @POST
  @Path("remove")
  @Produces(MediaType.APPLICATION_JSON)
  public Response removeMember(
    @Context HttpHeaders httpHeaders,
    Map<String, String> request
  ) {
    String secret = httpHeaders.getHeaderString("X-WCFC-TOKEN");
    if (secret.compareTo(WCFC_TOKEN) == 0) {
      String name = request.get("name");
      String email = request.get("email");

      Member member = Member.getByEmail(email);
      if (member != null) {
        LOG.info("Removing : {}, {}", name, email);
        member.delete();
      }

      return Response.ok().entity(member).build();
    } else {
      Map<String, String> response = new HashMap<String, String>();
      response.put("message", "Not authorized");
      return Response.status(401).entity(response).build();
    }
  }
}
