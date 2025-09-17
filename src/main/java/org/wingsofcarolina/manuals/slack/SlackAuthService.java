package org.wingsofcarolina.manuals.slack;

import java.io.IOException;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.common.APIError;
import org.wingsofcarolina.manuals.common.APIException;
import org.wingsofcarolina.manuals.common.ErrorUtils;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SlackAuthService {

  private static final Logger LOG = LoggerFactory.getLogger(SlackAuthService.class);

  private static final String BASE_URL = "https://slack.com/";

  private Retrofit retrofit;
  private SlackAuthApi api;
  private String client_id;
  private String client_secret;

  public SlackAuthService(String client_id, String client_secret) {
    this.client_id = client_id;
    this.client_secret = client_secret;

    //HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
    //interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
    OkHttpClient client = new OkHttpClient.Builder()
      //.addInterceptor(interceptor)
      .build();

    retrofit =
      new Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build();

    api = retrofit.create(SlackAuthApi.class);
  }

  /**
   * Authenticate a user based on the code returned from the Slack SSO page.
   * An example of the data returned is :
   *
   * {
   *   "ok": true,
   *   "app_id": "A01MEN43G4B",
   *   "authed_user": {
   *     "id": "REDACTED",
   *     "scope": "identity.basic",
   *     "access_token": "[token]",
   *     "token_type": "user"
   *   },
   *   "team": {
   *     "id": "REDACTED"
   *   },
   *   "enterprise": null,
   *   "is_enterprise_install": false
   * }
   *
   * @param code
   * @return
   * @throws APIException
   */
  public Map<String, Object> authenticate(String code) throws APIException {
    Call<Map<String, Object>> call = api.authenticate(client_id, client_secret, code);
    try {
      Response<Map<String, Object>> response = call.execute();
      if (response.isSuccessful()) {
        Map<String, Object> result = response.body();
        if (!(boolean) result.get("ok")) {
          throw new APIException("Return result from Slack was not ok");
        }
        return result;
      } else {
        APIError error = ErrorUtils.parseError(retrofit, response);
        LOG.info("Error message -- {}", error.message());
        throw new APIException(error.message());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Map<String, Object> identity(String token) throws APIException {
    Call<Map<String, Object>> call = api.identity("Bearer " + token);
    try {
      Response<Map<String, Object>> response = call.execute();
      if (response.isSuccessful()) {
        Map<String, Object> result = response.body();
        if (!(boolean) result.get("ok")) {
          throw new APIException("Return result from Slack was not ok");
        }
        return result;
      } else {
        APIError error = ErrorUtils.parseError(retrofit, response);
        LOG.info("Error message -- {}", error.message());
        throw new APIException(error.message());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}
