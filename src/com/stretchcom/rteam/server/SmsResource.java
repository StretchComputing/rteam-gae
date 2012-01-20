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
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;


public class SmsResource extends ServerResource {
	//private static final Logger log = Logger.getLogger(SmsResource.class.getName());
	private static RskyboxClient log = new RskyboxClient();
	
	private static final String SUBSCRIPTION_UPDATE_EVENT = "SUBSCRIPTION_UPDATE";
	private static final String MOBILE_ORIGINATED_EVENT = "MO";
	
    @Override  
    protected void doInit() throws ResourceException {  
    	log.debug("SmsResource::doInit() entered");
    	
    	////////////////////////////////////////////////////////////////////////////////////////////////////////
    	// NOTE: Other Resource classes can extract URL embedded query parameters in this doInit() method
    	//       but I was unable to extract parameters in this case which are embedded inside the HTTP frame.
    	//       By the time sendEmail() is called below, the extract works fine. Not sure why this is ....
    	////////////////////////////////////////////////////////////////////////////////////////////////////////
    }  

	// Handles "callbacks" from Zeep Mobile
    @Post  
    public StringRepresentation receiveText(Form theForm) {
    	JSONObject jsonReturn = new JSONObject();
    	String returnedText = "";
    	
    	log.debug("receiveText(@Post) entered ..... ");
    	
		String event = theForm.getFirstValue("event");
		log.debug("event parameter: " + event);
		
		if(event == null) {
			log.error("SmsResource:receiveTest:", "no event parameter present");
			return new StringRepresentation("unexpected ZeepMobile error");
		}
		
		String uid = theForm.getFirstValue("uid");
		log.debug("uid parameter: " + uid);

		if(event.equalsIgnoreCase(SUBSCRIPTION_UPDATE_EVENT)) {
			// SMS way of confirming membership.
	    	
			// in testing, the 'uid' is null and 'min' is the 10 digit phone number preceded by a 1
			String min = theForm.getFirstValue("min");
			log.debug("min parameter: " + min);
			min = Utility.extractAllDigits(min);
			log.debug("min digits only = " + min);
			min = stripLeadingOne(min);
			log.debug("calling Member.confirmNewMemberViaSms with min = " + min);
			returnedText = Member.confirmNewMemberViaSms(min);
		} else if(event.equalsIgnoreCase(MOBILE_ORIGINATED_EVENT)) {
			if(uid == null) {
				log.error("SmsResource:receivetText:unsubscribedZeepMobile", "member is still considered unsubscribed by zeepMobile");
				return new StringRepresentation("You must subscribe first by sending 'join rteam' to 88147");
			} else {
				String body = theForm.getFirstValue("body");
				log.debug("body parameter: " + body);
				uid = Utility.extractAllDigits(uid);
				log.debug("uid digits only = " + uid);
				uid = stripLeadingOne(uid);
				
				body = body.toLowerCase();
				if(body.contains("confirm")) {
					returnedText = Recipient.handleSmsResponse(uid, null);
				} else {
					// must be a Poll response
					returnedText = Recipient.handleSmsResponse(uid, body);
				}
			}
		}
		
		return new StringRepresentation(returnedText);
    }
    
    // strips the first character if it is a "1" and returns the shortened mobile number
    private String stripLeadingOne(String theMobileNumber) {
    	log.debug("stripLeadingOne(): theMobileNumber passed in = " + theMobileNumber);
    	if(theMobileNumber.startsWith("1")) {
    		return theMobileNumber.substring(1);
    	}
    	return theMobileNumber;
    }
}
