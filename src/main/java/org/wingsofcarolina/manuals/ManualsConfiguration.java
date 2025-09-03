package org.wingsofcarolina.manuals;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.knowm.dropwizard.sundial.SundialConfiguration;

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
  String manualsServer;

  @Valid
  @NotNull
  public SundialConfiguration sundialConfiguration = new SundialConfiguration();

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

  public String getManualsServer() {
    return manualsServer;
  }

  public String getGs() {
    if (getMode().equals("DEV")) {
      return "http://localhost:9323";
    } else {
      return "https://manuals.wingsofcarolina.org";
    }
  }

  @JsonProperty("sundial")
  public SundialConfiguration getSundialConfiguration() {
    return sundialConfiguration;
  }
}
