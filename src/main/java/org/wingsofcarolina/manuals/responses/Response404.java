package org.wingsofcarolina.manuals.responses;

import jakarta.ws.rs.core.Response;

public class Response404 extends AbstractResponse {

  public Response404() {
    super();
  }

  public Response404(String message) {
    super(message);
  }

  public Response build() {
    return super.build(404);
  }
}
