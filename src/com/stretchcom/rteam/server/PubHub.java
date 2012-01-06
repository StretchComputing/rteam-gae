package com.stretchcom.rteam.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.restlet.data.Reference;
import org.restlet.data.Status;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class PubHub {
	private static final Logger log = Logger.getLogger(PubHub.class.getName());
	
	//constants
	// determined this max size by trial and error - not very scientific
	public static final Integer MAX_ALERT_MESSAGE_SIZE = 167;
	public static final Integer MAX_SMS_MESSAGE_SIZE = 160;
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Enrich messageThread communication. Called after a messageThread and Recipients have already been created.
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Add additional communication to message thread already created by calling routine.
	//      Type(s): MessageThread.CONFIRMED_TYPE, MessageThread.POLL_TYPE, MessageThread.WHO_IS_COMING, MessageThread.PLAIN_TYPE
	//      Recipients: passed in as a parameter
	//      Options: depends on message type -- could be a confirmation or poll.
	//      Conditional: Yes. Based on member access preferences.
	// #1 Message Thread
	//      Inbox: Calling method creates message thread and recipients, so none created here.
	//      Inbox New Message Count: incremented for each user recipient.
	//      Link Reply: handled by calling method
	// #2 Message(s) Sent
	//      Non-Users: Email message (or SMS, see below) always sent.
	//      Users: Optionally, email message sent(not implemented yet -- for now, no emails sent to users)
	//      Embedded Link(s): poll and confirm messages have embedded links.
	// #3 Alert and Badges Sent 
	//      Alert: sent to Users if requested
	//      Badge: update always sent to Users
	// #4 Post Activity
	//      Yes
	// #5 SMS
	//      SMS confirmed members are sent a text message. Supports text responses for confirmations and polls.
	// -----------
	// PARAMETERS:
	// -----------
	// theAuthorizedTeamRecipients: memberInfos (with unique email addresses/phone numbers) that should receive communication.
	//                              Only contains email addresses and SMS info if they have been confirmed and are ok to send.
	// theSubject: subject of the message thread (just get from messageThread below?)
	// theBody: body of message thread (just get from messageThread below?)
	// theMessageThread: that was created by calling routine
	// theTeam: team that recipients receiving messageThread belong to.
	// theIsFollowup: true if the message being sent is a followup message which are handled differently
	// theSenderName: sender's full name used in message signature
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static void sendMessageThreadToMembers(List<UserMemberInfo> theAuthorizedTeamRecipients, String theSubject, String theBody,
    		                                      MessageThread theMessageThread, Team theTeam, Boolean theIsFollowup, String theSenderName) {

		///////////////////////////////////////////////////////////////////////////////////////////////////////////
    	// Split the UserMemberInfos into 3 lists
		// ---------------------------------------
		//  - userInfos:    Users that received the rTeam message via calling routine. Need for alerts and badges.
		//  - messageInfos: Members that will be sent an email message by this routine
		//  - smsInfos:     Members that will be sent a SMS message by this routine.
		//
		// Based on member 'access' preferences, a UserMemberInfo object can end up in multiple lists meaning a
		// member can receive the same message via multiple access paths.
		///////////////////////////////////////////////////////////////////////////////////////////////////////////
		List<UserMemberInfo> userInfos = new ArrayList<UserMemberInfo>();
		List<UserMemberInfo> messageInfos = new ArrayList<UserMemberInfo>();
		List<UserMemberInfo> smsInfos = new ArrayList<UserMemberInfo>();
		List<Key> userKeys = new ArrayList<Key>();
		List<User> alertBadgeUsers = new ArrayList<User>();
		for(UserMemberInfo umi : theAuthorizedTeamRecipients) {
			String userId = umi.getUserId();
			if(userId != null && umi.getHasRteamMessageAccessEnabled()) {
				userKeys.add(KeyFactory.stringToKey(userId));
				userInfos.add(umi);
			}
			if(umi.getEmailAddress() != null && umi.getHasEmailMessageAccessEnabled()) {
				messageInfos.add(umi);
			}
			if( (umi.getPhoneNumber() != null || umi.getSmsEmailAddress() != null) && umi.getHasSmsMessageAccessEnabled()) {
				smsInfos.add(umi);
			}
			if(!umi.isAnyMessageAccessEnabled()) {
				log.severe("No message access enabled for member = " + umi.getFullName());
			}
		}
		
		String fromUserName = theMessageThread.getType();
		if(fromUserName == null){
			fromUserName = Emailer.REPLY;
		} else if(MessageThread.isPoll(fromUserName)) {
			fromUserName = Emailer.POLL;
		} else if(MessageThread.isConfirm(fromUserName)) {
			fromUserName = Emailer.CONFIRM;
		} else {
			fromUserName = Emailer.REPLY;
		}

		////////////////////////////////////////////
		// #1 Message Thread: increment message count
		////////////////////////////////////////////
		getUserFromKeysAndIncrementMessageCount(userKeys, alertBadgeUsers);

		/////////////////////
		// #2 Message(s) Sent
		/////////////////////
		for(UserMemberInfo umi : messageInfos) {
    		String fullName = umi.getFirstName() + " " + umi.getLastName();
    		String body = null;
    		List<String> pollChoices = theMessageThread.getPollChoices();
    		if(theMessageThread.isConfirm()) {
    			body = Emailer.getConfirmedMessageThreadEmailBody(fullName, theBody, umi.getOneUseToken(), theTeam.getTeamName(), theTeam.getTwitterScreenName(), theSenderName);
    		} else if(theMessageThread.isPoll()) {
    			if(theIsFollowup) {
        			body = Emailer.getFollowupMessageThreadEmailBody(fullName, theBody, umi.getOneUseToken(), pollChoices, theTeam.getTeamName(), theTeam.getTwitterScreenName(), theSenderName);
    			} else {
        			body = Emailer.getPollMessageThreadEmailBody(fullName, theBody, umi.getOneUseToken(), pollChoices, theTeam.getTeamName(), theTeam.getTwitterScreenName(), theSenderName);
    			}
    		} else {
        		body = Emailer.getPlainMessageThreadEmailBody(fullName, theBody, umi.getOneUseToken(), theTeam.getTeamName(), theTeam.getTwitterScreenName(), theSenderName);
    		}
    		
    		Emailer.send(umi.getEmailAddress(), theSubject, body, fromUserName);
    	}
    	
		///////////////////////////
		// #3 Alert and Badges sent
		///////////////////////////
		// strip "rteam:" from subject if it exists
		theSubject = theSubject.replaceFirst("r[tT]eam:", "");
		String alertMessage = buildAlertMessage(theSubject, theBody, theSenderName);
		sendAlertBadges(alertBadgeUsers, theMessageThread.getIsAlert(), alertMessage);
		
		///////////////////
		// #4 Post Activity
		///////////////////
		// Only post if the message is being sent to the entire team and it is NOT an event messages.  Event messages are
		// not posted because they considered very targeted messages that are only meaningful in the context of a event.
		Boolean includeEntireTeam = theMessageThread.getIncludeEntireTeam() == null ? false : theMessageThread.getIncludeEntireTeam();
		Boolean isEventMessage = theMessageThread.getEventId() != null ? true : false;
		if(includeEntireTeam && !isEventMessage) {
			String messageTypeStr = "msg";
    		if(theMessageThread.isPoll()) {
    			if(theIsFollowup) {
    				messageTypeStr = "poll finalized";
    			} else {
        			messageTypeStr = "poll";
    			}
    		} else if(theMessageThread.getIsAlert()) {
    			messageTypeStr = "alert";
    		}
    		
    		Key userKey = KeyFactory.stringToKey(theMessageThread.getSenderUserId());
			User sender = User.getUserFromUserKey(userKey);
			
			if(sender != null) {
				String userIdentifier = sender.getDisplayName();
				String status = userIdentifier + " " + messageTypeStr + ": " + "[" + theSubject + "] " + theBody;
				postActivity(theTeam, status, true);
			}
		}
		
		//////////
		// #5 SMS 
		//////////
		for(UserMemberInfo umi : smsInfos) {
			// SMS address could be just a phone number or an SMS email address
			String smsAddress = umi.getSmsEmailAddress() == null ? umi.getPhoneNumber() : umi.getSmsEmailAddress();
			
			Boolean isSmsAddress = false;
			if(smsAddress.contains("@")) {
				isSmsAddress = true;
			}
			
			String smsMessage = getMessageThreadSmsMessage(theBody, theMessageThread, theIsFollowup, isSmsAddress);
			postSms(smsAddress, theSubject, smsMessage, fromUserName);
		}
		
//		// TEST CODE FOR NOW *****************************************************************
//		String smsMessage = theSubject + ": " + theBody;
//		if(theSubject.toLowerCase().contains("nicksms")) {
//	    	postSms("7089458201", smsMessage);
//		} else if(theSubject.toLowerCase().contains("joesms")) {
//	    	postSms("6302156979", smsMessage);
//		} else if(theSubject.toLowerCase().contains("homesms")) {
//	    	postSms("7082461682", smsMessage);
//		}
//		//************************************************************************************
    }
	
	private static String getMessageThreadSmsMessage(String theBody, MessageThread theMessageThread,
													 Boolean theIsFollowup, Boolean theIsSmsAddress) {
		StringBuffer sb = new StringBuffer();
		sb.append(" ");
		if(theMessageThread.isPoll() && theIsFollowup) {
			sb.append("(poll finalized)");
			sb.append("\n");
		}
		sb.append(theBody);
		sb.append("\n");
		
		if(theMessageThread.isConfirm()) {
			if(theIsSmsAddress) {
				sb.append("[Please reply with any message to confirm]");
			} else {
				sb.append("Please reply\nrTeam confirm\n");
			}
		} else if(theMessageThread.isPoll()) {
			List<String> pollChoices = theMessageThread.getPollChoices();
			if(theIsFollowup) {
				Map<String, Integer> pollResults =  theMessageThread.getPollResults();
				sb.append("results: ");
				for(String pc : pollChoices) {
					Integer pollCount = pollResults.get(pc);
					if(pollCount != null) {
						sb.append(pc);
						sb.append("(");
						sb.append(pollCount.toString());
						sb.append(") ");
					}
				}
			} else {
				if(theIsSmsAddress) {
	    			sb.append("Please reply [");
				} else {
	    			sb.append("Please reply\nrTeam [");
				}
    			int i = 0;
    			for(String pc : pollChoices) {
    				if(i != 0) sb.append(",");
        			sb.append(pc);
        			i++;
    			}
    			sb.append("]\n");
			}
		}
		return sb.toString();
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Creates a single message thread with one or more recipients.
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Creates a MessagThread and adds a recipient for each UMI. 
	// -----------------------------------
	// Called for the following scenarios:
	// -----------------------------------
	// 1. MessageThread created to send messages to users which end up in their inbox (theMessageLinkOnly = false)
	// 2. MessageThread created so replies to "embedded links" can be handled (theMessageLinkOnly = true)
	// -----------
	// PARAMETERS:
	// -----------
	// theAuthorizedTeamRecipients: complete recipient list.
	// theTeam: (strong assumption) all recipients are on the same team. May be null.
	// theSubject: Subject of the message thread.
	// theBodys: Used to set each recipient body. Supports personalized messages so each recipient body may be different.
	//           If only one body is provided, it is used for all recipients. May be null.
	// theMessageType: Supports several message types including auto-generated types.
	// theMessageLinkOnly: Used to set field of same name in recipient. If true, message is never shown in user's inbox.
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static void createMessageThread(List<UserMemberInfo> theAuthorizedTeamRecipients, Team theTeam, String theSubject,
			       List<String> theBodys, String theMessageType, Boolean theMessageLinkOnly) {
    	EntityManager em = EMF.get().createEntityManager();
    	
    	/////////////////////////
    	// DEBUG INPUT PARAMETERS
    	/////////////////////////
//    	log.info("theSubject = " + theSubject);
//    	if(theBodys == null) {
//    		log.info("theBodys = NULL");
//    	} else {
//    		log.info("size of theBodys = " + theBodys.size());
//    	}
//    	log.info("theMessageType = " + theMessageType);
//    	if(theTeam == null) {
//    		log.info("theTeam = NULL");
//    	} else {
//    		log.info("theTeam = " + theTeam.getTeamName());
//    	}
//    	log.info("theMessageLinkOnly = " + theMessageLinkOnly);
    	
    	
    	try {
        	// first, create the MessageThread and Recipient objects
        	MessageThread messageThread = new MessageThread();
    		Date gmtStartDate = new Date();
    		messageThread.setCreatedGmtDate(gmtStartDate);
    		messageThread.setSubject(theSubject);
    		
    		String body = Emailer.MESSAGE_THREAD_BODY;
    		if(theBodys == null) {
    			body = Emailer.MESSAGE_LINK_REPLY_BODY;
    		}
    		
    		messageThread.setMessage(body);
    		messageThread.setStatus(MessageThread.ACTIVE_STATUS);
    		messageThread.setType(theMessageType);
    		if(theTeam != null) messageThread.setTeamName(theTeam.getTeamName());
    		
    		//not a real user ID, but could be useful for queries later on 
    		messageThread.setSenderUserId(RteamApplication.RTEAM_USER_ID);
    		
    		List<Recipient> recipients = new ArrayList<Recipient>();
    		int index = 0;
    		for(UserMemberInfo atr : theAuthorizedTeamRecipients) {
    			Recipient recipient = new Recipient();
    			recipient.setMessageLinkOnly(theMessageLinkOnly);
    			if(theTeam != null) recipient.setTeamId(KeyFactory.keyToString(theTeam.getKey()));
    			
    			////////////////////////////////////////////////////////////////////////////////////////////////////////////
    			// Most messages type set both the recipient user id and member id. Email confirmation messages set one or
    			// the other. Also, member email confirmations provide only a single member ID.
    			////////////////////////////////////////////////////////////////////////////////////////////////////////////
    			recipient.setUserId(atr.getUserId());
    			recipient.setMemberId(atr.getMemberId());
    			
    			recipient.setToEmailAddress(atr.getEmailAddress());
    			recipient.setSubject(theSubject);
    			
        		if(theBodys == null) {
        			body = Emailer.MESSAGE_LINK_REPLY_BODY;
        		} else {
        			// if only one body provided, use the same one for all recipients
        			if(theBodys.size() == 1) {
            			body = theBodys.get(0);
        			} else {
            			body = theBodys.get(index);
        			}
        		}
    			recipient.setMessage(body);
    			
    			recipient.setStatus(Recipient.SENT_STATUS);
    			recipient.setNumOfSends(1);
    			recipient.setReceivedGmtDate(new Date());
    			recipient.setWasViewed(false);
    			if(theTeam != null) recipient.setTeamName(theTeam.getTeamName());
    			
    			// tokens are only passed in if user needs to respond to message link(s)
    			if(atr.getOneUseToken() != null || atr.getOneUseSmsToken() != null) {
    				recipient.setOneUseToken(atr.getOneUseToken());
    				recipient.setOneUseSmsToken(atr.getOneUseSmsToken());
    				recipient.setOneUseTokenStatus(Recipient.NEW_TOKEN_STATUS);
    			}
    			
    			log.info("adding recipient = " + recipient.toString());
    			recipients.add(recipient);
    			index++;
    		}
    		messageThread.setRecipients(recipients);
    		messageThread.setNumOfRecipients(recipients.size());
    		
    		em.getTransaction().begin();
    		em.persist(messageThread);
    	    em.getTransaction().commit();
    	    String keyWebStr = KeyFactory.keyToString(messageThread.getKey());
    	    log.info("message thread " + messageThread.getSubject() + " with key " + keyWebStr + " created successfully");
    	} finally {
    		em.close();
    	}
    }
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Welcome Email Message for New User
	//   User must reply to email to fully activate account and become Network Authenticated.
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Welcome new user and allow user to fully activate account by network authenticating.
	//      Type: MessageThread.USER_ACTIVATION_TYPE
	//      Recipients: new user.
	//      Options: [Activate Email Address]
	//      Conditional: No. Always sent to new user.
	// #1 Message Thread Created
	//      Inbox: not displayed.
	//      Inbox New Message Count: not applicable.
	//      Link Reply Configured: yes.
	// #2 Message(s) Sent
	//      Non-Users: not applicable.
	//      Users: email message is always sent.
	//      Embedded Link(s): yes.
	// #3 Alert and Badges Sent 
	//      Alert: not applicable.
	//      Badge: not applicable.
	// #4 Post Activity
	//      Not applicable.
	// #5 SMS
	//      Not applicable.
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static void sendUserWelcomeMessage(User theUser) {
	   	String subject = "Welcome to rTeam";
	   	
		String oneUseToken = TF.get();
    	String url = RteamApplication.BASE_URL_WITH_SLASH + RteamApplication.GWT_HOST_PAGE + "?uaoutoken=" + oneUseToken;
    	
    	String messageType = MessageThread.USER_ACTIVATION_TYPE;
    	String body = Emailer.getEmailActivationBody(theUser.getFullName(), url, oneUseToken, messageType, null, null);
    	List<String> messageBodys = new ArrayList<String>();
    	messageBodys.add(body);
    	
    	List<UserMemberInfo> welcomeMsgRecipient = new ArrayList<UserMemberInfo>();
    	UserMemberInfo umi = new UserMemberInfo();
    	umi.setEmailAddress(theUser.getEmailAddress());
    	umi.setOneUseToken(oneUseToken);
    	umi.setUserId(KeyFactory.keyToString(theUser.getKey()));
    	welcomeMsgRecipient.add(umi);
    	
    	////////////////////////////
    	// #1 Message Thread Created
    	////////////////////////////
    	//createMessageThread(theAuthorizedTeamRecipients, theTeam, theSubject, theBodys, theMessageType, theMessageLinkOnly)
    	createMessageThread(welcomeMsgRecipient, null, subject, messageBodys, messageType, true);
    		
    	////////////////////////////
    	// #2 Message(s) Sent: email
    	////////////////////////////
    	Emailer.send(theUser.getEmailAddress(), subject, body, MessageThread.CONFIRMED_TYPE);
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Phone Number Confirmation SMS for New User
	//   User must enter phone number confirmation code sent in SMS to confirm their phone number.
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Allow new user to confirm their phone number if it was entered.
	//      Type: not applicable
	//      Recipients: none.
	//      Options: not applicable.
	//      Conditional: No. Sent to allow new user to confirm phone number.
	// #1 Message Thread Created
	//      not applicable.
	// #2 Message(s) Sent
	//      not applicable
	// #3 Alert and Badges Sent 
	//      not applicable.
	// #4 Post Activity
	//      not applicable.
	// #5 SMS
	//      Uses mobile carrier email address to send the SMS.
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static void sendPhoneNumberConfirmation(String theSmsEmailAddress, String thePhoneNumberConfirmationCode) {
		if(theSmsEmailAddress == null || thePhoneNumberConfirmationCode == null) {return;}
		
		//////////
		// #5 SMS 
		//////////
		String smsMessage = getPhoneNumberConfirmationSmsMessage(thePhoneNumberConfirmationCode);
		postSms(theSmsEmailAddress, "Confirm Code", smsMessage, Emailer.NO_REPLY);
	}
    private static String getPhoneNumberConfirmationSmsMessage(String thePhoneNumberConfirmationCode) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("Your phone number");
    	sb.append("\n");
    	sb.append("confirmation code is");
    	sb.append("\n");
    	sb.append(thePhoneNumberConfirmationCode);
    	return sb.toString();
    }
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Email Address Changed for Existing User
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Allow user to network authenticate new email address.
	//      Type: MessageThread.EMAIL_UPDATE_CONFIRMATION_TYPE
	//      Recipients: single user who changed their email address.
	//      Options: [Activate New Email Address]
	//      Conditional: No. Always sent when member email address changes.
	// #1 Message Thread Created
	//      Inbox: not displayed.
	//      Inbox New Message Count: not applicable.
	//      Link Reply Configured: yes.
	// #2 Message(s) Sent
	//      Non-Users: not applicable.
	//      Users: email message always sent.
	//      Embedded Link(s): Yes.
	// #3 Alert and Badges Sent 
	//      Not applicable.
	// #4 Post Activity
	//      Not applicable.
	// #5 SMS
	//      Not applicable.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static void sendUserEmailUpdateMessage(User theUser) {
	   	String subject = "rTeam: please confirm new email address";
	   	
		String oneUseToken = TF.get();
    	String url = RteamApplication.BASE_URL_WITH_SLASH + RteamApplication.GWT_HOST_PAGE + "?uaoutoken=" + oneUseToken;

    	String messageType = MessageThread.EMAIL_UPDATE_CONFIRMATION_TYPE;
    	String body = Emailer.getEmailActivationBody(theUser.getFullName(), url, oneUseToken, messageType, null, null);
    	
    	List<UserMemberInfo> messageRecipient = new ArrayList<UserMemberInfo>();
    	UserMemberInfo umi = new UserMemberInfo();
    	umi.setEmailAddress(theUser.getEmailAddress());
    	umi.setOneUseToken(oneUseToken);
    	umi.setUserId(KeyFactory.keyToString(theUser.getKey()));
    	messageRecipient.add(umi);
    	
    	////////////////////////////
    	// #1 Message Thread Created
    	////////////////////////////
    	//createMessageThread(theAuthorizedTeamRecipients, theTeam, theSubject, theBodys, theMessageType, theMessageLinkOnly)
    	createMessageThread(messageRecipient, null, subject, null, messageType, true);
    	
    	////////////////////////////
    	// #2 Message(s) Sent: email
    	////////////////////////////
    	Emailer.send(theUser.getEmailAddress(), subject, body, MessageThread.CONFIRMED_TYPE);
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Team Membership Welcome Email Message
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Each individual in the membership given option to network authenticate their email address if any.
	//      Type: MessageThread.MEMBER_ACTIVATION_TYPE
	//      Recipients: one for each individual associated with the membership
	//      Options: [Accept Membership, Not Accept Membership]
	//      Conditional: No. Always sent to new members.
	//      Notes: SMS is not sent to new SMS members here because we don't know member's carrier. So initial SMS
	//             is sent by phone.
	// #1 Message Thread Created
	//      Inbox: Displayed for users. 
	//      Inbox New Message Count: incremented for each user.
	//      Link Reply Configured: Yes.
	// #2 Message(s) Sent
	//      Non-Users: Always receive an email.
	//      Users: right now, email always sent (see TODO above)
	//      Embedded Link(s): Yes.
	// #3 Alert and Badges Sent 
	//      Not applicable.
	// #4 Post Activity
	//      Not applicable.
	// #5 SMS
	//      Not applicable.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static void sendMemberWelcomeMessage(Member theMember, List<String> theNewMemberEmailAddresses, User theInitiator) {
		// for members, it is possible that an email address was not specified. If not, just return
		if(theNewMemberEmailAddresses.size() == 0) {
			log.info("sendMemberWelcomeMessage(): email address list was empty - no work to do ...");
			return;
		}
		
	   	String subject = "Welcome to rTeam";
	   	
		String oneUseToken = null;
    	String body = "";
    	
    	////////////////////////////
    	// #2 Message(s) Sent: email
    	String messageType = MessageThread.MEMBER_ACTIVATION_TYPE;
    	List<UserMemberInfo> messageRecipients = new ArrayList<UserMemberInfo>();
    	for(String ea : theNewMemberEmailAddresses){
    		UserMemberInfo umi = new UserMemberInfo();
    		oneUseToken = TF.get();
    		umi.setOneUseToken(oneUseToken);
    		umi.setEmailAddress(ea);
    		umi.setMemberId(KeyFactory.keyToString(theMember.getKey()));
    		messageRecipients.add(umi);
    		
			String encodedEmailAddress = Reference.encode(ea);
	    	String url = RteamApplication.BASE_URL_WITH_SLASH + RteamApplication.GWT_HOST_PAGE + "?uaoutoken=" + oneUseToken + "&" + "emailAddress=" + encodedEmailAddress;
	    	body = Emailer.getEmailActivationBody(theMember.getFullNameByEmailAddress(ea), url, oneUseToken, messageType, theMember, theInitiator);
	    	Emailer.send(ea, subject, body, MessageThread.CONFIRMED_TYPE);
    	}
    	
    	////////////////////////////
    	// #1 Message Thread Created
    	////////////////////////////
    	//createMessageThread(theAuthorizedTeamRecipients, theTeam, theSubject, theBodys, theMessageType, theMessageLinkOnly)
    	createMessageThread(messageRecipients, null, subject, null, messageType, true);
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Team Membership Welcome Message for Users
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Notify user they have been added to a new team
	//      Type: MessageThread.MEMBER_ACTIVATION_TYPE
	//      Recipients: one for each user passed in
	//      Options: Not applicable.
	//      Conditional: No. Always sent.
	// #1 Message Thread Created
	//      Inbox: Displayed for users. 
	//      Inbox New Message Count: incremented for each user.
	//      Link Reply Configured: Yes.
	// #2 Message(s) Sent
	//      Not applicable.
	// #3 Alert and Badges Sent 
	//      Alert: not applicable.
    //      Badge: update is always sent to Users
	// #4 Post Activity
	//      Yes.
	// #5 SMS
	//      Not applicable.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static void sendMemberWelcomeMessageToUsers(Member theMember, List<User> theUsersWithNewMembership, Team theTeam, User theInitiator) {
		if(theUsersWithNewMembership.size() == 0) {
			log.info("sendMemberWelcomeMessageToUsers(): user list was empty - no work to do ...");
			return;
		}
		
	   	String subject = "Welcome to '" + theTeam.getTeamName() + "' team";

    	List<String> messageBodys = new ArrayList<String>();
    	List<UserMemberInfo> messageRecipients = new ArrayList<UserMemberInfo>();

    	String messageType = MessageThread.MEMBER_ACTIVATION_TYPE;
    	for(User user : theUsersWithNewMembership){
    		UserMemberInfo umi = new UserMemberInfo();
    		umi.setEmailAddress(user.getEmailAddress());
    		umi.setUserId(KeyFactory.keyToString(user.getKey()));
    		umi.setTeam(theTeam);
    		umi.setMemberId(KeyFactory.keyToString(theMember.getKey()));
    		messageRecipients.add(umi);
			
    		String messageBody = getMemberWelcomeMessageToUsersMessageBody(theTeam, theInitiator);
    		messageBodys.add(messageBody);
		
    		///////////////////
    		// #4 Post Activity
    		///////////////////
        	StringBuffer sb = new StringBuffer();
        	sb.append("We welcome ");
        	sb.append(user.getFullName());
        	if(theMember.isGuardian(user.getEmailAddress())) {
        		sb.append(" (parent of ");
        		sb.append(theMember.getFullName());
        		sb.append(")");
        	}
        	sb.append(" as a new ");
        	sb.append(theMember.getParticipantRole());
        	sb.append(" of the ");
        	sb.append(theTeam.getTeamName());
    		
    		postActivity(theTeam, sb.toString());
    	}
    	
    	////////////////////////////
    	// #1 Message Thread Created
    	////////////////////////////
    	//createMessageThread(theAuthorizedTeamRecipients, theTeam, theSubject, theBodys, theMessageType, theMessageLinkOnly)
    	createMessageThread(messageRecipients, theTeam, subject, messageBodys, messageType, false);
    	
    	//////////////////////////////////////////////////////////////////
		// #3 Alert and Badges sent: badge only, alert does not apply here
		//////////////////////////////////////////////////////////////////
		sendAlertBadges(theUsersWithNewMembership, false, null);
	}
    
    private static String getMemberWelcomeMessageToUsersMessageBody(Team theTeam, User theInitiator) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("You have been added as a member to team '");
    	sb.append(theTeam.getTeamName());
    	sb.append("' by ");
    	sb.append(theInitiator.getFullName());
    	sb.append(". ");
    	return sb.toString();
    }

    
    // PRIORITY TODO If user, inbox message must be a poll to allow user to reply. Need to handle that reply as well.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Delete Confirmation Message
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Allow team member to accept or reject their membership being deleted.
	//      Type: MessageThread.MEMBER_DELETE_CONFIRMATION_TYPE
	//      Recipients: all individuals associated with membership that someone is trying to delete.
	//      Options: [Accept Membership Delete, Reject Membership Delete]
	//      Conditional: No. Always sent.
	// #1 Message Thread Created
	//      Inbox: Displayed for users. 
	//      Inbox New Message Count: incremented for each user.
	//      Link Reply Configured: Yes.
	// #2 Message(s) Sent
	//      Non-Users: Always receive an email.
	//      Users: right now, email always sent (see TODO above)
	//      Embedded Link(s): Yes.
	// #3 Alert and Badges Sent 
	//      Not applicable.
	// #4 Post Activity
	//      Not applicable.
	// #5 SMS
	//      Not applicable.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void sendDeleteConfirmationMessage(Member theMember) {
	   	String subject = "rTeam: request made to delete your team membershp";
	   	
		String oneUseToken = null;
		List<String> messageBodys = new ArrayList<String>();
		List<String> naEmailAddresses = theMember.getNetworkAuthenticatedActiveEmailAddresses();
    	List<UserMemberInfo> messageRecipients = new ArrayList<UserMemberInfo>();
    	
    	////////////////////////////
    	// #2 Message(s) Sent: email
    	////////////////////////////
    	String emailBody = "";
    	String messageBody = "";
    	String messageType = MessageThread.MEMBER_DELETE_CONFIRMATION_TYPE;
    	for(String naEA : naEmailAddresses){
    		UserMemberInfo umi = new UserMemberInfo();
    		umi.setEmailAddress(naEA);
    		oneUseToken = TF.get();
    		umi.setOneUseToken(oneUseToken);
    		umi.setTeam(theMember.getTeam());
    		umi.setMemberId(KeyFactory.keyToString(theMember.getKey()));
    		messageRecipients.add(umi);
    		
	    	String url = RteamApplication.BASE_URL_WITH_SLASH + RteamApplication.GWT_HOST_PAGE + "?delconftoken=" + oneUseToken;
	    	emailBody = Emailer.getDeleteConfirmationEmailBody(theMember, theMember.getFullNameByEmailAddress(naEA), oneUseToken, url, messageType);
	    	messageBody = getDeleteConfirmationMessageBody(theMember, url, messageType);
	    	messageBodys.add(messageBody);
	    	Emailer.send(naEA, subject, emailBody, MessageThread.CONFIRMED_TYPE);
		}

    	////////////////////////////
    	// #1 Message Thread Created
    	////////////////////////////
    	if(naEmailAddresses.size() > 0) {
        	//createMessageThread(theAuthorizedTeamRecipients, theTeam, theSubject, theBodys, theMessageType, theMessageLinkOnly)
        	createMessageThread(messageRecipients, theMember.getTeam(), subject, messageBodys, messageType, false);
    	}
    }

    
    private static String getDeleteConfirmationMessageBody(Member theMember, String theBaseUrl, String theMessageType) {
    	StringBuffer sb = new StringBuffer();
    	sb.append(theMember.getMarkedForDeletionRequester());
    	sb.append(" has requested that your membership on ");
    	sb.append(theMember.getTeam().getTeamName());
    	sb.append(" team should be deleted. ");
    	return sb.toString();
    }

    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Info Updated Message
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Notify member that their membership information has been updated.
	//      Type: MessageThread.PLAIN_TYPE
	//      Recipients: all individuals associated with the updated membership.
	//      Options: not applicable.
	//      Conditional: Yes. Based on member access preferences.
	// #1 Message Thread Created
	//      Inbox: Displayed for users. 
	//      Inbox New Message Count: incremented for each user.
	//      Link Reply Configured: not applicable.
	// #2 Message(s) Sent
	//      Non-Users: Always receive an email.
	//      Users: right now, email never sent (conditional sending of email message via user options not supported yet).
	//      Embedded Link(s): not applicable.
	// #3 Alert and Badges Sent 
	//      Alert: not applicable.
    //      Badge: update is always sent to Users
	// #4 Post Activity
	//      Yes.
	// #5 SMS
	//      yes
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void sendMemberUpdatedMessage(Member theMember, List<String> theModificationMessages, User theUser) {
    	String subject = "Your rTeam membership information has been updated";
		
    	List<String> messageBodys = new ArrayList<String>();
		List<Key> userKeys = new ArrayList<Key>();
		List<User> alertBadgeUsers = new ArrayList<User>();
		
		Team team = theMember.getTeam();
		
		//::BUSINESSRULE:: use only authorized membership participants
    	List<UserMemberInfo> authorizedMembershipRecipients = theMember.getAuthorizedRecipients(team);
		
		String emailBody = null;
		String messageBody = null;
		String oneUseToken = null;
		List<UserMemberInfo> authorizedUserRecipients = new ArrayList<UserMemberInfo>();
		for(UserMemberInfo umi : authorizedMembershipRecipients) {
    		// so far, token only need to support email reply
			oneUseToken = TF.get();
    		umi.setOneUseToken(oneUseToken);
			String userId = umi.getUserId();
			if(userId != null && umi.getHasRteamMessageAccessEnabled()) {
				// if member is user, send rTeam message. Message sent below, just built here.
				userKeys.add(KeyFactory.stringToKey(userId));
				authorizedUserRecipients.add(umi);
		    	messageBody = getMemberUpdatedMessageBody(theMember, theModificationMessages, theUser);
				messageBodys.add(messageBody);
			} 
			if(umi.getEmailAddress() != null && umi.getHasEmailMessageAccessEnabled()) {
		    	///////////////////////////////////////////////////
		    	// #2 Message(s) Sent: send email
		    	///////////////////////////////////////////////////
		    	emailBody = Emailer.getMemberUpdatedEmailBody(theMember, theMember.getFullNameByEmailAddress(umi.getEmailAddress()), oneUseToken, theModificationMessages, theUser, team.getTwitterScreenName());
		    	Emailer.send(umi.getEmailAddress(), subject, emailBody, Emailer.REPLY);
			}
			if( (umi.getPhoneNumber() != null || umi.getSmsEmailAddress() != null) && umi.getHasSmsMessageAccessEnabled()) {
				//////////
				// #5 SMS 
				//////////
				String smsMessage = getMemberUpdateSmsMessage(theMember, theModificationMessages, theUser);
				// the new ad-free SMS sent via SmsEmailAddress takes precedence over the phoneNumber (Zeep Mobile) SMS
				String smsAddress = umi.getSmsEmailAddress() == null ? umi.getPhoneNumber() : umi.getSmsEmailAddress();
				postSms(smsAddress, "Info Update", smsMessage, Emailer.NO_REPLY);
			}
			if(!umi.isAnyMessageAccessEnabled()) {
				log.severe("No message access enabled for member = " + umi.getFullName());
			}
		}
    	
    	////////////////////////////////////////////////////////
    	// #1 Message Thread Created - only users are recipients
    	////////////////////////////////////////////////////////
		if(authorizedUserRecipients.size() > 0) {
		    // only Users will be recipients of the messageThread 
	    	//createMessageThread(theAuthorizedTeamRecipients, theTeam, theSubject, theBodys, theMessageType, theMessageLinkOnly)
	    	createMessageThread(authorizedUserRecipients, team, subject, messageBodys, MessageThread.PLAIN_TYPE, false);
		}
    
		/////////////////////////////////////////////
		// #1 Message Thread: increment message count
		/////////////////////////////////////////////
		// alertBadgeUsers is an out parameter
		if(userKeys.size() > 0 ) getUserFromKeysAndIncrementMessageCount(userKeys, alertBadgeUsers);

		//////////////////////////////////////////////////////////////////
		// #3 Alert and Badges sent: badge only, alert does not apply here
		//////////////////////////////////////////////////////////////////
		if(alertBadgeUsers.size() > 0 ) sendAlertBadges(alertBadgeUsers, false, null);
		
		///////////////////
		// #4 Post Activity
		///////////////////
		User sender = User.getUserFromUserKey(theUser.getKey());
		String activityBody = theMember.getFullName() + " member info was updated by " + sender.getFullName();
		postActivity(team, activityBody);
    }
    
    private static String getMemberUpdatedMessageBody(Member theMember, List<String> theModificationMessages, User theUser) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("Your membership information on team '");
    	sb.append(theMember.getTeam().getTeamName());
    	sb.append("' has been updated by ");
    	sb.append(theUser.getFullName());
    	sb.append(". ");
    	int messageCnt = 0;
    	for(String mm : theModificationMessages) {
    		if(messageCnt > 0) sb.append(", ");
    		sb.append(mm);
    		messageCnt++;
    	}
    	return sb.toString();
    }
    private static String getMemberUpdateSmsMessage(Member theMember, List<String> theModificationMessages, User theUser) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("Your membership information\non team ");
    	sb.append(theMember.getTeam().getTeamName());
    	sb.append("\nhas been updated by ");
    	sb.append(theUser.getFullName());
    	sb.append("\n");
    	int messageCnt = 0;
    	for(String mm : theModificationMessages) {
    		if(messageCnt > 0) sb.append(", ");
    		sb.append(mm);
    		messageCnt++;
    	}
    	return sb.toString();
    }
    
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // New Coordinator Message
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Notify team members that all users are now coordinators.
	//      Type: MessageThread.PLAIN_TYPE
	//      Recipients: all team members.
	//      Options: not applicable.
	//      Conditional: Yes. Based on member access preferences.
	// #1 Message Thread Created
	//      Inbox: Displayed for users. 
	//      Inbox New Message Count: incremented for each user.
	//      Link Reply Configured: not applicable.
	// #2 Message(s) Sent
	//      Non-Users: Always receive an email.
	//      Users: right now, email never sent (conditional sending of email message via user options not supported yet).
	//      Embedded Link(s): not applicable.
	// #3 Alert and Badges Sent 
	//      Alert: not applicable.
    //      Badge: update is always sent to Users
	// #4 Post Activity
	//      Yes.
	// #5 SMS
	//      Not Applicable.
    //      TODO should send SMS if applicable.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void sendNewCoordinatorMessage(List<Member> theMembers, Team theTeam) {
    	String subject = "You are now an rTeam Coordinator";
    	
		List<Key> userKeys = new ArrayList<Key>();
    	List<String> messageBodys = new ArrayList<String>();
    	List<UserMemberInfo> authorizedTeamRecipients = new ArrayList<UserMemberInfo>();
		List<User> alertBadgeUsers = new ArrayList<User>();
    	
    	// Build the RAW team recipient list (which is filtered for duplicates below)
    	for(Member m : theMembers) {
        	List<UserMemberInfo> authorizedMembershipRecipients = m.getAuthorizedRecipients(theTeam);
        	for(UserMemberInfo umi : authorizedMembershipRecipients) {
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
		
		List<UserMemberInfo> authorizedUserRecipients = new ArrayList<UserMemberInfo>();
		for(UserMemberInfo umi : authorizedTeamRecipients) {
			String userId = umi.getUserId();
			if(userId != null && umi.getHasRteamMessageAccessEnabled()) {
				userKeys.add(KeyFactory.stringToKey(userId));
				authorizedUserRecipients.add(umi);
		    	String messageBody = getNewCoordinatorMessageBody(theTeam.getTeamName());
				messageBodys.add(messageBody);
			}
			if(umi.getEmailAddress() != null && umi.getHasEmailMessageAccessEnabled()) {
		    	///////////////////////////////////////////////////
		    	// #2 Message(s) Sent: email only sent to non-users
		    	///////////////////////////////////////////////////
	    		String emailBody = Emailer.getNewCoordinatorEmailBody(umi.getFullName(), theTeam.getTeamName(), umi.getOneUseToken(), theTeam.getTwitterScreenName());
	    		Emailer.send(umi.getEmailAddress(), subject, emailBody, Emailer.NO_REPLY);
			}
			if( (umi.getPhoneNumber() != null || umi.getSmsEmailAddress() != null) && umi.getHasSmsMessageAccessEnabled()) {
				//////////
				// #5 SMS 
				//////////
				String smsMessage = getNewCoordinatorSmsMessage(theTeam.getTeamName());
				// the new ad-free SMS sent via SmsEmailAddress takes precedence over the phoneNumber (Zeep Mobile) SMS
				String smsAddress = umi.getSmsEmailAddress() == null ? umi.getPhoneNumber() : umi.getSmsEmailAddress();
				postSms(smsAddress, "Team Update", smsMessage, Emailer.NO_REPLY);
			}
		}
    	
    	////////////////////////////////////////////////////////
    	// #1 Message Thread Created - only users are recipients
    	////////////////////////////////////////////////////////
		if(authorizedUserRecipients.size() > 0) {
	    	//createMessageThread(theAuthorizedTeamRecipients, theTeam, theSubject, theBodys, theMessageType, theMessageLinkOnly)
	    	createMessageThread(authorizedUserRecipients, theTeam, subject, messageBodys, MessageThread.PLAIN_TYPE, false);
		}
        
		/////////////////////////////////////////////
		// #1 Message Thread: increment message count
		/////////////////////////////////////////////
		// alertBadgeUsers is an out parameter
		if(userKeys.size() > 0 ) getUserFromKeysAndIncrementMessageCount(userKeys, alertBadgeUsers);

		//////////////////////////////////////////////////////////////////
		// #3 Alert and Badges sent: badge only, alert does not apply here
		//////////////////////////////////////////////////////////////////
		if(alertBadgeUsers.size() > 0 ) sendAlertBadges(alertBadgeUsers, false, null);
		
		///////////////////
		// #4 Post Activity
		///////////////////
		if(theTeam != null) postActivity(theTeam, "Coordinator left team. All other users have been promoted to coordinator");
    }
    
    private static String getNewCoordinatorMessageBody(String theTeamName) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("The only team coordinator has left the team '");
    	sb.append(theTeamName);
    	sb.append("'. So the team can continue, all members of the ");
    	sb.append("team that are also users have been 'promoted' to coordinator. You now have all coordinator ");
    	sb.append("permissions including creating games and practices and adding new members.");
    	return sb.toString();
    }
    
    private static String getNewCoordinatorSmsMessage(String theTeamName) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("Only team coordinator left the team '");
    	sb.append(theTeamName);
    	sb.append("'. So team can continue, you have been 'promoted' to coordinator");
    	return sb.toString();
    }
    
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // No Coordinator Message
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Notify members that the only remaining coordinator has left the team.
	//      Type: not applicable
	//      Recipients: all members of the team.
	//      Options: not applicable.
	//      Conditional: Yes. Based on member access preferences.
	// #1 Message Thread Created
	//      not applicable. There are no team users, that's why this message is being sent.
	// #2 Message(s) Sent
	//      Non-Users: Always receive an email.
	//      Users: not applicable. There are no team users, that's why this message is being sent.
	//      Embedded Link(s): not applicable.
	// #3 Alert and Badges Sent 
	//      not applicable.
	// #4 Post Activity
	//      Yes.
	// #5 SMS
	//      Yes.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void sendNoCoordinatorMessage(List<Member> theMembers, Team theTeam) {
    	String subject = "rTeam: team coordinator has left the team";
    	
    	// Build the RAW team recipient list (which is filtered for duplicates below)
    	List<UserMemberInfo> authorizedTeamRecipients = new ArrayList<UserMemberInfo>();
    	for(Member m : theMembers) {
        	List<UserMemberInfo> authorizedMembershipRecipients = m.getAuthorizedRecipients(theTeam);
        	for(UserMemberInfo umi : authorizedMembershipRecipients) {
        		umi.setOneUseToken(TF.get());
    			authorizedTeamRecipients.add(umi);
        	}
    	}
		
		///////////////////////////////////////////////////////////////////////////////////////
		// Filter Team Recipient List
		//      All entities in returned list have unique: emailAddress, userId and phoneNumber
		///////////////////////////////////////////////////////////////////////////////////////
		authorizedTeamRecipients = UserMemberInfo.filterDuplicates(authorizedTeamRecipients);
		for(UserMemberInfo umi : authorizedTeamRecipients) {
			if(umi.getEmailAddress() != null && umi.getHasEmailMessageAccessEnabled()) {
		    	//////////////////////////////////////////////////////////////////////////////
		    	// #2 Message(s) Sent: email sent to specified members which are all non-users
		    	//////////////////////////////////////////////////////////////////////////////
	    		String emailBody = Emailer.getNoCoordinatorEmailBody(umi.getFullName(), theTeam.getTeamName(), umi.getOneUseToken(), theTeam.getTwitterScreenName());
	    		Emailer.send(umi.getEmailAddress(), subject, emailBody, Emailer.NO_REPLY);
			}
			if( (umi.getPhoneNumber() != null || umi.getSmsEmailAddress() != null) && umi.getHasSmsMessageAccessEnabled()) {
				//////////
				// #5 SMS 
				//////////
				String smsMessage = "Team coord has left team. One or more members should become registered rTeam users";
				// the new ad-free SMS sent via SmsEmailAddress takes precedence over the phoneNumber (Zeep Mobile) SMS
				String smsAddress = umi.getSmsEmailAddress() == null ? umi.getPhoneNumber() : umi.getSmsEmailAddress();
				postSms(smsAddress, "Important", smsMessage, Emailer.NO_REPLY);
			}
		}
		
		///////////////////
		// #4 Post Activity
		///////////////////
		if(theTeam != null) postActivity(theTeam, "Team coordinator has left team. One or more members should become registered rTeam users.");
    }

    
    
	// PRIORITY TODO
    //   1. need messageThread recipients for non-users too when message type is a confirmation
    //   2. inbox message needs to be poll-like with handling on the back end too
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // New Event Message
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Notify team members that a new event has been scheduled.
	//      Type: parameter passed in
	//      Recipients: for games and events: all team members and fans. For practices: team members only.
	//      Options: not applicable.
	//      Conditional: Yes. Based on member access preferences.
	// #1 Message Thread Created
	//      Inbox: Displayed for users. 
	//      Inbox New Message Count: incremented for each user.
	//      Link Reply Configured: not applicable.
	// #2 Message(s) Sent
	//      Non-Users: Always receive an email.
	//      Users: right now, email never sent (conditional sending of email message via user options not supported yet).
	//      Embedded Link(s): not applicable.
	// #3 Alert and Badges Sent 
	//      Alert: sent if new event is "today".
    //      Badge: update is always sent to Users
	// #4 Post Activity
	//      Yes.
    // #5 SMS
	//      Yes.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void sendNewEventMessage(List<Member> theMembers, User theUser, Team theTeam, List<Game> theGames, List<Practice> thePractices, String theMessageType) {
    	String eventTypeStr = Utility.getEventType(theGames, thePractices);
    	String subject = "rTeam: a new " + eventTypeStr + " has been scheduled";
    	if((theGames != null && theGames.size() > 1) || (thePractices != null && thePractices.size() > 1)) {
    		subject = "rTeam: new " + eventTypeStr + "s have been scheduled";
    	}
    	
    	List<Key> userKeys = new ArrayList<Key>();
    	List<String> messageBodys = new ArrayList<String>();
    	List<User> alertBadgeUsers = new ArrayList<User>();
       	List<UserMemberInfo> authorizedTeamRecipients = new ArrayList<UserMemberInfo>();
    	
    	// Build the RAW team recipient list (which is filtered for duplicates below)
    	for(Member m : theMembers) {
    		// fans are not notified of practices
    		if(m.isFan() && eventTypeStr.equalsIgnoreCase(Practice.PRACTICE_EVENT_TYPE)) {
    			continue;
    		}
    		
        	List<UserMemberInfo> authorizedMembershipRecipients = m.getAuthorizedRecipients(theTeam);
        	for(UserMemberInfo umi : authorizedMembershipRecipients) {
    			umi.setOneUseToken(TF.get());
            	if(MessageThread.isConfirm(theMessageType)) {
        			umi.setOneUseSmsToken(umi.getPhoneNumber()); // could be null
            	}
    			authorizedTeamRecipients.add(umi);
        	}
    	}
		
		///////////////////////////////////////////////////////////////////////////////////////
		// Filter Team Recipient List
		//      All entities in returned list have unique: emailAddress, userId and phoneNumber
		///////////////////////////////////////////////////////////////////////////////////////
		authorizedTeamRecipients = UserMemberInfo.filterDuplicates(authorizedTeamRecipients);
		
		List<UserMemberInfo> authorizedUserRecipients = new ArrayList<UserMemberInfo>();
		for(UserMemberInfo umi : authorizedTeamRecipients) {
			String messageBody = null;
			String userId = umi.getUserId();
			if(userId != null && umi.getHasRteamMessageAccessEnabled()) {
				// if member is user, send rTeam message. Message sent below, just built here.
				userKeys.add(KeyFactory.stringToKey(userId));
	    		messageBody = getNewEventMessageBody(theUser.getFullName(), theTeam.getTeamName(), theGames, thePractices);
			}
			if(umi.getEmailAddress() != null && umi.getHasEmailMessageAccessEnabled()) {
		    	///////////////////////////////////////////////////
		    	// #2 Message(s) Sent: send email
		    	///////////////////////////////////////////////////
	    		String emailBody = Emailer.getNewEventEmailBody(umi.getFullName(), theUser.getFullName(), umi.getOneUseToken(), theTeam.getTeamName(), theTeam.getTwitterScreenName(), theGames, thePractices);
		    	if(messageBody == null) {messageBody = emailBody;}
		    	Emailer.send(umi.getEmailAddress(), subject, emailBody, Emailer.REPLY);
			}
			if( (umi.getPhoneNumber() != null || umi.getSmsEmailAddress() != null) && umi.getHasSmsMessageAccessEnabled()) {
				//////////
				// #5 SMS 
				//////////
				String smsMessage = getNewEventSmsMessage(theUser.getDisplayName(), theGames, thePractices);
				if(messageBody == null) {messageBody = smsMessage;}
				// the new ad-free SMS sent via SmsEmailAddress takes precedence over the phoneNumber (Zeep Mobile) SMS
				String smsAddress = umi.getSmsEmailAddress() == null ? umi.getPhoneNumber() : umi.getSmsEmailAddress();
				postSms(smsAddress, "New Event", smsMessage, Emailer.REPLY);
			}
			
			if(messageBody != null) {
				messageBodys.add(messageBody);
				authorizedUserRecipients.add(umi);
			}
		}
    	
    	////////////////////////////
    	// #1 Message Thread Created
    	////////////////////////////
		if(authorizedUserRecipients.size() > 0) {
			// messageThread recipients created for emails and SMS messages too - needed to support unsolicited replies
	    	//createMessageThread(theAuthorizedTeamRecipients, theTeam, theSubject, theBodys, theMessageType, theMessageLinkOnly)
	    	createMessageThread(authorizedUserRecipients, theTeam, subject, messageBodys, theMessageType, false);
		}
        
		/////////////////////////////////////////////
		// #1 Message Thread: increment message count
		/////////////////////////////////////////////
		// alertBadgeUsers is an out parameter
		if(userKeys.size() > 0 ) getUserFromKeysAndIncrementMessageCount(userKeys, alertBadgeUsers);

		//////////////////////////////////////////////////////////////////
		// #3 Alert and Badges sent: badge and alert if new event is today
		//////////////////////////////////////////////////////////////////
		if(alertBadgeUsers.size() > 0 ) {
			String alertMessage = getAlertIfAny(theGames, thePractices, eventTypeStr, true);
			Boolean isAlert = alertMessage != null;
			sendAlertBadges(alertBadgeUsers, isAlert, alertMessage);
		}
		
		///////////////////
		// #4 Post Activity
		///////////////////
		if(theTeam != null) postActivity(theTeam, getNewEventActivityBody(theUser.getFullName(), theTeam.getTeamName(), theGames, thePractices), true);
    }
   
    private static String getNewEventMessageBody(String theUserFullName, String theTeamName, List<Game> theGames, List<Practice> thePractices) {
    	StringBuffer sb = new StringBuffer();
    	sb.append(getNewEventCoreMessageBody(theUserFullName, theTeamName, theGames, thePractices));
    	return sb.toString();
    }
    private static String getNewEventCoreMessageBody(String theUserFullName, String theTeamName, List<Game> theGames, List<Practice> thePractices) {
    	String eventTypeStr = Utility.getEventType(theGames, thePractices);
    	StringBuffer sb = new StringBuffer();
    	sb.append("team coordinator ");
    	sb.append(theUserFullName);
    	sb.append(" of the '");
    	sb.append(theTeamName);
    	
    	if(theGames != null) {
    		if(theGames.size() > 1) {
    			sb.append("' has scheduled new games for ");
    			for(int i=0; i<theGames.size(); i++) {
    				if(i != 0) {
    					sb.append(", ");
    				}
    				sb.append(theGames.get(i).getEventLocalStartDate());
    			}
    		} else {
    	    	sb.append("' has scheduled a new ");
    	    	sb.append(eventTypeStr);
    	    	sb.append(" for ");
    	    	sb.append(theGames.get(0).getEventLocalStartDate());
    		}
    	} else if(thePractices != null) {
    		if(thePractices.size() > 1) {
    			sb.append("' has scheduled new practices for ");
    			for(int i=0; i<thePractices.size(); i++) {
    				if(i != 0) {
    					sb.append(", ");
    				}
    				sb.append(thePractices.get(i).getEventLocalStartDate());
    			}
    		} else {
    	    	sb.append("' has scheduled a new ");
    	    	sb.append(eventTypeStr);
    	    	sb.append(" for ");
    	    	sb.append(thePractices.get(0).getEventLocalStartDate());
    		}
    	}
    	
    	return sb.toString();
    }
    private static String getNewEventActivityBody(String theUserFullName, String theTeamName, List<Game> theGames, List<Practice> thePractices) {
    	String eventTypeStr = Utility.getEventType(theGames, thePractices);
    	StringBuffer sb = new StringBuffer();
    	sb.append("team coordinator ");
    	sb.append(theUserFullName);
    	
    	if(theGames != null) {
    		if(theGames.size() > 1) {
    			sb.append(" has scheduled new games for ");
    			for(int i=0; i<theGames.size(); i++) {
    				if(i != 0) {
    					sb.append(", ");
    				}
    				sb.append(theGames.get(i).getEventLocalStartDate());
    			}
    		} else {
    	    	sb.append(" has scheduled a new ");
    	    	sb.append(eventTypeStr);
    	    	sb.append(" for ");
    	    	sb.append(theGames.get(0).getEventLocalStartDate());
    		}
    	} else if(thePractices != null) {
    		if(thePractices.size() > 1) {
    			sb.append(" has scheduled new practices for ");
    			for(int i=0; i<thePractices.size(); i++) {
    				if(i != 0) {
    					sb.append(", ");
    				}
    				sb.append(thePractices.get(i).getEventLocalStartDate());
    			}
    		} else {
    	    	sb.append(" has scheduled a new ");
    	    	sb.append(eventTypeStr);
    	    	sb.append(" for ");
    	    	sb.append(thePractices.get(0).getEventLocalStartDate());
    		}
    	}
    	
    	return sb.toString();
    }
    private static String getNewEventSmsMessage(String theUserAbbreviatedName, List<Game> theGames, List<Practice> thePractices) {
    	String eventTypeStr = Utility.getEventType(theGames, thePractices);
    	StringBuffer sb = new StringBuffer();
    	sb.append("team coord ");
    	sb.append(theUserAbbreviatedName);
    	sb.append("\n");
    	
    	if(theGames != null) {
    		if(theGames.size() > 1) {
    			sb.append("sched new games for\n");
    			for(int i=0; i<theGames.size(); i++) {
    				if(i != 0) {
    					sb.append(", ");
    				}
    				sb.append(theGames.get(i).getEventLocalStartDate());
    			}
    		} else {
    	    	sb.append("sched a new ");
    	    	sb.append(eventTypeStr);
    	    	sb.append(" for\n");
    	    	sb.append(theGames.get(0).getEventLocalStartDate());
    		}
    	} else if(thePractices != null) {
    		if(thePractices.size() > 1) {
    			sb.append("sched new practices for\n");
    			for(int i=0; i<thePractices.size(); i++) {
    				if(i != 0) {
    					sb.append(", ");
    				}
    				sb.append(thePractices.get(i).getEventLocalStartDate());
    			}
    		} else {
    	    	sb.append("sched a new ");
    	    	sb.append(eventTypeStr);
    	    	sb.append(" for\n");
    	    	sb.append(thePractices.get(0).getEventLocalStartDate());
    		}
    	}

    	return sb.toString();
    }
    
	// PRIORITY TODO
    //   1. allow confirmation like New Event Message above (will need to do those TODOs here as well)
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Updated Message
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Notify team members that an event has been updated.
	//      Type: parameter passed in
	//      Recipients: all team members.
	//      Options: not applicable.
	//      Conditional: Yes. Based on member access preferences.
	// #1 Message Thread Created
	//      Inbox: Displayed for users. 
	//      Inbox New Message Count: incremented for each user.
	//      Link Reply Configured: not applicable.
	// #2 Message(s) Sent
	//      Non-Users: Always receive an email.
	//      Users: right now, email never sent (conditional sending of email message via user options not supported yet).
	//      Embedded Link(s): not applicable.
	// #3 Alert and Badges Sent 
	//      Alert: sent if updated event is scheduled for "today".
    //      Badge: update is always sent to Users
	// #4 Post Activity
	//      Yes.
	// #5 SMS
	//      Yes.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void sendEventUpdatedMessage(List<Member> theMembers, User theUser, Game theGame, Practice thePractice,
    		                                   String theNotificationMessage, Team theTeam, Boolean theUpdateAllLocations) {
    	String eventTypeStr = Utility.getEventType(theGame, thePractice);
    	String subject = "rTeam: " + Utility.capitalize(eventTypeStr) + " schedule has been updated";
    	
    	List<Key> userKeys = new ArrayList<Key>();
    	List<String> messageBodys = new ArrayList<String>();
    	List<User> alertBadgeUsers = new ArrayList<User>();
       	List<UserMemberInfo> authorizedTeamRecipients = new ArrayList<UserMemberInfo>();
    	
    	// Build the RAW team recipient list (which is filtered for duplicates below)
    	for(Member m : theMembers) {
    		// fans are not notified of practices
    		if(m.isFan() && eventTypeStr.equalsIgnoreCase(Practice.PRACTICE_EVENT_TYPE)) {
    			continue;
    		}
    		
        	List<UserMemberInfo> authorizedMembershipRecipients = m.getAuthorizedRecipients(theTeam);
        	for(UserMemberInfo umi : authorizedMembershipRecipients) {
    			authorizedTeamRecipients.add(umi);
        	}
    	}
		
		///////////////////////////////////////////////////////////////////////////////////////
		// Filter Team Recipient List
		//      All entities in returned list have unique: emailAddress, userId and phoneNumber
		///////////////////////////////////////////////////////////////////////////////////////
		authorizedTeamRecipients = UserMemberInfo.filterDuplicates(authorizedTeamRecipients);
		
		List<UserMemberInfo> authorizedUserRecipients = new ArrayList<UserMemberInfo>();
		for(UserMemberInfo umi : authorizedTeamRecipients) {
			umi.setOneUseToken(TF.get());
			String userId = umi.getUserId();
			if(userId != null && umi.getHasRteamMessageAccessEnabled()) {
				// if member is user, send rTeam message. Message sent below, just built here.
				userKeys.add(KeyFactory.stringToKey(userId));
				authorizedUserRecipients.add(umi);
	    		String messageBody = getEventUpdatedMessageBody(theUser.getFullName(), theTeam.getTeamName(), theGame, thePractice, theNotificationMessage, theUpdateAllLocations);
				messageBodys.add(messageBody);
			}
			if(umi.getEmailAddress() != null && umi.getHasEmailMessageAccessEnabled()) {
		    	///////////////////////////////////////////////////
		    	// #2 Message(s) Sent: send email
		    	///////////////////////////////////////////////////
	    		String emailBody = Emailer.getEventUpdatedEmailBody(umi.getFullName(), theUser.getFullName(), umi.getOneUseToken(), theTeam.getTeamName(), theTeam.getTwitterScreenName(), theGame, thePractice, theNotificationMessage, theUpdateAllLocations);
		    	Emailer.send(umi.getEmailAddress(), subject, emailBody, Emailer.REPLY);
			}
			if( (umi.getPhoneNumber() != null || umi.getSmsEmailAddress() != null) && umi.getHasSmsMessageAccessEnabled()) {
				//////////
				// #5 SMS 
				//////////
				String smsMessage = getEventUpdatedSmsMessage(theUser.getDisplayName(), theTeam.getTeamName(), theGame, thePractice, theNotificationMessage, theUpdateAllLocations);
				// the new ad-free SMS sent via SmsEmailAddress takes precedence over the phoneNumber (Zeep Mobile) SMS
				String smsAddress = umi.getSmsEmailAddress() == null ? umi.getPhoneNumber() : umi.getSmsEmailAddress();
				postSms(smsAddress, "Updated Event", smsMessage, Emailer.REPLY);
			}
		}
    	
    	////////////////////////////////////////////////////////
    	// #1 Message Thread Created - only users are recipients
    	////////////////////////////////////////////////////////
		if(authorizedUserRecipients.size() > 0) {
		    // only Users will be recipients of the messageThread 
	    	//createMessageThread(theAuthorizedTeamRecipients, theTeam, theSubject, theBodys, theMessageType, theMessageLinkOnly)
	    	createMessageThread(authorizedUserRecipients, theTeam, subject, messageBodys, MessageThread.PLAIN_TYPE, false);
		}
        
		/////////////////////////////////////////////
		// #1 Message Thread: increment message count
		/////////////////////////////////////////////
		// alertBadgeUsers is an out parameter
		if(userKeys.size() > 0 ) getUserFromKeysAndIncrementMessageCount(userKeys, alertBadgeUsers);

		//////////////////////////////////////////////////////////////////////
		// #3 Alert and Badges sent: badge and alert if updated event is today
		//////////////////////////////////////////////////////////////////////
		if(alertBadgeUsers.size() > 0 ) {
			String alertMessage = getAlertIfAny(theGame, thePractice, eventTypeStr, false);
			Boolean isAlert = alertMessage != null;
			sendAlertBadges(alertBadgeUsers, isAlert, alertMessage);
		}
		
		///////////////////
		// #4 Post Activity
		///////////////////
		if(theTeam != null) postActivity(theTeam, getEventUpdatedActivityBody(theUser.getFullName(), theTeam.getTeamName(), theGame, thePractice, theNotificationMessage, theUpdateAllLocations));
    }

    private static String getEventUpdatedMessageBody(String theUserFullName, String theTeamName, Game theGame,
    		Practice thePractice, String theNotificationMessage, Boolean theUpdateAllLocations) {
    	String eventTypeStr = Utility.getEventType(theGame, thePractice);
    	StringBuffer sb = new StringBuffer();
    	
    	if(theUpdateAllLocations) {
    		sb.append("The location of all ");
    		sb.append(eventTypeStr);
    		sb.append("s has been changed to ");
    		
    	} else {
        	sb.append("The ");
        	sb.append(eventTypeStr);
        	sb.append(" originally scheduled for ");
        	String startDateStr = theGame != null ? theGame.getOriginalEventLocalStartDate() : thePractice.getOriginalEventLocalStartDate();
        	sb.append(startDateStr);
        	sb.append(" for team '");
        	sb.append(theTeamName);
        	sb.append("' has been udpated as follows: ");
    	}
    	sb.append(theNotificationMessage);
    	sb.append(".");
    	return sb.toString();
    }
    // PRIORITY TODO
    //  1. pass in time zone and get a compact date format printing properly
    //  2. try to make this message fit into 140 chars
    private static String getEventUpdatedActivityBody(String theUserFullName, String theTeamName, Game theGame, Practice thePractice, String theNotificationMessage, Boolean theUpdateAllLocations) {
    	String eventTypeStr = Utility.getEventType(theGame, thePractice);
    	StringBuffer sb = new StringBuffer();
    	String startDateStr = theGame != null ? theGame.getOriginalEventLocalStartDate() : thePractice.getOriginalEventLocalStartDate();
    	
    	if(theUpdateAllLocations) {
    		sb.append("The location of all ");
    		sb.append(eventTypeStr);
    		sb.append("s has been changed to ");
    		
    	} else {
        	sb.append(startDateStr);
        	sb.append(" " + eventTypeStr);
        	sb.append(" updated:");
    	}
    	sb.append(theNotificationMessage);
    	sb.append(".");
    	return sb.toString();
    }
    private static String getEventUpdatedSmsMessage(String theUserAbbreviatedName, String theTeamName, Game theGame, Practice thePractice, String theNotificationMessage, Boolean theUpdateAllLocations) {
    	String eventTypeStr = Utility.getEventType(theGame, thePractice);
    	StringBuffer sb = new StringBuffer();
    	String startDateStr = theGame != null ? theGame.getOriginalEventLocalStartDate() : thePractice.getOriginalEventLocalStartDate();
    	
    	if(theUpdateAllLocations) {
    		sb.append("location of all ");
    		sb.append(eventTypeStr);
    		sb.append("s\nhas been changed to\n");
    		
    	} else {
        	sb.append(startDateStr);
        	sb.append(" " + eventTypeStr);
        	sb.append(" updated:\n");
    	}
    	sb.append(theNotificationMessage);
    	return sb.toString();
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Deleted Message
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Notify team members that an event has been deleted.
	//      Type: parameter passed in
	//      Recipients: all team members.
	//      Options: not applicable.
	//      Conditional: Yes. Based on member access preferences.
	// #1 Message Thread Created
	//      Inbox: Displayed for users. 
	//      Inbox New Message Count: incremented for each user.
	//      Link Reply Configured: not applicable.
	// #2 Message(s) Sent
	//      Non-Users: Always receive an email.
	//      Users: right now, email never sent (conditional sending of email message via user options not supported yet).
	//      Embedded Link(s): not applicable.
	// #3 Alert and Badges Sent 
	//      Alert: sent if deleted event is scheduled for "today".
    //      Badge: delete is always sent to Users
	// #4 Post Activity
	//      Yes.
	// #5 SMS
	//      Yes.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void sendEventDeletedMessage(List<Member> theMembers, User theUser, Game theGame, Practice thePractice, Team theTeam) {
    	String eventTypeStr = Utility.getEventType(theGame, thePractice);
    	String subject = "rTeam: " + Utility.capitalize(eventTypeStr) + " has been removed from the schedule";
    	
    	List<Key> userKeys = new ArrayList<Key>();
    	List<String> messageBodys = new ArrayList<String>();
    	List<User> alertBadgeUsers = new ArrayList<User>();
       	List<UserMemberInfo> authorizedTeamRecipients = new ArrayList<UserMemberInfo>();
    	
    	// Build the RAW team recipient list (which is filtered for duplicates below)
    	for(Member m : theMembers) {
    		// fans are not notified of practices
    		if(m.isFan() && eventTypeStr.equalsIgnoreCase(Practice.PRACTICE_EVENT_TYPE)) {
    			continue;
    		}
    		
        	List<UserMemberInfo> authorizedMembershipRecipients = m.getAuthorizedRecipients(theTeam);
        	for(UserMemberInfo umi : authorizedMembershipRecipients) {
    			authorizedTeamRecipients.add(umi);
        	}
    	}
		
		///////////////////////////////////////////////////////////////////////////////////////
		// Filter Team Recipient List
		//      All entities in returned list have unique: emailAddress, userId and phoneNumber
		///////////////////////////////////////////////////////////////////////////////////////
		authorizedTeamRecipients = UserMemberInfo.filterDuplicates(authorizedTeamRecipients);
		
		List<UserMemberInfo> authorizedUserRecipients = new ArrayList<UserMemberInfo>();
		for(UserMemberInfo umi : authorizedTeamRecipients) {
			umi.setOneUseToken(TF.get());
			String userId = umi.getUserId();
			if(userId != null && umi.getHasRteamMessageAccessEnabled()) {
				// if member is user, send rTeam message. Message sent below, just built here.
				userKeys.add(KeyFactory.stringToKey(userId));
				authorizedUserRecipients.add(umi);
	    		String messageBody = getEventDeletedMessageBody(theUser.getFullName(), theTeam.getTeamName(), theGame, thePractice);
				messageBodys.add(messageBody);
			}
			if(umi.getEmailAddress() != null && umi.getHasEmailMessageAccessEnabled()) {
		    	///////////////////////////////////////////////////
		    	// #2 Message(s) Sent: send email
		    	///////////////////////////////////////////////////
	    		String emailBody = Emailer.getEventDeletedEmailBody(umi.getFullName(), theUser.getFullName(), umi.getOneUseToken(), theTeam.getTeamName(), theTeam.getTwitterScreenName(), theGame, thePractice);
		    	Emailer.send(umi.getEmailAddress(), subject, emailBody, Emailer.REPLY);
			}
			if( (umi.getPhoneNumber() != null || umi.getSmsEmailAddress() != null) && umi.getHasSmsMessageAccessEnabled()) {
				//////////
				// #5 SMS 
				//////////
				String smsMessage = getEventDeletedSmsMessage(theUser.getDisplayName(), theTeam.getTeamName(), theGame, thePractice);
				// the new ad-free SMS sent via SmsEmailAddress takes precedence over the phoneNumber (Zeep Mobile) SMS
				String smsAddress = umi.getSmsEmailAddress() == null ? umi.getPhoneNumber() : umi.getSmsEmailAddress();
				postSms(smsAddress, "Canceled Event", smsMessage, Emailer.REPLY);
			}
		}
    	
    	////////////////////////////////////////////////////////
    	// #1 Message Thread Created - only users are recipients
    	////////////////////////////////////////////////////////
		if(authorizedUserRecipients.size() > 0) {
		    // only Users will be recipients of the messageThread 
	    	//createMessageThread(theAuthorizedTeamRecipients, theTeam, theSubject, theBodys, theMessageType, theMessageLinkOnly)
	    	createMessageThread(authorizedUserRecipients, theTeam, subject, messageBodys, MessageThread.PLAIN_TYPE, false);
		}
        
		/////////////////////////////////////////////
		// #1 Message Thread: increment message count
		/////////////////////////////////////////////
		// alertBadgeUsers is an out parameter
		if(userKeys.size() > 0 ) getUserFromKeysAndIncrementMessageCount(userKeys, alertBadgeUsers);

		////////////////////////////
		// #3 Alert and Badges sent: 
		////////////////////////////
		Date startDate =  null;
		TimeZone tz = null;
		Boolean isAlert = false;
		String alertMessage = null;
		if(theGame != null) {
			startDate = theGame.getEventGmtStartDate();
			tz = GMT.getTimeZone(theGame.getTimeZone());
		} else {
			startDate = thePractice.getEventGmtStartDate();
			tz = GMT.getTimeZone(thePractice.getTimeZone());
		}
		if(startDate != null && tz != null) {
			// TODO not perfect using the timezone in the event itself, but I don't know how else to do it.
			List<Date> todayStartAndEnd = GMT.getTodayBeginAndEndDates(tz);
			if(startDate.after(todayStartAndEnd.get(0)) && startDate.before(todayStartAndEnd.get(1))) {
				isAlert = true;
				alertMessage = "Today's " + eventTypeStr + " has been cancelled";
			}
		}
		if(alertBadgeUsers.size() > 0 ) sendAlertBadges(alertBadgeUsers, isAlert, alertMessage);
		
		///////////////////
		// #4 Post Activity
		///////////////////
		if(theTeam != null) postActivity(theTeam, getEventDeletedActivityBody(theUser.getFullName(), theTeam.getTeamName(), theGame, thePractice));
    }

    private static String getEventDeletedMessageBody(String theUserFullName, String theTeamName, Game theGame, Practice thePractice) {
    	String eventTypeStr = Utility.getEventType(theGame, thePractice);
    	StringBuffer sb = new StringBuffer();
    	sb.append("The ");
    	sb.append(eventTypeStr);
    	sb.append(" originally scheduled for ");
    	String startDateStr = theGame != null ? theGame.getEventLocalStartDate() : thePractice.getEventLocalStartDate();
    	sb.append(startDateStr);
    	sb.append(" for team '");
    	sb.append(theTeamName);
    	sb.append("' has been removed from the schedule by ");
    	sb.append(theUserFullName);
    	sb.append(".");
    	return sb.toString();
    }
    // PRIORITY TODO
    //  1. pass in time zone and get a compact date format printing properly
    //  2. try to make this message fit into 140 chars
    private static String getEventDeletedActivityBody(String theUserFullName, String theTeamName, Game theGame, Practice thePractice) {
    	String eventTypeStr = Utility.getEventType(theGame, thePractice);
    	StringBuffer sb = new StringBuffer();
    	sb.append("The ");
    	sb.append(eventTypeStr);
    	sb.append(" originally scheduled for ");
    	String startDateStr = theGame != null ? theGame.getEventLocalStartDate() : thePractice.getEventLocalStartDate();
    	sb.append(startDateStr);
    	sb.append(" has been removed from the schedule by ");
    	sb.append(theUserFullName);
    	sb.append(".");
    	return sb.toString();
    }
    private static String getEventDeletedSmsMessage(String theUserAbbreviatedName, String theTeamName, Game theGame, Practice thePractice) {
    	String eventTypeStr = Utility.getEventType(theGame, thePractice);
    	StringBuffer sb = new StringBuffer();
    	sb.append(eventTypeStr);
    	sb.append(" originally\nscheduled for\n");
    	String startDateStr = theGame != null ? theGame.getEventLocalStartDate() : thePractice.getEventLocalStartDate();
    	sb.append(startDateStr);
    	sb.append("\nhas been cancelled\nby ");
    	sb.append(theUserAbbreviatedName);
    	return sb.toString();
    }
    
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Team Info Update Message
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Notify team members that the team information has been updated.
	//      Type: parameter passed in
	//      Recipients: all team members.
	//      Options: not applicable.
	//      Conditional: Yes. Based on member access preferences.
	// #1 Message Thread Created
	//      Inbox: Displayed for users. 
	//      Inbox New Message Count: incremented for each user.
	//      Link Reply Configured: not applicable.
	// #2 Message(s) Sent
	//      Non-Users: Always receive an email.
	//      Users: right now, email never sent (conditional sending of email message via user options not supported yet).
	//      Embedded Link(s): not applicable.
	// #3 Alert and Badges Sent 
	//      Alert: not applicable.
    //      Badge: update is always sent to Users
	// #4 Post Activity
	//      Yes.
	// #5 SMS
	//      Yes.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void sendTeamInfoUpdateMessage(List<Member> theMembers, String theNotificationMessage, Team theTeam) {
    	String subject = "rTeam: team info has been updated";
    	
    	List<Key> userKeys = new ArrayList<Key>();
    	List<String> messageBodys = new ArrayList<String>();
    	List<User> alertBadgeUsers = new ArrayList<User>();
       	List<UserMemberInfo> authorizedTeamRecipients = new ArrayList<UserMemberInfo>();
    	
    	// Build the RAW team recipient list (which is filtered for duplicates below)
    	for(Member m : theMembers) {
        	List<UserMemberInfo> authorizedMembershipRecipients = m.getAuthorizedRecipients(theTeam);
        	for(UserMemberInfo umi : authorizedMembershipRecipients) {
    			authorizedTeamRecipients.add(umi);
        	}
    	}
		
		///////////////////////////////////////////////////////////////////////////////////////
		// Filter Team Recipient List
		//      All entities in returned list have unique: emailAddress, userId and phoneNumber
		///////////////////////////////////////////////////////////////////////////////////////
		authorizedTeamRecipients = UserMemberInfo.filterDuplicates(authorizedTeamRecipients);
		
		List<UserMemberInfo> authorizedUserRecipients = new ArrayList<UserMemberInfo>();
		for(UserMemberInfo umi : authorizedTeamRecipients) {
			umi.setOneUseToken(TF.get());
			String userId = umi.getUserId();
			if(userId != null && umi.getHasRteamMessageAccessEnabled()) {
				// if member is user, send rTeam message. Message sent below, just built here.
				userKeys.add(KeyFactory.stringToKey(userId));
				authorizedUserRecipients.add(umi);
	    		String messageBody = getTeamInfoUpdateMessageBody(theNotificationMessage);
				messageBodys.add(messageBody);
			}
			if(umi.getEmailAddress() != null && umi.getHasEmailMessageAccessEnabled()) {
		    	///////////////////////////////////////////////////
		    	// #2 Message(s) Sent: send email
		    	///////////////////////////////////////////////////
	    		String emailBody = Emailer.getTeamInfoUpdateEmailBody(umi.getFullName(), theNotificationMessage, umi.getOneUseToken(), theTeam.getTwitterScreenName());
		    	Emailer.send(umi.getEmailAddress(), subject, emailBody, Emailer.REPLY);
			}
			if( (umi.getPhoneNumber() != null || umi.getSmsEmailAddress() != null) && umi.getHasSmsMessageAccessEnabled()) {
				//////////
				// #5 SMS 
				//////////
				String smsMessage = getTeamInfoUpdateSmsMessage(theNotificationMessage, theTeam.getTeamName());
				// the new ad-free SMS sent via SmsEmailAddress takes precedence over the phoneNumber (Zeep Mobile) SMS
				String smsAddress = umi.getSmsEmailAddress() == null ? umi.getPhoneNumber() : umi.getSmsEmailAddress();
				postSms(smsAddress, "Updated Team Info", smsMessage, Emailer.REPLY);
			}
		}
    	
    	////////////////////////////////////////////////////////
    	// #1 Message Thread Created - only users are recipients
    	////////////////////////////////////////////////////////
		if(authorizedUserRecipients.size() > 0) {
		    // only Users will be recipients of the messageThread 
	    	//createMessageThread(theAuthorizedTeamRecipients, theTeam, theSubject, theBodys, theMessageType, theMessageLinkOnly)
	    	createMessageThread(authorizedUserRecipients, theTeam, subject, messageBodys, MessageThread.PLAIN_TYPE, false);
		}
        
		/////////////////////////////////////////////
		// #1 Message Thread: increment message count
		/////////////////////////////////////////////
		// alertBadgeUsers is an out parameter
		if(userKeys.size() > 0 ) getUserFromKeysAndIncrementMessageCount(userKeys, alertBadgeUsers);

		//////////////////////////////////////////////////////////////////
		// #3 Alert and Badges sent: badge only, alert does not apply here
		//////////////////////////////////////////////////////////////////
		if(alertBadgeUsers.size() > 0 ) sendAlertBadges(alertBadgeUsers, false, null);
		
		///////////////////
		// #4 Post Activity
		///////////////////
		if(theTeam != null) postActivity(theTeam, getTeamInfoUpdateActivityBody(theNotificationMessage, theTeam.getTeamName()));
    }
    
    private static String getTeamInfoUpdateMessageBody(String theNotificationMessage) {
    	StringBuffer sb = new StringBuffer();
    	sb.append(theNotificationMessage);
    	return sb.toString();
    }
    private static String getTeamInfoUpdateActivityBody(String theNotificationMessage, String theTeamName) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("'");
    	sb.append(theTeamName);
    	sb.append("' team info updated: ");
    	sb.append(theNotificationMessage);
    	return sb.toString();
    }
    private static String getTeamInfoUpdateSmsMessage(String theNotificationMessage, String theTeamName) {
    	StringBuffer sb = new StringBuffer();
    	sb.append(theTeamName);
    	sb.append("\n");
    	sb.append("team info updated:\n");
    	sb.append(theNotificationMessage);
    	return sb.toString();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Team Inactivated Message
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Notify team members that the team is no longer active.
	//      Type: parameter passed in
	//      Recipients: all team members.
	//      Options: not applicable.
	//      Conditional: Yes. Based on member access preferences.
	// #1 Message Thread Created
	//      Inbox: Displayed for users. 
	//      Inbox New Message Count: incremented for each user.
	//      Link Reply Configured: not applicable.
	// #2 Message(s) Sent
	//      Non-Users: Always receive an email.
	//      Users: right now, email never sent (conditional sending of email message via user options not supported yet).
	//      Embedded Link(s): not applicable.
	// #3 Alert and Badges Sent 
	//      Alert: not applicable.
    //      Badge: update is always sent to Users
	// #4 Post Activity
	//      Yes.
	// #5 SMS
	//      Yes.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void sendTeamInactiveMessage(List<Member> theMembers, Team theTeam) {
    	String subject = "rTeam: team has been disbanded";
    	
    	List<Key> userKeys = new ArrayList<Key>();
    	List<String> messageBodys = new ArrayList<String>();
    	List<User> alertBadgeUsers = new ArrayList<User>();
       	List<UserMemberInfo> authorizedTeamRecipients = new ArrayList<UserMemberInfo>();
    	
    	// Build the RAW team recipient list (which is filtered for duplicates below)
    	for(Member m : theMembers) {
        	List<UserMemberInfo> authorizedMembershipRecipients = m.getAuthorizedRecipients(theTeam);
        	for(UserMemberInfo umi : authorizedMembershipRecipients) {
    			authorizedTeamRecipients.add(umi);
        	}
    	}
		
		///////////////////////////////////////////////////////////////////////////////////////
		// Filter Team Recipient List
		//      All entities in returned list have unique: emailAddress, userId and phoneNumber
		///////////////////////////////////////////////////////////////////////////////////////
		authorizedTeamRecipients = UserMemberInfo.filterDuplicates(authorizedTeamRecipients);
		
		List<UserMemberInfo> authorizedUserRecipients = new ArrayList<UserMemberInfo>();
		for(UserMemberInfo umi : authorizedTeamRecipients) {
			umi.setOneUseToken(TF.get());
			String userId = umi.getUserId();
			if(userId != null && umi.getHasRteamMessageAccessEnabled()) {
				// if member is user, send rTeam message. Message sent below, just built here.
				userKeys.add(KeyFactory.stringToKey(userId));
				authorizedUserRecipients.add(umi);
	    		String messageBody = getTeamInactiveMessageBody(theTeam.getTeamName());
				messageBodys.add(messageBody);
			}
			if(umi.getEmailAddress() != null && umi.getHasEmailMessageAccessEnabled()) {
		    	///////////////////////////////////////////////////
		    	// #2 Message(s) Sent: send email
		    	///////////////////////////////////////////////////
	    		String emailBody = Emailer.getTeamInactiveEmailBody(umi.getFullName(), theTeam.getTeamName(), umi.getOneUseToken(), theTeam.getTwitterScreenName());
		    	Emailer.send(umi.getEmailAddress(), subject, emailBody, Emailer.NO_REPLY);
			}
			if( (umi.getPhoneNumber() != null || umi.getSmsEmailAddress() != null) && umi.getHasSmsMessageAccessEnabled()) {
				//////////
				// #5 SMS 
				//////////
				String smsMessage = getTeamInactiveSmsMessage(theTeam.getTeamName());
				// the new ad-free SMS sent via SmsEmailAddress takes precedence over the phoneNumber (Zeep Mobile) SMS
				String smsAddress = umi.getSmsEmailAddress() == null ? umi.getPhoneNumber() : umi.getSmsEmailAddress();
				postSms(smsAddress, "Team Inactivated", smsMessage, Emailer.NO_REPLY);
			}
		}
    	
    	////////////////////////////////////////////////////////
    	// #1 Message Thread Created - only users are recipients
    	////////////////////////////////////////////////////////
		if(authorizedUserRecipients.size() > 0) {
		    // only Users will be recipients of the messageThread 
	    	//createMessageThread(theAuthorizedTeamRecipients, theTeam, theSubject, theBodys, theMessageType, theMessageLinkOnly)
	    	createMessageThread(authorizedUserRecipients, theTeam, subject, messageBodys, MessageThread.PLAIN_TYPE, false);
		}
        
		/////////////////////////////////////////////
		// #1 Message Thread: increment message count
		/////////////////////////////////////////////
		// alertBadgeUsers is an out parameter
		if(userKeys.size() > 0 ) getUserFromKeysAndIncrementMessageCount(userKeys, alertBadgeUsers);

		//////////////////////////////////////////////////////////////////
		// #3 Alert and Badges sent: badge only, alert does not apply here
		//////////////////////////////////////////////////////////////////
		if(alertBadgeUsers.size() > 0 ) sendAlertBadges(alertBadgeUsers, false, null);
		
		///////////////////
		// #4 Post Activity
		///////////////////
		if(theTeam != null) postActivity(theTeam, getTeamInactiveCoreBody(theTeam.getTeamName()));
    }

    private static String getTeamInactiveMessageBody(String theTeamName) {
    	StringBuffer sb = new StringBuffer();
    	sb.append(getTeamInactiveCoreBody(theTeamName));
    	return sb.toString();
    }
    private static String getTeamInactiveCoreBody(String theTeamName) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("The team '");
    	sb.append(theTeamName);
    	sb.append("' has been disbanded. You can remove the team from your team list.");
    	return sb.toString();
    }
    private static String getTeamInactiveSmsMessage(String theTeamName) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("The team '");
    	sb.append(theTeamName);
    	sb.append("'\nhas been disbanded.\nYou can remove the team\nfrom your team list.");
    	return sb.toString();
    }

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // New Team Member Message
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Notify team that a new [coordinator, member, fan] has joined the team.
	//      Type: not applicable
	//      Recipients: none.
	//      Options: not applicable.
	//      Conditional: No. 
	// #1 Message Thread Created
	//      not applicable.
	// #2 Message(s) Sent
	//      not applicable
	// #3 Alert and Badges Sent 
	//      not applicable.
	// #4 Post Activity
	//      Yes.
	// #5 SMS
	//      Not applicable.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void sendNewTeamMemberMessage(UserMemberInfo theUserMemberInfo) {
    	String displayName = Member.getDisplayName(theUserMemberInfo.getFirstName(), theUserMemberInfo.getLastName(),
    			                               theUserMemberInfo.getEmailAddress(), theUserMemberInfo.getPhoneNumber());
    	StringBuffer sb = new StringBuffer();
    	sb.append("We welcome ");
    	sb.append(displayName);
    	
    	if(theUserMemberInfo.getIsGuardian()) {
    		sb.append(" (parent of ");
    		sb.append(theUserMemberInfo.getPrimaryDisplayName());
    		sb.append(")");
    	}
    	
    	sb.append(" as a new ");
    	sb.append(theUserMemberInfo.getParticipantRole());
    	sb.append(" of the ");
    	sb.append(theUserMemberInfo.getTeam().getTeamName());
		
		///////////////////
		// #4 Post Activity
		///////////////////
		postActivity(theUserMemberInfo.getTeam(), sb.toString());
    }
    
 	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Attendance Post
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Post activity that attendance has been taken.
	//      Type: not applicable
	//      Recipients: not applicable.
	//      Options: not applicable.
	//      Conditional: No.
	// #1 Message Thread Created
	//      not applicable.
	// #2 Message(s) Sent
	//      not applicable
	// #3 Alert and Badges Sent 
	//      not applicable.
	// #4 Post Activity
	//      Yes.
	// #5 SMS
	//      Not applicable.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void postAttendanceActivity(Team theTeam, List<Member> theMembersPresent, User theUser, Game theGame, Practice thePractice) {
		
		///////////////////
		// #4 Post Activity
		///////////////////
    	// If the post exceeds the maximum sized, then do multiple posts so everything is included.
		if(theTeam != null) postActivity(theTeam, getAttendanceActivityPost(theUser, theMembersPresent, theGame, thePractice), true);

    }
    private static String getAttendanceActivityPost(User theUser, List<Member> theMembersPresent, Game theGame, Practice thePractice) {
    	String abbrLastName = theUser.getLastName().length() > 1 ? theUser.getLastName().substring(0, 1) : theUser.getLastName();
    	String displayName = Member.getDisplayName(theUser.getFirstName(), abbrLastName, theUser.getEmailAddress(), theUser.getPhoneNumber());
    	String eventTypeStr = Utility.getEventType(theGame, thePractice);
    	StringBuffer sb = new StringBuffer();
    	sb.append(displayName);
    	sb.append(" took attendance for ");
    	sb.append(eventTypeStr);
    	sb.append(" ");
    	String startDateStr = theGame != null ? theGame.getEventLocalStartDate() : thePractice.getEventLocalStartDate();
    	sb.append(startDateStr);
    	sb.append(". Members present include: ");
    	sb.append(buildMembersPresentList(theMembersPresent));
    	return sb.toString();
    }
    private static String buildMembersPresentList(List<Member> theMembersPresent) {
    	StringBuffer sb = new StringBuffer();
    	boolean isFirstMember = true;
    	for(Member m : theMembersPresent) {
    		if(!isFirstMember) {
    			sb.append(", ");
    		}
    		sb.append(m.getDisplayName());
    		isFirstMember = false;
    	}
    	return sb.toString();
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Game Summary Message
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Send game summary message describing the highlights of the game (continue to enhance this over time)
	//      Type: parameter passed in
	//      Recipients: all team members.
	//      Options: not applicable.
	//      Conditional: Yes. Based on member access preferences.
	// #1 Message Thread Created
	//      Inbox: Displayed for users. 
	//      Inbox New Message Count: incremented for each user.
	//      Link Reply Configured: not applicable.
	// #2 Message(s) Sent
	//      Non-Users: Receive an email or SMS as appropriate
	//      Users: right now, email never sent (conditional sending of email message via user options not supported yet).
	//      Embedded Link(s): not applicable.
	// #3 Alert and Badges Sent 
	//      Alert: not applicable.
    //      Badge: update is always sent to Users
	// #4 Post Activity
	//      Yes.
	// #5 SMS
	//      Yes.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void sendGameSummaryMessage(List<Member> theMembers, Team theTeam, Game theGame) {
    	String subject = "rTeam: " + theGame.getEventLocalStartDate() + " Game Summary";
    	
    	List<Key> userKeys = new ArrayList<Key>();
    	List<String> messageBodys = new ArrayList<String>();
    	List<User> alertBadgeUsers = new ArrayList<User>();
       	List<UserMemberInfo> authorizedTeamRecipients = new ArrayList<UserMemberInfo>();
    	
    	// Build the RAW team recipient list (which is filtered for duplicates below)
    	for(Member m : theMembers) {
        	List<UserMemberInfo> authorizedMembershipRecipients = m.getAuthorizedRecipients(theTeam);
        	for(UserMemberInfo umi : authorizedMembershipRecipients) {
    			authorizedTeamRecipients.add(umi);
        	}
    	}
		
		///////////////////////////////////////////////////////////////////////////////////////
		// Filter Team Recipient List
		//      All entities in returned list have unique: emailAddress, userId and phoneNumber
		///////////////////////////////////////////////////////////////////////////////////////
		authorizedTeamRecipients = UserMemberInfo.filterDuplicates(authorizedTeamRecipients);
		
		List<UserMemberInfo> authorizedUserRecipients = new ArrayList<UserMemberInfo>();
		for(UserMemberInfo umi : authorizedTeamRecipients) {
			umi.setOneUseToken(TF.get());
			String userId = umi.getUserId();
			if(userId != null && umi.getHasRteamMessageAccessEnabled()) {
				// if member is user, send rTeam message. Message sent below, just built here.
				userKeys.add(KeyFactory.stringToKey(userId));
				authorizedUserRecipients.add(umi);
	    		String messageBody = getGameSummaryMessageBody(theTeam, theGame, theGame.getScoreUs(), theGame.getScoreThem());
				messageBodys.add(messageBody);
			}
			if(umi.getEmailAddress() != null && umi.getHasEmailMessageAccessEnabled()) {
		    	///////////////////////////////////////////////////
		    	// #2 Message(s) Sent: send email
		    	///////////////////////////////////////////////////
	    		String emailBody = Emailer.getGameSummaryEmailBody(umi.getFullName(), theTeam, umi.getOneUseToken(), theGame);
		    	Emailer.send(umi.getEmailAddress(), subject, emailBody, Emailer.REPLY);
			}
			if( (umi.getPhoneNumber() != null || umi.getSmsEmailAddress() != null) && umi.getHasSmsMessageAccessEnabled()) {
				//////////
				// #5 SMS 
				//////////
				String smsMessage = getGameSummarySmsMessage(theTeam, theGame, theGame.getScoreUs(), theGame.getScoreThem());
				// the new ad-free SMS sent via SmsEmailAddress takes precedence over the phoneNumber (Zeep Mobile) SMS
				String smsAddress = umi.getSmsEmailAddress() == null ? umi.getPhoneNumber() : umi.getSmsEmailAddress();
				postSms(smsAddress, "Game Summary", smsMessage, Emailer.REPLY);
			}
		}
    	
    	////////////////////////////////////////////////////////
    	// #1 Message Thread Created - only users are recipients
    	////////////////////////////////////////////////////////
		if(authorizedUserRecipients.size() > 0) {
		    // only Users will be recipients of the messageThread 
	    	//createMessageThread(theAuthorizedTeamRecipients, theTeam, theSubject, theBodys, theMessageType, theMessageLinkOnly)
	    	createMessageThread(authorizedUserRecipients, theTeam, subject, messageBodys, MessageThread.PLAIN_TYPE, false);
		}
        
		/////////////////////////////////////////////
		// #1 Message Thread: increment message count
		/////////////////////////////////////////////
		// alertBadgeUsers is an out parameter
		if(userKeys.size() > 0 ) getUserFromKeysAndIncrementMessageCount(userKeys, alertBadgeUsers);

		//////////////////////////////////////////////////////////////////
		// #3 Alert and Badges sent: badge only, alert does not apply here
		//////////////////////////////////////////////////////////////////
		if(alertBadgeUsers.size() > 0 ) sendAlertBadges(alertBadgeUsers, false, null);
		
		///////////////////
		// #4 Post Activity
		///////////////////
		if(theTeam != null) postActivity(theTeam, getGameSummaryActivityPost(theTeam, theGame, theGame.getScoreUs(), theGame.getScoreThem()));
    }

    private static String getGameSummaryMessageBody(Team theTeam, Game theGame, Integer theScoreUs, Integer theScoreThem) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("Final score for game ");
    	sb.append(theGame.getEventLocalStartDate());
    	sb.append(": ");
    	sb.append(theTeam.getTeamName());
    	sb.append("=");
    	String scoreUs = theScoreUs == null ? "0" : theScoreUs.toString();
    	sb.append(scoreUs);
    	sb.append(" ");
    	String opponent = theGame.getOpponent() == null || theGame.getOpponent().length() == 0 ? "Them" : theGame.getOpponent();
    	sb.append(opponent);
    	sb.append("=");
    	String scoreThem = theScoreThem == null ? "0" : theScoreThem.toString();
    	sb.append(scoreThem);
    	return sb.toString();
    }
    private static String getGameSummaryActivityPost(Team theTeam, Game theGame, Integer theScoreUs, Integer theScoreThem) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("Final score for game ");
    	sb.append(theGame.getEventLocalStartDate());
    	sb.append(": ");
    	sb.append(theTeam.getTeamName());
    	sb.append("=");
    	String scoreUs = theScoreUs == null ? "0" : theScoreUs.toString();
    	sb.append(scoreUs);
    	sb.append(" ");
    	String opponent = theGame.getOpponent() == null || theGame.getOpponent().length() == 0 ? "Them" : theGame.getOpponent();
    	sb.append(opponent);
    	sb.append("=");
    	String scoreThem = theScoreThem == null ? "0" : theScoreThem.toString();
    	sb.append(scoreThem);
    	return sb.toString();
    }
    private static String getGameSummarySmsMessage(Team theTeam, Game theGame, Integer theScoreUs, Integer theScoreThem) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("Final score for");
    	sb.append("\n");
    	sb.append(theGame.getEventLocalStartDate());
    	sb.append(" game");
    	sb.append("\n");
    	sb.append(theTeam.getTeamName());
    	sb.append("=");
    	String scoreUs = theScoreUs == null ? "0" : theScoreUs.toString();
    	sb.append(scoreUs);
    	sb.append("\n");
    	String opponent = theGame.getOpponent() == null || theGame.getOpponent().length() == 0 ? "Them" : theGame.getOpponent();
    	sb.append(opponent);
    	sb.append("=");
    	String scoreThem = theScoreThem == null ? "0" : theScoreThem.toString();
    	sb.append(scoreThem);
    	return sb.toString();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // End of Game Score Post
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Post end of game score
	//      Type: not applicable
	//      Recipients: not applicable.
	//      Options: not applicable.
	//      Conditional: No.
	// #1 Message Thread Created
	//      not applicable.
	// #2 Message(s) Sent
	//      not applicable
	// #3 Alert and Badges Sent 
	//      not applicable.
	// #4 Post Activity
	//      Yes.
	// #5 SMS
	//      Not applicable.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void postScoreActivity(Team theTeam, Game theGame, Integer theScoreUs, Integer theScoreThem) {
		//log.info("posting end of game score: us=" + theScoreUs + " them=" + theScoreThem);
		///////////////////
		// #4 Post Activity
		///////////////////
		postActivity(theTeam, getScoreActivityPost(theTeam, theGame, theScoreUs, theScoreThem));

    }
    private static String getScoreActivityPost(Team theTeam, Game theGame, Integer theScoreUs, Integer theScoreThem) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("Final score for game ");
    	sb.append(theGame.getEventLocalStartDate());
    	sb.append(": ");
    	sb.append(theTeam.getTeamName());
    	sb.append("=");
    	String scoreUs = theScoreUs == null ? "0" : theScoreUs.toString();
    	sb.append(scoreUs);
    	sb.append(" ");
    	String opponent = theGame.getOpponent() == null || theGame.getOpponent().length() == 0 ? "Them" : theGame.getOpponent();
    	sb.append(opponent);
    	sb.append("=");
    	String scoreThem = theScoreThem == null ? "0" : theScoreThem.toString();
    	sb.append(scoreThem);
    	return sb.toString();
    }
    
 	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Game MVP Post
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Post game MVP
	//      Type: not applicable
	//      Recipients: not applicable.
	//      Options: not applicable.
	//      Conditional: No.
	// #1 Message Thread Created
	//      not applicable.
	// #2 Message(s) Sent
	//      not applicable
	// #3 Alert and Badges Sent 
	//      not applicable.
	// #4 Post Activity
	//      Yes.
	// #5 SMS
	//      Not applicable.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void postGameMvpActivity(Team theTeam, Game theGame, String theMvp) {
		///////////////////
		// #4 Post Activity
		///////////////////
		postActivity(theTeam, getGameMvpActivityPost(theTeam, theGame, theMvp));

    }
    private static String getGameMvpActivityPost(Team theTeam, Game theGame, String theMvp) {
    	StringBuffer sb = new StringBuffer();
    	sb.append(theMvp);
    	sb.append(" was voted MVP for game ");
    	sb.append(theGame.getEventLocalStartDate());
    	return sb.toString();
    }
    
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Reply to an Email Message
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #0 Overview
	//      Purpose: Send reply to an email message
	//      Type: MessageThread.PLAIN_TYPE
	//      Recipients: team coordinators/members.
	//      Options: not applicable
	//      Conditional: ??? ::jpw::
	// #1 Message Thread Created
	//      Inbox: Displayed for users. 
	//      Inbox New Message Count: incremented for each user.
	//      Link Reply Configured: not applicable.
	// #2 Message(s) Sent
	//      not applicable
	// #3 Alert and Badges Sent 
	//      Alert: not applicable.
	//      Badge: not applicable.
	// #4 Post Activity
	//      Not applicable.
	// #5 SMS
	//      Not applicable.
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static void sendReplyToEmailMessage(String theSubject, Team theTeam, List<Member> theMemberRecipients, String theReplyBody) {
	   	// subject is the same as the subject of the original message thread
	   	String messageType = MessageThread.PLAIN_TYPE;
    	
    	List<String> messageBodys = new ArrayList<String>();
    	List<Key> userKeys = new ArrayList<Key>();
    	List<User> alertBadgeUsers = new ArrayList<User>();
       	List<UserMemberInfo> authorizedTeamRecipients = new ArrayList<UserMemberInfo>();
    	
    	// Build the RAW team recipient list (which is filtered for duplicates below)
    	for(Member m : theMemberRecipients) {
        	List<UserMemberInfo> authorizedMembershipRecipients = m.getAuthorizedRecipients(theTeam);
        	for(UserMemberInfo umi : authorizedMembershipRecipients) {
    			authorizedTeamRecipients.add(umi);
        	}
    	}
		
		///////////////////////////////////////////////////////////////////////////////////////
		// Filter Team Recipient List
		//      All entities in returned list have unique: emailAddress, userId and phoneNumber
		///////////////////////////////////////////////////////////////////////////////////////
		authorizedTeamRecipients = UserMemberInfo.filterDuplicates(authorizedTeamRecipients);
		
		List<UserMemberInfo> authorizedUserRecipients = new ArrayList<UserMemberInfo>();
		for(UserMemberInfo umi : authorizedTeamRecipients) {
			String userId = umi.getUserId();
			if(userId != null && umi.getHasRteamMessageAccessEnabled()) {
				// if member is user, send rTeam message. Message sent below, just built here.
				userKeys.add(KeyFactory.stringToKey(userId));
				authorizedUserRecipients.add(umi);
	        	messageBodys.add(theReplyBody);
			}
		}
    	
    	////////////////////////////////////////////////////////
    	// #1 Message Thread Created - only users are recipients
    	////////////////////////////////////////////////////////
		if(authorizedUserRecipients.size() > 0) {
		    // only Users will be recipients of the messageThread 
	    	//createMessageThread(theAuthorizedTeamRecipients, theTeam, theSubject, theBodys, theMessageType, theMessageLinkOnly)
	    	createMessageThread(authorizedUserRecipients, theTeam, theSubject, messageBodys, MessageThread.PLAIN_TYPE, false);
		}
        
		/////////////////////////////////////////////
		// #1 Message Thread: increment message count
		/////////////////////////////////////////////
		// alertBadgeUsers is an out parameter
		if(userKeys.size() > 0 ) getUserFromKeysAndIncrementMessageCount(userKeys, alertBadgeUsers);

		//////////////////////////////////////////////////////////////////
		// #3 Alert and Badges sent: badge only, alert does not apply here
		//////////////////////////////////////////////////////////////////
		if(alertBadgeUsers.size() > 0 ) sendAlertBadges(alertBadgeUsers, false, null);
	}
    
    private static void sendAlertBadges(List<User> theAlertBadgeUsers, Boolean theIsAlert, String theMessage) {
		for(User u : theAlertBadgeUsers) {
			Boolean isDeveloper = false;
			// right now, only Nick needs to use Apple's Development Push Server
//			if(u.getEmailAddress().equalsIgnoreCase("njw438@gmail.com")) {
//				isDeveloper = true;
//			}
			String httpResponse = UrbanAirshipClient.register(u.getAlertToken(), isDeveloper);
			if(httpResponse != null) {
				String alertMsg = null;
				if(theIsAlert) {
					alertMsg = theMessage;
				}
				
				Integer newMessageCount = null;
				if(u.getNewMessageCount() != null && u.getNewMessageCount() > 0) {
					newMessageCount = u.getNewMessageCount();
				}
				
				if(alertMsg != null || newMessageCount != null) {
					httpResponse = UrbanAirshipClient.push(alertMsg, newMessageCount, u.getAlertToken(), isDeveloper);
					log.info("UrbanAirship response = " + httpResponse + " for user = " + u.getFullName());
				}
			}
		}
    }
    
    // theUserKeys: keys of users that need to be retrieved
    // theRetrievedUsers: out parameter to store users
    private static void getUserFromKeysAndIncrementMessageCount(List<Key> theUserKeys, List<User> theRetrievedUsers) {
    	// return right away if no work to do
    	if(theUserKeys.size() == 0) return;
    	
		// cannot use a NamedQuery for a batch get of keys
		EntityManager em = EMF.get().createEntityManager();
		try {
			List<User> users = null;
			if(theUserKeys.size() > 0) {
				users = (List<User>) em.createQuery(
						"select from " + User.class.getName() + " where key = :keys")
					.setParameter("keys", theUserKeys)
					.getResultList();
			}
			
			for(User u : users) {
				/////////////////////////////////////////////
				// #1 Message Thread: Inbox New Message Count
				/////////////////////////////////////////////
				u.incrementNewMessageCount();
				
				String deviceToken = u.getAlertToken();
				if(deviceToken != null && deviceToken.length() > 0) {
					theRetrievedUsers.add(u);
				}
			}
			
			// TODO Add userInfos to messageInfos if user option set up to receive message
		} finally {
			em.close();
		}
    }
    
    private static void postActivity(Team theTeam, String theActivityBody) {
    	// unless explicitly requested, multiPost if turned off and the post will be truncated if necessary
    	postActivity(theTeam, theActivityBody, false);
    }
    
    private static void postActivity(Team theTeam, String theActivityBody, Boolean theMultiPostActive) {
		// abbreviate only if necessary
		if(theActivityBody.length() > TwitterClient.MAX_TWITTER_CHARACTER_COUNT) {
			theActivityBody = Language.abbreviate(theActivityBody);
		}
		
    	log.info("abbreviated postActivity() body = " + theActivityBody);
		// enforce twitter max character count by truncating if necessary
		List<String> posts = new ArrayList<String>();
		if(theActivityBody.length() > TwitterClient.MAX_TWITTER_CHARACTER_COUNT) {
			if(theMultiPostActive) {
				int lowerIndex = 0;
				int upperIndex = TwitterClient.MAX_TWITTER_CHARACTER_COUNT;
				while(true) {
					String postMsg = theActivityBody.substring(lowerIndex, upperIndex);
					//log.info("postMsg = " + postMsg);
					posts.add(postMsg);
					lowerIndex = lowerIndex + TwitterClient.MAX_TWITTER_CHARACTER_COUNT;
					upperIndex = upperIndex + TwitterClient.MAX_TWITTER_CHARACTER_COUNT;
					
					if(lowerIndex >= theActivityBody.length()) {
						// were done, the activity body is fully parsed
						break;
					}
					
					if(upperIndex > theActivityBody.length()) {
						upperIndex = theActivityBody.length();
					}
				}
			} else {
				// just truncated since multi-post was not requested.
				posts.add(theActivityBody.substring(0, TwitterClient.MAX_TWITTER_CHARACTER_COUNT));
			}
		} else {
			// post is less than max size, no truncation needed
			posts.add(theActivityBody);
		}
		
		// TODO -- think this might be a Twitter4J bug
		// calling Twitter twice, first time with a very short message and then a longer one.
		// Here are the log messages
//		com.stretchcom.rteam.server.PubHub postActivity: calling updateStatus(): message = M, Joe W
//		I 2011-03-13 16:24:48.914
//		com.stretchcom.rteam.server.TwitterClient updateStatus: updated status sent to Twitter = team coordinator Joe Wroblewski has scheduled a new game for Sun, Mar 13, 11:20 PM. Status ID = 47065524617752577
//		I 2011-03-13 16:24:48.914
//		com.stretchcom.rteam.server.PubHub postActivity: calling updateStatus(): message = Joe Wroblewski took attendance for game Sun, Mar 13, 11:20 PM. Members present include: Jerry C, Wilma F, Pebbles F, William S, Fred F, Tom 
//		I 2011-03-13 16:24:49.204
//		com.stretchcom.rteam.server.TwitterClient updateStatus: updated status sent to Twitter = Joe Wroblewski took attendance for game Sun, Mar 13, 11:20 PM. Members present include: Jerry C, Wilma F, Pebbles F, William S, Fred F, Tom. Status ID = 47075670295195648			
		
		int numberOfPosts = posts.size();
		log.info("number of posts = " + numberOfPosts);
		twitter4j.Status twitterStatus = null;
		EntityManager emActivity = EMF.get().createEntityManager();
		try {
			while(numberOfPosts > 0) {
				String postMsg = posts.get(numberOfPosts - 1);
				
				Activity newActivity = new Activity();
				newActivity.setText(postMsg);
				newActivity.setCreatedGmtDate(new Date());
				newActivity.setTeamId(KeyFactory.keyToString(theTeam.getKey()));
				newActivity.setTeamName(theTeam.getTeamName());
				newActivity.setContributor(RteamApplication.AUTO_POST);
				
				// cacheId held in team is the last used.
				Long cacheId = theTeam.getNewestCacheId() + 1;
				newActivity.setCacheId(cacheId);
				theTeam.updateNewestCacheId(cacheId);

				if(theTeam.getUseTwitter()) {
					twitterStatus = TwitterClient.updateStatus(postMsg, theTeam.getTwitterAccessToken(), theTeam.getTwitterAccessTokenSecret());
					
					// if Twitter update failed, log error, but continue because activity post will be stored by rTeam
					if(twitterStatus == null) {
						log.severe("Twitter update failed, but continuing on ...");
					} else {
						newActivity.setTwitterId(twitterStatus.getId());
						// if posted to twitter, match the exact twitter date
						newActivity.setCreatedGmtDate(twitterStatus.getCreatedAt());
					}
				}
				
				// ** error when multiple activities created unless I surrounded it with a transaction **
				emActivity.getTransaction().begin();
				emActivity.persist(newActivity);
				emActivity.getTransaction().commit();
				log.info("new Activity was successfully persisted");
				
				numberOfPosts--;
			}
		} catch(Exception e) {
			log.severe("createActivity() exception = " + e.getMessage());
		} finally {
			emActivity.close();
		}
    }
    
