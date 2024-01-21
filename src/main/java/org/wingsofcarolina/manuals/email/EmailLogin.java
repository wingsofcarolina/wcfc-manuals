package org.wingsofcarolina.manuals.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.domain.VerificationCode;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

public class EmailLogin {

	// This address must be verified with Amazon SES.
	static final String FROM = "webmaster@wingsofcarolina.org";
	
	// This is the server to which we need to direct the verification
	static String SERVER = null;

	// The subject line for the email.
	static final String SUBJECT = "WCFC Manuals Login";

	// The HTML body for the email.
	static final String HTMLBODY = "<html><div class=body>"
			+ "<img src=https://manuals.wingsofcarolina.org/WCFC-logo.jpg>"
			+ "<div class=title>WCFC Manuals Login</div>"
			+ "<p>This email contains a verification code you can use to log into the WCFC Manuals server. "
			+ "This code is good for roughly 2 hours after which you will need to request another verification code. Once "
			+ "your login is verified a token will be stored in your browser and subsequent attempts "
			+ "to access the server will NOT require logging in again, as long as you use the same "
			+ "system/browser and don't clear browser data. </p>"
			+ "<p>Enter the following code into the login verification input :</p>"
			+ "<div class=code>CODE</div>"
			+ "<p>We hope you find good value in having these online manuals available. Please report any issues/problems you encounter.</p>"
			+ "<div class=signature>-- WCFC Manuals Server Administration</div>"
			+ "</div></html>"
			+ "<style>p{width:70%;}"
			+ ".body{margin-top:30px;margin-left:30px;}"
			+ ".title{font-size:1.2em;font-weight:bold;}"
			+ ".code{text-decoration: none;margin:30px;font-size:36pt;font-family:Verdana}"
			+ ".signature{margin-left:30px;}</style>";

	// The email body for recipients with non-HTML email clients.
	static final String TEXTBODY = "WCFC Manuals Login\n"
			+ "This email contains a verification code you can use to log into the WCFC Manuals server.\n"
			+ "This code is good for roughly 2 hours after which you will need to request another verification\n"
			+ "code. Once your login is verified a token will be stored in your browser and subsequent attempts\n"
			+ "to access the server will NOT require logging in again, as long as you use the same\n"
			+ "system/browser and don't clear browser data.\n\n"
			+ "        CODE\n\n"
			+ "	We hope you find good value in having these online manuals available. Please\n"
			+ "report any issues/problems you encounter.\n\n"
			+ "-- WCFC Manuals Server Administration";

	private static final Logger LOG = LoggerFactory.getLogger(EmailLogin.class);
	
	public static void initialize(String server) {
		EmailLogin.SERVER = server;
	}
	
	public void emailTo(String email, String uuid) {
		if (SERVER != null) {
			Integer code = VerificationCode.makeEntry(uuid).getCode();
			
			String htmlBody = HTMLBODY.replace("SERVER", SERVER).replace("UUID", uuid).replace("EMAIL", email).replace("CODE", code.toString());
			String textBody = TEXTBODY.replace("SERVER", SERVER).replace("UUID", uuid).replace("EMAIL", email).replace("CODE", code.toString());
	
			try {
				AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
						// Replace US_WEST_2 with the AWS Region you're using for
						// Amazon SES.
						.withRegion(Regions.US_EAST_1).build();
				SendEmailRequest request = new SendEmailRequest().withDestination(new Destination().withToAddresses(email))
						.withMessage(new Message()
								.withBody(new Body()
										.withHtml(new Content().withCharset("UTF-8").withData(htmlBody))
										.withText(new Content().withCharset("UTF-8").withData(textBody)))
								.withSubject(new Content().withCharset("UTF-8").withData(SUBJECT)))
						.withSource(FROM);
				client.sendEmail(request);
				LOG.info("Email sent to {} with id {} and code {}", email, uuid, code);
			} catch (Exception ex) {
				LOG.info("The email was not sent. Error message: {}", ex.getMessage());
			}
		}
	}
}