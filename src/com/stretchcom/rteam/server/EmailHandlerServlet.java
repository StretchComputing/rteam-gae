package com.stretchcom.rteam.server;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.activation.DataHandler;

public class EmailHandlerServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(EmailHandlerServlet.class.getName());

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.info("EmailHandlerServlet.doGet() entered - SHOULD NOT BE CALLED!!!!!!!!!!!!!!!!!");
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		log.info("EmailHandlerServlet.doPost() entered");
		String response = "email sent successfully";
		resp.setContentType("text/plain");
		String returnedText = "";
		String fromUserName = Emailer.NO_REPLY;
		Boolean isTextMessage = false;

		try {
			Properties props = new Properties(); 
	        Session session = Session.getDefaultInstance(props, null); 
	        MimeMessage message = new MimeMessage(session, req.getInputStream());
	        
        	Address[] fromAddress = message.getFrom();
        	Address[] replyToAddress = message.getReplyTo();
        	Address sender = message.getSender();
        	String subject = message.getSubject();
        	Address[] toRecipient = message.getRecipients(RecipientType.TO);
        	String fromEmailAddress = fromAddress[0].toString();
        	if(fromAddress != null && fromAddress.length > 0) log.info("from address: " + fromEmailAddress);
        	if(sender != null) log.info("sender: " + sender.toString());
        	if(replyToAddress != null && replyToAddress.length > 0) log.info("reply to address: " + replyToAddress[0].toString());
        	if(subject != null) log.info("subject: " + sender);
        	if(toRecipient != null && toRecipient.length > 0) log.info("toRecipient: " + toRecipient[0].toString());

	        String contentType = message.getContentType();
	        Object content = message.getContent();
	        String body = null;
	        // TODO is this the right way to get the body of the message?
	        if(content instanceof String) {
	        	String contentStr = content.toString();
	        	int contentLength = contentStr.length();
	        	if(contentLength > 1000) {contentLength = 1000;}
	        	log.info("beginning of message received = " + contentStr.substring(0, contentLength));
	        	body = contentStr;
	        } else if(content instanceof Multipart) {
	        	log.info("received multipart email");
	        	for(int i=0; i<((Multipart)content).getCount(); i++) {
	        		BodyPart bodyPart = ((Multipart)content).getBodyPart(i);
	        		log.info("bodyPart " + i + " has content type = " + bodyPart.getContentType());
	        		String disposition = bodyPart.getDisposition();
	        		log.info("disposition = " + disposition);
	        		if (disposition != null && (disposition.equals(BodyPart.ATTACHMENT))) {
	        			log.info("Mail have some attachment : ");
	                } else {
	                	log.info(bodyPart.getContent().toString());
	                	if(i == 0) {body = bodyPart.getContent().toString();}
	                }	        	
	        	}
	        } else {
	        	log.severe("email received neither plain text or multi-part");
	        }
	        
	        String toRecipientEmailAddress = toRecipient[0].toString().toLowerCase();
	        
	        // TODO
	        // ::REMINDER:: rTeam mailboxes that are forwarding are holding a copy of the email in the inbox. This was
	        //              done for debugging purposes. After initial interval, change setting to NOT hold copy in inbox.
	        if(toRecipientEmailAddress.startsWith(Emailer.JOIN)) {
	        	isTextMessage = true;
	        	fromUserName = Emailer.SMS;
	        	log.info("handling 'join' response");
	        	returnedText = Member.confirmNewMemberViaSms(fromEmailAddress);
	        } else if(toRecipientEmailAddress.startsWith(Emailer.SMS)) {
	        	isTextMessage = true;
	        	fromUserName = Emailer.SMS;
	        	// Confirmation, poll response or unsolicited response from a SMS member
	        	log.info("handling 'text' response");
	        	returnedText = Recipient.handleSmsResponse(extractPhoneNumber(fromEmailAddress), body);
	        } else if(toRecipientEmailAddress.startsWith(Emailer.REPLY)) {
	        	// Unsolicited email reply
	        	log.info("handling 'reply' response");
	        	// extract the token from the body
	        	String emailReplyToken = getEmailReplyToken(body);
	        	if(emailReplyToken != null && emailReplyToken.length() > 0) {
	        		returnedText = Recipient.handleEmailReplyUsingToken(body, emailReplyToken);
	        	} else {
	        		// If body of reply email does not contain original email, then embedded rTeam token will NOT be found.
	        		// Depends on email client how likely or unlikely this is. In any case, attempt to handle anyway.
	        		returnedText = Recipient.handleEmailReplyUsingFromAddressAndSubject(body, fromEmailAddress, subject);
	        	}
	        } else if(toRecipientEmailAddress.startsWith("noreply")) {
	        	log.info("handling 'noreply' response");
	        	// not sure if I have the originator of the email, but if so, let them know this is a noreply mailbox
	        	returnedText = UserInterfaceMessage.NOREPLY_MESSAGE;
	        }
	        
	        if(returnedText.length() > 0) {
		        if(isTextMessage) {returnedText = Language.abbreviate(returnedText);}
	    		// send via mobile carrier email address
	    		Emailer.send(fromEmailAddress, "Ack", returnedText, fromUserName);
	        }
		} catch (MessagingException ex) {
			log.severe("Should not happen. Email MessagingException: " + ex.getMessage());
		} catch (Exception ex) {
			log.severe("Should not happen. Email Exception: " + ex.getMessage());
		}
	}
	
	// returns emailReplyToken if found; null otherwise
	private String getEmailReplyToken(String theBody) {
		if(theBody == null) {return null;}
		
		String emailReplyToken = null;
		int startingIndex = theBody.indexOf(RteamApplication.EMAIL_START_TOKEN_MARKER);
		if(startingIndex > -1) {
			int endingIndex = theBody.indexOf(RteamApplication.EMAIL_END_TOKEN_MARKER, startingIndex);
			if(endingIndex > -1) {
				startingIndex = startingIndex + RteamApplication.EMAIL_START_TOKEN_MARKER.length();
				emailReplyToken = theBody.substring(startingIndex, endingIndex);
			}
		}
		
		return emailReplyToken;
	}
	
	private String extractToken(String thePrefix, String theEmailAddress) {
		int beginIndex = thePrefix.length();
		int endIndex = theEmailAddress.indexOf("@");
		return theEmailAddress.substring(beginIndex, endIndex);
	}
	
	
	private String extractPhoneNumber(String theEmailAddress) {
		int endIndex = theEmailAddress.indexOf("@");
		String pn = theEmailAddress.substring(0, endIndex);
		log.info("extracted phone number = " + pn);
		return pn;
	}
}
