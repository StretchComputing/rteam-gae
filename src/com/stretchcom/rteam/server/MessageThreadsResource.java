package com.stretchcom.rteam.server;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
  
/** 
 * Resource that manages message threads. 
 *  
 */  
public class MessageThreadsResource extends ServerResource {  
	private static final Logger log = Logger.getLogger(MessageThreadsResource.class.getName());
	private static String SMS_PSEUDO_EMAIL_ADDRESS = "smsPseudoEmailAddress";
  
    // The sequence of characters that identifies the resource.
    String teamId;
    String timeZoneStr;
    String oneUseToken;
    String messageGroup;
    String eventId;
    String eventType;
    String status;
    String includeBodyAndChoicesStr;
    String wasViewedStr;
    String newCount;
    String resynchCounter;
    String includeNewActivity;
    String useThreadsStr;
    
    @Override  
    protected void doInit() throws ResourceException {  
        // Get the "teamId" attribute value taken from the URI template /team/{teamId} 
        this.teamId = (String)getRequest().getAttributes().get("teamId"); 
        log.info("MessageThreadsResource:doInit() - teamId = " + this.teamId);
        if(this.teamId != null) {
            this.teamId = Reference.decode(this.teamId);
            log.info("MessageThreadsResource:doInit() - decoded teamId = " + this.teamId);
        }
   
        this.timeZoneStr = (String)getRequest().getAttributes().get("timeZone"); 
        log.info("MessageThreadsResource:doInit() - timeZone = " + this.timeZoneStr);
        if(this.timeZoneStr != null) {
            this.timeZoneStr = Reference.decode(this.timeZoneStr);
            log.info("MessageThreadsResource:doInit() - decoded timeZone = " + this.timeZoneStr);
        }
        
        this.newCount = (String)getRequest().getAttributes().get("newCount"); 
        log.info("MessageThreadsResource:doInit() - newCount = " + this.newCount);
        if(this.newCount != null) {
            this.newCount = Reference.decode(this.newCount);
            log.info("MessageThreadsResource:doInit() - decoded newCount = " + this.newCount);
            // TODO ::RELEASE 1 PATCH::
            if(this.newCount.contains("resynchCounter=true")) {
            	this.resynchCounter = "true";
            }
        }
        
		Form form = getRequest().getResourceRef().getQueryAsForm();
		for (Parameter parameter : form) {
			log.info("parameter " + parameter.getName() + " = " + parameter.getValue());
			if(parameter.getName().equals("eventId")) {
				this.eventId = (String)parameter.getValue();
				this.eventId = Reference.decode(this.eventId);
				log.info("MessageThreadsResource:doInit() - decoded eventId = " + this.eventId);
			} else if(parameter.getName().equals("eventType")) {
				this.eventType = (String)parameter.getValue();
				this.eventType = Reference.decode(this.eventType);
				log.info("MessageThreadsResource:doInit() - decoded eventType = " + this.eventType);
			} else if(parameter.getName().equals("oneUseToken")) {
				this.oneUseToken = (String)parameter.getValue();
				this.oneUseToken = Reference.decode(this.oneUseToken);
				log.info("MessageThreadsResource:doInit() - decoded oneUseToken = " + this.oneUseToken);
			} else if(parameter.getName().equals("messageGroup")) {
				this.messageGroup = (String)parameter.getValue();
				this.messageGroup = Reference.decode(this.messageGroup);
				log.info("MessageThreadsResource:doInit() - decoded messageGroup = " + this.messageGroup);
			} else if(parameter.getName().equals("status")) {
				this.status = (String)parameter.getValue();
				this.status = Reference.decode(this.status);
				log.info("MessageThreadsResource:doInit() - decoded status = " + this.status);
			} else if(parameter.getName().equals("includeBodyAndChoices")) {
				this.includeBodyAndChoicesStr = (String)parameter.getValue();
				this.includeBodyAndChoicesStr = Reference.decode(this.includeBodyAndChoicesStr);
				log.info("MessageThreadsResource:doInit() - decoded includeBodyAndChoices = " + this.includeBodyAndChoicesStr);
			} else if(parameter.getName().equals("wasViewed")) {
				this.wasViewedStr = (String)parameter.getValue();
				this.wasViewedStr = Reference.decode(this.wasViewedStr);
				log.info("MessageThreadsResource:doInit() - decoded wasViewed = " + this.wasViewedStr);
			} else if(parameter.getName().equals("teamId")) {
				this.teamId = (String)parameter.getValue();
				this.teamId = Reference.decode(this.teamId);
				log.info("MessageThreadsResource:doInit() - decoded teamId = " + this.teamId);
			} else if(parameter.getName().equals("resynchCounter")) {
				this.resynchCounter = (String)parameter.getValue();
				this.resynchCounter = Reference.decode(this.resynchCounter);
				log.info("MessageThreadsResource:doInit() - decoded resynchCounter = " + this.resynchCounter);
			} else if(parameter.getName().equals("includeNewActivity")) {
				this.includeNewActivity = (String)parameter.getValue();
				this.includeNewActivity = Reference.decode(this.includeNewActivity);
				log.info("MessageThreadsResource:doInit() - decoded includeNewActivity = " + this.includeNewActivity);
			} else if(parameter.getName().equals("useThreads")) {
				this.useThreadsStr = (String)parameter.getValue();
				this.useThreadsStr = Reference.decode(this.useThreadsStr);
				log.info("MessageThreadsResource:doInit() - decoded useThreadsStr = " + this.useThreadsStr);
			}
		}
    }  
    
