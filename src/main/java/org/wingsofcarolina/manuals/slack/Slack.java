package org.wingsofcarolina.manuals.slack;

import java.io.IOException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.ManualsConfiguration;

public class Slack {

  private static final Logger LOG = LoggerFactory.getLogger(Slack.class);

  public static enum Channel {
    NOTIFY,
    MANUALS,
  }

  private static Slack instance = null;

  private String notificationUrl;
  private String manualsUrl;

  private ManualsConfiguration config;

  public Slack(ManualsConfiguration config) {
    // Build webhook URLs from the configuration
    if (config.getSlackNotify() != null && !config.getSlackNotify().isEmpty()) {
      if (config.getSlackNotify().startsWith("https://")) {
        notificationUrl = config.getSlackNotify();
      } else {
        // Assume it's in the old format (T/B/X) and convert to webhook URL
        String[] tokenParts = config.getSlackNotify().split("/");
        if (tokenParts.length == 3) {
          notificationUrl =
            "https://hooks.slack.com/services/" +
            tokenParts[0] +
            "/" +
            tokenParts[1] +
            "/" +
            tokenParts[2];
        } else {
          throw new RuntimeException("Bad Slack #notification token, shutting down!");
        }
      }
    } else {
      throw new RuntimeException("Slack notification URL not configured!");
    }

    if (config.getSlackManuals() != null && !config.getSlackManuals().isEmpty()) {
      if (config.getSlackManuals().startsWith("https://")) {
        manualsUrl = config.getSlackManuals();
      } else {
        // Assume it's in the old format (T/B/X) and convert to webhook URL
        String[] tokenParts = config.getSlackManuals().split("/");
        if (tokenParts.length == 3) {
          manualsUrl =
            "https://hooks.slack.com/services/" +
            tokenParts[0] +
            "/" +
            tokenParts[1] +
            "/" +
            tokenParts[2];
        } else {
          throw new RuntimeException("Bad Slack #manuals token, shutting down!");
        }
      }
    } else {
      throw new RuntimeException("Slack manuals URL not configured!");
    }

    Slack.instance = this;
    this.config = config;
  }

  public static Slack instance() {
    if (instance == null) {
      throw new RuntimeException(
        "Slack API communications object has not been initialized."
      );
    }
    return Slack.instance;
  }

  public void sendString(Channel channel, String msg) {
    if (msg != null) {
      LOG.debug("{}", msg);
      if (config.getMode().equals("PROD")) {
        sendMessage(channel, msg);
      }
    }
  }

  public boolean sendMessage(Channel channel, String msg) {
    if (msg != null) {
      LOG.info("Sending : {}", msg);
      if (config.getMode().equals("PROD")) {
        String url = getWebhookUrl(channel);
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        String json = "{\"text\":\"MANUALS: " + escapeJson(msg) + "\"}";
        HttpEntity stringEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        try {
          CloseableHttpResponse response = httpclient.execute(httpPost);
          if (response.getCode() != 200) {
            LOG.error(
              "Failed to successfully send message to Slack: {} {}",
              response.getCode(),
              response.getReasonPhrase()
            );
            return false;
          }
          return true;
        } catch (IOException e) {
          LOG.error("Error sending message to Slack", e);
          return false;
        }
      }
    }
    return false;
  }

  // For backward compatibility with the old MessageRequest API
  public boolean sendMessage(Channel channel, Object messageRequest) {
    // For now, just convert to string - this maintains compatibility
    // but loses the rich formatting. Could be enhanced later if needed.
    if (messageRequest != null) {
      return sendMessage(channel, messageRequest.toString());
    }
    return false;
  }

  private String getWebhookUrl(Channel channel) {
    // Select the desired channel.
    switch (channel) {
      case NOTIFY:
        return notificationUrl;
      case MANUALS:
        return manualsUrl;
      default:
        return notificationUrl;
    }
  }

  private String escapeJson(String text) {
    if (text == null) return "";
    return text
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace("\t", "\\t");
  }
}
