package com.stretchcom.rteam.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.restlet.data.Status;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

@Entity
@NamedQueries({
    @NamedQuery(
    		name="Recipient.getByKey",
    		query="SELECT r FROM Recipient r WHERE r.key = :key"
    ),
    @NamedQuery(
    		name="Recipient.getByMemberIdAndEmailAddress",
    		query="SELECT r FROM Recipient r WHERE " +
    		      "r.memberId = :memberId" + " AND " +
    		      "r.toEmailAddress = :emailAddress" + " AND " +
    		      "r.messageLinkOnly = FALSE"
    ),
    @NamedQuery(
    		name="Recipient.getByUserId",
    		query="SELECT r FROM Recipient r WHERE r.userId = :userId"  + " AND " +
    			  "r.messageLinkOnly = FALSE"
    ),
    @NamedQuery(
    		name="Recipient.getByMemberIdAndEmailAddressAndNotViewed",
    		query="SELECT r FROM Recipient r WHERE " +
    		      "r.memberId = :memberId" + " AND " +
    		      "r.toEmailAddress = :emailAddress" + " AND " +
    		      "r.wasViewed = :wasViewed"+ " AND " +
    		      "r.messageLinkOnly = FALSE"
    ),
    @NamedQuery(
    		name="Recipient.getByUserIdAndNotViewed",
    		query="SELECT r FROM Recipient r WHERE " +
    		      "r.userId = :userId" + " AND " +
    		      "r.wasViewed = :wasViewed" + " AND " +
    		      "r.messageLinkOnly = FALSE"
    ),
    @NamedQuery(
    		name="Recipient.getByMemberIdAndEmailAddressAndEventIdAndEventType",
    		query="SELECT r FROM Recipient r WHERE " +
    				"r.eventId = :eventId" + " AND " +
    				"r.isGame = :isGame" + " AND " +
    				"r.toEmailAddress = :emailAddress" + " AND " +
    				"r.memberId = :memberId"  + " AND " +
    				"r.messageLinkOnly = FALSE"
    ),
    @NamedQuery(
    		name="Recipient.getByUserIdAndEventIdAndEventType",
    		query="SELECT r FROM Recipient r WHERE " +
    				"r.eventId = :eventId" + " AND " +
    				"r.isGame = :isGame" + " AND " +
    				"r.userId = :userId" + " AND " +
    				"r.messageLinkOnly = FALSE"
    ),
    @NamedQuery(
    		name="Recipient.getByMemberIdAndEmailAddressAndEventIdAndEventTypeAndNotViewed",
    		query="SELECT r FROM Recipient r WHERE " +
    				"r.eventId = :eventId" + " AND " +
    				"r.isGame = :isGame" + " AND " +
    				"r.wasViewed = :wasViewed" + " AND " +
    				"r.toEmailAddress = :emailAddress" + " AND " +
    				"r.memberId = :memberId" + " AND " +
    				"r.messageLinkOnly = FALSE"
    ),
    @NamedQuery(
    		name="Recipient.getByUserIdAndEventIdAndEventTypeAndNotViewed",
    		query="SELECT r FROM Recipient r WHERE " +
    				"r.eventId = :eventId" + " AND " +
    				"r.isGame = :isGame" + " AND " +
    				"r.wasViewed = :wasViewed" + " AND " +
    				"r.userId = :userId" + " AND " +
    				"r.messageLinkOnly = FALSE"
    ),
    @NamedQuery(
    		name="Recipient.getByMemberIdAndEmailAddressAndNotStatus",
    		query="SELECT r FROM Recipient r WHERE " +
    		      "r.memberId = :memberId" + " AND " +
    		      "r.toEmailAddress = :emailAddress" + " AND " +
    		      "r.status <> :status" + " AND " +
    		      "r.messageLinkOnly = FALSE"
    ),
    @NamedQuery(
    		name="Recipient.getByNotStatus",
    		query="SELECT r FROM Recipient r WHERE " +
    		      "r.status <> :status" + " AND " +
    		      "r.messageLinkOnly = FALSE"
    ),
    @NamedQuery(
    		name="Recipient.getByUserIdAndNotStatus",
    		query="SELECT r FROM Recipient r WHERE " +
    		      "r.userId = :userId" + " AND " +
    		      "r.status <> :status" + " AND " +
    		      "r.messageLinkOnly = FALSE"
    ),
    @NamedQuery(
    		name="Recipient.getByMemberIdAndEmailAddressAndNotStatusAndNotViewed",
    		query="SELECT r FROM Recipient r WHERE " +
    		      "r.memberId = :memberId" + " AND " +
    		      "r.toEmailAddress = :emailAddress" + " AND " +
    		      "r.wasViewed = :wasViewed" + " AND " +
    		      "r.status <> :status" + " AND " +
    		      "r.messageLinkOnly = FALSE"
    ),
    @NamedQuery(
    		name="Recipient.getByUserIdAndNotStatusAndNotViewed",
    		query="SELECT r FROM Recipient r WHERE " +
    		      "r.userId = :userId" + " AND " +
    		      "r.wasViewed = :wasViewed" + " AND " +
    		      "r.status <> :status" + " AND " +
    		      "r.messageLinkOnly = FALSE"
    ),
    @NamedQuery(
    		name="Recipient.getByMemberIdAndEmailAddressAndEventIdAndEventTypeAndNotStatus",
    		query="SELECT r FROM Recipient r WHERE " +
    				"r.eventId = :eventId" + " AND " +
    				"r.isGame = :isGame" + " AND " +
    				"r.memberId = :memberId" + " AND " +
    				"r.toEmailAddress = :emailAddress" + " AND " +
    				"r.status <> :status" + " AND " +
    				"r.messageLinkOnly = FALSE"
    ),
    @NamedQuery(
    		name="Recipient.getByUserIdAndEventIdAndEventTypeAndNotStatus",
    		query="SELECT r FROM Recipient r WHERE " +
    				"r.eventId = :eventId" + " AND " +
    				"r.isGame = :isGame" + " AND " +
    				"r.userId = :userId" + " AND " +
    				"r.status <> :status" + " AND " +
    				"r.messageLinkOnly = FALSE"
    ),
    @NamedQuery(
    		name="Recipient.getByMemberIdAndEmailAddressAndEventIdAndEventTypeAndNotStatusAndNotViewed",
    		query="SELECT r FROM Recipient r WHERE " +
    				"r.eventId = :eventId" + " AND " +
    				"r.isGame = :isGame" + " AND " +
    				"r.memberId = :memberId" + " AND " +
    				"r.toEmailAddress = :emailAddress" + " AND " +
    				"r.wasViewed = :wasViewed" + " AND " +
    				"r.status <> :status" + " AND " +
    				"r.messageLinkOnly = FALSE"
    ),
    @NamedQuery(
    		name="Recipient.getByUserIdAndEventIdAndEventTypeAndNotStatusAndNotViewed",
    		query="SELECT r FROM Recipient r WHERE " +
    				"r.eventId = :eventId" + " AND " +
    				"r.isGame = :isGame" + " AND " +
    				"r.userId = :userId" + " AND " +
    				"r.wasViewed = :wasViewed" + " AND " +
    				"r.status <> :status" + " AND " +
    				"r.messageLinkOnly = FALSE"
    ),
    @NamedQuery(
    		name="Recipient.getByOneUseTokenAndTokenStatus",
    		query="SELECT r FROM Recipient r WHERE " +
    		      "r.oneUseToken = :oneUseToken" + " AND " +
    		      "r.oneUseTokenStatus = :oneUseTokenStatus"
    ),
    @NamedQuery(
    		name="Recipient.getByOneUseTokenAndTokenStatusAndNotStatus",
    		query="SELECT r FROM Recipient r WHERE " +
    		      "r.oneUseToken = :oneUseToken" + " AND " +
    		      "r.oneUseTokenStatus = :oneUseTokenStatus" + " AND " +
    		      "r.status <> :status"
    ),
    @NamedQuery(
    		name="Recipient.getByOneUseSmsTokenAndTokenStatusAndNotStatus",
    		query="SELECT r FROM Recipient r WHERE " +
    		      "r.oneUseSmsToken = :oneUseSmsToken" + " AND " +
    		      "r.oneUseTokenStatus = :oneUseTokenStatus" + " AND " +
    		      "r.status <> :status"
    ),
    @NamedQuery(
    		name="Recipient.getOldActiveThru",
    		query="SELECT r FROM Recipient r WHERE " + 
    				"r.activeThruGmtDate < :currentDate"  + " AND " +
    				"r.status = :status" + " AND " +
    				"r.messageLinkOnly = FALSE"
      ),
      @NamedQuery(
      		name="Recipient.getByEmailAddressAndNotStatus",
      		query="SELECT r FROM Recipient r WHERE " +
      				"r.toEmailAddress = :emailAddress" + " AND " +
      				"r.status <> :status" + " AND " +
      				"r.messageLinkOnly = FALSE"
      ),
})
public class Recipient {
	private static final Logger log = Logger.getLogger(Recipient.class.getName());
	
