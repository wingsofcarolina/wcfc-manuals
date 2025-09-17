package org.wingsofcarolina.manuals;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class ManualsConfiguration extends Configuration {

  private static ManualsConfiguration instance = null;

  @JsonProperty
  String mode;

  @JsonProperty
  String root;

  @JsonProperty
  Boolean auth;

  @JsonProperty
  String mockUser;

  @JsonProperty
  String slackNotify;

  @JsonProperty
  String slackManuals;

  @JsonProperty
  String mongodb;

  @JsonProperty
  String mongodbDatabase;

  @JsonProperty
  String manualsServer;

  @JsonProperty
  String gmailImpersonateUser;

  @JsonProperty
  String slackClientId;

  @JsonProperty
  String slackClientSecret;

  public ManualsConfiguration() {
    ManualsConfiguration.instance = this;
  }

  public static ManualsConfiguration instance() {
    return instance;
  }

  public Boolean getAuth() {
    return auth;
  }

  public String getMockUser() {
    return mockUser;
  }

  public String getRoot() {
    return root;
  }

  public String getSlackNotify() {
    return slackNotify;
  }

  public String getSlackManuals() {
    return slackManuals;
  }

  public String getMode() {
    return mode;
  }

  public String getMongodb() {
    return mongodb;
  }

  public String getMongodbDatabase() {
    return mongodbDatabase;
  }

  public String getManualsServer() {
    return manualsServer;
  }

  public String getGmailImpersonateUser() {
    return gmailImpersonateUser;
  }

  public String getSlackClientId() {
    return slackClientId;
  }

  public String getSlackClientSecret() {
    return slackClientSecret;
  }

  public String getGs() {
    if (getMode().equals("DEV")) {
      return "http://localhost:9323";
    } else {
      return getManualsServer();
    }
  }
}
