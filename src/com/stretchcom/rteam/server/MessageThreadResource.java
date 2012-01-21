package com.stretchcom.rteam.server;
	
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ServerResource;
import org.restlet.resource.ResourceException;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

/**
 * @author joepwro
 */
public class MessageThreadResource extends ServerResource {
	//private static final Logger log = Logger.getLogger(MessageThreadResource.class.getName());
	private RskyboxClient log = new RskyboxClient(this);
	private static RskyboxClient slog = new RskyboxClient();

    // The sequence of characters that identifies the resource.
    String teamId;
    String messageThreadId;
    String timeZoneStr;
    String includeMemberInfo;
    String oneUseToken;
    String pollResponse;
    
    @Override  
    protected void doInit() throws ResourceException {  
    	log.info("API requested with URL: " + this.getReference().toString());
    	
        // attribute values taken from the URI template /team/{teamId}/messageThreads/{messageThreadId}/{timeZone}
    	
        this.teamId = (String)getRequest().getAttributes().get("teamId"); 
        log.debug("MessageThreadResource:doInit() - teamId = " + this.teamId);
        if(this.teamId != null) {
            this.teamId = Reference.decode(this.teamId);
            log.debug("MessageThreadResource:doInit() - decoded teamId = " + this.teamId);
        }
   
        this.messageThreadId = (String)getRequest().getAttributes().get("messageThreadId"); 
        log.debug("MessageThreadResource:doInit() - messageThreadId = " + this.messageThreadId);
        if(this.messageThreadId != null) {
            this.messageThreadId = Reference.decode(this.messageThreadId);
            log.debug("MessageThreadResource:doInit() - decoded messageThreadId = " + this.messageThreadId);
        }
        
        this.timeZoneStr = (String)getRequest().getAttributes().get("timeZone"); 
        log.debug("MessageThreadResource:doInit() - timeZone = " + this.timeZoneStr);
        if(this.timeZoneStr != null) {
            this.timeZoneStr = Reference.decode(this.timeZoneStr);
            log.debug("MessageThreadResource:doInit() - decoded timeZone = " + this.timeZoneStr);
        }
        
		Form form = getRequest().getResourceRef().getQueryAsForm();
		for (Parameter parameter : form) {
			log.debug("parameter " + parameter.getName() + " = " + parameter.getValue());
			if(parameter.getName().equals("includeMemberInfo"))  {
				this.includeMemberInfo = (String)parameter.getValue();
				this.includeMemberInfo = Reference.decode(this.includeMemberInfo);
				log.debug("MessageThreadResource:doInit() - decoded includeMemberInfo = " + this.includeMemberInfo);
			} else if(parameter.getName().equals("oneUseToken")) {
				this.oneUseToken = (String)parameter.getValue();
				this.oneUseToken = Reference.decode(this.oneUseToken);
				log.debug("MessageThreadResource:doInit() - decoded oneUseToken = " + this.oneUseToken);
			} else if(parameter.getName().equals("pollResponse")) {
				this.pollResponse = (String)parameter.getValue();
				this.pollResponse = Reference.decode(this.pollResponse);
				log.debug("MessageThreadResource:doInit() - decoded pollResponse = " + this.pollResponse);
			}
		}
    }  

