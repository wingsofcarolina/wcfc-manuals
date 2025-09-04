package org.wingsofcarolina.manuals.common;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {

  private static final Logger LOG = LoggerFactory.getLogger(RuntimeExceptionMapper.class);

  @Override
  public Response toResponse(RuntimeException exception) {
    Integer code = 500;
    if (exception instanceof NotFoundException) {
      code = 404;
    }

    LOG.info(
      "{} : {} : {}",
      code,
      exception.getClass().getSimpleName(),
      exception.getMessage()
    );

    // exception.printStackTrace();

    return Response
      .serverError()
      .entity(
        new Error(
          code,
          exception.getClass().getSimpleName() + " : " + exception.getMessage()
        )
      )
      .type(MediaType.APPLICATION_JSON)
      .build();
  }
}
