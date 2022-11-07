package org.wingsofcarolina.manuals.slack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.palantir.roboslack.api.MessageRequest;
import com.palantir.roboslack.api.markdown.SlackMarkdown;
import com.palantir.roboslack.webhook.api.model.WebHookToken;
import com.palantir.roboslack.webhook.api.model.response.ResponseCode;
import com.palantir.roboslack.webhook.SlackWebHookService;

import org.wingsofcarolina.manuals.ManualsConfiguration;

public class Slack {
	private static final Logger LOG = LoggerFactory.getLogger(Slack.class);
	
	public static enum Channel { NOTIFY, MANUALS };
	
	private static Slack instance = null;

	WebHookToken general;
	WebHookToken notification;

	private ManualsConfiguration config;
	
	public Slack(ManualsConfiguration config) {
		String[] tokenParts = config.getSlackNotify().split("/");
		if (tokenParts.length == 3) {
			notification = WebHookToken.builder()
	                .partT(tokenParts[0])
	                .partB(tokenParts[1])
	                .partX(tokenParts[2])
	                .build();
		} else {
			throw new RuntimeException("Bad Slack #notification token, shutting down!");
		}
	
		tokenParts = config.getSlackManuals().split("/");
		if (tokenParts.length == 3) {
			general = WebHookToken.builder()
	                .partT(tokenParts[0])
	                .partB(tokenParts[1])
	                .partX(tokenParts[2])
	                .build();
		} else {
			throw new RuntimeException("Bad Slack #contact token, shutting down!");
		}
		Slack.instance = this;
		this.config = config;
	}
	
	public static Slack instance() {
		if (instance == null) {
			throw new RuntimeException("Slack API communications object has not been initialized.");
		}
		return Slack.instance;
	}
	
	public void sendString(Channel channel, String msg) {		
		if (msg != null) {
			LOG.debug("{}", msg);
			if (config.getMode().equals("PROD") ) {
				MessageRequest message = MessageRequest.builder()
		                .username("roboslack")
		                // SlackMarkdown string decoration is handled automatically in fields that require it,
		                // so this is valid:
		                .iconEmoji(SlackMarkdown.EMOJI.decorate("smile"))
		                .text(msg)
		                .build();
				ResponseCode response = SlackWebHookService.with(getToken(channel))
	                    .sendMessage(message);
				if (response.name().equals("ok")) {
					LOG.error("Failed to send slack message : {}", response);
				}
			}
		}
	}

	public boolean sendMessage(Channel channel, MessageRequest msg) {
		if (msg != null) {
			LOG.info("Sending : {}", msg);
			if (config.getMode().equals("PROD") ) {
				ResponseCode response = SlackWebHookService.with(getToken(channel)).sendMessage(msg);
				if (response.name().equals("ok")) {
					LOG.error("Failed to send slack message : {}", response);
				} else {
					return true;
				}
			}
		}
		return false;
	}
	
	private WebHookToken getToken(Channel channel) {
		WebHookToken token;
		
		// Select the desired channel.
		switch (channel) {
			case NOTIFY : token = notification; break;
			case MANUALS: token = general; break;
			default: token = notification; break;
		}
		return token;
	}
}