    // Handles 'Get message thread info' API  
    @Get("json")
    public JsonRepresentation getMessageThreadInfo(Variant variant) {
        log.debug("MessageThreadResource:toJson() entered");
        JSONObject jsonReturn = new JSONObject();
		EntityManager em = EMF.get().createEntityManager();

		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		User currentUser = null;
		TimeZone tz = null;
		try {
    		currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
    			this.setStatus(Status.SERVER_ERROR_INTERNAL);
				log.error("MessageThreadResource:getMessageThreadInfo:currentUser", "user could not be retrieved from Request attributes!!");
    		}
    		//::BUSINESSRULE:: user must be network authenticated to send a message
    		else if(!currentUser.getIsNetworkAuthenticated()) {
    			apiStatus = ApiStatusCode.USER_NOT_NETWORK_AUTHENTICATED;
    		}
    		// teamId, messageThreadId and time zone are all required
    		else if(this.teamId == null || this.teamId.length() == 0 ||
    	        	   this.messageThreadId == null || this.messageThreadId.length() == 0 ||
    	        	   this.timeZoneStr == null || this.timeZoneStr.length() == 0) {
    	        		log.debug("MessageThreadResource:toJson() teamId, messageThreadId or timeZone null or zero length");
    	        		apiStatus = ApiStatusCode.TEAM_ID_MESSAGE_THREAD_ID_AND_TIME_ZONE_REQUIRED;
    	    }
    		// must be a member of the team
    		else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
				log.debug(apiStatus);
        	} else {
    			tz = GMT.getTimeZone(this.timeZoneStr);
    			if(tz == null) {
    				apiStatus = ApiStatusCode.INVALID_TIME_ZONE_PARAMETER;
    			}
        	}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}

    		Key messageThreadKey = KeyFactory.stringToKey(this.messageThreadId);
    		MessageThread messageThread = (MessageThread)em.createNamedQuery("MessageThread.getByKey")
    			.setParameter("key", messageThreadKey)
    			.getSingleResult();
    		log.debug("messageThread retrieved = " + messageThread.getSubject());
        	
        	jsonReturn.put("subject", messageThread.getSubject());
        	jsonReturn.put("body", messageThread.getMessage());
        	jsonReturn.put("type", messageThread.getType());
        	
        	if(messageThread.getEventId() !=  null) {
        		jsonReturn.put("eventId", messageThread.getEventId());
        		jsonReturn.put("eventType", messageThread.getIsGame() ? Game.GAME : Practice.PRACTICE);	
        	}
        	
    		JSONArray jsonPollChoices = new JSONArray();
    		for(String s : messageThread.getPollChoices()) {
    			jsonPollChoices.put(s);
    		}
    		jsonReturn.put("pollChoices", jsonPollChoices);
    		
    		if(this.includeMemberInfo != null && this.includeMemberInfo.equalsIgnoreCase("true"))  {
	    		JSONArray jsonMemberArray = new JSONArray();
	    		List<Recipient> recipients = messageThread.getRecipients();
	    		log.debug("# of recipients = " + recipients.size());
	    		// go through recipient list and find a single reply disposition for each member
	    		HashMap<String, Recipient> uniqueMemberRecipients = new HashMap<String, Recipient>();
	    		String currentUserId = KeyFactory.keyToString(currentUser.getKey());
	    		for(Recipient r : recipients) {
    				if(r.getUserId() != null && r.getUserId().equals(currentUserId)) {
    					r.setBelongToUser(true);
    				}
    				
	    			Recipient memberRecipient = uniqueMemberRecipients.get(r.getMemberId());
	    			if(memberRecipient == null) {
	    				// no recipient associated with this member ID in the list, so add it
	    				uniqueMemberRecipients.put(r.getMemberId(), r);
	    			} else if(memberRecipient.getReply() == null && r.getReply() != null) {
    					// replace the recipient in the list with no reply with this one that has a reply
	    				if(memberRecipient.getBelongToUser()) {
	    					r.setBelongToUser(true);
	    				}
    					uniqueMemberRecipients.remove(r.getMemberId());
    					uniqueMemberRecipients.put(r.getMemberId(), r);
	    			}
	    		}
	    		
	    		Collection<Recipient> uniqueMemberCollection = uniqueMemberRecipients.values();
	    		for(Recipient r : uniqueMemberCollection) {
	    			JSONObject jsonMemberObj = new JSONObject();
	    			Boolean belongsToUserReply = r.getBelongToUser() == null ? false : r.getBelongToUser();
	    			jsonMemberObj.put("belongsToUser", belongsToUserReply);
	    			jsonMemberObj.put("memberId", r.getMemberId());
	    			jsonMemberObj.put("memberName", r.getMemberName());
	    			
	    			if(r.getReply() != null) {
	    				String reply = "private";
	    				// send real reply if messageThread is public or if private and this user is the messageThread originator
	    				if(messageThread.getIsPublic() || 
	    					(!messageThread.getIsPublic() && messageThread.getSenderUserId().equals(KeyFactory.keyToString(currentUser.getKey())) )) {
	    					reply = r.getReply();
	    				}
	    				jsonMemberObj.put("reply", reply);
	    			}
	    			if(r.getReplyEmailAddress() != null) jsonMemberObj.put("replyEmailAddress", r.getReplyEmailAddress());
	    			if(r.getReplyGmtDate() != null) jsonMemberObj.put("replyDate", GMT.convertToLocalDate(r.getReplyGmtDate(), tz));
	    			jsonMemberArray.put(jsonMemberObj);
	    		}
	    		jsonReturn.put("members", jsonMemberArray);
    		}
    		
    		jsonReturn.put("isAlert", messageThread.getIsAlert() ? "true" : "false");
    		jsonReturn.put("createdDate", GMT.convertToLocalDate(messageThread.getCreatedGmtDate(), tz));
    		if(messageThread.getFinalizedGmtDate() != null) jsonReturn.put("finalizedDate", GMT.convertToLocalDate(messageThread.getFinalizedGmtDate(), tz));
    		jsonReturn.put("status", messageThread.getStatus());
    		if(messageThread.getFollowupMessage() !=  null) jsonReturn.put("followUpMessage", messageThread.getFollowupMessage());
    		jsonReturn.put("isPublic", messageThread.getIsPublic() ? "true" : "false");
        } catch (NoResultException e) {
        	log.debug("no result exception, messageThread not found");
        	apiStatus = ApiStatusCode.MESSAGE_THREAD_NOT_FOUND;
		} catch (JSONException e) {
			log.exception("MessageThreadResource:getMessageThreadInfo:JSONException1", "", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (NonUniqueResultException e) {
			log.exception("MessageThreadResource:getMessageThreadInfo:NonUniqueResultException", "", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} finally {
			em.close();
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("MessageThreadResource:getMessageThreadInfo:JSONException2", "", e);
		}
        return new JsonRepresentation(jsonReturn);
    }

    // Handles 'Update message thread' API
    // Handles 'Message thread response' API (for GWT)
    @Put 
    public JsonRepresentation updateMessageThread(Representation entity) {
    	JSONObject jsonReturn = new JSONObject();
    	log.debug("updateMessageThread(@Put) entered ..... ");
		EntityManager em = EMF.get().createEntityManager();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		User currentUser = null;
        try {
        	if(this.oneUseToken != null) {
    			// --------------------------------------------------
    			// This is the 'Message thread response' API call
    			// --------------------------------------------------
        		//::EVENT::
        		log.debug("this is 'Message thread response' API handling");
        		
        		Recipient recipient = null;
        		MessageThread messageThread = null;
        		try {
        			em.getTransaction().begin();
        			recipient = (Recipient)em.createNamedQuery("Recipient.getByOneUseTokenAndTokenStatus")
        				.setParameter("oneUseToken", this.oneUseToken)
        				.setParameter("oneUseTokenStatus", Recipient.NEW_TOKEN_STATUS)
        				.getSingleResult();
        			log.debug("updateMessageThread(): recipient found");
        			
        			String userReply = Recipient.CONFIRMED_REPLY_MESSAGE;
        			if(this.pollResponse != null) {
        				userReply = this.pollResponse;
        			}
        			
        			// This is a "first" response wins scenario. So mark other individuals that are part of this same
        			// membership as having replied.
        			messageThread = recipient.getMessageThread();
        			List<Recipient> recipients = messageThread.getRecipients();
        			for(Recipient r : recipients) {
        				if(recipient.getMemberId().equals(r.getMemberId())) {
        					//::TRIAL::PERIOD::
        					// By not changing the token status to USED, an email participant can reply multiple times, thus changing their answer
        					// if it is a poll. If it is a confirm, no harm done to confirm multiple times. Try this and see how it works out
            				//r.setOneUseTokenStatus(Recipient.USED_TOKEN_STATUS);
            				r.setReply(userReply);
            				r.setReplyGmtDate(new Date());
            				r.setReplyEmailAddress(recipient.getToEmailAddress());
            				r.setStatus(Recipient.REPLIED_STATUS);
        				}
        			}
        			messageThread.addMemberIdThatReplied(recipient.getMemberId());

					em.getTransaction().commit();
					
					// who's coming reply has to update the pre-game attendance
					if(messageThread.getType().equalsIgnoreCase(MessageThread.WHO_IS_COMING_TYPE)) {
						log.debug("who's coming reply = " + userReply);
						String eventType = Utility.getEventType(recipient.getIsGame());
						// ::BACKWARD COMPATIBILITY:: recipient started storing eventName on 1/5/2012 so handle null eventName for awhile
						String eventName = recipient.getEventName() == null ? "" : recipient.getEventName();
						
						// update pre-event attendee status or create attendee if it doesn't exist yet for this event/member combination
						Attendee.updatePreGameAttendance(recipient.getEventId(), eventType, recipient.getMemberId(),
								recipient.getTeamId(), recipient.getEventGmtStartDate(), eventName, userReply);
					}
        		} catch (NoResultException e) {
        			// Not an error - multiple people associated with the same membership may respond. Or, the same user may click
        			// on the link in the message multiple times.  In either case, it is inactive after first response.
        			apiStatus = ApiStatusCode.MESSAGE_THREAD_CONFIRMATION_LINK_NO_LONGER_ACTIVE;
        		} catch (NonUniqueResultException e) {
        			log.exception("MessageThreadResource:updateMessageThread:NonUniqueResultException1", "", e);
        			this.setStatus(Status.SERVER_ERROR_INTERNAL);
        		}
        		// try/catch at end of method does entity manager cleanup
        		// TODO maybe move entity manager cleanup here for better code clarity
        		
    			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
    				jsonReturn.put("apiStatus", apiStatus);
    				return new JsonRepresentation(jsonReturn);
    			}

    			EntityManager em2 = EMF.get().createEntityManager();
    			Member memberConfirming = null;
	    		String memberId = null;
    	    	try {
    				Key memberKey = KeyFactory.stringToKey(recipient.getMemberId());
    				memberConfirming = (Member)em2.createNamedQuery("Member.getByKey")
    	    			.setParameter("key", memberKey)
    	    			.getSingleResult();
    	    		log.debug("member confirming = " + memberConfirming.getFullName());
    	    		memberId = KeyFactory.keyToString(memberConfirming.getKey());
    	    		
                	// TODO eventually return -- for all members -- who has and hasn't confirmed so far
                	jsonReturn.put("firstName", memberConfirming.getFirstName());
        			jsonReturn.put("lastName", memberConfirming.getLastName());
    	    	} catch (NoResultException e) {
    				this.setStatus(Status.SERVER_ERROR_INTERNAL);
        			log.exception("MessageThreadResource:updateMessageThread:NoResultException", "", e);
    			} catch (NonUniqueResultException e) {
    				this.setStatus(Status.SERVER_ERROR_INTERNAL);
        			log.exception("MessageThreadResource:updateMessageThread:NonUniqueResultException2", "two or more members have same member ID", e);
    			} finally {
        		    em2.close();
        		}
        		
    			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
    				jsonReturn.put("apiStatus", apiStatus);
    				return new JsonRepresentation(jsonReturn);
    			}
        		
            	jsonReturn.put("subject", messageThread.getSubject());
            	jsonReturn.put("body", messageThread.getMessage());
            	
        		JSONArray jsonPollChoices = new JSONArray();
        		for(String s : messageThread.getPollChoices()) {
        			jsonPollChoices.put(s);
        		}
        		jsonReturn.put("pollChoices", jsonPollChoices);
        		
	    		JSONArray jsonMemberArray = new JSONArray();
	    		List<Recipient> recipients = messageThread.getRecipients();
	    		log.debug("# of recipients = " + recipients.size());
	    		// go through recipient list and find a single reply disposition for each member
	    		HashMap<String, Recipient> uniqueMemberRecipients = new HashMap<String, Recipient>();
	    		for(Recipient r : recipients) {
	    			Recipient memberRecipient = uniqueMemberRecipients.get(r.getMemberId());
	    			if(memberRecipient == null) {
	    				// no recipient associated with this member ID in the list, so add it
	    				uniqueMemberRecipients.put(r.getMemberId(), r);
	    			} else if(memberRecipient.getReply() == null && r.getReply() != null) {
    					// replace the recipient in the list with no reply with this one that has a reply
    					uniqueMemberRecipients.remove(r.getMemberId());
    					uniqueMemberRecipients.put(r.getMemberId(), r);
	    			}
	    		}
	    		
	    		Collection<Recipient> uniqueMemberCollection = uniqueMemberRecipients.values();
	    		for(Recipient r : uniqueMemberCollection) {
	    			JSONObject jsonMemberObj = new JSONObject();
	    			Boolean belongsToMemberReply = memberId.equals(r.getMemberId());
	    			jsonMemberObj.put("belongsToMember", belongsToMemberReply);
	    			jsonMemberObj.put("memberId", r.getMemberId());
	    			jsonMemberObj.put("memberName", r.getMemberName());
	    			if(r.getReply() != null) jsonMemberObj.put("reply", r.getReply());
	    			if(r.getReplyEmailAddress() != null) jsonMemberObj.put("replyEmailAddress", r.getReplyEmailAddress());
	    			// TODO need to figure out a way to know the member's timezone to return the replyDate
	    			//if(r.getReplyGmtDate() != null) jsonMemberObj.put("replyDate", GMT.convertToLocalDate(r.getReplyGmtDate(), tz));
	    			jsonMemberArray.put(jsonMemberObj);
	    		}
	    		log.debug("uniqueMemberCollection size = " + uniqueMemberCollection.size());
	    		jsonReturn.put("members", jsonMemberArray);
        	} else {
    			// --------------------------------------------
    			// This is the 'Update message thread' API call
    			// --------------------------------------------
        		log.debug("this is 'Update message thread' API handling");
        		currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
        		if(currentUser == null) {
        			this.setStatus(Status.SERVER_ERROR_INTERNAL);
        			log.error("MessageThreadResource:updateMessageThread:currentUser", "user could not be retrieved from Request attributes!!");
        		}
        		//::BUSINESSRULE:: user must be network authenticated to update a message thread
        		else if(!currentUser.getIsNetworkAuthenticated()) {
        			apiStatus = ApiStatusCode.USER_NOT_NETWORK_AUTHENTICATED;
        		}
        		// teamId, messageThreadId and time zone are all required
        		else if(this.teamId == null || this.teamId.length() == 0 ||
        	        	   this.messageThreadId == null || this.messageThreadId.length() == 0) {
        	        		log.debug("MessageThreadResource:toJson() teamId or messageThreadId null or zero length");
        	        		apiStatus = ApiStatusCode.TEAM_ID_AND_MESSAGE_THREAD_ID_REQUIRED;
        	    }
        		// must be a member of the team
        		else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
    				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
    				log.debug(apiStatus);
            	} 
        		
    			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
    				jsonReturn.put("apiStatus", apiStatus);
    				return new JsonRepresentation(jsonReturn);
    			}

    			JsonRepresentation jsonRep = new JsonRepresentation(entity);
    			JSONObject json = jsonRep.toJsonObject();
    			log.debug("received json object = " + json.toString());
            	
    			// enforce the query parameter defaults and rules
    			// sendReminder, reply and followupMessage are all mutually exclusive
    			int mutualExclusiveCount = 0;
    			if(json.has("sendReminder")) {mutualExclusiveCount++;}
    			if(json.has("reply")) {mutualExclusiveCount++;}
    			if(json.has("followupMessage")) {mutualExclusiveCount++;}
    			if(json.has("status")) {mutualExclusiveCount++;}
    			if(mutualExclusiveCount > 1) {
                    log.debug("MessageThreadResource:toJson() can only specify one of: sendReminder, reply, followupMessage, status");
    				jsonReturn.put("apiStatus", ApiStatusCode.MUTUALLY_EXCLUSIVE_PARAMETERS_SPECIFIED);
    				return new JsonRepresentation(jsonReturn);
    			}
    			
    			Boolean wasViewed = null;
    			if(json.has("wasViewed")) {
    				String wasViewedStr = json.getString("wasViewed");
    				if(wasViewedStr.equalsIgnoreCase("true")) {
    					wasViewed = true;
    				} else if(wasViewedStr.equalsIgnoreCase("false")) {
    					wasViewed = false;
    				} else {
    	                log.debug("MessageThreadResource:toJson() wasViewed must be 'true' or 'false'");
    	 				jsonReturn.put("apiStatus", ApiStatusCode.INVALID_WAS_VIEWED_PARAMETER);
    					return new JsonRepresentation(jsonReturn);
    				}
    			}
    			
    			if(json.has("status")) {
    				String status = json.getString("status");
    				if(!status.equalsIgnoreCase(MessageThread.ARCHIVED_STATUS)) {
    	                log.debug("MessageThreadResource:toJson() status did not have a value of 'archived'");
    	 				jsonReturn.put("apiStatus", ApiStatusCode.INVALID_STATUS_PARAMETER);
    					return new JsonRepresentation(jsonReturn);
    				}
    			}
    			
    			Team team = null;
    			try {
    				Key teamKey = KeyFactory.stringToKey(this.teamId);
            		team = (Team)em.createNamedQuery("Team.getByKey")
    					.setParameter("key", teamKey)
    					.getSingleResult();
        			
				} catch (NoResultException e) {
					log.debug("team not found");
					apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
				} catch (NonUniqueResultException e) {
        			log.exception("MessageThreadResource:updateMessageThread:NonUniqueResultException3", "", e);
					this.setStatus(Status.SERVER_ERROR_INTERNAL);
				}
    			
    			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
    				jsonReturn.put("apiStatus", apiStatus);
    				return new JsonRepresentation(jsonReturn);
    			}

				// member only needed if need to verify request came from a recipient
    			List<Member> memberships = null;
    			if(json.has("reply") || json.has("wasViewed") || json.has("status")) {
            		memberships = Member.getMemberShipsWithEmailAddress(currentUser.getEmailAddress(), team);
            		for(Member m : memberships) {
            			log.debug("primary name of matching membership = " + m.getFullName());
            		}
            		if(memberships.size() == 0) {
    					log.debug("requesting user is not of member of the team. Must be team member.");
    					apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
            		}
    			}
    			
    			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
    				jsonReturn.put("apiStatus", apiStatus);
    				return new JsonRepresentation(jsonReturn);
    			}

    			em.getTransaction().begin();
        		Key messageThreadKey = KeyFactory.stringToKey(this.messageThreadId);
        		MessageThread messageThread = (MessageThread)em.createNamedQuery("MessageThread.getByKey")
        			.setParameter("key", messageThreadKey)
        			.getSingleResult();
        		log.debug("messageThread retrieved = " + messageThread.getSubject());
        		
    			// for some updated message threads, user must be a recipient, so if appropriate, try to find
    			Recipient recipientOfThisUser = null;
        		if(json.has("reply") || json.has("wasViewed") || json.has("status")) {
        			List<Recipient> recipients = messageThread.getRecipients();
        			outerLoop:for(Recipient r: recipients) {
        				for(Member m : memberships) {
            				if(r.getToEmailAddress().equalsIgnoreCase(currentUser.getEmailAddress()) &&
            						r.getMemberId().equals(KeyFactory.keyToString(m.getKey()))) {
            					recipientOfThisUser = r;
            					break outerLoop;
            				}
        				}
        			}
        		}
    			
    			List<String> memberIdsToBeNotified = new ArrayList<String>();
    			String reply = "";
    			String followupMessage = "";
    			String reminderMessage = "";
    		    Boolean wasWhoIsComingReply = false;
    			if(json.has("sendReminder")) {
    				// user sending reminder must be the originator of this message thread
    				if(messageThread.getSenderUserId().equals(KeyFactory.keyToString(currentUser.getKey()))) {
    					
    					JSONArray recipientsJsonArray = json.getJSONArray("sendReminder");
    					int numOfJsonRecipients = recipientsJsonArray.length();
    					log.debug("json recipients array length = " + numOfJsonRecipients);
    					
    					boolean memberIdsProvided = numOfJsonRecipients == 0 ? false : true;
    					
    					List<Recipient> recipients = messageThread.getRecipients();
    					for(Recipient r: recipients) {
    						boolean memberToBeNotified = false;
    						if(memberIdsProvided) {
    							// see if this recipient is one of the members provided/specified
    	    					for(int i=0; i<numOfJsonRecipients; i++) {
    								if(recipientsJsonArray.getString(i).equals(r.getMemberId())) {
    									// will only be notified if they have NOT yet replied to the poll/confirm message
    									if(r.isPendingReply()) {
        									memberIdsToBeNotified.add(r.getMemberId());
        									memberToBeNotified = true;
    									}
    									break;
    								}
    	    					}
    							
    						} else {
    							// No members provided, so build memberIds list to include ALL recipients in messageThread that have NOT yet replied
    							if(r.isPendingReply()) {
        							memberIdsToBeNotified.add(r.getMemberId());
        							memberToBeNotified = true;
    							}
    						}
    						
    						// only update recipient if member is to receive the reminder message
    						// updating recipient will make poll show up for rTeam user again
    						if(memberToBeNotified == true) {
    							int numOfSends = r.getNumOfSends();
    							numOfSends++;
    							r.setNumOfSends(numOfSends);
    							r.setWasViewed(false);
    							r.setReceivedGmtDate(new Date());
    							r.setOneUseTokenStatus(Recipient.NEW_TOKEN_STATUS);
    						}
    					}
    					
    					reminderMessage = messageThread.getMessage();
    				} else {
    					apiStatus = ApiStatusCode.USER_NOT_ORIGINATOR_OF_MESSAGE_THREAD;
    					log.debug("requester is not the originator of this message thread -- cannot send reminder unless you are the originator");
    				}
    			} else if(json.has("reply")) {
    				reply = json.getString("reply");
    				
    				if(recipientOfThisUser != null) {
    					recipientOfThisUser.setReply(reply);
    					recipientOfThisUser.setStatus(Recipient.REPLIED_STATUS);
    					recipientOfThisUser.setReplyGmtDate(new Date());
    					messageThread.addMemberIdThatReplied(recipientOfThisUser.getMemberId());
    					
    					// who's coming reply has to update the pre-game attendance
    					if(messageThread.getType().equalsIgnoreCase(MessageThread.WHO_IS_COMING_TYPE)) {
    						log.debug("who's coming reply = " + reply);
    						wasWhoIsComingReply = true;
    					}
    				} else {
    					apiStatus = ApiStatusCode.USER_NOT_RECIPIENT_OF_MESSAGE_THREAD;
    					log.debug("requester is not a recipient of this message thread -- cannot reply unless you are a recipient");
    				}
    			} else if(json.has("followupMessage")) {
    				// cannot send a follow-up message if messageThread is already finalized
    				if(messageThread.getStatus().equalsIgnoreCase(MessageThread.FINALIZED_STATUS)) {
    					apiStatus = ApiStatusCode.FOLLOWUP_NOT_ALLOWED_ON_FINALIZED_MESSAGE_THREAD;
    					log.debug("messageThread already finalized. Followup message is not permitted.");
    				} else if(!messageThread.getSenderUserId().equals(KeyFactory.keyToString(currentUser.getKey()))) {
    					apiStatus = ApiStatusCode.USER_NOT_ORIGINATOR_OF_MESSAGE_THREAD;
    					log.debug("requester is not the originator of this message thread");
    				} else {
    					followupMessage = json.getString("followupMessage");
    					messageThread.setFollowupMessage(followupMessage);
    					messageThread.setStatus(MessageThread.FINALIZED_STATUS);
    					messageThread.setFinalizedGmtDate(new Date());
    					
    					List<Recipient> recipients = messageThread.getRecipients();
    					for(Recipient r: recipients) {
    						r.setReceivedGmtDate(new Date());
    						r.setFollowupMessage(followupMessage);
    						r.setStatus(MessageThread.FINALIZED_STATUS);
    						r.setWasViewed(false);
    						memberIdsToBeNotified.add(r.getMemberId());
    					}
    				}
    			} else if(json.has("status")) {
    				if(recipientOfThisUser == null && !messageThread.getSenderUserId().equals(KeyFactory.keyToString(currentUser.getKey()))) {
    					apiStatus = ApiStatusCode.USER_NOT_ORIGINATOR_OR_RECIPIENT_OF_MESSAGE_THREAD;
    					log.debug("user is neither the originator or recipient of this message thread");
    				} else {
    					if(recipientOfThisUser != null) {
    						// user is a recipient, so archive message out of inbox
    						recipientOfThisUser.setStatus(Recipient.ARCHIVED_STATUS);
    					} else {
    						// user is originator, so archive message out of sent messages
    						messageThread.setStatus(MessageThread.ARCHIVED_STATUS);
    					}
    				}
    			}
    			
    			// wasViewed is ignored if this is also a reminder or a follow-up message
    			if(json.has("wasViewed") && !json.has("sendReminder") && !json.has("followupMessage") ) {
    				if(recipientOfThisUser != null) {
    					Boolean oldWasViewed = recipientOfThisUser.getWasViewed();
    					recipientOfThisUser.setWasViewed(wasViewed);
    					// for a user, viewing is also a confirmation if this is a messageThread requesting confirmation
    					if(messageThread.isConfirm()) {
        					messageThread.addMemberIdThatReplied(recipientOfThisUser.getMemberId());
        					recipientOfThisUser.setReplyGmtDate(new Date());
    					}
    				} else {
    					apiStatus = ApiStatusCode.USER_NOT_RECIPIENT_OF_MESSAGE_THREAD;
    					log.debug("requester is not a recipient of this message thread -- cannot change view status unless you are a recipient");
    				}
    			}
    			
    			// Need to commit before sending messages below.
    			em.getTransaction().commit();
    			
    			if(wasWhoIsComingReply) {
					String eventType = Utility.getEventType(recipientOfThisUser.getIsGame());
					// ::BACKWARD COMPATIBILITY:: recipient started storing eventName on 1/5/2012 so handle null eventName for awhile
					String eventName = recipientOfThisUser.getEventName() == null ? "" : recipientOfThisUser.getEventName();
					
					// update pre-event attendee status or create attendee if it doesn't exist yet for this event/member combination
					Attendee.updatePreGameAttendance(recipientOfThisUser.getEventId(), eventType, recipientOfThisUser.getMemberId(),
							recipientOfThisUser.getTeamId(), recipientOfThisUser.getEventGmtStartDate(), eventName, reply);
    			}
    			
    			// send followup message to recipients if appropriate
    			// TODO right now, iPhone returning "none" when user does not enter followup message
    			if(reminderMessage.length() > 0 || (followupMessage.length() > 0 && !followupMessage.equalsIgnoreCase("none")) ) {
    				// convert memberIds to keys
    				Set<Key> memberKeys = new HashSet<Key>();
    				for(String mId : memberIdsToBeNotified) {
    					memberKeys.add(KeyFactory.stringToKey(mId));
    				}
    				
    				try {
    		    		List<Member> members = null;
    		    		if(memberKeys.size() > 0) {
    		    			members = (List<Member>)em.createQuery("select from " + Member.class.getName() + " where key = :keys")
    		    	    		.setParameter("keys", memberKeys)
    		    	    		.getResultList();
    		    			
    		    			List<UserMemberInfo> authorizedTeamRecipients = new ArrayList<UserMemberInfo>();
    		    			////////////////////////////////
    		    			// Build Raw Team Recipient List
    		    			////////////////////////////////
    		    			for(Member m : members) {
    		    				List<UserMemberInfo> authorizedMembershipRecipients = m.getAuthorizedRecipients(m.getTeam());
    		    				for(UserMemberInfo umi : authorizedMembershipRecipients) {
    		    					// filter out current user
    		    					if( (umi.getEmailAddress() != null && umi.getEmailAddress().equalsIgnoreCase(currentUser.getEmailAddress())) ||
    		    						 (umi.getPhoneNumber() != null && currentUser.getPhoneNumber() != null && umi.getPhoneNumber().equals(currentUser.getPhoneNumber()))  ) {
    		    						continue;
    		    					}
    		    						
    		    					// need to know associated member for loop below
    		    					umi.setMember(m);
    		    					umi.setOneUseToken(TF.get());
    		    					umi.setOneUseSmsToken(umi.getPhoneNumber()); // could be null
    		    					authorizedTeamRecipients.add(umi);
    		    				}
    		    			} 
        		    		
		    				///////////////////////////////////////////////////////////////////////////////////////
		    				// Filter Team Recipient List
		    				//      All entities in returned list have unique: emailAddress, userId and phoneNumber
		    				///////////////////////////////////////////////////////////////////////////////////////
		    				authorizedTeamRecipients = UserMemberInfo.filterDuplicates(authorizedTeamRecipients);

		    				if(reminderMessage.length() > 0) {
			    				PubHub.sendMessageThreadToMembers(authorizedTeamRecipients, messageThread.getSubject(), reminderMessage, messageThread, team, false, currentUser.getFullName());
		    				} else {
			    				PubHub.sendMessageThreadToMembers(authorizedTeamRecipients, messageThread.getSubject(), followupMessage, messageThread, team, true, currentUser.getFullName());
		    				}
    		    		}
    				} catch (Exception e) {
            			log.exception("MessageThreadResource:updateMessageThread:Exception", "", e);
    				}
    			}
        	}
		} catch (IOException e) {
			log.exception("MessageThreadResource:updateMessageThread:IOException", "", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (JSONException e) {
			log.exception("MessageThreadResource:updateMessageThread:JSONException1", "", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (NoResultException e) {
			log.exception("MessageThreadResource:updateMessageThread:NoResultException2", "messageThread not found", e);
			apiStatus = ApiStatusCode.MESSAGE_THREAD_NOT_FOUND;
		} catch (NonUniqueResultException e) {
			log.exception("MessageThreadResource:updateMessageThread:NonUniqueResultException4", "two or more messageThreads have same team name", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} finally {
		    if (em.getTransaction().isActive()) {
		        em.getTransaction().rollback();
		    }
		    em.close();
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("MessageThreadResource:updateMessageThread:JSONException2", "", e);
		}
		return new JsonRepresentation(jsonReturn);
    }
    
    //::EMAIL::EVENT::
    // Handles email response from new users and new members.
    public static UserMemberInfo handleUserMemberConfirmEmailResponse(String theOneUseToken) {
		String userId = null;
		String memberId = null;
		String toEmailAddress = null;
		UserMemberInfo userMemberInfo = new UserMemberInfo(ApiStatusCode.SUCCESS);
		EntityManager em = EMF.get().createEntityManager();
		try {
			em.getTransaction().begin();
			Recipient recipient = (Recipient)em.createNamedQuery("Recipient.getByOneUseTokenAndTokenStatus")
				.setParameter("oneUseToken", theOneUseToken)
				.setParameter("oneUseTokenStatus", Recipient.NEW_TOKEN_STATUS)
				.getSingleResult();
			slog.debug("handleUserMemberConfirmEmailResponse(): recipient found");
			userId = recipient.getUserId();
			memberId = recipient.getMemberId();
			toEmailAddress = recipient.getToEmailAddress();
			
			// obviously, token is only good one time
			recipient.setOneUseTokenStatus(Recipient.USED_TOKEN_STATUS);
			
			em.getTransaction().commit();
		} catch (NoResultException e) {
			// not an error - email could be received with a bad or old token -- just ignore
			userMemberInfo.setApiStatus(ApiStatusCode.EMAIL_CONFIRMATION_LINK_NO_LONGER_ACTIVE);
		} catch (NonUniqueResultException e) {
			slog.exception("MessageThreadResource:handleUserMemberConfirmEmailResponse:NonUniqueResultException", "two or more recipients have same oneUseToken", e);
			userMemberInfo.setApiStatus(ApiStatusCode.SERVER_ERROR);
		} finally {
		    if (em.getTransaction().isActive()) {
		        em.getTransaction().rollback();
		    }
		    em.close();
		}
		
		if(!userMemberInfo.getApiStatus().equals(ApiStatusCode.SUCCESS)) {
			return userMemberInfo;
		}
		
		User emailRecipientUser = null;
		Member emailRecipientMember = null;
		if(userId != null || memberId != null) {
			// get the user entity of the user responding to the email and mark the user as networkAuthenticated
			EntityManager em2 = EMF.get().createEntityManager();
			try {
				
				if(userId != null) {
					slog.debug("userId set, so this is a USER authorizing their email address");
					
					// Verify this email address is not in use.
					// -----------------------------------------
					// Here is how the email address may be in use
					// - user1 registers with email address xyz
					// - user2 registers with email address xyz. Allowed since email address xyz is not yet NA
					// - user1 confirms email address xyz
					// - user2 attempts to confirm email address xyz (this is what we are checking for here)
					User matchingUser = User.getUserWithEmailAddress(toEmailAddress);
					
					em2.getTransaction().begin();
					emailRecipientUser = (User)em2.createNamedQuery("User.getByKey")
						.setParameter("key", KeyFactory.stringToKey(userId))
						.getSingleResult();

					if(matchingUser != null) {
						slog.debug("email confirmation failed because email address " + toEmailAddress + " already in use and network authenticated");
						userMemberInfo.setApiStatus(ApiStatusCode.EMAIL_ADDRESS_ALREADY_USED);
						userMemberInfo.setEmailAddress(emailRecipientUser.getEmailAddress());
						
						// this user will be deleted by CRON job later
						// TODO
						emailRecipientUser.setInactivatedDueToDuplicateEmail(true);
					} else {
						emailRecipientUser.setIsNetworkAuthenticated(true);
						userMemberInfo.setFirstName(emailRecipientUser.getFirstName());
						userMemberInfo.setLastName(emailRecipientUser.getLastName());
						userMemberInfo.setEmailAddress(emailRecipientUser.getEmailAddress());
						userMemberInfo.setPhoneNumber(emailRecipientUser.getPhoneNumber());
						userMemberInfo.setOneUseToken(emailRecipientUser.getToken());
					}
					em2.getTransaction().commit();
				} else if(memberId != null) {
					slog.debug("memberId set, so this is a MEMBER authorizing their email address");
					
					em2.getTransaction().begin();
					emailRecipientMember = (Member)em2.createNamedQuery("Member.getByKey")
						.setParameter("key", KeyFactory.stringToKey(memberId))
						.getSingleResult();
					slog.debug("NAing member, toEmailAddress = " + toEmailAddress);
					emailRecipientMember.networkAuthenticateEmailAddress(toEmailAddress);
					emailRecipientMember.setHasEmailMessageAccessEnabledByEmailAddress(toEmailAddress, true);
					
					userMemberInfo.setFirstName(emailRecipientMember.getFirstNameByEmailAddress(toEmailAddress));
					userMemberInfo.setLastName(emailRecipientMember.getLastNameByEmailAddress(toEmailAddress));
					userMemberInfo.setEmailAddress(toEmailAddress);
					userMemberInfo.setTeam(emailRecipientMember.getTeam());
					userMemberInfo.setParticipantRole(emailRecipientMember.getParticipantRole());
					userMemberInfo.setIsGuardian(emailRecipientMember.isGuardian(toEmailAddress));
					userMemberInfo.setPrimaryDisplayName(emailRecipientMember.getPrimaryDisplayName());
					userMemberInfo.setHasEmailMessageAccessEnabled(true);
					em2.getTransaction().commit();
				}
				slog.debug("handleUserMemberConfirmEmailResponse(): mail recipient user/member found");
			} catch (NoResultException e) {
				slog.exception("MessageThreadResource:handleUserMemberConfirmEmailResponse:NoResultException", "could not find email recipient user/member associated with messageThread", e);
				userMemberInfo.setApiStatus(ApiStatusCode.SERVER_ERROR);
			} catch (NonUniqueResultException e) {
				slog.exception("MessageThreadResource:handleUserMemberConfirmEmailResponse:NonUniqueResultException2", "two or more users/members have same key", e);
				userMemberInfo.setApiStatus(ApiStatusCode.SERVER_ERROR);
			} finally {
			    if (em2.getTransaction().isActive()) {
			    	em2.getTransaction().rollback();
			    }
			    em2.close();
			}
			
			// only do synch ups if no errors above. Must be done after transaction above committed
			if(userMemberInfo.getApiStatus().equals(ApiStatusCode.SUCCESS)) {
				if(userId != null) {
					User.synchUpWithAuthorizedMemberships(emailRecipientUser);
				} else if(memberId != null) {
					Member.synchUpWithAuthorizedUser(emailRecipientMember, toEmailAddress);
				}
			}
		}
		
		return userMemberInfo;
    }
}