	//constants
	public static final String PENDING_NETWORK_AUTHENTICATION_STATUS = "pending_network_authentication";
	public static final String SENT_STATUS = "sent";
	public static final String SPECIAL_ATTENTION_STATUS = "special_attention";
	public static final String REPLIED_STATUS = "replied";
	public static final String FINALIZED_STATUS = "finalized";
	public static final String ARCHIVED_STATUS = "archived";
	
	public static final String CONFIRMED_REPLY_MESSAGE = "confirmed"; // when confirmed, reply field is set to this string
	
	public static final String NEW_TOKEN_STATUS = "new";
	public static final String USED_TOKEN_STATUS = "used";

	private String oneUseToken; // used when response is un-authenticated (i.e. response from non-rTeam client: email, facebook, etc)
	private String oneUseTokenStatus; // multiple messages can be sent, but only the first response is handled because
	                                  // the status is changed after that first response.
	private String oneUseSmsToken; // needed because SMS responses use phone number as token & multiple message types might get sent
	private String oneUseEmailToken; // used as a 'backup' to handle email replies where the body of reply does NOT contain embedded
	                                // rTeam token.
	private String subject;    // subject duplicated here so messageThread doesn't have to be referenced just to display subject
	private Text message;      // message duplicated here so messageThread doesn't have to be referenced just to display message
	private String followupMessage;
	private String type;       // type of message
	private String reply;      // reply to original message.  For a poll, this is the poll answer. For confirm, it is CONFIRMED_REPLY_MESSAGE
	private Date replyGmtDate;
	private String replyEmailAddress;  // EA of person in membership who replied to the message.
	private Date receivedGmtDate;
	private Date activeThruGmtDate;  // Active thru this date.  Member specific.
	private String teamId;     // all message and recipients associated with a specific team
    private String teamName;
	private String memberId;   // this combined with toEmailAddress determines the recipient membership and specific person inside member
	private String userId;     // userId of recipient
	private String memberName; // full name of primary member
	private String status;
	private int numOfSends;
	private Boolean beforeThreadFinalized;
	private Boolean wasViewed;
	private String eventId;    // soft schema: either a game ID or a practice ID
	private Date eventGmtStartDate;
	private Boolean isGame;
	private String eventName;  // only a practice has an event name, but store it anyhow
	private String senderMemberId;
	private String senderName;
	private String toEmailAddress;
	private Boolean messageLinkOnly; // true if recipient only exists for replying to message link (i.e should never be displayed in inbox)
	private String participantRole;

