package org.wingsofcarolina.manuals.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.domain.VerificationCode;

public class EmailLogin {

  // This address will be used as the sender for SMTP emails
  static final String FROM = "webmaster@wingsofcarolina.org";

  // This is the server to which we need to direct the verification
  static String SERVER = null;

  // The subject line for the email.
  static final String SUBJECT = "WCFC Manuals Login";

  // SMTP service instance
  private static SmtpService smtpService;

  // The HTML body for the email.
  static final String HTMLBODY =
    "<html><div class=body>" +
    "<img src=https://manuals.wingsofcarolina.org/WCFC-logo.jpg>" +
    "<div class=title>WCFC Manuals Login</div>" +
    "<p>This email contains a verification code you can use to log into the WCFC Manuals server. " +
    "This code is good for roughly 2 hours after which you will need to request another verification code. Once " +
    "your login is verified a token will be stored in your browser and subsequent attempts " +
    "to access the server will NOT require logging in again, as long as you use the same " +
    "system/browser and don't clear browser data. </p>" +
    "<p>Enter the following code into the login verification input :</p>" +
    "<div class=code>CODE</div>" +
    "<p>We hope you find good value in having these online manuals available. Please report any issues/problems you encounter.</p>" +
    "<div class=signature>-- WCFC Manuals Server Administration</div>" +
    "</div></html>" +
    "<style>p{width:70%;}" +
    ".body{margin-top:30px;margin-left:30px;}" +
    ".title{font-size:1.2em;font-weight:bold;}" +
    ".code{text-decoration: none;margin:30px;font-size:36pt;font-family:Verdana}" +
    ".signature{margin-left:30px;}</style>";

  // The email body for recipients with non-HTML email clients.
  static final String TEXTBODY =
    "WCFC Manuals Login\n" +
    "This email contains a verification code you can use to log into the WCFC Manuals server.\n" +
    "This code is good for roughly 2 hours after which you will need to request another verification\n" +
    "code. Once your login is verified a token will be stored in your browser and subsequent attempts\n" +
    "to access the server will NOT require logging in again, as long as you use the same\n" +
    "system/browser and don't clear browser data.\n\n" +
    "        CODE\n\n" +
    "	We hope you find good value in having these online manuals available. Please\n" +
    "report any issues/problems you encounter.\n\n" +
    "-- WCFC Manuals Server Administration";

  private static final Logger LOG = LoggerFactory.getLogger(EmailLogin.class);

  /**
   * Initialize the email service with server information using SMTP.
   *
   * @param server The server URL for email templates
   * @param smtpHost The SMTP host to connect to
   */
  public static void initialize(String server, String smtpHost) {
    LOG.info(
      "Initializing email service with server: {} and SMTP host: {}",
      server,
      smtpHost
    );

    if (server == null || server.trim().isEmpty()) {
      LOG.error("Server parameter is null or empty");
      throw new IllegalArgumentException("Server parameter cannot be null or empty");
    }

    if (smtpHost == null || smtpHost.trim().isEmpty()) {
      LOG.error("SMTP host parameter is null or empty");
      throw new IllegalArgumentException("SMTP host parameter cannot be null or empty");
    }

    EmailLogin.SERVER = server;

    try {
      LOG.debug(
        "Creating SMTP service with from address: {} and host: {}",
        FROM,
        smtpHost
      );
      smtpService = new SmtpService(FROM, smtpHost);
      LOG.info("Email service initialized successfully with SMTP host: {}", smtpHost);
    } catch (IllegalArgumentException e) {
      LOG.error("Invalid configuration for SMTP service: {}", e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      LOG.error(
        "Failed to initialize SMTP service with host {}: {}",
        smtpHost,
        e.getMessage(),
        e
      );
      throw new RuntimeException("SMTP service initialization failed", e);
    }
  }

  public void emailTo(String email, String uuid) {
    // Validate input parameters
    if (email == null || email.trim().isEmpty()) {
      LOG.error("Email parameter is null or empty");
      throw new IllegalArgumentException("Email parameter cannot be null or empty");
    }

    if (uuid == null || uuid.trim().isEmpty()) {
      LOG.error("UUID parameter is null or empty");
      throw new IllegalArgumentException("UUID parameter cannot be null or empty");
    }

    // Check service initialization
    if (SERVER == null) {
      LOG.error("Cannot send email: SERVER not initialized (SERVER is null)");
      throw new IllegalStateException(
        "Email service not properly initialized - SERVER is null"
      );
    }

    if (smtpService == null) {
      LOG.error("Cannot send email: smtpService not initialized (smtpService is null)");
      throw new IllegalStateException(
        "Email service not properly initialized - smtpService is null"
      );
    }

    // Generate verification code
    Integer code;
    try {
      code = VerificationCode.makeEntry(uuid).getCode();
    } catch (Exception e) {
      LOG.error(
        "Failed to generate verification code for UUID {}: {}",
        uuid,
        e.getMessage(),
        e
      );
      throw new RuntimeException("Failed to generate verification code", e);
    }

    // Prepare email content
    String htmlBody;
    String textBody;
    try {
      htmlBody =
        HTMLBODY
          .replace("SERVER", SERVER)
          .replace("UUID", uuid)
          .replace("EMAIL", email)
          .replace("CODE", code.toString());
      textBody =
        TEXTBODY
          .replace("SERVER", SERVER)
          .replace("UUID", uuid)
          .replace("EMAIL", email)
          .replace("CODE", code.toString());
    } catch (Exception e) {
      LOG.error("Failed to prepare email content for {}: {}", email, e.getMessage(), e);
      throw new RuntimeException("Failed to prepare email content", e);
    }

    // Send the email
    try {
      smtpService.sendEmail(email, SUBJECT, textBody, htmlBody);
      LOG.info("Email sent to {}", email);
    } catch (javax.mail.MessagingException e) {
      String errorMsg = e.getMessage();
      LOG.error(
        "MessagingException while sending verification email to {}: {}",
        email,
        errorMsg,
        e
      );

      // Log specific guidance based on error type
      if (errorMsg != null) {
        if (errorMsg.contains("Connection refused")) {
          LOG.error(
            "EMAIL SEND FAILURE: Cannot connect to SMTP server. " +
            "Check Google Cloud firewall rules and SMTP relay configuration."
          );
        } else if (errorMsg.contains("Authentication failed")) {
          LOG.error(
            "EMAIL SEND FAILURE: SMTP authentication failed. " +
            "Verify Google Cloud SMTP relay settings and authentication configuration."
          );
        } else if (errorMsg.contains("550")) {
          LOG.error(
            "EMAIL SEND FAILURE: Recipient {} rejected by server. " +
            "Email address may be invalid or blocked.",
            email
          );
        } else if (errorMsg.contains("553") || errorMsg.contains("Relaying denied")) {
          LOG.error(
            "EMAIL SEND FAILURE: SMTP relay denied. " +
            "Check Google Cloud SMTP relay authorization and IP restrictions."
          );
        } else if (errorMsg.contains("timeout")) {
          LOG.error(
            "EMAIL SEND FAILURE: Connection timeout. " +
            "Check Google Cloud network connectivity and SMTP server availability."
          );
        }
      }

      throw new RuntimeException(
        "Failed to send verification email due to messaging error",
        e
      );
    } catch (Exception e) {
      LOG.error(
        "Unexpected error while sending verification email to {}: {}",
        email,
        e.getMessage(),
        e
      );
      throw new RuntimeException(
        "Failed to send verification email due to unexpected error",
        e
      );
    }
  }
}
