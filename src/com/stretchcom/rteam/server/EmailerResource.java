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
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;


///////////////////////////////////////////////////
// NOT BEING USED - SEE EmailTaskServlet instead //
///////////////////////////////////////////////////

public class EmailerResource extends ServerResource {
	private static final Logger log = Logger.getLogger(EmailerResource.class.getName());

    @Override  
    protected void doInit() throws ResourceException {  
    	log.info("EmailerResource::doInit() entered");
    	
    	////////////////////////////////////////////////////////////////////////////////////////////////////////
    	// NOTE: Other Resource classes can extract URL embedded query parameters in this doInit() method
    	//       but I was unable to extract parameters in this case which are embedded inside the HTTP frame.
    	//       By the time sendEmail() is called below, the extract works fine. Not sure why this is ....
    	////////////////////////////////////////////////////////////////////////////////////////////////////////
    }  

	// Called by task queue to send a single email  
    @Post  
    public void sendEmail(Form theForm) {
    	log.info("sendEmail(@Post) entered ..... ");
    	
		String emailAddress = theForm.getFirstValue("emailAddress");
		log.info("emailAddress parameter: "	+ emailAddress);
		String fromEmailAddress = theForm.getFirstValue("fromEmailAddress");
		log.info("fromEmailAddress parameter: "	+ fromEmailAddress);
		String fromEmailUser = theForm.getFirstValue("fromEmailUser");
		log.info("fromEmailUser parameter: "	+ fromEmailUser);
		String subject = theForm.getFirstValue("subject");
		log.info("subject parameter: "	+ subject);
		String message = theForm.getFirstValue("message");
		log.info("message parameter: "	+ message);
		
		this.setStatus(Status.SUCCESS_CREATED);
	    Properties props = new Properties();
	    Session session = Session.getDefaultInstance(props, null);
	    
	    // ensure valid parameters
	    if(emailAddress == null || emailAddress.length() == 0 ||
	    		subject == null || subject.length() == 0 ||
	    		message == null || message.length() == 0) {
	    	log.severe("Emailer.send(): null or empty parameter");
	    	return;
	    }

	    try {
	        Message msg = new MimeMessage(session);
	        msg.setFrom(new InternetAddress(fromEmailAddress, fromEmailUser));
	        msg.addRecipient(Message.RecipientType.TO, new InternetAddress(emailAddress));
	        //::TODO:: only for testing **********************************************************
	        msg.addRecipient(Message.RecipientType.TO, new InternetAddress("joepwro@gmail.com"));
	        msg.addRecipient(Message.RecipientType.TO, new InternetAddress("njw438@gmail.com"));
	        //************************************************************************************
	        msg.setSubject(subject);
	        msg.setContent(message, "text/html");
	        log.info("sending email to: " + emailAddress + " with subject: " + subject);
	        Transport.send(msg);

	    } catch (AddressException e) {
	        log.severe("email Address exception " + e.getMessage());
	    } catch (MessagingException e) {
	    	log.severe("email had bad message: " + e.getMessage());
	    } catch (UnsupportedEncodingException e) {
	    	log.severe("email address with unsupported format "  + e.getMessage());
	    } catch (Exception e) {
	    	// Received com.google.apphosting.api.ApiProxy$ApiDeadlineExceededException a couple of times.
	    	// Not sure why this is happening since Transport.send() is supposed to be a asynchronous call.
	    	log.severe("email address exception "  + e.getMessage());
	    	e.printStackTrace();
	    }
		return;
    }

}
