package org.wingsofcarolina.manuals;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

public class ManualsConfiguration extends Configuration {
	private static ManualsConfiguration instance = null;

	@JsonProperty String mode;
	@JsonProperty String root;
	@JsonProperty Boolean auth;
	@JsonProperty String mockUser;
	@JsonProperty String slackNotify;
	@JsonProperty String slackContact;

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

	public String getSlackContact() {
		return slackContact;
	}

	public String getMode() {
		return mode;
	}

	public String getGs() {
		if (getMode().equals("DEV")) {
			return "http://localhost:9323";
		} else {
			return "https://manuals.wingsofcarolina.org";
		}
	}
}
