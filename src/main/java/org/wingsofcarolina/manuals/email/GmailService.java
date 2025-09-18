package org.wingsofcarolina.manuals.email;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gmail API service for sending emails using service account authentication.
 */
public class GmailService {

  private static final Logger LOG = LoggerFactory.getLogger(GmailService.class);
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final String APPLICATION_NAME = "WCFC Manuals";

  private Gmail service;
  private String fromAddress;
  private String gmailApiBaseUrl;

  /**
   * Initialize Gmail service with service account credentials.
   *
   * @param serviceAccountKeyJson The service account key in JSON format
   * @param impersonateUser The email address to impersonate (must be in the same domain)
   * @throws IOException If credential loading fails
   * @throws GeneralSecurityException If HTTP transport initialization fails
   */
  public GmailService(String serviceAccountKeyJson, String impersonateUser)
    throws IOException, GeneralSecurityException {
    this(serviceAccountKeyJson, impersonateUser, "https://www.googleapis.com");
  }

  /**
   * Initialize Gmail service with service account credentials and configurable API base URL.
   *
   * @param serviceAccountKeyJson The service account key in JSON format
   * @param impersonateUser The email address to impersonate (must be in the same domain)
   * @param gmailApiBaseUrl The base URL for Gmail API (for testing with WireMock)
   * @throws IOException If credential loading fails
   * @throws GeneralSecurityException If HTTP transport initialization fails
   */
  public GmailService(
    String serviceAccountKeyJson,
    String impersonateUser,
    String gmailApiBaseUrl
  ) throws IOException, GeneralSecurityException {
    if (serviceAccountKeyJson == null || serviceAccountKeyJson.trim().isEmpty()) {
      throw new IllegalArgumentException(
        "Service account key JSON cannot be null or empty"
      );
    }

    if (impersonateUser == null || impersonateUser.trim().isEmpty()) {
      throw new IllegalArgumentException("Impersonate user cannot be null or empty");
    }

    if (gmailApiBaseUrl == null || gmailApiBaseUrl.trim().isEmpty()) {
      throw new IllegalArgumentException("Gmail API base URL cannot be null or empty");
    }

    this.fromAddress = impersonateUser; // Gmail API enforces From address = impersonated user
    this.gmailApiBaseUrl = gmailApiBaseUrl;

    try {
      // Validate email address format
      new InternetAddress(impersonateUser, true);
    } catch (Exception e) {
      LOG.error("Invalid impersonate user address format: {}", impersonateUser, e);
      throw new IllegalArgumentException(
        "Invalid impersonate user address: " + impersonateUser,
        e
      );
    }

    this.service = createGmailService(serviceAccountKeyJson, impersonateUser);
    LOG.info(
      "Gmail service initialized for user: {} with API base URL: {}",
      impersonateUser,
      gmailApiBaseUrl
    );
  }

  /**
   * Create Gmail service with service account credentials and domain-wide delegation.
   */
  private Gmail createGmailService(String serviceAccountKeyJson, String impersonateUser)
    throws IOException, GeneralSecurityException {
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

    // Load service account credentials
    GoogleCredentials credentials;
    try (
      ByteArrayInputStream keyStream = new ByteArrayInputStream(
        serviceAccountKeyJson.getBytes(StandardCharsets.UTF_8)
      )
    ) {
      credentials =
        ServiceAccountCredentials
          .fromStream(keyStream)
          .createScoped(Collections.singletonList(gmailApiBaseUrl + "/auth/gmail.send"))
          .createDelegated(impersonateUser);
    }

    Gmail.Builder builder = new Gmail.Builder(
      HTTP_TRANSPORT,
      JSON_FACTORY,
      new HttpCredentialsAdapter(credentials)
    )
      .setApplicationName(APPLICATION_NAME);

    // Set custom root URL if not using the default Google APIs base URL
    if (!gmailApiBaseUrl.equals("https://www.googleapis.com")) {
      builder.setRootUrl(gmailApiBaseUrl + "/");
    }

    return builder.build();
  }

