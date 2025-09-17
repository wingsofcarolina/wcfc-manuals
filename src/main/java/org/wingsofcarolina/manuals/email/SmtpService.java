package org.wingsofcarolina.manuals.email;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMTP service for sending emails with TLS but no authentication.
 */
public class SmtpService {

  private static final int SMTP_PORT = 587;
  private static final Logger LOG = LoggerFactory.getLogger(SmtpService.class);

  private Session session;
  private String fromAddress;
  private String smtpHost;

  /**
   * Initialize SMTP service with TLS enabled but no authentication.
   *
   * @param fromAddress The email address to send from
   * @param smtpHost The SMTP host to connect to
   */
  public SmtpService(String fromAddress, String smtpHost) {
    this.fromAddress = fromAddress;
    this.smtpHost = smtpHost;

    try {
      // Validate email address format
      new InternetAddress(fromAddress, true);
    } catch (AddressException e) {
      LOG.error("Invalid from address format: {}", fromAddress, e);
      throw new IllegalArgumentException("Invalid from address: " + fromAddress, e);
    }

    this.session = createSmtpSession();
    LOG.info(
      "SMTP service initialized for sender: {} using host: {}:{}",
      fromAddress,
      smtpHost,
      SMTP_PORT
    );
  }

  /**
   * Create SMTP session with TLS enabled and no authentication.
   */
  private Session createSmtpSession() {
    Properties props = new Properties();

    // SMTP server configuration
    props.put("mail.smtp.host", smtpHost);
    props.put("mail.smtp.port", SMTP_PORT);

    // Enable TLS
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.starttls.required", "true");

    // No authentication required for smtp-relay.gmail.com
    props.put("mail.smtp.auth", "false");

    // Additional security settings
    props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");

    // Connection timeout settings (30 seconds)
    props.put("mail.smtp.connectiontimeout", "30000");
    props.put("mail.smtp.timeout", "30000");
    props.put("mail.smtp.writetimeout", "30000");

    // Enable debug mode for detailed SMTP protocol logging
    props.put("mail.debug", "false"); // Set to "true" for very verbose debugging

    // Create session without authentication
    return Session.getInstance(props);
  }

  /**
   * Send an email with both text and HTML content.
   *
   * @param to Recipient email address
   * @param subject Email subject
   * @param bodyText Plain text body
   * @param bodyHtml HTML body
   * @throws MessagingException If email creation or sending fails
   */
  public void sendEmail(String to, String subject, String bodyText, String bodyHtml)
    throws MessagingException {
    // Validate recipient email address
    try {
      new InternetAddress(to, true);
    } catch (AddressException e) {
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

    // Attempt to send the email with detailed error handling
    Transport transport = null;
    try {
      transport = session.getTransport("smtp");

      // Connect without authentication
      transport.connect(smtpHost, SMTP_PORT, null, null);

      // Send the message
      transport.sendMessage(email, email.getAllRecipients());
    } catch (MessagingException e) {
      String errorMsg = e.getMessage();
      LOG.error("MessagingException while sending email to {}: {}", to, errorMsg, e);
      LOG.error(
        "Email details - From: {}, To: {}, SMTP Host: {}, Port: {}",
        fromAddress,
        to,
        smtpHost,
        SMTP_PORT
      );

      // Log specific error types for better diagnosis
      if (errorMsg != null) {
        if (
          errorMsg.contains("Connection refused") || errorMsg.contains("ConnectException")
        ) {
          LOG.error(
            "CONNECTION ERROR: Unable to connect to SMTP server {}:{}. " +
            "Check if the server is reachable and the port is correct.",
            smtpHost,
            SMTP_PORT
          );
        } else if (
          errorMsg.contains("Authentication failed") || errorMsg.contains("535")
        ) {
          LOG.error(
            "AUTHENTICATION ERROR: SMTP authentication failed. " +
            "Check credentials or authentication settings."
          );
        } else if (errorMsg.contains("550") || errorMsg.contains("Mailbox unavailable")) {
          LOG.error(
            "RECIPIENT ERROR: Recipient address {} rejected by server. " +
            "The email address may not exist or be invalid.",
            to
          );
        } else if (errorMsg.contains("552") || errorMsg.contains("Message size")) {
          LOG.error("MESSAGE SIZE ERROR: Email message too large for server limits.");
        } else if (errorMsg.contains("553") || errorMsg.contains("Relaying denied")) {
          LOG.error(
            "RELAY ERROR: SMTP server {} denied relaying. " +
            "Check if the server allows relaying from this IP.",
            smtpHost
          );
        } else if (errorMsg.contains("timeout") || errorMsg.contains("timed out")) {
          LOG.error(
            "TIMEOUT ERROR: Connection to SMTP server timed out. " +
            "Server may be slow or network issues present."
          );
        } else if (errorMsg.contains("TLS") || errorMsg.contains("SSL")) {
          LOG.error(
            "TLS/SSL ERROR: Secure connection failed. " +
            "Check TLS/SSL configuration and supported protocols."
          );
        } else {
          LOG.error("UNKNOWN SMTP ERROR: {}", errorMsg);
        }
      }

      throw e;
    } catch (Exception e) {
      LOG.error("Unexpected error while sending email to {}: {}", to, e.getMessage(), e);
      LOG.error(
        "Email details - From: {}, To: {}, SMTP Host: {}, Port: {}",
        fromAddress,
        to,
        smtpHost,
        SMTP_PORT
      );
      throw new MessagingException("Unexpected error during email sending", e);
    } finally {
      if (transport != null && transport.isConnected()) {
        try {
          transport.close();
        } catch (MessagingException e) {
          LOG.warn("Failed to close SMTP transport: {}", e.getMessage());
        }
      }
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
    MimeMessage email = new MimeMessage(session);
    email.setFrom(new InternetAddress(fromAddress));
    email.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
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
   * Get the configured sender address.
   */
  public String getFromAddress() {
    return fromAddress;
  }
}