	@Transient
	private Boolean belongToUser;  // a transient attribute used for building recipient list associated with a specific user

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Key key;
	
	@ManyToOne(fetch = FetchType.LAZY)
    private MessageThread messageThread;

	@Basic
	private List<String> pollChoices;

	public Key getKey() {
        return key;
    }
	
	public MessageThread getMessageThread() {
		return messageThread;
	}

	public void setMessageThread(MessageThread messageThread) {
		this.messageThread = messageThread;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public String getMessage() {
		return message == null? null : message.getValue();
	}

	public void setMessage(String message) {
		this.message = new Text(message);
	}

	public String getFollowupMessage() {
		return followupMessage;
	}

	public void setFollowupMessage(String followupMessage) {
		this.followupMessage = followupMessage;
	}

	public String getReply() {
		return reply;
	}

	public void setReply(String reply) {
		this.reply = reply;
	}
	
	public Date getReplyGmtDate() {
		return replyGmtDate;
	}

	public void setReplyGmtDate(Date replyGmtDate) {
		this.replyGmtDate = replyGmtDate;
	}

	public Date getReceivedGmtDate() {
		return receivedGmtDate;
	}

	public void setReceivedGmtDate(Date receivedGmtDate) {
		this.receivedGmtDate = receivedGmtDate;
	}

	public String getTeamId() {
		return teamId;
	}

	public void setTeamId(String teamId) {
		this.teamId = teamId;
	}
	
    public String getMemberId() {
		return memberId;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}
	
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getMemberName() {
		return memberName;
	}

	public void setMemberName(String memberName) {
		this.memberName = memberName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
    
	public int getNumOfSends() {
		return numOfSends;
	}

	public void setNumOfSends(int numOfSends) {
		this.numOfSends = numOfSends;
	}

	public List<String> getPollChoices() {
		return pollChoices;
	}

	public void setPollChoices(List<String> pollChoices) {
		this.pollChoices = pollChoices;
	}
	
	public Boolean getBeforeThreadFinalized() {
		return beforeThreadFinalized;
	}

	public void setBeforeThreadFinalized(Boolean beforeThreadFinalized) {
		this.beforeThreadFinalized = beforeThreadFinalized;
	}

	public Boolean getWasViewed() {
		return wasViewed;
	}

	public void setWasViewed(Boolean wasViewed) {
		this.wasViewed = wasViewed;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public Boolean getIsGame() {
		return isGame;
	}

	public void setIsGame(Boolean isGame) {
		this.isGame = isGame;
	}

	public String getOneUseToken() {
		return oneUseToken;
	}

	public void setOneUseToken(String oneUseToken) {
		this.oneUseToken = oneUseToken;
	}
	
	public String getOneUseTokenStatus() {
		return oneUseTokenStatus;
	}

	public void setOneUseTokenStatus(String oneUseTokenStatus) {
		this.oneUseTokenStatus = oneUseTokenStatus;
	}
	
	public String getOneUseSmsToken() {
		return oneUseSmsToken;
	}

	public void setOneUseSmsToken(String oneUseSmsToken) {
		this.oneUseSmsToken = oneUseSmsToken;
	}

	public String getOneUseEmailToken() {
		return oneUseEmailToken;
	}

	public void setOneUseEmailToken(String oneUseEmailToken) {
		this.oneUseEmailToken = oneUseEmailToken;
	}

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public String getSenderMemberId() {
		return senderMemberId;
	}

	public void setSenderMemberId(String senderMemberId) {
		this.senderMemberId = senderMemberId;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}
	
	public String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	public String getToEmailAddress() {
		return toEmailAddress;
	}

	public void setToEmailAddress(String toEmailAddress) {
		this.toEmailAddress = toEmailAddress;
	}

	public String getReplyEmailAddress() {
		return replyEmailAddress;
	}

	public void setReplyEmailAddress(String replyEmailAddress) {
		this.replyEmailAddress = replyEmailAddress;
	}

	public Boolean getBelongToUser() {
		return belongToUser;
	}

	public void setBelongToUser(Boolean belongToUser) {
		this.belongToUser = belongToUser;
	}
	
	public Boolean getMessageLinkOnly() {
		return messageLinkOnly;
	}

	public void setMessageLinkOnly(Boolean messageLinkOnly) {
		this.messageLinkOnly = messageLinkOnly;
	}

    public Date getActiveThruGmtDate() {
		return activeThruGmtDate;
	}

	public void setActiveThruGmtDate(Date activeThruGmtDate) {
		this.activeThruGmtDate = activeThruGmtDate;
	}
	
	public Date getEventGmtStartDate() {
		return eventGmtStartDate;
	}

	public void setEventGmtStartDate(Date eventGmtStartDate) {
		this.eventGmtStartDate = eventGmtStartDate;
	}
	
	public String getParticipantRole() {
		return participantRole;
	}

	public void setParticipantRole(String participantRole) {
		this.participantRole = participantRole;
	}


	////////////////////
	// STATIC METHODS //
	////////////////////
    
	// theDateAdjustment: can be negative or positive. Never results in an ActiveThruGmtDate before 24 hours from now.
	public static void upateUserActiveThruGmtDate(User theUser, Integer theDateAdjustment) {
    	EntityManager emRecipients = EMF.get().createEntityManager();
    	
    	try {
    		List<Recipient> recipients = (List<Recipient>)emRecipients.createNamedQuery("Recipient.getByUserIdAndNotStatus")
				.setParameter("userId", KeyFactory.keyToString(theUser.getKey()))
				.setParameter("status", Recipient.ARCHIVED_STATUS)
				.getResultList();
    		
    		for(Recipient r : recipients) {
    			emRecipients.getTransaction().begin();
        		Date activeThruGmtDate = r.getActiveThruGmtDate();
        		if(activeThruGmtDate == null) {activeThruGmtDate = new Date();}

        		// the following line could subtract days in adjustment is negative
        		activeThruGmtDate = GMT.addDaysToDate(activeThruGmtDate, theDateAdjustment);
        		if(theDateAdjustment < 0 ) {activeThruGmtDate = GMT.setToFutureDate(activeThruGmtDate);}
        		r.setActiveThruGmtDate(activeThruGmtDate);
        		emRecipients.getTransaction().commit();
    		}
    		log.info("ActiveThruGmtDate adjusted: recipients count = " + recipients.size());
    		
    	} catch (Exception e) {
    		log.severe("Query Recipient.getByUserIdAndNotStatus failed");
    	} finally {
    		emRecipients.close();
    	}
    	return;
    }
	
	//::SMS::EVENT::
	// theToken: 10 digit phone number or real token
	// theSmsResponse: response SMS message received. May be NULL.
	public static String handleSmsResponse(String theToken, String theSmsResponse) {
		log.info("handleSmsResponse() entered, theToken = " + theToken);
		if(theSmsResponse == null) {theSmsResponse = "";}
		else {theSmsResponse = theSmsResponse.trim();}
		
		EntityManager emRecipient = EMF.get().createEntityManager();
		String returnResult = "";
		
		// It is possible this member has multiple outstanding confirm/poll responses. This one
		// response will answer all "matching" confirms/polls.
		try {
			List<Recipient> recipients = (List<Recipient>)emRecipient.createNamedQuery("Recipient.getByOneUseSmsTokenAndTokenStatusAndNotStatus")
				.setParameter("oneUseSmsToken", theToken)
				.setParameter("oneUseTokenStatus", Recipient.NEW_TOKEN_STATUS)
				.setParameter("status", Recipient.ARCHIVED_STATUS)
				.getResultList();
			
			if(recipients.size() == 0) {
				// UNSOLICITED MESSAGE
				if(theSmsResponse.length() > 0) {
					log.info("processing unsolicited SMS message = " + theSmsResponse);
					
					List<Member> smsConfirmedMemberships = (List<Member>)emRecipient.createNamedQuery("Member.getBySmsConfirmedPhoneNumber")
						.setParameter("phoneNumber", theToken)
						.getResultList();
					log.info("number of memberships with confirmed SMS phone number " + theToken + " = " + smsConfirmedMemberships.size());
					
					Member smsSender = null;
					if(smsConfirmedMemberships.size() > 0) {
						// TODO
						// ::PINGME::
						smsSender = smsConfirmedMemberships.get(0);
						if(smsConfirmedMemberships.size() > 1) {
							log.info("sender member of multiple teams - need PingMe feature");
						}
						sendReplyMessage(smsSender.getTeam(), theSmsResponse, emRecipient);
						returnResult = UserInterfaceMessage.UNSOLICITED_MESSAGE_SUCCESSFUL;
					}
				} else {
					returnResult = UserInterfaceMessage.UNSOLICITED_MESSAGE_SUCCESSFUL;
				}
				log.info("empty unsolicited SMS message received");
			} else {
				// REPLY to rTeam MESSAGE SENT
	    		int matchingPollCount = 0;
	    		int totalPollCount = 0;
	    		int confirmedMessageCount = 0;
	    		Boolean unsolicitiedReplySuccessful = false;
				for(Recipient r : recipients) {
					MessageThread messageThread = r.getMessageThread();
	    			if(messageThread.getType().equalsIgnoreCase(MessageThread.POLL_TYPE)) {
	    				totalPollCount++;
	    				
	    				String matchingPollChoice = doesResponseMatchPollChoices(theSmsResponse, r.getPollChoices());
	        			if(matchingPollChoice != null) {
	            			// This is a "first" response wins scenario. So mark other individuals that are part
	            			// of this same membership as having replied.
	            			List<Recipient> sameMemberrecipients = messageThread.getRecipients();
	            			for(Recipient smr : sameMemberrecipients) {
	            				if(r.getMemberId().equals(smr.getMemberId())) {
	                    			smr.setOneUseTokenStatus(Recipient.USED_TOKEN_STATUS);
	                    			smr.setReply(matchingPollChoice);
	                    			smr.setReplyGmtDate(new Date());
	                				messageThread.addMemberIdThatReplied(smr.getMemberId());
	            				}
	            			}
	        				matchingPollCount++;
	        			}
	    			} else if(messageThread.getType().equalsIgnoreCase(MessageThread.CONFIRMED_TYPE)) {
	    				r.setOneUseTokenStatus(Recipient.USED_TOKEN_STATUS);
	    				r.setReply(Recipient.CONFIRMED_REPLY_MESSAGE);
	    				r.setReplyGmtDate(new Date());
	    				messageThread.addMemberIdThatReplied(r.getMemberId());
	    				confirmedMessageCount++;
	    			} else {
	    				// Possible UNSOLICITED MESSAGE
	    				// Neither a Poll or Confirmation. If the reply length is non-zero, this is a unsolicited message
	    				// Note: This recipient was found using SMS token which is always set when message
	    				//       created just to handle unsolicited replies like this!
	    				if(theSmsResponse.length() > 0) {
	    					sendReplyMessage(r, theSmsResponse, emRecipient);
	    					unsolicitiedReplySuccessful = true;
	    				}
	    			}
	    		}
				
				if(matchingPollCount > 0 && confirmedMessageCount > 0) {
					returnResult = UserInterfaceMessage.POLL_AND_CONFIRMATION_RESPONSE_SUCCESSFUL;
				} else if(matchingPollCount > 0) {
					returnResult = UserInterfaceMessage.POLL_RESPONSE_SUCCESSFUL;
				} else if(confirmedMessageCount > 0) {
					returnResult = UserInterfaceMessage.CONFIRMATION_RESPONSE_SUCCESSFUL;
				} else if(unsolicitiedReplySuccessful){
					returnResult = UserInterfaceMessage.UNSOLICITED_RESPONSE_SUCCESSFUL;
				}
				
	    		log.info("recipients with token " + theToken + ": # outstanding polls = " + totalPollCount + " matching polls = " + matchingPollCount);
	    		log.info("number of recipients with token " + theToken + " confirmed via SMS = " + confirmedMessageCount);
			}
		} catch (Exception e) {
    		log.severe("handleSmsResponse(): Query Recipient.getByOneUseSmsTokenAndTokenStatusAndNotStatus failed. exception = " + e.getMessage());
    		return UserInterfaceMessage.SERVER_ERROR;
    	} finally {
    		emRecipient.close();
    	}
    	
    	return returnResult;
	}
	
	//::EMAIL::EVENT::
	public static String handleEmailReplyUsingToken(String theReplyBody, String theEmailReplyToken) {
		EntityManager emRecipient = EMF.get().createEntityManager();
		String returnResult = "";
		
		try {
			Recipient recipient = (Recipient)emRecipient.createNamedQuery("Recipient.getByOneUseTokenAndTokenStatusAndNotStatus")
				.setParameter("oneUseToken", theEmailReplyToken)
				.setParameter("oneUseTokenStatus", Recipient.NEW_TOKEN_STATUS)
				.setParameter("status", Recipient.ARCHIVED_STATUS)
				.getSingleResult();

			// TODO made need more sophisticated logic here to remove the original message.
			// typically, the reply of an email will contain the original message. Extract just the reply portion.
			log.info("full body of reply email = " +  theReplyBody);
			int indexOfOriginalMessage = theReplyBody.indexOf(recipient.getMessage());
			if(indexOfOriginalMessage > -1) {
				theReplyBody = theReplyBody.substring(0, indexOfOriginalMessage);
			}
			log.info("body of reply email less than original message = " +  theReplyBody);
    		
    		sendReplyMessage(recipient, theReplyBody, emRecipient);
    		returnResult = UserInterfaceMessage.UNSOLICITED_RESPONSE_SUCCESSFUL;
		} catch (NoResultException e) {
			log.severe("handleEmailReplyUsingToken(): recipient/team token not found");
			returnResult = UserInterfaceMessage.ALREADY_REPLIED;
		} catch (NonUniqueResultException e) {
			log.severe("handleEmailReplyUsingToken(): should never happen - two or more recipients/teams had the same token");
			returnResult = UserInterfaceMessage.SERVER_ERROR;
		} finally {
    		emRecipient.close();
    	}
    	
    	return returnResult;
	}
	
	//::EMAIL::EVENT::
	public static String handleEmailReplyUsingFromAddressAndSubject(String theReplyBody, String theFromAddress, String theSubject) {
		EntityManager emRecipient = EMF.get().createEntityManager();
		String returnResult = "";
		
		try {
			log.info("about to query recipients");
			List<Recipient> recipients = (List<Recipient>)emRecipient.createNamedQuery("Recipient.getByEmailAddressAndNotStatus")
				.setParameter("emailAddress", theFromAddress)
				.setParameter("status", Recipient.ARCHIVED_STATUS)
				.getResultList();
			log.info("number of recipients = " + recipients.size());
			
			Recipient matchingRecipient = null;
			if(recipients.size() == 1) {
				matchingRecipient = recipients.get(0);
			} else {
				// emailAddress has multiple active messages so response can be to any one of them.
				// This is expected and probably even typical.
				// Find Subject that matches the best.
				// TODO: if subjects don't match well, then send a PingMe message to determine which team member responding to.
				matchingRecipient = findRecipientWithBestSubjectMatch(recipients, theSubject);
			}
			
			log.info("matchingRecipient = " + matchingRecipient);
			if(matchingRecipient != null) {
	    		sendReplyMessage(matchingRecipient, theReplyBody, emRecipient);
	    		returnResult = UserInterfaceMessage.UNSOLICITED_RESPONSE_SUCCESSFUL;
			} else {
				returnResult = UserInterfaceMessage.INACTIVE_MESSAGE;
			}
		} catch (Exception e) {
			log.severe("handleEmailReplyUsingFromAddressAndSubject(): exception = " + e.getMessage());
			e.printStackTrace();
			returnResult = UserInterfaceMessage.SERVER_ERROR;
		} finally {
    		emRecipient.close();
    	}
    	
    	return returnResult;
	}
	
	private static void sendReplyMessage(Recipient theRecipient, String theReplyBody, EntityManager theEmRecipient) {
		MessageThread messageThread = theRecipient.getMessageThread();
		String senderUserId = messageThread.getSenderUserId();
		log.info("senderUserId = " + senderUserId);
		
		String teamId = theRecipient.getTeamId();
		log.info("teamId = " + teamId);
		Team team = null;
		try {
			team = (Team)theEmRecipient.createNamedQuery("Team.getByKey")
				.setParameter("key", KeyFactory.stringToKey(teamId))
				.getSingleResult();
			log.info("team retrieved = " + team.getTeamName());
		} catch (NoResultException e) {
			log.severe("sendReplyMessage(): team not found");
			return;
		} catch (NonUniqueResultException e) {
			log.severe("sendReplyMessage(): should never happen - two or more teams had the same token");
			return;
		}

		List<Member> memberRecipients = new ArrayList<Member>();
		List<Member> members = team.getMembers();

		if(senderUserId.equals(RteamApplication.RTEAM_USER_ID)) {
			/////////////////////////////////////////
			// replying to an automated rTeam message
			/////////////////////////////////////////
			
			// If there are any team coordinators, send reply to them; otherwise, send reply to all members that are users.
			///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    		
    		// assemble list of team coordinators
    		for(Member m : members) {
    			if(m.isCoordinator()) {memberRecipients.add(m);}
    		}
    		
    		// no coordinators, so assemble all members that have an associated user
    		if(memberRecipients.size() == 0) {
	    		for(Member m : members) {
	    			if(m.isAssociatedWithUser()) {memberRecipients.add(m);}
	    		}
    		}
    		
    		if(memberRecipients.size() > 0) {
	    		PubHub.sendReplyToEmailMessage(messageThread.getSubject(), team, memberRecipients, theReplyBody);
    		} else {
    			log.info("no reply sent since no team members are users");
    		}
		} else {
			//////////////////////////////
			// replying to a specific user
			//////////////////////////////
    		for(Member m : members) {
    			if(m.isAssociatedWithUser(senderUserId)) {
    				memberRecipients.add(m);
    				break;
    			}
    			if(memberRecipients.size() > 0) {
    				PubHub.sendReplyToEmailMessage(messageThread.getSubject(), team, memberRecipients, theReplyBody);
    			} else {
	    			log.info("no reply sent since sender of original message no longer a member of this team");
    			}
    		}
		}
	}
	
	private static void sendReplyMessage(Team theSenderTeam, String theReplyBody, EntityManager theEmRecipient) {
		List<Member> memberRecipients = new ArrayList<Member>();
		List<Member> members = theSenderTeam.getMembers();

		///////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// If there are any team coordinators, send reply to them; otherwise, send reply to all members that are users.
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		// assemble list of team coordinators
		for(Member m : members) {
			if(m.isCoordinator()) {memberRecipients.add(m);}
		}
		
		// no coordinators, so assemble all members that have an associated user
		if(memberRecipients.size() == 0) {
    		for(Member m : members) {
    			if(m.isAssociatedWithUser()) {memberRecipients.add(m);}
    		}
		}
		
		if(memberRecipients.size() > 0) {
    		PubHub.sendReplyToEmailMessage(UserInterfaceMessage.SMS_SUBJECT_FOR_UNSOLICITED_MESSAGE, theSenderTeam, memberRecipients, theReplyBody);
		} else {
			log.info("no reply sent since no team members are users");
		}
	}
	
	// TODO improve subject matching algorithm
	private static Recipient findRecipientWithBestSubjectMatch(List<Recipient> theRecipients, String theSubject) {
		Recipient bestSubjectMatch = null;
		Double bestPercentWordsMatching = 0.0;
		
		for(Recipient r : theRecipients) {
			int wordCount = 0;
			int matchingWords = 0;
			
			String rSubject = r.getSubject();
			
			StringTokenizer st = new StringTokenizer(rSubject);
			Boolean isFirstWord = true;
			while (st.hasMoreTokens()) {
				String word = st.nextToken();
				wordCount++;
				if(theSubject.contains(word)) {matchingWords++;}
			}
			
			if(wordCount > 0) {
				Double percentWordsMatching = (1.0 * matchingWords)/wordCount;
				if(percentWordsMatching > bestPercentWordsMatching) {
					bestPercentWordsMatching = percentWordsMatching;
					bestSubjectMatch = r;
				}
			}
		}
		return bestSubjectMatch;
	}

	// Returns the matching choice if there is a match, null otherwise
	private static String doesResponseMatchPollChoices(String thePollResponse, List<String> thePollChoices) {
		//::TODO make the matching more flexible
		log.info("attempting to match poll repsonse = " + thePollResponse);
		for(String pc : thePollChoices) {
			log.info("checking poll choice = " + pc);
			if(pc.equalsIgnoreCase(thePollResponse)) {
				return pc;
			}
		}
		return null;
	}

}
