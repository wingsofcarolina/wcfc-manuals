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
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gmail API service wrapper for sending emails via Google's Gmail API.
 * Uses service account authentication for server-to-server communication.
 */
public class GmailService {

  private static final String APPLICATION_NAME = "WCFC Manuals";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final String GMAIL_SEND_SCOPE =
    "https://www.googleapis.com/auth/gmail.send";
  private static final Logger LOG = LoggerFactory.getLogger(GmailService.class);

  private Gmail service;
  private String fromAddress;

  /**
   * Initialize Gmail service with service account credentials.
   *
   * @param credentialsJson Service account JSON content as string
   * @param fromAddress The email address to send from
   * @throws IOException If credentials cannot be parsed
   * @throws GeneralSecurityException If Gmail service cannot be initialized
   */
  public GmailService(String credentialsJson, String fromAddress)
    throws IOException, GeneralSecurityException {
    this.fromAddress = fromAddress;
    this.service = createGmailService(credentialsJson);
    LOG.info("Gmail service initialized successfully for sender: {}", fromAddress);
  }

  /**
   * Create Gmail service instance with service account authentication.
   */
  private Gmail createGmailService(String credentialsJson)
    throws IOException, GeneralSecurityException {
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

    GoogleCredentials credentials = ServiceAccountCredentials
      .fromStream(new ByteArrayInputStream(credentialsJson.getBytes()))
      .createScoped(Collections.singleton(GMAIL_SEND_SCOPE));

    return new Gmail.Builder(
      HTTP_TRANSPORT,
      JSON_FACTORY,
      new HttpCredentialsAdapter(credentials)
    )
      .setApplicationName(APPLICATION_NAME)
      .build();
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
    MimeMessage email = createEmail(to, subject, bodyText, bodyHtml);
    Message message = createMessageWithEmail(email);

    try {
      Message result = service.users().messages().send("me", message).execute();
      LOG.debug("Email sent successfully. Message ID: {}", result.getId());
    } catch (IOException e) {
      LOG.error("Failed to send email to {}: {}", to, e.getMessage());
      throw e;
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

    // Add text part if provided
    if (bodyText != null && !bodyText.trim().isEmpty()) {
      MimeBodyPart textPart = new MimeBodyPart();
      textPart.setText(bodyText, "UTF-8");
      multipart.addBodyPart(textPart);
    }

    // Add HTML part if provided
    if (bodyHtml != null && !bodyHtml.trim().isEmpty()) {
      MimeBodyPart htmlPart = new MimeBodyPart();
      htmlPart.setContent(bodyHtml, "text/html; charset=UTF-8");
      multipart.addBodyPart(htmlPart);
    }

    email.setContent(multipart);
    return email;
  }

  /**
   * Convert MimeMessage to Gmail API Message format.
   */
  private Message createMessageWithEmail(MimeMessage emailContent)
    throws MessagingException, IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    emailContent.writeTo(buffer);
    byte[] bytes = buffer.toByteArray();
    String encodedEmail = java.util.Base64.getUrlEncoder().encodeToString(bytes);

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