//    private static void postSms(String theSmsAddress, String theSmsMessage) {
//    	postSms(theSmsAddress, null, theSmsMessage, null);
//    }
    
    // theSmsAddress: either a 10 digit phone number or an SMS email address
    // theSmsSubject: subject, if any.  Can be null. If null and subject is required, then a default subject will be inserted.
    // theSmsMessage: message to send. Message will be abbreviated to fit as much on the initial SMS screen msg as possible.
    // theSmsType: [confirm, poll, reply]. No checking done to verify one of these types passed in.
    private static void postSms(String theSmsAddress, String theSmsSubject, String theSmsMessage, String theSmsType) {
    	// always abbreviate text messages even if less than the 160 to fit as much of the message as
    	// possible in the text message alert pop-up
    	theSmsMessage = Language.abbreviate(theSmsMessage);
    	
    	if(theSmsAddress.contains("@")) {
    		if(theSmsSubject == null || theSmsSubject.length() == 0) theSmsSubject = ".";
    		// send via mobile carrier email address. All text messages from sms@rteam.com
    		Emailer.send(theSmsAddress, theSmsSubject, theSmsMessage, Emailer.SMS);
    	} else {
    		// if subject present, concatenate with message
    		if(theSmsSubject == null || theSmsSubject.length() > 0) {
    			theSmsMessage = theSmsSubject + "\n" + theSmsMessage;
    		}
    		
    		// send via ZeepMobile
        	// ZeepMobile expects the phone number to be prefixed with "+1"
        	theSmsAddress = "+1" + theSmsAddress;
        	ZeepMobileClient.postSms(theSmsAddress, theSmsMessage);
    	}
    }
    
    private static String getAlertIfAny(Game theGame, Practice thePractice, String theEventType, Boolean theIsNew) {
		Date startDate =  null;
		TimeZone tz = null;
		String alertMessage = null;
		if(theGame != null) {
			startDate = theGame.getEventGmtStartDate();
			tz = GMT.getTimeZone(theGame.getTimeZone());
		} else {
			startDate = thePractice.getEventGmtStartDate();
			tz = GMT.getTimeZone(thePractice.getTimeZone());
		}
		
		alertMessage = getAlertIfEventToday(startDate, theEventType, tz, theIsNew);
		return alertMessage;
    }
    
    private static String getAlertIfAny(List<Game> theGames, List<Practice> thePractices, String theEventType, Boolean theIsNew) {
		Date startDate =  null;
		TimeZone tz = null;
		String alertMessage = null;
		
		if(theGames != null) {
			for(Game g : theGames) {
				startDate = g.getEventGmtStartDate();
				tz = GMT.getTimeZone(g.getTimeZone());
				alertMessage = getAlertIfEventToday(startDate, theEventType, tz, theIsNew);
				if(alertMessage != null) {
					break;
				}
			}
		} else if(thePractices != null) {
			for(Practice p : thePractices) {
				startDate = p.getEventGmtStartDate();
				tz = GMT.getTimeZone(p.getTimeZone());
				alertMessage = getAlertIfEventToday(startDate, theEventType, tz, theIsNew);
				if(alertMessage != null) {
					break;
				}
			}
		}
		
		return alertMessage;
    }
    
    private static String getAlertIfEventToday(Date theStartDate, String theEventType, TimeZone theTz, Boolean theIsNew) {
    	String alertMessage = null;
		if(theStartDate != null && theTz != null) {
			// TODO not perfect using the timezone in the event itself, but I don't know how else to do it.
			List<Date> todayStartAndEnd = GMT.getTodayBeginAndEndDates(theTz);
			if(theStartDate.after(todayStartAndEnd.get(0)) && theStartDate.before(todayStartAndEnd.get(1))) {
				if(theIsNew) {
					alertMessage = "New " + theEventType + " just scheduled for today";
				} else {
					alertMessage = "Today's " + theEventType + " details have been updated";
				}
			}
		}
		return alertMessage;
    }
    
    private static String buildAlertMessage(String theSubject, String theBody, String theSendersName) {
    	StringBuffer sb = new StringBuffer();
    	sb.append(theSubject);
    	sb.append(": ");
    	sb.append(theBody);
    	
    	String rawMessage = sb.toString();
    	int sendersNameLength = theSendersName.length() + 5;
    	if((rawMessage.length() + sendersNameLength) > MAX_ALERT_MESSAGE_SIZE) {
    		int truncatedRawMessageSize = MAX_ALERT_MESSAGE_SIZE - sendersNameLength;
    		rawMessage = rawMessage.substring(0, truncatedRawMessageSize) + "..";
    	}
    	String alertMessage = rawMessage + " (" + theSendersName + ")";
    	return alertMessage;
    }

}
