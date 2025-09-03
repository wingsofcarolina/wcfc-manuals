package org.wingsofcarolina.manuals.responses;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.wingsofcarolina.manuals.common.Error;

public class AbstractResponse {

  protected int code;
  protected Object entity;
  protected String message;
  private NewCookie cookie;

  public AbstractResponse() {
    this.message = "n/a";
  }

  public AbstractResponse(String message) {
    this.message = message;
  }

  public AbstractResponse code(int code) {
    this.code = code;
    return this;
  }

  public AbstractResponse cookie(NewCookie cookie) {
    this.cookie = cookie;
    return this;
  }

  public AbstractResponse entity(Object entity) {
    this.entity = entity;
    return this;
  }

  public Response build(int code) {
    return Response.status(code).entity(new Error(code, message)).build();
  }

  public Response build() {
    ResponseBuilder builder = Response.status(code);
    if (entity != null) {
      builder.entity(entity);
    }
    if (cookie != null) {
      builder.cookie(cookie);
    }
    return builder.build();
  }
}
