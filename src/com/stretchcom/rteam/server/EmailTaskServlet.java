package com.stretchcom.rteam.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EmailTaskServlet extends HttpServlet {
	//private static final Logger log = Logger.getLogger(EmailTaskServlet.class.getName());
	private static RskyboxClient log = new RskyboxClient();
	
	private static int MAX_TASK_RETRY_COUNT = 3;

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.debug("EmailTaskServlet.doGet() entered - SHOULD NOT BE CALLED!!!!!!!!!!!!!!!!!");
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		log.debug("EmailTaskServlet.doPost() entered");
		String response = "email sent successfully";
		resp.setContentType("text/plain");

		try {
			String emailAddress = req.getParameter("emailAddress");
			log.debug("emailAddress parameter: "	+ emailAddress);
			String fromEmailAddress = req.getParameter("fromEmailAddress");
			log.debug("fromEmailAddress parameter: "	+ fromEmailAddress);
			String fromEmailUser = req.getParameter("fromEmailUser");
			log.debug("fromEmailUser parameter: "	+ fromEmailUser);
			String subject = req.getParameter("subject");
			log.debug("subject parameter: "	+ subject);
			String message = req.getParameter("message");
			log.debug("message parameter: "	+ message);
			
			// need to get the retry count
			String taskRetryCountStr = req.getHeader("X-AppEngine-TaskRetryCount");
			// default the retry count to max because if it can't be extracted, we are packing up the books and going home
			int taskRetryCount = MAX_TASK_RETRY_COUNT;
			try {
				taskRetryCount = new Integer(taskRetryCountStr);
			} catch (Exception e1) {
				log.debug("should never happen, but no harm, no foul");
			}
			log.debug("taskRetryCount = " + taskRetryCount);

		    Properties props = new Properties();
		    Session session = Session.getDefaultInstance(props, null);
		    
		    // ensure valid parameters
		    if(emailAddress == null || emailAddress.length() == 0 ||
		    		message == null || message.length() == 0) {
		    	log.error("EmailTaskServlet:doPost:parameters", "null or empty parameter");
		    	return;
		    }
			
			// ***** VERIZON iPhone PATCH ******
		    emailAddress = emailAddress.replaceFirst("vzwpix", "vtext");
			// **********************************

		    try {
		    	// If the MobileCarrier Email/SMS gateway uses the from address, then a GAE email can be sent; otherwise,
		    	// must send the email from Rackspace. 
		    	if(Utility.doesEmailAddressStartWithPhoneNumber(emailAddress) && !MobileCarrier.usesFromAddress(emailAddress)) {
		    		String httpResponse = EmailToSmsClient.sendMail(subject, message, emailAddress, fromEmailAddress);
		    		log.debug("EmailToSmsClient response = " + httpResponse);
		    	} else {
			        Message msg = new MimeMessage(session);
			        msg.setFrom(new InternetAddress(fromEmailAddress, fromEmailUser));
			        //msg.setHeader("mailed-by", fromEmailAddress);
//			        msg.setFrom(new InternetAddress("reply@rteamtest.appspotmail.com", fromEmailUser));
//			        InternetAddress[] replyToAddresses = new InternetAddress[1];
//			        replyToAddresses[0] = new InternetAddress(fromEmailAddress, fromEmailUser);
//			        msg.setReplyTo(replyToAddresses);
			        msg.addRecipient(Message.RecipientType.TO, new InternetAddress(emailAddress));
			        //::TODO:: only for testing **********************************************************
			        //msg.addRecipient(Message.RecipientType.BCC, new InternetAddress("joepwro@gmail.com"));
			        //msg.addRecipient(Message.RecipientType.BCC, new InternetAddress("njw438@gmail.com"));
			        //************************************************************************************
			        if(subject == null || subject.trim().length() == 0) {
			        	subject = "...";
			        	log.debug("setting subject to ...");
			        }
			        msg.setSubject(subject);
			        msg.setContent(message, "text/html");
			        log.debug("sending email to: " + emailAddress + " with subject: " + subject);
			        Transport.send(msg);
		    	}
		        
		        resp.setStatus(HttpServletResponse.SC_OK);
		    } catch (AddressException e) {
				log.exception("EmailTaskServlet:doPost:AddressException", "", e);
		        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		    } catch (MessagingException e) {
				log.exception("EmailTaskServlet:doPost:MessagingException", "", e);
		    	resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		    } catch (UnsupportedEncodingException e) {
				log.exception("EmailTaskServlet:doPost:UnsupportedEncodingException", "", e);
		    	resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		    } catch (Exception e) {
				log.exception("EmailTaskServlet:doPost:Exception", "", e);
		    	resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		    }
		    
			// Return status depends on how many times this been attempted. If max retry count reached, return HTTP 200 so
		    // retries attempt stop.
		    if(taskRetryCount >= MAX_TASK_RETRY_COUNT) {
		    	resp.setStatus(HttpServletResponse.SC_OK);
		    }
		    
			resp.getWriter().println(response);
		}
		catch (Exception ex) {
			response = "Should not happen. Email send: failure : " + ex.getMessage();
			log.debug(response);
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.getWriter().println(response);
		}
	}
}