  /**
   * Send an email with both text and HTML content.
   *
   * @param to Recipient email address
   * @param subject Email subject
   * @param bodyText Plain text body
   * @param bodyHtml HTML body
   * @throws MessagingException If email creation fails
   * @throws IOException If Gmail API call fails
   */
  public void sendEmail(String to, String subject, String bodyText, String bodyHtml)
    throws MessagingException, IOException {
    // Validate recipient email address
    try {
      new InternetAddress(to, true);
    } catch (Exception e) {
      LOG.error("Invalid recipient address format: {}", to, e);
      throw new MessagingException("Invalid recipient address: " + to, e);
    }

    MimeMessage email;
    try {
      email = createEmail(to, subject, bodyText, bodyHtml);
    } catch (MessagingException e) {
      LOG.error("Failed to create email message for {}: {}", to, e.getMessage(), e);
      throw new MessagingException("Email creation failed", e);
    }

    // Convert to Gmail message and send
    try {
      Message message = createMessageWithEmail(email);
      service.users().messages().send("me", message).execute();
      LOG.info("Email sent successfully to {} via Gmail API", to);
    } catch (IOException e) {
      LOG.error("Gmail API error while sending email to {}: {}", to, e.getMessage(), e);

      // Log specific error types for better diagnosis
      String errorMsg = e.getMessage();
      if (errorMsg != null) {
        if (errorMsg.contains("403") || errorMsg.contains("Forbidden")) {
          LOG.error(
            "PERMISSION ERROR: Gmail API access denied. " +
            "Check service account permissions and domain-wide delegation setup."
          );
        } else if (errorMsg.contains("401") || errorMsg.contains("Unauthorized")) {
          LOG.error(
            "AUTHENTICATION ERROR: Gmail API authentication failed. " +
            "Check service account credentials and impersonation settings."
          );
        } else if (errorMsg.contains("400") || errorMsg.contains("Bad Request")) {
          LOG.error(
            "REQUEST ERROR: Invalid email format or content. " +
            "Check email addresses and message structure."
          );
        } else if (errorMsg.contains("429") || errorMsg.contains("quota")) {
          LOG.error(
            "QUOTA ERROR: Gmail API quota exceeded. " +
            "Check API usage limits and quotas in Google Cloud Console."
          );
        } else if (errorMsg.contains("timeout")) {
          LOG.error(
            "TIMEOUT ERROR: Gmail API request timed out. " +
            "Check network connectivity and API availability."
          );
        } else {
          LOG.error("UNKNOWN GMAIL API ERROR: {}", errorMsg);
        }
      }

      throw new IOException("Failed to send email via Gmail API", e);
    } catch (Exception e) {
      LOG.error("Unexpected error while sending email to {}: {}", to, e.getMessage(), e);
      throw new IOException("Unexpected error during email sending", e);
    }
  }

  /**
   * Create a MimeMessage with multipart content (text and HTML).
   */
  private MimeMessage createEmail(
    String to,
    String subject,
    String bodyText,
    String bodyHtml
  ) throws MessagingException {
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);

    MimeMessage email = new MimeMessage(session);
    email.setFrom(new InternetAddress(fromAddress));
    email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
    email.setSubject(subject);

    MimeMultipart multipart = new MimeMultipart("alternative");
    int partCount = 0;

    // Add text part if provided
    if (bodyText != null && !bodyText.trim().isEmpty()) {
      MimeBodyPart textPart = new MimeBodyPart();
      textPart.setText(bodyText, "UTF-8");
      multipart.addBodyPart(textPart);
      partCount++;
    }

    // Add HTML part if provided
    if (bodyHtml != null && !bodyHtml.trim().isEmpty()) {
      MimeBodyPart htmlPart = new MimeBodyPart();
      htmlPart.setContent(bodyHtml, "text/html; charset=UTF-8");
      multipart.addBodyPart(htmlPart);
      partCount++;
    }

    if (partCount == 0) {
      LOG.warn(
        "Email created with no content parts - both text and HTML bodies are empty"
      );
    }

    email.setContent(multipart);
    return email;
  }

  /**
   * Create a Gmail Message from a MimeMessage.
   */
  private Message createMessageWithEmail(MimeMessage emailContent)
    throws MessagingException, IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    emailContent.writeTo(buffer);
    byte[] bytes = buffer.toByteArray();
    String encodedEmail = Base64.encodeBase64URLSafeString(bytes);

    Message message = new Message();
    message.setRaw(encodedEmail);
    return message;
  }

  /**
   * Get the configured sender address.
   */
  public String getFromAddress() {
    return fromAddress;
  }
}
