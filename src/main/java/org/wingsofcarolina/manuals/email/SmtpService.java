package org.wingsofcarolina.manuals.email;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMTP service for sending emails via smtp-relay.gmail.com with TLS but no authentication.
 */
public class SmtpService {

  private static final String SMTP_HOST = "smtp-relay.gmail.com";
  private static final int SMTP_PORT = 587;
  private static final Logger LOG = LoggerFactory.getLogger(SmtpService.class);

  private Session session;
  private String fromAddress;

  /**
   * Initialize SMTP service with TLS enabled but no authentication.
   *
   * @param fromAddress The email address to send from
   */
  public SmtpService(String fromAddress) {
    this.fromAddress = fromAddress;
    this.session = createSmtpSession();
    LOG.info("SMTP service initialized successfully for sender: {}", fromAddress);
  }

  /**
   * Create SMTP session with TLS enabled and no authentication.
   */
  private Session createSmtpSession() {
    Properties props = new Properties();

    // SMTP server configuration
    props.put("mail.smtp.host", SMTP_HOST);
    props.put("mail.smtp.port", SMTP_PORT);

    // Enable TLS
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.starttls.required", "true");

    // No authentication required for smtp-relay.gmail.com
    props.put("mail.smtp.auth", "false");

    // Additional security settings
    props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");

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
    MimeMessage email = createEmail(to, subject, bodyText, bodyHtml);

    try {
      Transport.send(email);
      LOG.debug("Email sent successfully to: {}", to);
    } catch (MessagingException e) {
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
    MimeMessage email = new MimeMessage(session);
    email.setFrom(new InternetAddress(fromAddress));
    email.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
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
   * Get the configured sender address.
   */
  public String getFromAddress() {
    return fromAddress;
  }
}
