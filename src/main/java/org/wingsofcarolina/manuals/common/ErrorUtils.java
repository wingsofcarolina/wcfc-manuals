package org.wingsofcarolina.manuals.common;

import java.io.IOException;
import java.lang.annotation.Annotation;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ErrorUtils {

  public static APIError parseError(Retrofit retrofit, Response<?> response) {
    APIError error;
    Converter<ResponseBody, APIError> converter = retrofit.responseBodyConverter(
      APIError.class,
      new Annotation[0]
    );

    try {
      error = converter.convert(response.errorBody());
    } catch (IOException e) {
      return new APIError("Could not parse error response.");
    }

    return error;
  }
}
