package org.wingsofcarolina.manuals.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;

/**
 * Resource to handle SPA (Single Page Application) fallback routing.
 * When a user refreshes the page on a client-side route, this resource
 * serves the index.html file so the frontend router can handle the routing.
 */
@Path("/")
public class SpaFallbackResource {

  @GET
  @Path("equipment")
  @Produces(MediaType.TEXT_HTML)
  public Response getEquipmentPage() {
    return serveIndexHtml();
  }

  @GET
  @Path("contact")
  @Produces(MediaType.TEXT_HTML)
  public Response getContactPage() {
    return serveIndexHtml();
  }

  @GET
  @Path("about")
  @Produces(MediaType.TEXT_HTML)
  public Response getAboutPage() {
    return serveIndexHtml();
  }

  @GET
  @Path("login")
  @Produces(MediaType.TEXT_HTML)
  public Response getLoginPage() {
    return serveIndexHtml();
  }

  @GET
  @Path("manage")
  @Produces(MediaType.TEXT_HTML)
  public Response getManagePage() {
    return serveIndexHtml();
  }

  @GET
  @Path("view")
  @Produces(MediaType.TEXT_HTML)
  public Response getViewPage() {
    return serveIndexHtml();
  }

  private Response serveIndexHtml() {
    try {
      InputStream indexStream = getClass().getResourceAsStream("/assets/index.html");
      if (indexStream == null) {
        return Response
          .status(Response.Status.NOT_FOUND)
          .entity("index.html not found")
          .build();
      }

      return Response.ok(indexStream, MediaType.TEXT_HTML).build();
    } catch (Exception e) {
      return Response
        .status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity("Error serving index.html: " + e.getMessage())
        .build();
    }
  }
}
