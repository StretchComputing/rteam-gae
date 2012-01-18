package com.stretchcom.rteam.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.appengine.api.datastore.KeyFactory;


public class CronResource extends ServerResource {
	//private static final Logger log = Logger.getLogger(CronResource.class.getName());
	private static RskyboxClient log = new RskyboxClient();
	
	String job;

    @Override  
    protected void doInit() throws ResourceException {  
    	log.debug("CronResource::doInit() entered");
    	
        this.job = (String)getRequest().getAttributes().get("job");
        if(this.job != null) {
            this.job = Reference.decode(this.job);
            log.debug("UserResource:doInit() - decoded job = " + this.job);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////
    	// NOTE: Other Resource classes can extract URL embedded query parameters in this doInit() method
    	//       but I was unable to extract parameters in this case which are embedded inside the HTTP frame.
    	//       By the time sendEmail() is called below, the extract works fine. Not sure why this is ....
    	////////////////////////////////////////////////////////////////////////////////////////////////////////
    }  

    @Get  
    public StringRepresentation runCronJob(Form theForm) {
    	JSONObject jsonReturn = new JSONObject();
    	
    	if(this.job != null && this.job.equalsIgnoreCase("messageArchiver")) {
    		runMessageArchiver();
    	} else if(this.job != null && this.job.equalsIgnoreCase("emailSmsIsAlive")) {
    		emailSmsIsAlive();
    	}
    	
    	return new StringRepresentation("success");
    }
    
    private void emailSmsIsAlive() {
    	String response = EmailToSmsClient.isAlive();
    	log.debug("emailSmsIsAlive() response = " + response);
    }
    
    private void runMessageArchiver() {
    	log.debug("runMessageArchiver() entered");
    	
    	CronLog messageArchiverCronLog = null;
    	int numberOfArchivedMessageThreads = 0;
    	int numberOfArchivedRecipients = 0;
    	
    	// MessageThreads that are ACTIVE_STATUS
    	EntityManager emMessageThread = EMF.get().createEntityManager();
    	try {
        	List<MessageThread> oldMessageThreads = (List<MessageThread>)emMessageThread.createNamedQuery("MessageThread.getOldActiveThru")
				.setParameter("currentDate", new Date())
				.setParameter("status", MessageThread.ACTIVE_STATUS)
				.getResultList();
        	log.debug("number of old messageThreads found = " + oldMessageThreads.size());
        	
    		for(MessageThread mt : oldMessageThreads) {
    	    	emMessageThread.getTransaction().begin();
        		MessageThread aMessageThread = (MessageThread)emMessageThread.createNamedQuery("MessageThread.getByKey")
        			.setParameter("key", mt.getKey())
        			.getSingleResult();
    			
    			aMessageThread.setStatus(MessageThread.ARCHIVED_STATUS);
    			emMessageThread.getTransaction().commit();
    		}
    		log.debug("all messageThreads archived successfully");
    		numberOfArchivedMessageThreads = oldMessageThreads.size();
    	} finally {
    		emMessageThread.close();
    	}
    	
    	
    	// Recipients in SENT_STATUS
    	EntityManager emRecipient = EMF.get().createEntityManager();
    	try {
    		List<Recipient> oldRecipients = new ArrayList<Recipient>();
    		
        	List<Recipient> oldSentRecipients = (List<Recipient>)emRecipient.createNamedQuery("Recipient.getOldActiveThru")
				.setParameter("currentDate", new Date())
				.setParameter("status", Recipient.SENT_STATUS)
				.getResultList();
        	log.debug("number of old SENT_STATUS recipients found = " + oldSentRecipients.size());
        	oldRecipients.addAll(oldSentRecipients);
        	
        	List<Recipient> oldRepliedRecipients = (List<Recipient>)emRecipient.createNamedQuery("Recipient.getOldActiveThru")
				.setParameter("currentDate", new Date())
				.setParameter("status", Recipient.REPLIED_STATUS)
				.getResultList();
	    	log.debug("number of old REPLIED_STATUS recipients found = " + oldRepliedRecipients.size());
	    	oldRecipients.addAll(oldRepliedRecipients);
        	
        	List<Recipient> oldFinalizedRecipients = (List<Recipient>)emRecipient.createNamedQuery("Recipient.getOldActiveThru")
				.setParameter("currentDate", new Date())
				.setParameter("status", Recipient.FINALIZED_STATUS)
				.getResultList();
	    	log.debug("number of old FINALIZED_STATUS recipients found = " + oldFinalizedRecipients.size());
	    	oldRecipients.addAll(oldFinalizedRecipients);
       	
    		for(Recipient r : oldRecipients) {
    			emRecipient.getTransaction().begin();
    			Recipient aRecipient = (Recipient)emRecipient.createNamedQuery("Recipient.getByKey")
        			.setParameter("key", r.getKey())
        			.getSingleResult();
    			
    			aRecipient.setStatus(Recipient.ARCHIVED_STATUS);
    			emRecipient.getTransaction().commit();
    		}
    		log.debug("total old recipients archived = " + oldRecipients.size());
    		numberOfArchivedRecipients = oldRecipients.size();
    	} finally {
    		emRecipient.close();
    	}
    	
    	messageArchiverCronLog = new CronLog();
    	messageArchiverCronLog.setJobName("messageArchiver");
    	String logMessage = "Number of old messageThreads archived = " + numberOfArchivedMessageThreads +
    	                    ".  Number of old recipients archived = " + numberOfArchivedRecipients + ".";
    	messageArchiverCronLog.setLogMessage(logMessage);
    	messageArchiverCronLog.setCreatedGmtDate(new Date());
    	
    	EntityManager emCronLog = EMF.get().createEntityManager();
    	try {
    		emCronLog.persist(messageArchiverCronLog);
    	} catch(Exception e) {
			log.exception("CronResource:runMessageArchiver:Exception", "persisting cron Logs", e);
    	} finally {
    		emCronLog.close();
    	}
    }
}
