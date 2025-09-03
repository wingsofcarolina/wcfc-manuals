package org.wingsofcarolina.manuals.slack;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface SlackAuthApi {
  // GET https://slack.com/api/oauth.v2.access?client_id=CLIENT_ID&client_secret=CLIENT_SECRET&code=XXYYZZ
  @GET("api/oauth.v2.access")
  Call<Map<String, Object>> authenticate(
    @Query("client_id") String client_id,
    @Query("client_secret") String client_secret,
    @Query("code") String code
  );

  // GET https://slack.com/api/users.identity
  @GET("api/users.identity")
  Call<Map<String, Object>> identity(@Header("Authorization") String token);
}