    // TODO support user and member settings for when to receive emails (see use case)
    // Handles 'Create a new message thread for the team' API  
    @Post  
    public JsonRepresentation createMessageThread(Representation entity) {
    	JSONObject jsonReturn = new JSONObject();
    	log.info("createMessageThread(@Post) entered ..... ");
		EntityManager em = EMF.get().createEntityManager();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_CREATED);
		MessageThread messageThread = null;
		User currentUser = null;
        try {
    		currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
    			log.severe("user could not be retrieved from Request attributes!!");
				return Utility.apiError(null);
    		}
    		//::BUSINESSRULE:: user must be network authenticated to send a message
    		else if(!currentUser.getIsNetworkAuthenticated()) {
				return Utility.apiError(ApiStatusCode.USER_NOT_NETWORK_AUTHENTICATED);
    		}
    		// teamId is required
    		else if(this.teamId == null || this.teamId.length() == 0) {
				return Utility.apiError(ApiStatusCode.TEAM_ID_REQUIRED);
    		} else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
				return Utility.apiError(ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM);
        	}
    		
			///////////////////////////////////////////////////////////////////////////////////////////////////
    		// need to get membership of current user to check if user is a coordinator or member participant
			///////////////////////////////////////////////////////////////////////////////////////////////////
			Team team = (Team)em.createNamedQuery("Team.getByKey")
				.setParameter("key", KeyFactory.stringToKey(this.teamId))
				.getSingleResult();
			log.info("team retrieved = " + team.getTeamName());
			
			// TODO use query instead of walking through entire members list.  See MemberResource for example of query.
			List<Member> members = team.getMembers();
			
   			Boolean isCoordinator = false;
   			Boolean isMember = false;
			if(currentUser.getIsNetworkAuthenticated()) {
				List<Member> memberships = Member.getMemberShipsWithEmailAddress(currentUser.getEmailAddress(), team);
				for(Member m : memberships) {
    	    		if(m.isCoordinator()) {
    	    			isCoordinator = true;
    	    			break;
    	    		} else if(m.isMemberParticipant()) {
    	    			isMember = true;
    	    			break;
    	    		}
				}
			}
			
			//::BUSINESSRULE:: user must be a coordinator or member to create a message thread
			if(!isCoordinator && !isMember) {
				return Utility.apiError(ApiStatusCode.USER_NOT_A_COORDINATOR_OR_MEMBER_PARTICIPANT);
			}
    		
			messageThread = new MessageThread();
    		JsonRepresentation jsonRep = new JsonRepresentation(entity);
			JSONObject json = jsonRep.toJsonObject();
			
			String eventIdStr = null;
			if(json.has("eventId")) {
				eventIdStr = json.getString("eventId");
				messageThread.setEventId(eventIdStr);
			}

			String eventTypeStr = null;
			if(json.has("eventType")) {
				eventTypeStr = json.getString("eventType");
			}
			if(eventIdStr != null && eventTypeStr == null) {
				return Utility.apiError(ApiStatusCode.EVENT_ID_AND_EVENT_TYPE_MUST_BE_SPECIFIED_TOGETHER);
			}
			if(eventTypeStr != null) messageThread.setIsGame(eventTypeStr.equalsIgnoreCase(Game.GAME) ? true : false);

			// if this message is associated with an event, get the date which will be stored in the message
			Date eventDate = null;
			String localEventDateStr = null;
			String eventName = null;
			if(eventIdStr != null && eventTypeStr != null) {
				List eventInfo = Practice.getEventInfo(eventIdStr, eventTypeStr);
				if(eventInfo == null || eventInfo.size() == 0) {
					return Utility.apiError(ApiStatusCode.EVENT_NOT_FOUND);
				}
				eventDate = (Date)eventInfo.get(0);
				localEventDateStr = (String)eventInfo.get(1);
				eventName = (String)eventInfo.get(2);
			}

			String type = null;
			if(json.has("type")) {
				type = json.getString("type");
				messageThread.setType(type);
			}
			if(type == null || type.length() == 0) {
				return Utility.apiError(ApiStatusCode.SUBJECT_BODY_AND_TYPE_REQUIRED);
			}
			if(!type.equalsIgnoreCase(MessageThread.CONFIRMED_TYPE) && 
			   !type.equalsIgnoreCase(MessageThread.POLL_TYPE) &&	
			   !type.equalsIgnoreCase(MessageThread.PLAIN_TYPE) &&
			   !type.equalsIgnoreCase(MessageThread.WHO_IS_COMING_TYPE)) {
				return Utility.apiError(ApiStatusCode.INVALID_TYPE_PARAMETER);
			}
			
			// out parameters for 'handleJson' methods below
			List<String> memberIds = new ArrayList<String>();
			Boolean coordinatorsOnly = new Boolean(false);
			Boolean includeFans = new Boolean(true);
			if(type != null && type.equalsIgnoreCase(MessageThread.WHO_IS_COMING_TYPE)) {
				////////////////////////////////////////////////////////////////////////////////////////////////////////
				// for who's coming message, only eventId and eventType are used -- all other fields are "predetermined"
				////////////////////////////////////////////////////////////////////////////////////////////////////////
				apiStatus = buildWhoIsComingMessageThreadApi(messageThread, localEventDateStr, coordinatorsOnly, includeFans);
			} else {
				apiStatus = handleJsonForCreateMessageThreadApi(json, type, messageThread, memberIds, coordinatorsOnly, includeFans);
			}
			if(!apiStatus.equals(ApiStatusCode.SUCCESS)) {
				return Utility.apiError(apiStatus);
			}
			
			String senderMemberId = "";
			Date gmtStartDate = new Date();
			messageThread.setCreatedGmtDate(gmtStartDate);

			// Build the final recipient list
			boolean wereMemberIdsSpecified = true;
			if(memberIds.size() == 0) {
				wereMemberIdsSpecified = false;
				messageThread.setIncludeEntireTeam(true);
			}
			
			// TODO slow for a large # of members?
			List<Member> recipientMembers = new ArrayList<Member>();
			for(Member m : members) {
				String memberEmail = m.getEmailAddress();
				
				// if members specified in API call, then need to reduce member list to those members specified
				// NOTE: if an API specified member ID doesn't match actual member ID, then it is silently ignored
				if(wereMemberIdsSpecified) {
					String thisMemberId = KeyFactory.keyToString(m.getKey());
					if(isInList(thisMemberId, memberIds)) {
						recipientMembers.add(m);
					}
				} else if(coordinatorsOnly) {
					if(m.isCoordinator()) {
						recipientMembers.add(m);
					}
				} else {
					// may or may not include fans depending on parameter
					if(includeFans || !m.isFan()) {
						recipientMembers.add(m);
					}
				}
				
				// need to know the sender's member ID
				if(memberEmail != null && memberEmail.equalsIgnoreCase(currentUser.getEmailAddress())) {
					senderMemberId = KeyFactory.keyToString(m.getKey());
				}
			}

			messageThread.setStatus(MessageThread.ACTIVE_STATUS);
			messageThread.setType(type);
			messageThread.setEventGmtStartDate(eventDate);
			messageThread.setTeamId(this.teamId);
			messageThread.setTeamName(team.getTeamName());
			messageThread.setSenderUserId(KeyFactory.keyToString(currentUser.getKey()));
			Integer autoArchiveDayCount = currentUser.getAutoArchiveDayCount();
			if(autoArchiveDayCount != null) {
				messageThread.setActiveThruGmtDate(GMT.addDaysToDate(new Date(), autoArchiveDayCount));
			}
			
			log.info("number of recipientMembers = " + recipientMembers.size());
			List<Recipient> recipients = new ArrayList<Recipient>();
			List<String> oneUseTokens = new ArrayList<String>();
			List<UserMemberInfo> authorizedTeamRecipients = new ArrayList<UserMemberInfo>();
			////////////////////////////////
			// Build Raw Team Recipient List
			////////////////////////////////
			for(Member rm : recipientMembers) {
				List<UserMemberInfo> authorizedMembershipRecipients = rm.getAuthorizedRecipients(team);
				for(UserMemberInfo umi : authorizedMembershipRecipients) {
					// filter out current user
					if( (umi.getEmailAddress() != null && umi.getEmailAddress().equalsIgnoreCase(currentUser.getEmailAddress())) ||
						 (umi.getPhoneNumber() != null && currentUser.getPhoneNumber() != null && umi.getPhoneNumber().equals(currentUser.getPhoneNumber()))  ) {
						continue;
					}
						
					// need to know associated member for loop below
					umi.setMember(rm);
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
			
			////////////////////////////////////////////////////////////////////////////////////////////////////////////
			// Create Recipients
			// -----------------
			//    Recipients are created for each participant receiving some type of message. One purpose of a recipient
			//    is to handle responses. Member responses can come in the following ways:
			//        1. via rTeam inbox (uses unique token)
			//        2. via embedded link in email (uses unique token)
			//        3. via SMS response (uses PN as token)
			//    Based on member preferences, one, two or all three message types could be sent to a member for a single
			//    message instance, so a single recipient must be able to handle responses from all three types. NOTE: The
			//    recipient count in MessageThread is set to the number of memberships (not people) receiving some type of message.
			////////////////////////////////////////////////////////////////////////////////////////////////////////////
			for(UserMemberInfo umi : authorizedTeamRecipients) {
				Recipient recipient = new Recipient();
				String memId = KeyFactory.keyToString(umi.getMember().getKey());
				recipient.setMemberId(memId);
				recipient.setMemberName(umi.getMember().getFullName());  // TODO setting to Primary member name - does it matter???
				recipient.setSubject(messageThread.getSubject());
				recipient.setMessage(messageThread.getMessage());
				recipient.setTeamId(this.teamId);
				recipient.setTeamName(team.getTeamName());
				recipient.setMessageLinkOnly(false);
				recipient.setParticipantRole(umi.getMember().getParticipantRole());
				
				recipient.setStatus(Recipient.SENT_STATUS);
				// TODO Pending NA status is not activated right now since all email addresses in this loop are authenticated
				//recipient.setStatus(Recipient.PENDING_NETWORK_AUTHENTICATION_STATUS);
				
				// recipient holds the user ID of the recipient member, not the sender. Could be NULL
				recipient.setUserId(umi.getUserId());
				
				// add unique/email oneUseToken to any message types that have one or more embedded message response links
				if(messageThread.isConfirm() || messageThread.isPoll()) {
    				recipient.setOneUseToken(umi.getOneUseToken());
				}
				// SMS unsolicited replies are handled by finding the recipient using SMS token, so SMS token always needs to be set
				recipient.setOneUseSmsToken(umi.getOneUseSmsToken());
				recipient.setOneUseTokenStatus(Recipient.NEW_TOKEN_STATUS);
				
				recipient.setToEmailAddress(umi.getEmailAddress());
				recipient.setNumOfSends(1);
				recipient.setIsGame(messageThread.getIsGame());
				recipient.setEventId(messageThread.getEventId());
				recipient.setEventName(eventName);
				recipient.setEventGmtStartDate(messageThread.getEventGmtStartDate());
				recipient.setReceivedGmtDate(new Date());
				recipient.setPollChoices(messageThread.getPollChoices());
				recipient.setSenderMemberId(senderMemberId);
				recipient.setSenderName(currentUser.getFullName());
				recipient.setWasViewed(false);
				
				autoArchiveDayCount = umi.getAutoArchiveDayCount();
				if(autoArchiveDayCount == null) {
					autoArchiveDayCount = MessageThread.MAX_NUMBER_OF_AUTO_ARCHIVE_DAY_COUNT;
				}
				recipient.setActiveThruGmtDate(GMT.addDaysToDate(new Date(), autoArchiveDayCount));
				
				recipients.add(recipient);
			}
			
			messageThread.setRecipients(recipients);
			// for number of recipients, each membership is only counted once
			messageThread.setNumOfRecipients(recipientMembers.size());
			
			em.getTransaction().begin();
			em.persist(messageThread);
		    em.getTransaction().commit();
		    String keyWebStr = KeyFactory.keyToString(messageThread.getKey());
		    log.info("message thread " + messageThread.getSubject() + " with key " + keyWebStr + " created successfully");
		    
		    // PubHub will filter and only send email if appropriate
		    PubHub.sendMessageThreadToMembers(authorizedTeamRecipients, messageThread.getSubject(), messageThread.getMessage(), messageThread, team, false, currentUser.getFullName());
		    
			String baseUri = this.getRequest().getHostRef().getIdentifier();
			this.getResponse().setLocationRef(baseUri + "/" + keyWebStr);
			jsonReturn.put("messageThreadId", keyWebStr);
		} catch (IOException e) {
			log.severe("error extracting JSON object from Post");
			e.printStackTrace();
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (JSONException e) {
			log.severe("error converting json representation into a JSON object");
			e.printStackTrace();
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (NoResultException e) {
			log.severe("team not found");
			apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
		} catch (NonUniqueResultException e) {
			log.severe("should never happen - two or more teams have same team name");
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
			e.printStackTrace();
		} finally {
		    if (em.getTransaction().isActive()) {
		        em.getTransaction().rollback();
		    }
		    em.close();
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.severe("error creating JSON return object");
			e.printStackTrace();
		}
		return new JsonRepresentation(jsonReturn);
    }
    
    // Handles 'Get New Message Thread Count' API
    // Handles 'Get MessageThreads for a specified team' API
    // Handles 'Get MessageThreads for all user teams' API
    @Get("json")
    public JsonRepresentation getMessageThreads(Variant variant) {
        log.info("MessageThreadsResource:getMessageThreads() entered");
        JSONObject jsonReturn = new JSONObject();
		EntityManager em = EMF.get().createEntityManager();

		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		Boolean includeBodyAndChoices = null;
		Boolean wasViewed = null;
		Boolean useThreads = null;
		List<Recipient> inboxMessages = new ArrayList<Recipient>();
		List<MessageThread> outboxMessages = new ArrayList<MessageThread>();
		TimeZone tz = null;
		try {
    		User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
    			log.severe("user could not be retrieved from Request attributes!!");
    		}
    		//::BUSINESSRULE:: user must be network authenticated to get messages
    		else if(!currentUser.getIsNetworkAuthenticated()) {
    			apiStatus = ApiStatusCode.USER_NOT_NETWORK_AUTHENTICATED;
    		}
    		// time zone is required except for 'Get New Message Thread Count
    		else if(this.newCount == null) {
        		if(this.timeZoneStr == null || this.timeZoneStr.length() == 0) {
        			log.info("MessageThreadResource:toJson() timeZone null or zero length");
     	        	apiStatus = ApiStatusCode.TIME_ZONE_REQUIRED;
        		} else {
        			tz = GMT.getTimeZone(this.timeZoneStr);
        			if(tz == null) {
                		apiStatus = ApiStatusCode.INVALID_TIME_ZONE_PARAMETER;
        			}
        		}
    		}
    		
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			
			// enforce the query parameter defaults and rules
			Boolean isGame = null;
			
			if(this.newCount == null) {
				// parameter checking for 'Get Message Threads ...' APIs
				if(this.status == null) this.status = MessageThread.ACTIVE_STATUS;
				if(this.messageGroup == null) this.messageGroup = MessageThread.ALL_MESSAGE_GROUP;
				if(this.includeBodyAndChoicesStr == null) this.includeBodyAndChoicesStr = "true";
				if(this.useThreadsStr == null) this.useThreadsStr = "false";
				
				if(this.eventId != null && this.eventType == null) {
					apiStatus = ApiStatusCode.EVENT_ID_AND_EVENT_TYPE_MUST_BE_SPECIFIED_TOGETHER;
					log.info("if eventId specified, then eventType must also be specified");
				} else if(!this.status.equalsIgnoreCase(MessageThread.ACTIVE_STATUS) &&
						  !this.status.equalsIgnoreCase(MessageThread.ALL_STATUS) &&
						  !this.status.equalsIgnoreCase(MessageThread.FINALIZED_STATUS) ) {
					apiStatus = ApiStatusCode.INVALID_STATUS_PARAMETER;
					log.info("specified status is not supported");
				} else if(!this.messageGroup.equalsIgnoreCase(MessageThread.INBOX_MESSAGE_GROUP) &&
						  !this.messageGroup.equalsIgnoreCase(MessageThread.OUTBOX_MESSAGE_GROUP) &&
						  !this.messageGroup.equalsIgnoreCase(MessageThread.ALL_MESSAGE_GROUP)  ) {
					apiStatus = ApiStatusCode.INVALID_MESSAGE_GROUP_PARAMETER;
					log.info("specified messageGroup is not supported");
				}
				
				if(apiStatus.equals(ApiStatusCode.SUCCESS) && this.eventId != null && this.eventType != null) {
					if(this.eventType.equalsIgnoreCase(Game.GAME)) {
						isGame = true;
					} else if(this.eventType.equalsIgnoreCase(Practice.PRACTICE)) {
						isGame = false;
					} else {
						apiStatus = ApiStatusCode.INVALID_EVENT_TYPE_PARAMETER;
						log.info("specified eventType is not supported");
					}
					
					//::BUSINESS_RULE:: if eventId is specified, useThreads is silently ignored
					this.useThreadsStr = "false";
				}
				
				if(apiStatus.equals(ApiStatusCode.SUCCESS)) {
					if(this.includeBodyAndChoicesStr.equalsIgnoreCase("true")) {
						includeBodyAndChoices = true;
					} else if(this.includeBodyAndChoicesStr.equalsIgnoreCase("false")) {
						includeBodyAndChoices = false;
					} else {
						apiStatus = ApiStatusCode.INVALID_INCLUDE_BODY_AND_CHOICES_PARAMETER;
						log.info("specified includeBodyAndChoices value is not supported");
					}
				}
				
				if(apiStatus.equals(ApiStatusCode.SUCCESS)) {
					if(this.useThreadsStr.equalsIgnoreCase("true")) {
						useThreads = true;
					} else if(this.useThreadsStr.equalsIgnoreCase("false")) {
						useThreads = false;
					} else {
						apiStatus = ApiStatusCode.INVALID_USE_THREADS_PARAMETER;
						log.info("specified useThreads value is not supported");
					}
				}
				
				if(apiStatus.equals(ApiStatusCode.SUCCESS) && this.wasViewedStr != null) {
					if(this.wasViewedStr.equalsIgnoreCase("false")) {
						wasViewed = false;
					} else {
						apiStatus = ApiStatusCode.INVALID_WAS_VIEWED_PARAMETER;
						log.info("specified wasViewed is not supported");
					}
				}
			} else {
				/////////////////////////////////////////////////////
				// parameter validation for message thread count API
				/////////////////////////////////////////////////////
				if(this.eventId != null && this.teamId == null) {
					apiStatus = ApiStatusCode.TEAM_ID_MUST_BE_SPECIFIED_WITH_EVENT_ID;
					log.info("if eventId specified, then teamId must also be specified");
				}

				if(apiStatus.equals(ApiStatusCode.SUCCESS) && this.eventId != null && this.eventType == null) {
						apiStatus = ApiStatusCode.EVENT_ID_AND_EVENT_TYPE_MUST_BE_SPECIFIED_TOGETHER;
						log.info("if eventId specified, then eventType must also be specified");
				}
				
				if(apiStatus.equals(ApiStatusCode.SUCCESS) && this.eventId != null && this.eventType != null) {
					if(this.eventType.equalsIgnoreCase(Game.GAME)) {
						isGame = true;
					} else if(this.eventType.equalsIgnoreCase(Practice.PRACTICE)) {
						isGame = false;
					} else {
						apiStatus = ApiStatusCode.INVALID_EVENT_TYPE_PARAMETER;
						log.info("specified eventType is not supported");
					}
				}

				if(apiStatus.equals(ApiStatusCode.SUCCESS) && this.resynchCounter != null && !this.resynchCounter.equalsIgnoreCase("true")) {
						apiStatus = ApiStatusCode.INVALID_RESYNCH_COUNTER_PARAMETER;
						log.info("resynch counter if present must be set to 'true'");
				}

				if(apiStatus.equals(ApiStatusCode.SUCCESS) && this.includeNewActivity != null &&
						!this.includeNewActivity.equalsIgnoreCase("true") && !this.includeNewActivity.equalsIgnoreCase("false")) {
						apiStatus = ApiStatusCode.INVALID_INCLUDE_NEW_ACTIVITY_PARAMETER;
						log.info("includeNewActivity must be set to 'true' or 'false'");
				}
			}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			
			// get associated memberships if team ID has been specified
			List<Member> memberships = null;
			Team team = null;
			if(this.teamId != null) {
 				try {
     				Key teamKey = KeyFactory.stringToKey(this.teamId);
             		team = (Team)em.createNamedQuery("Team.getByKey")
     					.setParameter("key", teamKey)
     					.getSingleResult();
             		
             		memberships =  Member.getMemberShipsWithEmailAddress(currentUser.getEmailAddress(), team);
             		for(Member m : memberships) {
             			log.info("primary name of matching membership = " + m.getFullName());
             		}
             		
             		if(memberships.size() == 0) {
     					log.info("member not part of specified team");
     					apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
             		}
 				} catch (NoResultException e) {
 					apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
 					log.info("invalid team id");
 				} catch (NonUniqueResultException e) {
 					log.severe("should never happen - two teams have the same key");
 				}
			}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			
			if(this.newCount != null) {
    			// --------------------------------------------------------------
				// This is the 'Get New Message Thread Count' API call
    			// --------------------------------------------------------------
				log.info("This is the 'Get New Message Thread Count' API call");
				
				// need to set status and message group before calling getMessages()
				this.status = MessageThread.ACTIVE_STATUS;
				this.messageGroup = MessageThread.INBOX_MESSAGE_GROUP;
				
				// get messages for all memberships, if appropriate (i.e. a teamId was specified and associated memberships found)
				if(memberships != null) {
					for(Member m : memberships) {
						getMessages(isGame, false, m, currentUser, em, inboxMessages, outboxMessages, true);
					}
				} else {
					getMessages(isGame, false, null, currentUser, em, inboxMessages, outboxMessages, true);
				}
				
				// only resynch counter if count is for all teams (i.e. teamId is null)
				if(this.resynchCounter != null && this.teamId == null) {
					currentUser.setAndPersistNewMessageCount(inboxMessages.size());
					log.info("resynching new message counter of current user = " + inboxMessages.size());
				}
				
				// include new activity if it is being requested
				Boolean newActivityFound = false;
				if(this.includeNewActivity != null && this.includeNewActivity.equalsIgnoreCase("true")) {
					List<Long> newestCacheIdsFromUser = currentUser.getTeamNewestCacheIds();
					List<Key> teamKeys = currentUser.getTeams();
					
					// PRIORITY TODO: remove this debug code
					// verify the key was added to the end of the list
					log.info("size of Team key list = " + teamKeys.size());
					for(Key tk : teamKeys) {
						log.info("team key = " + tk);					
					}

					if(team == null) {
						//////////////////////////////////////////////////////////////////
		    			// no specific team was given, so need to get all the user's teams
						//////////////////////////////////////////////////////////////////
						
						//::JPA_BUG:: ?? if I get all teams by passing  in a list of keys, the list of teams is not in same order as keys!!!!
//		    			List<Team> teams = (List<Team>) em.createQuery("select from " + Team.class.getName() + " where key = :keys")
//							.setParameter("keys", teamKeys)
//							.getResultList();
						
						List<Team> teams = new ArrayList<Team>();
						for(Key tk : teamKeys) {
							Team aTeam = null;
							try {
								 aTeam = (Team)em.createNamedQuery("Team.getByKey")
									.setParameter("key", tk)
									.getSingleResult();
							} catch(Exception e) {
								log.severe("should never happen. Could not find team with the team key");
							}
							if(aTeam != null) {teams.add(aTeam);}
						}
						log.info("number of teams retrieved for current user = " + teams.size());
						
						// teams should be in the same order as the newestCacheId, so we can just loop and compare
						int index = 0;
						for(Team t : teams) {
							Long newestCacheId = newestCacheIdsFromUser.get(index);
							// newestCacheId could be 0L, but that's not a problem because even if team Activity not active,
							// getNewestCacheId() will return a 0L as well.
							log.info("comparing against team = " + t.getTeamName());
							log.info("newestCacheId = " + newestCacheId);
							log.info("t.getNewestCacheId() = " + t.getNewestCacheId());
							if(newestCacheId < t.getNewestCacheId()) {
								newActivityFound = true;
								log.info("new activity found for team = " + t.getTeamName());
								break;
							}
							index++;
						}
					} else {
						////////////////////////////////////////////////////////////////////////////////////////////////
						// this.teamId was passed in. Need to match this teamId to the teamKeys from the currentUser so
						// we know which newestCacheId to compare it against.
						////////////////////////////////////////////////////////////////////////////////////////////////
						int index = 0;
						for(Key teamKey : teamKeys) {
							if(teamKey.equals(KeyFactory.stringToKey(this.teamId))) {
								Long newestCacheId = newestCacheIdsFromUser.get(index);
								// newestCacheId could be 0L, but that's not a problem because even if team Activity not active,
								// getNewestCacheId() will return a 0L as well.
								log.info("matching team = " + team.getTeamName());
								log.info("team key = " + teamKey);
								log.info("newestCacheId = " + newestCacheId);
								log.info("team.getNewestCacheId() = " + team.getNewestCacheId());
								if(newestCacheId < team.getNewestCacheId()) {
									newActivityFound = true;
									log.info("new activity found for team = " + team.getTeamName());
									break;
								}
								break;
							}
							index++;
						}
					}
					jsonReturn.put("newActivity", newActivityFound);
				}
				
				jsonReturn.put("count", inboxMessages.size());
			} else {
				if(this.teamId != null) {
	    			// --------------------------------------------------------------
					// This is the 'Get MessageThreads for a specified team' API call
	    			// --------------------------------------------------------------
	    			log.info("This is the 'Get MessageThreads for a specified team' API call");
	            	
	    			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
	    				jsonReturn.put("apiStatus", apiStatus);
	    				return new JsonRepresentation(jsonReturn);
	    			}

	    			// Retrieve messages for all memberships. Each membership has its own inboxMessages; however, is
	    			// common for the user so we only need to request the outboxMessages the first time through the loop.
	    			Boolean isOutBoxDisabled = false;
	    			for(Member m : memberships) {
		    			getMessages(isGame, wasViewed, m, currentUser, em, inboxMessages, outboxMessages, isOutBoxDisabled);
		    			isOutBoxDisabled = true;
	    			}
	    			if(inboxMessages == null) {
	    				log.info("inboxMessages is NULL");
	    			} else {
	    				log.info("size of inboxMessages = " + inboxMessages.size());
	    			}
				} else {
	    			// --------------------------------------------------------------
					// This is the 'Get MessageThreads for all user teams' API call
	    			// --------------------------------------------------------------
	    			log.info("This is the 'Get MessageThreads for all user teams' API call");
	    			
	    			getMessages(isGame, wasViewed, null, currentUser, em, inboxMessages, outboxMessages, false);
				}
				
	        	//---------------------------
				// Common code for both APIs
	        	//---------------------------
	        	if(inboxMessages != null) {
		    		JSONArray jsonInboxMessages = new JSONArray();
		    		buildInboxJsonArray(inboxMessages, jsonInboxMessages, includeBodyAndChoices, useThreads, tz);
		    		jsonReturn.put("inbox", jsonInboxMessages);
	        	}
	        	
	        	if(outboxMessages != null) {
		    		JSONArray jsonOutboxMessages = new JSONArray();
		    		buildOutboxJsonArray(outboxMessages, jsonOutboxMessages, includeBodyAndChoices, useThreads, tz);
		    		jsonReturn.put("outbox", jsonOutboxMessages);
	        	}
	        	
	        	// TODO remove in phase 2 release
	        	jsonReturn.put("removedOld", true);
			}
        } catch (NoResultException e) {
        	log.info("no result exception, messageThread not found");
        	apiStatus = ApiStatusCode.MESSAGE_THREAD_NOT_FOUND;
		} catch (JSONException e) {
			log.severe("error building JSON object");
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
			e.printStackTrace();
		} catch (NonUniqueResultException e) {
			log.severe("should never happen - two or more messageThreads have same ID");
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
			e.printStackTrace();
		} finally {
			em.close();
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.severe("error converting json representation into a JSON object");
			e.printStackTrace();
		}
		
        return new JsonRepresentation(jsonReturn);
    }
    
    // theJsonInboxMessages: out parameter that is filled in by this method
    private void buildInboxJsonArray(List<Recipient> theInboxMessages, JSONArray theJsonInboxMessages,
    		                         Boolean theIncludeBodyAndChoices, Boolean theUseThreads, TimeZone theTz) throws JSONException {
		
    	List<Recipient> inboxEventMessages = new ArrayList<Recipient>();
		// PROCESS all non-event messages and move all event messages to inboxEventMessages
    	for(Recipient r : theInboxMessages) {
			JSONObject inboxMessageJsonObj = new JSONObject();
			// only separate the eventMessages if we are 'using threads'
			if(theUseThreads && r.getEventId() != null) {
				inboxEventMessages.add(r);
			} else {
				buildInboxMessageJsonObj(r, inboxMessageJsonObj, theIncludeBodyAndChoices, theTz);
	    		theJsonInboxMessages.put(inboxMessageJsonObj);
			}
		}
    	
    	// if we are not using threads, this while loop will NOT get entered
    	List<Recipient> threadMessages = new ArrayList<Recipient>();
    	List<Recipient> remainingMessages = new ArrayList<Recipient>();
    	while(inboxEventMessages.size() > 0) {
    		Recipient recipient = inboxEventMessages.get(0);
        	for(Recipient iem : inboxEventMessages) {
        		if(iem.getEventId().equals(recipient.getEventId())) {
        			threadMessages.add(iem);
        		} else {
        			remainingMessages.add(iem);
        		}
        	}
        	
        	// PROCESS the threadMessage which are ready to go
        	JSONObject eventJsonObj = new JSONObject();
        	JSONArray threadMessageJsonArray = new JSONArray();
        	Recipient peekAheadRecipient = threadMessages.get(0);
        	String eventIdStr = peekAheadRecipient.getIsGame() ? "gameId" : "practiceId";
        	for(Recipient tm : threadMessages) {
        		JSONObject inboxMessageJsonObj = new JSONObject();
				buildInboxMessageJsonObj(tm, inboxMessageJsonObj, theIncludeBodyAndChoices, theTz);
				threadMessageJsonArray.put(inboxMessageJsonObj);
         	}
        	eventJsonObj.put(eventIdStr, peekAheadRecipient.getEventId());
        	if(peekAheadRecipient.getEventGmtStartDate() != null) {
            	eventJsonObj.put("date", GMT.convertToLocalDate(peekAheadRecipient.getEventGmtStartDate(), theTz));
        	}
        	eventJsonObj.put("messages", threadMessageJsonArray);
        	theJsonInboxMessages.put(eventJsonObj);
           	
        	// prepare for next iteration in this while loop
        	inboxEventMessages.clear();
        	inboxEventMessages.addAll(remainingMessages);
        	threadMessages.clear();
        	remainingMessages.clear();
    	}
    }
    
    // theInboxMessageJsonObj: out parameter that is filled in by this method
    private void buildInboxMessageJsonObj(Recipient theRecipient, JSONObject theInboxMessageJsonObj,
    		                              Boolean theIncludeBodyAndChoices, TimeZone theTz) throws JSONException {
		if(this.teamId == null) {
			// no team specified, so messages returned are for all teams
			theInboxMessageJsonObj.put("teamId", theRecipient.getTeamId());
			theInboxMessageJsonObj.put("teamName", theRecipient.getTeamName());
			theInboxMessageJsonObj.put("participantRole", theRecipient.getParticipantRole());
		}
		theInboxMessageJsonObj.put("messageThreadId", KeyFactory.keyToString(theRecipient.getMessageThread().getKey()));
		theInboxMessageJsonObj.put("subject", theRecipient.getSubject());
		
		if(theIncludeBodyAndChoices) {
			theInboxMessageJsonObj.put("body", theRecipient.getMessage());
			theInboxMessageJsonObj.put("followupMessage", theRecipient.getFollowupMessage());
    		JSONArray jsonPollChoices = new JSONArray();
    		for(String s : theRecipient.getPollChoices()) {
    			jsonPollChoices.put(s);
    		}
    		theInboxMessageJsonObj.put("pollChoices", jsonPollChoices);
		}
		
		theInboxMessageJsonObj.put("type", theRecipient.getType());
		theInboxMessageJsonObj.put("status", theRecipient.getStatus());
		if(theRecipient.getReceivedGmtDate() != null) {
			theInboxMessageJsonObj.put("receivedDate", GMT.convertToLocalDate(theRecipient.getReceivedGmtDate(), theTz));
		}
		theInboxMessageJsonObj.put("isReminder", theRecipient.getNumOfSends() > 1 ? "true" : "false");
		theInboxMessageJsonObj.put("wasViewed", theRecipient.getWasViewed());
		theInboxMessageJsonObj.put("senderMemberId", theRecipient.getSenderMemberId());
		theInboxMessageJsonObj.put("senderName", theRecipient.getSenderName());
    }
    
    // theJsonOutboxMessages: out parameter that is filled in by this method
    private void buildOutboxJsonArray(List<MessageThread> theOutboxMessages, JSONArray theJsonOutboxMessages,
    		                          Boolean theIncludeBodyAndChoices, Boolean theUseThreads, TimeZone theTz) throws JSONException {
		
    	List<MessageThread> outboxEventMessages = new ArrayList<MessageThread>();
		// PROCESS all non-event messages and move all event messages to inboxEventMessages
    	for(MessageThread tom : theOutboxMessages) {
			JSONObject outboxMessageJsonObj = new JSONObject();
			// only separate the eventMessages if we are 'using threads'
			if(theUseThreads && tom.getEventId() != null) {
				outboxEventMessages.add(tom);
			} else {
				buildOutboxMessageJsonObj(tom, outboxMessageJsonObj, theIncludeBodyAndChoices, theTz);
				theJsonOutboxMessages.put(outboxMessageJsonObj);
			}
		}
    	
    	// if we are not 'using threads', this while loop will NOT get entered
    	List<MessageThread> threadMessages = new ArrayList<MessageThread>();
    	List<MessageThread> remainingMessages = new ArrayList<MessageThread>();
    	while(outboxEventMessages.size() > 0) {
    		MessageThread messageThread = outboxEventMessages.get(0);
        	for(MessageThread oem : outboxEventMessages) {
        		if(oem.getEventId().equals(messageThread.getEventId())) {
        			threadMessages.add(oem);
        		} else {
        			remainingMessages.add(oem);
        		}
        	}
        	
        	// PROCESS the threadMessages which are ready to go
        	JSONObject eventJsonObj = new JSONObject();
        	JSONArray threadMessageJsonArray = new JSONArray();
        	MessageThread peekAheadMessageThread = threadMessages.get(0);
        	String eventIdStr = peekAheadMessageThread.getIsGame() ? "gameId" : "practiceId";
        	for(MessageThread tm : threadMessages) {
        		JSONObject outboxMessageJsonObj = new JSONObject();
				buildOutboxMessageJsonObj(tm, outboxMessageJsonObj, theIncludeBodyAndChoices, theTz);
				threadMessageJsonArray.put(outboxMessageJsonObj);
         	}
        	eventJsonObj.put(eventIdStr, peekAheadMessageThread.getEventId());
        	if(peekAheadMessageThread.getEventGmtStartDate() != null) {
            	eventJsonObj.put("date", GMT.convertToLocalDate(peekAheadMessageThread.getEventGmtStartDate(), theTz));
        	}
        	eventJsonObj.put("messages", threadMessageJsonArray);
        	theJsonOutboxMessages.put(eventJsonObj);
        	
        	// prepare for next iteration in this while loop
        	outboxEventMessages.clear();
        	outboxEventMessages.addAll(remainingMessages);
        	threadMessages.clear();
        	remainingMessages.clear();
    	}
    }
    
    // theOutboxMessageJsonObj: out parameter that is filled in by this method
    private void buildOutboxMessageJsonObj(MessageThread theMessageThread, JSONObject theOutboxMessageJsonObj,
    		                              Boolean theIncludeBodyAndChoices, TimeZone theTz) throws JSONException {
		if(this.teamId == null) {
			// no team specified, so messages returned are for all teams
			theOutboxMessageJsonObj.put("teamId", theMessageThread.getTeamId());
			theOutboxMessageJsonObj.put("teamName", theMessageThread.getTeamName());
		}
		theOutboxMessageJsonObj.put("messageThreadId", KeyFactory.keyToString(theMessageThread.getKey()));
		theOutboxMessageJsonObj.put("subject", theMessageThread.getSubject());
		
		if(theIncludeBodyAndChoices) {
			theOutboxMessageJsonObj.put("body", theMessageThread.getMessage());
			theOutboxMessageJsonObj.put("followupMessage", theMessageThread.getFollowupMessage());
		}

		theOutboxMessageJsonObj.put("type", theMessageThread.getType());
		theOutboxMessageJsonObj.put("status", theMessageThread.getStatus());
		theOutboxMessageJsonObj.put("createdDate", GMT.convertToLocalDate(theMessageThread.getCreatedGmtDate(), theTz));
		theOutboxMessageJsonObj.put("numOfRecipients", theMessageThread.getNumOfRecipients());
		int numOfReplies = theMessageThread.getMemberIdsThatReplied() == null ? 0 : theMessageThread.getMemberIdsThatReplied().size();
		theOutboxMessageJsonObj.put("numOfReplies", numOfReplies);
    }
    
    // Handles 'Update message threads' API
    @Put 
    public JsonRepresentation updateMessageThreads(Representation entity) {
    	JSONObject jsonReturn = new JSONObject();
    	log.info("updateMessageThreads(@Put) entered ..... ");
		EntityManager em = EMF.get().createEntityManager();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		User currentUser = null;
        try {
    		currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
    			this.setStatus(Status.SERVER_ERROR_INTERNAL);
    			log.severe("user could not be retrieved from Request attributes!!");
    		}
    		//::BUSINESSRULE:: user must be network authenticated to update message threads
    		else if(!currentUser.getIsNetworkAuthenticated()) {
    			apiStatus = ApiStatusCode.USER_NOT_NETWORK_AUTHENTICATED;
    		}
    		
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}

			JsonRepresentation jsonRep = new JsonRepresentation(entity);
			JSONObject json = jsonRep.toJsonObject();
			log.info("received json object = " + json.toString());
			
			//////////////////////////////////
			// Parse the incoming JSON object
			//////////////////////////////////
			List<String> messageThreadIds = new ArrayList<String>();
			if(json.has("messageThreadIds")) {
				JSONArray messageThreadIdsJsonArray = json.getJSONArray("messageThreadIds");
				int numOfMessageThreads = messageThreadIdsJsonArray.length();
				log.info("json messageThread IDs array length = " + numOfMessageThreads);
				for(int i=0; i<numOfMessageThreads; i++) {
					messageThreadIds.add(messageThreadIdsJsonArray.getString(i));
				}
			}
			
			String status = null;
			if(json.has("status")) {
				status = json.getString("status");
				if(!status.equalsIgnoreCase(MessageThread.ARCHIVED_STATUS)) {
	                log.info("updateMessageThreads() status did not have a value of 'archived'");
	 				jsonReturn.put("apiStatus", ApiStatusCode.INVALID_STATUS_PARAMETER);
					return new JsonRepresentation(jsonReturn);
				}
			}
			
			String messageLocation = null;
			if(json.has("messageLocation")) {
				messageLocation = json.getString("messageLocation");
				if(!messageLocation.equalsIgnoreCase("inbox") && !messageLocation.equalsIgnoreCase("outbox")) {
	                log.info("updateMessageThreads() invalid message location");
	 				jsonReturn.put("apiStatus", ApiStatusCode.INVALID_MESSAGE_LOCATION);
					return new JsonRepresentation(jsonReturn);
				}
			}
			
			// All parameters are required.
			if(messageThreadIds.size() == 0 || status ==  null || messageLocation == null) {
				jsonReturn.put("apiStatus", ApiStatusCode.ALL_PARAMETERS_REQUIRED);
				return new JsonRepresentation(jsonReturn);
			}
			
			//////////////////////////////
			// Archive the message threads
			//////////////////////////////
			try {
				for(String mtId : messageThreadIds) {
			    	em.getTransaction().begin();
		    		Key messageThreadKey = KeyFactory.stringToKey(mtId);
		    		MessageThread messageThread = (MessageThread)em.createNamedQuery("MessageThread.getByKey")
		    			.setParameter("key", messageThreadKey)
		    			.getSingleResult();
			    	
	    			if(messageLocation.equalsIgnoreCase("inbox")) {
	    				// User must be a recipient of the messageThread ID
			    		Recipient recipientOfThisUser = null;
	        			List<Recipient> recipients = messageThread.getRecipients();
	        			for(Recipient r: recipients) {
	        				String userId = r.getUserId();
	        				if(userId == null) {
	        					continue;
	        				} else {
	        					String currentUserId = KeyFactory.keyToString(currentUser.getKey());
	        					if(currentUserId.equals(userId)) {
	        						recipientOfThisUser = r;
	        						break;
	        					}
	        				}
	        			}
	        			
		        		if(recipientOfThisUser != null) {
		        			// the whole reason we are here -- archive the messageThread for this recipient.
		        			recipientOfThisUser.setStatus(Recipient.ARCHIVED_STATUS);
		        		} else {
		        			// no error because this API silently ignores when this happens. Log for debug purposes only.
		        			log.info("no recipient found for messageThreadId = " + mtId);
		        		}
	    			} else {
	    				// User must be the creator of the messageThread ID
	    				if(messageThread.getSenderUserId().equals(KeyFactory.keyToString(currentUser.getKey()))) {
	    					messageThread.setStatus(MessageThread.ARCHIVED_STATUS);
	    				} else {
		        			// no error because this API silently ignores when this happens. Log for debug purposes only.
		        			log.info("user is not creator of messageThreadId = " + mtId);
		        		}
	    			}
        			
					em.getTransaction().commit();
				}
			} catch (NoResultException e) {
				log.info("messageThread not found");
				apiStatus = ApiStatusCode.MESSAGE_THREAD_NOT_FOUND;
			} catch (NonUniqueResultException e) {
				log.severe("should never happen - two or more messageThreads have same team name");
				e.printStackTrace();
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
			} finally {
			    if (em.getTransaction().isActive()) {
			        em.getTransaction().rollback();
			    }
			    em.close();
			}
        } catch (IOException e) {
			log.severe("error extracting JSON object from Put");
			e.printStackTrace();
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (JSONException e) {
			log.severe("error converting json representation into a JSON object");
			e.printStackTrace();
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.severe("error creating JSON return object");
			e.printStackTrace();
		}
		return new JsonRepresentation(jsonReturn);
        
    }

    
    private Boolean isInList(String theMemberId, List<String> theMemberIds) {
    	for(String mid : theMemberIds) {
    		if(mid.equals(theMemberId)) return true;
    	}
    	return false;
    }
	
    private void getMessages(Boolean theIsGame, Boolean theWasViewed, Member theMember, User theCurrentUser, EntityManager em,
    		List<Recipient> theInboxMessages, List<MessageThread> theOutboxMessages, Boolean theIsOutboxDisabled) {
		log.info("getMessages() entered");
    	List<Recipient> inboxMessages = null;
		List<MessageThread> outboxMessages = null;
		if(this.status.equalsIgnoreCase(MessageThread.ALL_STATUS)) {
			if(theIsGame == null) {
				if(this.messageGroup.equalsIgnoreCase(MessageThread.INBOX_MESSAGE_GROUP) || this.messageGroup.equalsIgnoreCase(MessageThread.ALL_MESSAGE_GROUP)) {
					// do query for inbox data
					if(theWasViewed == null) {
						if(theMember == null) {
							inboxMessages = (List<Recipient>)em.createNamedQuery("Recipient.getByUserId")
								.setParameter("userId", KeyFactory.keyToString(theCurrentUser.getKey()))
								.getResultList();
						} else {
							inboxMessages = (List<Recipient>)em.createNamedQuery("Recipient.getByMemberIdAndEmailAddress")
								.setParameter("memberId", KeyFactory.keyToString(theMember.getKey()))
								.setParameter("emailAddress", theCurrentUser.getEmailAddress())
								.getResultList();
						}
						log.info("MessageThreadsResource.getMessageThreads(): found " + inboxMessages.size() + " (1)inbox messages with any status");
					} else {
						if(theMember == null) {
							inboxMessages = (List<Recipient>)em.createNamedQuery("Recipient.getByUserIdAndNotViewed")
								.setParameter("userId", KeyFactory.keyToString(theCurrentUser.getKey()))
								.setParameter("wasViewed", theWasViewed)
								.getResultList();
						} else {
							inboxMessages = (List<Recipient>)em.createNamedQuery("Recipient.getByMemberIdAndEmailAddressAndNotViewed")
								.setParameter("memberId", KeyFactory.keyToString(theMember.getKey()))
								.setParameter("emailAddress", theCurrentUser.getEmailAddress())
								.setParameter("wasViewed", theWasViewed)
								.getResultList();
						}
						log.info("MessageThreadsResource.getMessageThreads(): found " + inboxMessages.size() + " (2)unviewed inbox messages with any status and not viewed");
					}
				}
				if(!theIsOutboxDisabled && (this.messageGroup.equalsIgnoreCase(MessageThread.OUTBOX_MESSAGE_GROUP) || this.messageGroup.equalsIgnoreCase(MessageThread.ALL_MESSAGE_GROUP))) {
					// do query for outbox data
					if(theMember == null) {
						outboxMessages = (List<MessageThread>)em.createNamedQuery("MessageThread.getBySenderUserId")
							.setParameter("senderUserId", KeyFactory.keyToString(theCurrentUser.getKey()))
							.getResultList();
					} else {
						outboxMessages = (List<MessageThread>)em.createNamedQuery("MessageThread.getBySenderUserIdAndTeamId")
							.setParameter("senderUserId", KeyFactory.keyToString(theCurrentUser.getKey()))
							.setParameter("teamId", this.teamId)
							.getResultList();
					}
					log.info("MessageThreadsResource.getMessageThreads(): found " + outboxMessages.size() + " (3)outbox messages with any status");
				}
			} else {
				if(this.messageGroup.equalsIgnoreCase(MessageThread.INBOX_MESSAGE_GROUP) || this.messageGroup.equalsIgnoreCase(MessageThread.ALL_MESSAGE_GROUP)) {
					// do query for inbox data
					if(theWasViewed == null) {
						if(theMember == null) {
							inboxMessages = (List<Recipient>)em.createNamedQuery("Recipient.getByUserIdAndEventIdAndEventType")
								.setParameter("userId", KeyFactory.keyToString(theCurrentUser.getKey()))
								.setParameter("eventId", this.eventId)
								.setParameter("isGame", theIsGame)
								.getResultList();
						} else {
							inboxMessages = (List<Recipient>)em.createNamedQuery("Recipient.getByMemberIdAndEmailAddressAndEventIdAndEventType")
								.setParameter("memberId", KeyFactory.keyToString(theMember.getKey()))
								.setParameter("emailAddress", theCurrentUser.getEmailAddress())
								.setParameter("eventId", this.eventId)
								.setParameter("isGame", theIsGame)
								.getResultList();
						}
						log.info("MessageThreadsResource.getMessageThreads(): found " + inboxMessages.size() + " (4)inbox messages with any status and a specific event ID");
					} else {
						if(theMember == null) {
							inboxMessages = (List<Recipient>)em.createNamedQuery("Recipient.getByUserIdAndEventIdAndEventTypeAndNotViewed")
								.setParameter("userId", KeyFactory.keyToString(theCurrentUser.getKey()))
								.setParameter("eventId", this.eventId)
								.setParameter("isGame", theIsGame)
								.setParameter("wasViewed", theWasViewed)
								.getResultList();
						} else {
							inboxMessages = (List<Recipient>)em.createNamedQuery("Recipient.getByMemberIdAndEmailAddressAndEventIdAndEventTypeAndNotViewed")
								.setParameter("memberId", KeyFactory.keyToString(theMember.getKey()))
								.setParameter("emailAddress", theCurrentUser.getEmailAddress())
								.setParameter("eventId", this.eventId)
								.setParameter("isGame", theIsGame)
								.setParameter("wasViewed", theWasViewed)
								.getResultList();
						}
						log.info("MessageThreadsResource.getMessageThreads(): found " + inboxMessages.size() + " (5)unviewed inbox messages with any status and a specific event ID");
					}
				}
				if(!theIsOutboxDisabled && (this.messageGroup.equalsIgnoreCase(MessageThread.OUTBOX_MESSAGE_GROUP) || this.messageGroup.equalsIgnoreCase(MessageThread.ALL_MESSAGE_GROUP))) {
					// do query for outbox data
					if(theMember == null) {
						outboxMessages = (List<MessageThread>)em.createNamedQuery("MessageThread.getBySenderUserIdAndEventIdAndEventType")
							.setParameter("senderUserId", KeyFactory.keyToString(theCurrentUser.getKey()))
							.setParameter("eventId", this.eventId)
							.setParameter("isGame", theIsGame)
							.getResultList();
					} else {
						outboxMessages = (List<MessageThread>)em.createNamedQuery("MessageThread.getBySenderUserIdAndEventIdAndEventTypeAndTeamId")
							.setParameter("senderUserId", KeyFactory.keyToString(theCurrentUser.getKey()))
							.setParameter("eventId", this.eventId)
							.setParameter("isGame", theIsGame)
							.setParameter("teamId", this.teamId)
							.getResultList();
					}
					log.info("MessageThreadsResource.getMessageThreads(): found " + outboxMessages.size() + " (6)outbox messages with any status");
				}
			}
		} else {
			// following queries are for active status only. 'Active' means not archived.
			if(theIsGame == null) {
				if(this.messageGroup.equalsIgnoreCase(MessageThread.INBOX_MESSAGE_GROUP) || this.messageGroup.equalsIgnoreCase(MessageThread.ALL_MESSAGE_GROUP)) {
					// do query for inbox data
					if(theWasViewed == null) {
						if(theMember == null) {
							inboxMessages = (List<Recipient>)em.createNamedQuery("Recipient.getByUserIdAndNotStatus")
								.setParameter("userId", KeyFactory.keyToString(theCurrentUser.getKey()))
								.setParameter("status", Recipient.ARCHIVED_STATUS)
								.getResultList();
						} else {
							inboxMessages = (List<Recipient>)em.createNamedQuery("Recipient.getByMemberIdAndEmailAddressAndNotStatus")
								.setParameter("memberId", KeyFactory.keyToString(theMember.getKey()))
								.setParameter("emailAddress", theCurrentUser.getEmailAddress())
								.setParameter("status", Recipient.ARCHIVED_STATUS)
								.getResultList();
						}
						log.info("MessageThreadsResource.getMessageThreads(): found " + inboxMessages.size() + " (7)inbox messages with status = " + this.status);
					} else {
						if(theMember == null) {
							inboxMessages = (List<Recipient>)em.createNamedQuery("Recipient.getByUserIdAndNotStatusAndNotViewed")
								.setParameter("userId", KeyFactory.keyToString(theCurrentUser.getKey()))
								.setParameter("status", Recipient.ARCHIVED_STATUS)
								.setParameter("wasViewed", theWasViewed)
								.getResultList();
						} else {
							inboxMessages = (List<Recipient>)em.createNamedQuery("Recipient.getByMemberIdAndEmailAddressAndNotStatusAndNotViewed")
								.setParameter("memberId", KeyFactory.keyToString(theMember.getKey()))
								.setParameter("emailAddress", theCurrentUser.getEmailAddress())
								.setParameter("status", Recipient.ARCHIVED_STATUS)
								.setParameter("wasViewed", theWasViewed)
								.getResultList();
						}
						log.info("MessageThreadsResource.getMessageThreads(): found " + inboxMessages.size() + " (8)unviewed inbox messages with status = " + this.status);
					}
				}
				if(!theIsOutboxDisabled && (this.messageGroup.equalsIgnoreCase(MessageThread.OUTBOX_MESSAGE_GROUP) || this.messageGroup.equalsIgnoreCase(MessageThread.ALL_MESSAGE_GROUP))) {
					// do query for outbox data
					if(theMember == null) {
						outboxMessages = (List<MessageThread>)em.createNamedQuery("MessageThread.getBySenderUserIdAndStatus")
							.setParameter("senderUserId", KeyFactory.keyToString(theCurrentUser.getKey()))
							.setParameter("status", this.status)
							.getResultList();
					} else {
						outboxMessages = (List<MessageThread>)em.createNamedQuery("MessageThread.getBySenderUserIdAndStatusAndTeamId")
							.setParameter("senderUserId", KeyFactory.keyToString(theCurrentUser.getKey()))
							.setParameter("status", this.status)
							.setParameter("teamId", this.teamId)
							.getResultList();
					}
					log.info("MessageThreadsResource.getMessageThreads(): found " + outboxMessages.size() + " (9)outbox messages with status = " + this.status);
				}
			} else {
				if(this.messageGroup.equalsIgnoreCase(MessageThread.INBOX_MESSAGE_GROUP) || this.messageGroup.equalsIgnoreCase(MessageThread.ALL_MESSAGE_GROUP)) {
					// do query for inbox data
					if(theWasViewed == null) {
						if(theMember == null) {
							inboxMessages = (List<Recipient>)em.createNamedQuery("Recipient.getByUserIdAndEventIdAndEventTypeAndNotStatus")
								.setParameter("userId", KeyFactory.keyToString(theCurrentUser.getKey()))
								.setParameter("eventId", this.eventId)
								.setParameter("isGame", theIsGame)
								.setParameter("status", Recipient.ARCHIVED_STATUS)
								.getResultList();
						} else {
							inboxMessages = (List<Recipient>)em.createNamedQuery("Recipient.getByMemberIdAndEmailAddressAndEventIdAndEventTypeAndNotStatus")
								.setParameter("memberId", KeyFactory.keyToString(theMember.getKey()))
								.setParameter("emailAddress", theCurrentUser.getEmailAddress())
								.setParameter("eventId", this.eventId)
								.setParameter("isGame", theIsGame)
								.setParameter("status", Recipient.ARCHIVED_STATUS)
								.getResultList();
						}
						log.info("MessageThreadsResource.getMessageThreads(): found " + inboxMessages.size() + " (10)inbox messages with a specific event ID and with status = " + this.status);
					} else {
						if(theMember == null) {
							inboxMessages = (List<Recipient>)em.createNamedQuery("Recipient.getByUserIdAndEventIdAndEventTypeAndNotStatusAndNotViewed")
								.setParameter("userId", KeyFactory.keyToString(theCurrentUser.getKey()))
								.setParameter("eventId", this.eventId)
								.setParameter("isGame", theIsGame)
								.setParameter("status", Recipient.ARCHIVED_STATUS)
								.setParameter("wasViewed", theWasViewed)
								.getResultList();
						} else {
							inboxMessages = (List<Recipient>)em.createNamedQuery("Recipient.getByMemberIdAndEmailAddressAndEventIdAndEventTypeAndNotStatusAndNotViewed")
								.setParameter("memberId", KeyFactory.keyToString(theMember.getKey()))
								.setParameter("emailAddress", theCurrentUser.getEmailAddress())
								.setParameter("eventId", this.eventId)
								.setParameter("isGame", theIsGame)
								.setParameter("status", Recipient.ARCHIVED_STATUS)
								.setParameter("wasViewed", theWasViewed)
								.getResultList();
						}
						log.info("MessageThreadsResource.getMessageThreads(): found " + inboxMessages.size() + " (11)unviewed inbox messages with a specific event ID and with status = " + this.status);
					}
				}
				if(!theIsOutboxDisabled && (this.messageGroup.equalsIgnoreCase(MessageThread.OUTBOX_MESSAGE_GROUP) || this.messageGroup.equalsIgnoreCase(MessageThread.ALL_MESSAGE_GROUP))) {
					// do query for outbox data
					if(theMember == null) {
						outboxMessages = (List<MessageThread>)em.createNamedQuery("MessageThread.getBySenderUserIdAndEventIdAndEventTypeAndStatus")
							.setParameter("senderUserId", KeyFactory.keyToString(theCurrentUser.getKey()))
							.setParameter("eventId", this.eventId)
							.setParameter("isGame", theIsGame)
							.setParameter("status", this.status)
							.getResultList();
					} else {
						outboxMessages = (List<MessageThread>)em.createNamedQuery("MessageThread.getBySenderUserIdAndEventIdAndEventTypeAndStatusAndTeamId")
							.setParameter("senderUserId", KeyFactory.keyToString(theCurrentUser.getKey()))
							.setParameter("eventId", this.eventId)
							.setParameter("isGame", theIsGame)
							.setParameter("status", this.status)
							.setParameter("teamId", this.teamId)
						.getResultList();
					}
					log.info("MessageThreadsResource.getMessageThreads(): found " + outboxMessages.size() + " (12)outbox messages with any status");
				}
			}
		}
		
		// copy lists to 'out' parameters
		if(theInboxMessages != null && inboxMessages != null) {
			for(Recipient r : inboxMessages) {
				theInboxMessages.add(r);
			}
		}
		if(theOutboxMessages != null && outboxMessages != null) {
			for(MessageThread mt : outboxMessages) {
				theOutboxMessages.add(mt);
			}
		}
    }
 
    // theJson: JSON object with input to API
    // theMessageThread: out parameter to hold data extracted from the input JSON object
    // theMemberIds: out parameter to store member IDs extracted from the input JSON object
    // theCoordinatorsOnly: out parameter
    // theIncludeFans: out parameter
    private String handleJsonForCreateMessageThreadApi(JSONObject theJson, String theType, MessageThread theMessageThread, List<String> theMemberIds,
    		Boolean theCoordinatorsOnly, Boolean theIncludeFans)  throws JSONException {
    	String subject = null;
    	if(theJson.has("subject")) {
    		subject = theJson.getString("subject");
			theMessageThread.setSubject(subject);
		}
		
		String body = null;
    	if(theJson.has("body")) {
    		body = theJson.getString("body");
			theMessageThread.setMessage(body);
		}
		
		List<String> pollChoices = new ArrayList<String>();
		if(theJson.has("pollChoices")) {
			JSONArray pollChoicesJsonArray = theJson.getJSONArray("pollChoices");
			int arraySize = pollChoicesJsonArray.length();
			for(int i=0; i<arraySize; i++) {
				pollChoices.add(pollChoicesJsonArray.getString(i));
			}
			theMessageThread.setPollChoices(pollChoices);
		}
		
		if(theJson.has("recipients")) {
			JSONArray recipientsJsonArray = theJson.getJSONArray("recipients");
			int arraySize = recipientsJsonArray.length();
			log.info("json recipients array length = " + arraySize);
			for(int i=0; i<arraySize; i++) {
				log.info("storing member " + i);
				theMemberIds.add(recipientsJsonArray.getString(i));
			}
		}
		
		// defaults to 'false' if not specified
		theCoordinatorsOnly = false;
		if(theJson.has("coordinatorsOnly")) {
			theCoordinatorsOnly = theJson.getBoolean("coordinatorsOnly");
			log.info("json coordinatorsOnly = " + theCoordinatorsOnly.toString());
		}
		
		// defaults to 'false' if not specified
		theMessageThread.setIsAlert(false);
		String isAlertStr = null;
		if(theJson.has("isAlert")) {
			isAlertStr = theJson.getString("isAlert");
			log.info("json isAlert = " + isAlertStr);
		}
		if(isAlertStr != null && isAlertStr.equalsIgnoreCase("true")) {
			theMessageThread.setIsAlert(false);
		}
		
		// defaults to 'true' if not specified
		theIncludeFans = true;
		String includeFansStr = null;
		if(theJson.has("includeFans")) {
			includeFansStr = theJson.getString("includeFans");
			log.info("json includeFans = " + includeFansStr);
		}
		if(includeFansStr != null && !includeFansStr.equalsIgnoreCase("true")) {
			theIncludeFans = false;
		}
		
		// defaults to 'true' if not specified
		theMessageThread.setIsPublic(true);
		String isPublicStr = null;
		if(theJson.has("isPublic")) {
			isPublicStr = theJson.getString("isPublic");
			log.info("json isPublic = " + isPublicStr);
		}
		if(isPublicStr != null && !isPublicStr.equalsIgnoreCase("true")) {
			theMessageThread.setIsPublic(false);
		}
    	
		// Enforce Rules
		if(subject == null || subject.length() == 0 || body ==  null || body.length() == 0) {
			return ApiStatusCode.SUBJECT_BODY_AND_TYPE_REQUIRED;
		}
		else {
			if(MessageThread.isPoll(theType) && pollChoices.size() == 0) {
				return ApiStatusCode.POLL_AND_POLL_CHOICES_MUST_BE_SPECIFIED_TOGETHER;
			}
			if(MessageThread.isPoll(theType) && pollChoices.size() > MessageThread.MAX_NUMBER_OF_POLL_CHOICES) {
				return ApiStatusCode.INVALID_NUMBER_OF_POLL_CHOICES_PARAMETER;
			}
		}
		
		return ApiStatusCode.SUCCESS;
    }

    // theJson: JSON object with input to API
    // theMessageThread: out parameter to hold data extracted from the input JSON object and values set by this method
    // theMemberIds: out parameter to store member IDs extracted from the input JSON object
    // theCoordinatorsOnly: out parameter
    // theIncludeFans: out parameter
    private String buildWhoIsComingMessageThreadApi(MessageThread theMessageThread, String theLocalEventDateStr, Boolean theCoordinatorsOnly, Boolean theIncludeFans)  throws JSONException {
		if(theMessageThread.getEventId() == null && theMessageThread.getIsGame() == null) {
			return ApiStatusCode.EVENT_ID_AND_EVENT_TYPE_REQUIRED;
		}
    	
    	String subject = "Who's coming?";
    	theMessageThread.setSubject(subject);
		
    	String eventTypeStr = theMessageThread.getIsGame() ? Practice.GAME_EVENT_TYPE : Practice.PRACTICE_EVENT_TYPE;
		String body = "Are you coming to the " + eventTypeStr + " on " + theLocalEventDateStr + "?";
		theMessageThread.setMessage(body);
		
		List<String> pollChoices = new ArrayList<String>();
		pollChoices.add(MessageThread.YES_WHO_IS_COMING_CHOICE);
		pollChoices.add(MessageThread.NO_WHO_IS_COMING_CHOICE);
		pollChoices.add(MessageThread.MAYBE_WHO_IS_COMING_CHOICE);
		theMessageThread.setPollChoices(pollChoices);
		
		// who's coming is not just for coordinators
		theCoordinatorsOnly = false;
		
		// who's coming is always an alert
		theMessageThread.setIsAlert(true);
		
		// who's coming is always visible to all members
		theMessageThread.setIsPublic(true);
		
		// who's coming never includes fans
		theIncludeFans = false;
    	
    	return ApiStatusCode.SUCCESS;
    }

}  
