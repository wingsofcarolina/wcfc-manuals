package org.wingsofcarolina.manuals.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.domain.VerificationCode;

public class EmailLogin {

  // This address must be configured with Gmail API access
  static final String FROM = "webmaster@wingsofcarolina.org";

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
   * Initialize the email service with server information and Gmail credentials from environment.
   *
   * @param server The server URL for email templates
   */
  public static void initialize(String server) {
    EmailLogin.SERVER = server;

    // Get Gmail service account JSON from environment variable
    String credentialsJson = System.getenv("GMAIL_SERVICE_ACCOUNT_JSON");
    if (credentialsJson == null || credentialsJson.trim().isEmpty()) {
      LOG.error("GMAIL_SERVICE_ACCOUNT_JSON environment variable not set");
      throw new RuntimeException("Gmail service account JSON not configured");
    }

    try {
      gmailService = new GmailService(credentialsJson, FROM);
      LOG.info("Email service initialized successfully with Gmail API");
    } catch (Exception e) {
      LOG.error("Failed to initialize Gmail service: {}", e.getMessage(), e);
      throw new RuntimeException("Gmail service initialization failed", e);
    }
  }

  public void emailTo(String email, String uuid) {
    if (SERVER != null && gmailService != null) {
      Integer code = VerificationCode.makeEntry(uuid).getCode();

      String htmlBody = HTMLBODY
        .replace("SERVER", SERVER)
        .replace("UUID", uuid)
        .replace("EMAIL", email)
        .replace("CODE", code.toString());
      String textBody = TEXTBODY
        .replace("SERVER", SERVER)
        .replace("UUID", uuid)
        .replace("EMAIL", email)
        .replace("CODE", code.toString());

      try {
        gmailService.sendEmail(email, SUBJECT, textBody, htmlBody);
        LOG.info(
          "Verification email sent successfully to {} with id {} and code {}",
          email,
          uuid,
          code
        );
      } catch (Exception e) {
        LOG.error("Failed to send email to {}: {}", email, e.getMessage(), e);
        throw new RuntimeException("Failed to send verification email", e);
      }
    } else {
      LOG.error("Cannot send email: SERVER or gmailService not initialized");
      throw new IllegalStateException("Email service not properly initialized");
    }
  }
}
