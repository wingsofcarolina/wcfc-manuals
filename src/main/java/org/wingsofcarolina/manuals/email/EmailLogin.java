package org.wingsofcarolina.manuals.email;

import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.domain.VerificationCode;

public class EmailLogin {

  // This is the server to which we need to direct the verification
  static String SERVER = null;

  // The subject line for the email.
  static final String SUBJECT = "WCFC Manuals Login";

  // Gmail service instance
  private static GmailService gmailService;

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
   * Initialize the email service with server information using Gmail API.
   * The service account key is read directly from the GMAIL_SERVICE_ACCOUNT_KEY environment variable.
   *
   * @param server The server URL for email templates
   * @param impersonateUser The email address to impersonate
   */
  public static void initialize(String server, String impersonateUser) {
    initialize(server, impersonateUser, "https://www.googleapis.com");
  }

  /**
   * Initialize the email service with server information using Gmail API and configurable API base URL.
   * The service account key is read directly from the GMAIL_SERVICE_ACCOUNT_KEY environment variable.
   *
   * @param server The server URL for email templates
   * @param impersonateUser The email address to impersonate
   * @param gmailApiBaseUrl The base URL for Gmail API (for testing with WireMock)
   */
  public static void initialize(
    String server,
    String impersonateUser,
    String gmailApiBaseUrl
  ) {
    LOG.info(
      "Initializing email service with server: {}, Gmail API impersonation user: {}, Gmail API base URL: {}",
      server,
      impersonateUser,
      gmailApiBaseUrl
    );

    if (server == null || server.trim().isEmpty()) {
      LOG.error("Server parameter is null or empty");
      throw new IllegalArgumentException("Server parameter cannot be null or empty");
    }

    if (impersonateUser == null || impersonateUser.trim().isEmpty()) {
      LOG.error("Impersonate user parameter is null or empty");
      throw new IllegalArgumentException(
        "Impersonate user parameter cannot be null or empty"
      );
    }

    if (gmailApiBaseUrl == null || gmailApiBaseUrl.trim().isEmpty()) {
      LOG.error("Gmail API base URL parameter is null or empty");
      throw new IllegalArgumentException(
        "Gmail API base URL parameter cannot be null or empty"
      );
    }

    // Read service account key from environment variable
    String serviceAccountKeyJson = System.getenv("GMAIL_SERVICE_ACCOUNT_KEY");
    if (serviceAccountKeyJson == null || serviceAccountKeyJson.trim().isEmpty()) {
      LOG.error("GMAIL_SERVICE_ACCOUNT_KEY environment variable is null or empty");
      throw new IllegalArgumentException(
        "GMAIL_SERVICE_ACCOUNT_KEY environment variable must be set with the service account JSON"
      );
    }

    EmailLogin.SERVER = server;

    try {
      LOG.debug(
        "Creating Gmail service with impersonate user: {} and API base URL: {}",
        impersonateUser,
        gmailApiBaseUrl
      );
      gmailService =
        new GmailService(serviceAccountKeyJson, impersonateUser, gmailApiBaseUrl);
      LOG.info(
        "Email service initialized successfully with Gmail API for user: {} using base URL: {}",
        impersonateUser,
        gmailApiBaseUrl
      );
    } catch (IllegalArgumentException e) {
      LOG.error("Invalid configuration for Gmail service: {}", e.getMessage(), e);
      throw e;
    } catch (IOException e) {
      LOG.error(
        "Failed to initialize Gmail service due to IO error: {}",
        e.getMessage(),
        e
      );
      throw new RuntimeException(
        "Gmail service initialization failed due to IO error",
        e
      );
    } catch (GeneralSecurityException e) {
      LOG.error(
        "Failed to initialize Gmail service due to security error: {}",
        e.getMessage(),
        e
      );
      throw new RuntimeException(
        "Gmail service initialization failed due to security error",
        e
      );
    } catch (Exception e) {
      LOG.error("Failed to initialize Gmail service: {}", e.getMessage(), e);
      throw new RuntimeException("Gmail service initialization failed", e);
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

    if (gmailService == null) {
      LOG.error("Cannot send email: gmailService not initialized (gmailService is null)");
      throw new IllegalStateException(
        "Email service not properly initialized - gmailService is null"
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
      gmailService.sendEmail(email, SUBJECT, textBody, htmlBody);
      LOG.info("Email sent to {} via Gmail API", email);
    } catch (MessagingException e) {
      String errorMsg = e.getMessage();
      LOG.error(
        "MessagingException while sending verification email to {}: {}",
        email,
        errorMsg,
        e
      );

      throw new RuntimeException(
        "Failed to send verification email due to messaging error",
        e
      );
    } catch (IOException e) {
      String errorMsg = e.getMessage();
      LOG.error(
        "IOException while sending verification email to {}: {}",
        email,
        errorMsg,
        e
      );

      // Log specific guidance based on error type
      if (errorMsg != null) {
        if (errorMsg.contains("403") || errorMsg.contains("Forbidden")) {
          LOG.error(
            "EMAIL SEND FAILURE: Gmail API access denied. " +
            "Check service account permissions and domain-wide delegation setup."
          );
        } else if (errorMsg.contains("401") || errorMsg.contains("Unauthorized")) {
          LOG.error(
            "EMAIL SEND FAILURE: Gmail API authentication failed. " +
            "Check service account credentials and impersonation settings."
          );
        } else if (errorMsg.contains("400") || errorMsg.contains("Bad Request")) {
          LOG.error(
            "EMAIL SEND FAILURE: Invalid email format or content. " +
            "Check email addresses and message structure."
          );
        } else if (errorMsg.contains("429") || errorMsg.contains("quota")) {
          LOG.error(
            "EMAIL SEND FAILURE: Gmail API quota exceeded. " +
            "Check API usage limits and quotas in Google Cloud Console."
          );
        } else if (errorMsg.contains("timeout")) {
          LOG.error(
            "EMAIL SEND FAILURE: Gmail API request timed out. " +
            "Check network connectivity and API availability."
          );
        }
      }

      throw new RuntimeException(
        "Failed to send verification email due to Gmail API error",
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
