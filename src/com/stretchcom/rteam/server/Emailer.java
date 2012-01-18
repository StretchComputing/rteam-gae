package com.stretchcom.rteam.server;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
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

import org.restlet.data.Reference;
import org.restlet.data.Status;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.google.appengine.api.labs.taskqueue.TaskOptions.Method;


public class Emailer {
	//private static final Logger log = Logger.getLogger(Emailer.class.getName());
	private static RskyboxClient log = new RskyboxClient();
	
	private static final String BASE_FROM_EMAIL_ADDRESS = "@rteam.com";
	private static final String FROM_EMAIL_ADDRESS = "reply@rteam.com";
	private static final String FROM_EMAIL_USER = "automated rTeam email";
	public static final String MESSAGE_THREAD_BODY = "see recipeint body";
	public static final String MESSAGE_LINK_REPLY_BODY = "used only for message link reply";
	public static final String REPLY_EMAIL_ADDRESS = "reply" + BASE_FROM_EMAIL_ADDRESS;
	public static final String REPLY = "reply";
	public static final String NO_REPLY = "noreply";
	public static final String SMS = "sms";
	public static final String JOIN = "join";
	public static final String POLL = "poll";
	public static final String CONFIRM = "confirm";
	
//	public static void send(String theEmailAddress, String theSubject, String theMessageBody) {
//		send(theEmailAddress, theSubject, theMessageBody, null);
//	}
	
	// Creates an email task and enqueues it.
	// TODO fromEmailUser should be a parameter passed in so email signature can be different when messageThread sent as email??
	public static void send(String theEmailAddress, String theSubject, String theMessageBody, String theFromUserName) {
		String fromEmailAddress = FROM_EMAIL_ADDRESS;
		if(theFromUserName != null) {
			fromEmailAddress = theFromUserName + BASE_FROM_EMAIL_ADDRESS;
		}
		
		// URL "/sendEmailTask" is routed to EmailTaskServlet in web.xml
		// not calling name() to name the task, so the GAE will assign a unique name that has not been used in 7 days (see book)
		// method defaults to POST, but decided to be explicit here
		// PRIORITY TODO need to somehow secure this URL. Book uses web.xml <security-constraint> but not sure why it restricts the
		//               URL to task queues (I see how it restricts it to admins)
		TaskOptions taskOptions = TaskOptions.Builder.url("/sendEmailTask")
				.method(Method.POST)
				.param("emailAddress", theEmailAddress)
				.param("fromEmailAddress", fromEmailAddress)
				.param("fromEmailUser", FROM_EMAIL_USER)
				.param("subject", theSubject)
				.param("message", theMessageBody);
		Queue queue = QueueFactory.getQueue("email"); // "email" queue is defined in WEB-INF/queue.xml
		queue.add(taskOptions);
	}

	public static String getPlainMessageThreadEmailBody(String theFullName, String theBody, String theOneUseToken, String theTeamName, String theTwitterScreenName, String theSenderName) {
    	StringBuffer sb = new StringBuffer();
    	buildStandardEmailHeader(sb);
    	
    	sb.append("<div style='margin-bottom:15px;'>");
    	sb.append("<div>");
    	sb.append(theFullName + ", ");
    	sb.append("</div>");
    	sb.append("<div style='font-size:10px;'>");
    	sb.append("(member of '" + theTeamName + "' team)");
    	sb.append("</div>");
    	sb.append("</div>");
    	sb.append(theBody);
    	
    	// end of div with background color. This div starts in the email header
    	sb.append("</div>");

    	buildStandardEmailSignature(sb, theSenderName, theTwitterScreenName, theOneUseToken);
    	return sb.toString();
    }
    
    public static String getConfirmedMessageThreadEmailBody(String theFullName, String theBody, String theOneUseToken, String theTeamName,
    		                                                String theTwitterScreenName, String theSenderName) {
    	StringBuffer sb = new StringBuffer();
    	String url = RteamApplication.BASE_URL_WITH_SLASH + RteamApplication.GWT_HOST_PAGE + "?confirmedtoken=" + theOneUseToken;

    	buildStandardEmailHeader(sb);
    	
    	sb.append("<div style='margin-bottom:15px;'>");
    	sb.append("<div>");
    	sb.append(theFullName + ", ");
    	sb.append("</div>");
    	sb.append("<div style='font-size:10px;'>");
    	sb.append("(member of '" + theTeamName + "' team)");
    	sb.append("</div>");
    	sb.append("</div>");
    	sb.append(theBody);
    	
    	sb.append("<div style='margin-bottom:10px; margin-top:20px; font-weight:bold'>");
    	sb.append("Please confirm receipt of this message by clicking on the link below.");
    	sb.append("</div>");
    	
    	// end of div with background color. This div starts in the email header
    	sb.append("</div>");
    	
    	sb.append("<div style='height:20px;'></div>");
    	sb.append("<div>");
    	sb.append("<span style='margin-left:15px; margin-right:10px;'>");
    	sb.append("<img style='vertical-align:middle;' src='" + RteamApplication.BASE_URL_WITH_SLASH + "desktop/images/arrow_right_green_24.png' width='24' height='24' border='0' alt='*'>");
    	sb.append("</span>");
    	sb.append("<a href='" + url + "'>Send confirmation now</a>");
    	sb.append("</div>");
    	
    	buildStandardEmailSignature(sb, theSenderName, theTwitterScreenName, theOneUseToken);
    	return sb.toString();
    }
    
    public static String getPollMessageThreadEmailBody(String theFullName, String theBody, String theOneUseToken,
    		List<String> thePollChoices, String theTeamName, String theTwitterScreenName, String theSenderName) {
    	StringBuffer sb = new StringBuffer();
    	String baseUrl = RteamApplication.BASE_URL_WITH_SLASH + RteamApplication.GWT_HOST_PAGE + "?polltoken=" + theOneUseToken + "&" + "pollResponse=";
    	
    	buildStandardEmailHeader(sb);
    	
    	sb.append("<div style='margin-bottom:15px;'>");
    	sb.append("<div>");
    	sb.append(theFullName + ", ");
    	sb.append("</div>");
    	sb.append("<div style='font-size:10px;'>");
    	sb.append("(member of '" + theTeamName + "' team)");
    	sb.append("</div>");
    	sb.append("</div>");
    	sb.append(theBody);
    	
    	sb.append("<div style='margin-bottom:10px; margin-top:20px; font-weight:bold'>");
    	sb.append("Please respond to the Poll by clicking on the appropriate link below.");
    	sb.append("</div>");
    	
    	// end of div with background color. This div starts in the email header
    	sb.append("</div>");
    	
    	sb.append("<div style='height:20px;'></div>");
    	
    	for(String pollChoice : thePollChoices) {
    		String encodedPollChoice = Reference.encode(pollChoice);
    		String url = baseUrl + encodedPollChoice;
        	sb.append("<div>");
        	sb.append("<span style='margin-left:15px; margin-right:10px;'>");
        	sb.append("<img style='vertical-align:middle;' src='" + RteamApplication.BASE_URL_WITH_SLASH + "desktop/images/arrow_right_green_24.png' width='24' height='24' border='0' alt='*'>");
        	sb.append("</span>");
        	sb.append("<a href='" + url + "'>" + pollChoice + "</a>");
        	sb.append("</div>");
    	}
    	
    	buildStandardEmailSignature(sb, theSenderName, theTwitterScreenName, theOneUseToken);
    	return sb.toString();
    }
    
    public static String getFollowupMessageThreadEmailBody(String theFullName, String theBody, String theOneUseToken,
    		List<String> thePollChoices, String theTeamName, String theTwitterScreenName, String theSenderName) {
    	StringBuffer sb = new StringBuffer();
    	String baseUrl = RteamApplication.BASE_URL_WITH_SLASH + RteamApplication.GWT_HOST_PAGE + "?followuptoken=" + theOneUseToken;
    	
    	buildStandardEmailHeader(sb);
    	
    	sb.append("<div style='margin-bottom:15px;'>");
    	sb.append("<div>");
    	sb.append(theFullName + ", ");
    	sb.append("</div>");
    	sb.append("<div>");
    	sb.append("(Member of: '" + theTeamName + "' team)");
    	sb.append("</div>");
    	sb.append("</div>");
    	sb.append(theBody);
    	
    	sb.append("<div style='margin-bottom:10px; margin-top:20px; font-weight:bold'>");
    	sb.append("You can view the details of the Poll responses by clicking on the link below.");
    	sb.append("</div>");
    	
    	// end of div with background color. This div starts in the email header
    	sb.append("</div>");
    	
    	sb.append("<div style='height:20px;'></div>");
    	
		String url = baseUrl;
    	sb.append("<div>");
    	sb.append("<span style='margin-left:15px; margin-right:10px;'>");
    	sb.append("<img style='vertical-align:middle;' src='" + RteamApplication.BASE_URL_WITH_SLASH + "desktop/images/arrow_right_green_24.png' width='24' height='24' border='0' alt='*'>");
    	sb.append("</span>");
    	sb.append("<a href='" + url + "'>View Poll Details</a>");
    	sb.append("</div>");
    	
    	buildStandardEmailSignature(sb, theSenderName, theTwitterScreenName, theOneUseToken);
    	return sb.toString();
    }
    
    public static String getEmailActivationBody(String theFullName, String theUrl, String theOneUseToken, String theMessageType, Member theMember, User theInitiator) {
    	StringBuffer sb = new StringBuffer();
    	buildStandardEmailHeader(sb);
    	
    	sb.append("<div style='margin-bottom:15px;'>");
    	sb.append(theFullName + ", ");
    	sb.append("</div>");
    	
    	
    	if(theMessageType.equalsIgnoreCase(MessageThread.USER_ACTIVATION_TYPE)) {
        	sb.append("Thank you for becoming a rTeam user.");
        	sb.append("<div style='margin-top:10px;'>");
        	sb.append("The goal of rTeam is to enhance the Game experience by making it easier for everyone to participate in the fun.");
        	sb.append(" Once you confirm, you will have full access to rTeam. By default, all team notifications are sent only as");
        	sb.append(" rTeam messages that are handled directly inside the rTeam app. Later, you can specify an option to receive");
        	sb.append(" notifications at this email address as well.");
        	sb.append("</div>");
    	} else if(theMessageType.equalsIgnoreCase(MessageThread.MEMBER_ACTIVATION_TYPE)) {
    		if(theMember.isFan()) {
            	sb.append("You have been invited to become a fan of team '");
    		} else {
            	sb.append("You have been added as a member of team '");
    		}
        	sb.append(theMember.getTeam().getTeamName() + "' by ");
        	sb.append(theInitiator.getFullName() + ".");
        	sb.append("<div style='margin-top:10px;'>");
    		if(theMember.isFan()) {
            	sb.append("Join the fun and help support our team.");
    		} else {
            	sb.append("The goal of rTeam is to enhance the Game experience by making it easier for everyone to participate in the fun.");
    		}
    		sb.append(" Once you activate, all team notifications will be delivered to this email address allowing you");
    		sb.append(" to fully participate in team communications by confirming messages and responding to polls. At any time, you");
    		sb.append(" can further enhance your participation by downloading the rTeam application on your phone. When you do so, all");
    		sb.append(" existing team memberships will automatically be recognized and you can instantly start reaping the benefits");
    		sb.append(" of cloud based mobile computing.");
        	sb.append("</div>");
    	} else if(theMessageType.equalsIgnoreCase(MessageThread.EMAIL_UPDATE_CONFIRMATION_TYPE)) {
        	sb.append("Your email address has changed.");
    	}
    	
    	sb.append("<div style='margin-bottom:10px; margin-top:20px; font-weight:bold'>");
    	if(theMessageType.equalsIgnoreCase(MessageThread.USER_ACTIVATION_TYPE)) {
        	sb.append("Please confirm your rTeam account ");
    	} else if(theMessageType.equalsIgnoreCase(MessageThread.MEMBER_ACTIVATION_TYPE)) {
        	sb.append("Please activate your team membership ");
    	} else if(theMessageType.equalsIgnoreCase(MessageThread.EMAIL_UPDATE_CONFIRMATION_TYPE)) {
        	sb.append("Please activate your recently modified email address ");
    	}
    	sb.append("by clicking on the link below. ");
    	sb.append("</div>");
    	
    	// end of div with background color. This div starts in the email header
    	sb.append("</div>");
    	
    	sb.append("<div style='height:20px;'></div>");
    	sb.append("<div>");
    	sb.append("<span style='margin-left:15px; margin-right:10px;'>");
    	sb.append("<img style='vertical-align:middle;' src='" + RteamApplication.BASE_URL_WITH_SLASH + "desktop/images/arrow_right_green_24.png' width='24' height='24' border='0' alt='*'>");
    	sb.append("</span>");
    	sb.append("<a href='" + theUrl + "'>");
    	if(theMessageType.equalsIgnoreCase(MessageThread.USER_ACTIVATION_TYPE)) {
        	sb.append("Confirm your rTeam account now");
    	} else if(theMessageType.equalsIgnoreCase(MessageThread.MEMBER_ACTIVATION_TYPE)) {
        	sb.append("Activate your team membership now");
    	} else if(theMessageType.equalsIgnoreCase(MessageThread.EMAIL_UPDATE_CONFIRMATION_TYPE)) {
        	sb.append("Activate your new email now");
    	}
    	sb.append("</a>");
    	sb.append("</div>");
    	
    	buildStandardEmailSignature(sb, null, null, theOneUseToken);
    	
    	String notMyAccountUrl = theUrl + "&notMyAccount=true";
    	sb.append("<div style='font-size:10px; margin-top:20px; color:grey;'>");
    	if(theMessageType.equalsIgnoreCase(MessageThread.USER_ACTIVATION_TYPE)) {
        	sb.append("If you received this message in error and did not sign up for a rTeam account, click ");
    	} else if(theMessageType.equalsIgnoreCase(MessageThread.MEMBER_ACTIVATION_TYPE)) {
        	sb.append("If you believe this email was not intended for you, click ");
    	} else if(theMessageType.equalsIgnoreCase(MessageThread.EMAIL_UPDATE_CONFIRMATION_TYPE)) {
        	sb.append("If you are not a rTeam member that changed their email address, click ");
    	}
    	sb.append("<a href='" + notMyAccountUrl + "'>");
    	if(theMessageType.equalsIgnoreCase(MessageThread.USER_ACTIVATION_TYPE)) {
        	sb.append("not my account.");
    	} else if(theMessageType.equalsIgnoreCase(MessageThread.MEMBER_ACTIVATION_TYPE)) {
        	sb.append("delivered to the wrong person.");
    	} else if(theMessageType.equalsIgnoreCase(MessageThread.EMAIL_UPDATE_CONFIRMATION_TYPE)) {
        	sb.append("delivered to the wrong person.");
    	}
    	sb.append("</a>");
    	sb.append("</div>");
    	return sb.toString();
    }
    
    public static String getDeleteConfirmationEmailBody(Member theMember, String theMemberFullName, String theOneUseToken, String theBaseUrl, String theMessageType) {
    	StringBuffer sb = new StringBuffer();
    	buildStandardEmailHeader(sb);

    	sb.append("<div style='margin-bottom:15px;'>");
    	sb.append(theMemberFullName + ", ");
    	sb.append("</div>");
    	
    	sb.append(theMember.getMarkedForDeletionRequester());
    	sb.append(" has requested that your membership on ");
    	sb.append(theMember.getTeam().getTeamName());
    	sb.append(" team should be deleted.");
    	
    	sb.append("<div style='margin-bottom:10px; margin-top:20px; font-weight:bold'>");
    	sb.append("Please click on the appropriate link below to either accept or reject this request");
    	sb.append("</div>");
    	
    	// beginning of the div is in the email header
    	sb.append("</div>");
    	
    	String acceptUrl = theBaseUrl + "&" + "decision=accept";
    	String rejectUrl = theBaseUrl + "&" + "decision=reject";
    	
    	sb.append("<div style='height:20px;'></div>");
    	sb.append("<div>");
    	sb.append("<span style='margin-left:15px; margin-right:10px;'>");
    	sb.append("<img style='vertical-align:middle;' src='" + RteamApplication.BASE_URL_WITH_SLASH + "desktop/images/arrow_right_green_24.png' width='24' height='24' border='0' alt='*'>");
    	sb.append("</span>");
    	sb.append("<a href='" + acceptUrl + "'>Delete my membership</a> on this team");
    	sb.append("</div>");
    	
    	sb.append("<div style='height:20px;'></div>");
    	sb.append("<div>");
    	sb.append("<span style='margin-left:15px; margin-right:10px;'>");
    	sb.append("<img style='vertical-align:middle;' src='" + RteamApplication.BASE_URL_WITH_SLASH + "desktop/images/arrow_right_green_24.png' width='24' height='24' border='0' alt='*'>");
    	sb.append("</span>");
    	sb.append("<a href='" + rejectUrl + "'>Keep my membership</a> on this team");
    	sb.append("</div>");
    	
    	buildStandardEmailSignature(sb, null, null, theOneUseToken);
    	return sb.toString();
    }

    public static String getMemberUpdatedEmailBody(Member theMember, String theMemberFullName, String theOneUseToken, List<String> theModificationMessages,
    		                                       User theUser, String theTwitterScreenName) {
    	StringBuffer sb = new StringBuffer();
    	buildStandardEmailHeader(sb);
    	
    	sb.append("<div style='margin-bottom:15px;'>");
    	sb.append(theMemberFullName + ", ");
    	sb.append("</div>");
    	
    	sb.append("Your membership information on team '");
    	sb.append(theMember.getTeam().getTeamName());
    	sb.append("' has been updated by ");
    	sb.append(theUser.getFullName());
    	sb.append(".");
    	
    	sb.append("<div>");
    	for(String mm : theModificationMessages) {
    		sb.append(mm);
    		sb.append("<br/>");
    	}
    	sb.append("</div>");
    	
    	// end of div with background color. This <div> starts in the email header
    	sb.append("</div>");
    	
    	
    	buildStandardEmailSignature(sb, null, theTwitterScreenName, theOneUseToken);
    	return sb.toString();
    }
    
    // TODO add the names of the new coordinators to the message
    public static String getNewCoordinatorEmailBody(String theFullName, String theTeamName, String theOneUseToken, String theTwitterScreenName) {
    	StringBuffer sb = new StringBuffer();
    	buildStandardEmailHeader(sb);
    	
    	sb.append("<div style='margin-bottom:15px;'>");
    	sb.append(theFullName + ", ");
    	sb.append("</div>");
    	
    	sb.append("The only team coordinator has left the team '");
    	sb.append(theTeamName);
    	sb.append("'. So the team can continue, all members of the ");
    	sb.append("team that are also users have been 'promoted' to coordinator.");
    	sb.append("</div>");
    	
    	buildStandardEmailSignature(sb, null, theTwitterScreenName, theOneUseToken);
    	return sb.toString();
    }
    
    public static String getNoCoordinatorEmailBody(String theFullName, String theTeamName, String theOneUseToken, String theTwitterScreenName) {
    	StringBuffer sb = new StringBuffer();
    	buildStandardEmailHeader(sb);
    	
    	sb.append("<div style='margin-bottom:15px;'>");
    	sb.append(theFullName + ", ");
    	sb.append("</div>");

    	sb.append("The only team coordinator has left the team '");
    	sb.append(theTeamName);
    	sb.append("'. No one else on the team ");
    	sb.append("is a registered user of rTeam. One or more members should become registered rTeam users.");
     	sb.append("</div>");
    	
     	buildStandardEmailSignature(sb, null, theTwitterScreenName, theOneUseToken);
     	buildStandardEmailSignature(sb, theTwitterScreenName);
    	return sb.toString();
    }
    
    public static String getNewEventEmailBody(String theMemberFullName, String theUserFullName, String theOneUseToken, String theTeamName,
    		                                  String theTwitterScreenName, List<Game> theGames, List<Practice> thePractices) {
    	String eventTypeStr = Utility.getEventType(theGames, thePractices);
    	StringBuffer sb = new StringBuffer();
    	buildStandardEmailHeader(sb);

    	sb.append("<div style='margin-bottom:15px;'>");
    	sb.append(theMemberFullName + ", ");
    	sb.append("</div>");
    	
    	sb.append("Team coordinator ");
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
     	sb.append("</div>");
    	
     	buildStandardEmailSignature(sb, null, theTwitterScreenName, theOneUseToken);
    	return sb.toString();
    }
    
    public static String getTeamInfoUpdateEmailBody(String theFullName, String theNotificationMessage, String theOneUseToken, String theTwitterScreenName) {
    	StringBuffer sb = new StringBuffer();
    	buildStandardEmailHeader(sb);
    	
    	sb.append("<div style='margin-bottom:15px;'>");
    	sb.append(theFullName + ", ");
    	sb.append("</div>");

    	sb.append(theNotificationMessage);
     	sb.append("</div>");
    	
     	buildStandardEmailSignature(sb, null, theTwitterScreenName, theOneUseToken);
    	return sb.toString();
    }
    
    
    public static void sendPasswordResetEmail(User theUser, String theNewPassword) {
    	String subject = "rTeam: Password Reset";
    	String notifyMessage = "as requested, your password has been reset.  Your new password is: " + theNewPassword;
    	String body = getTeamInfoUpdateEmailBody(theUser.getFullName(), notifyMessage, null, null);
    	Emailer.send(theUser.getEmailAddress(), subject, body, Emailer.NO_REPLY);
    }
    
    private static String getPasswordResetEmailBody(String theFullName, String theNewPassword, String theOneUseToken) {
    	StringBuffer sb = new StringBuffer();
    	buildStandardEmailHeader(sb);
    	
    	sb.append("<div style='margin-bottom:15px;'>");
    	sb.append(theFullName + ", ");
    	sb.append("</div>");

    	sb.append("Your password has been reset to: ");
    	sb.append(theNewPassword);
     	sb.append("</div>");
    	
     	buildStandardEmailSignature(sb, null, null, theOneUseToken);
    	return sb.toString();
    }
    
    public static String getTeamInactiveEmailBody(String theFullName, String theTeamName, String theOneUseToken, String theTwitterScreenName) {
    	StringBuffer sb = new StringBuffer();
    	buildStandardEmailHeader(sb);
    	
    	sb.append("<div style='margin-bottom:15px;'>");
    	sb.append(theFullName + ", ");
    	sb.append("</div>");

    	sb.append("The team '");
    	sb.append(theTeamName);
    	sb.append("' has been disbanded. You can remove the team from your team list.");
    	sb.append("</div>");
    	
    	buildStandardEmailSignature(sb, null, theTwitterScreenName, theOneUseToken);
    	return sb.toString();
    }
    
    public static String getGameSummaryEmailBody(String theFullName, Team theTeam, String theOneUseToken, Game theGame, String thePhotoUrl) {
    	StringBuffer sb = new StringBuffer();
    	buildStandardEmailHeader(sb);
    	
    	sb.append("<div style='margin-bottom:15px;'>");
    	sb.append(theFullName + ", ");
    	sb.append("</div>");

    	sb.append("Final score for game on ");
    	sb.append(theGame.getEventLocalStartDate());
    	sb.append(": ");
	    	sb.append("<div style='margin-top:20px; margin-left:20px; font-size:20px;'>");
	    	sb.append(theTeam.getTeamName());
	    	sb.append(": ");
	    	String scoreUs = theGame.getScoreUs() == null ? "0" : theGame.getScoreUs().toString();
	    	sb.append(scoreUs);
	    	sb.append("<span style='margin-left:30px;'>");
	    	String opponent = theGame.getOpponent() == null || theGame.getOpponent().length() == 0 ? "Them" : theGame.getOpponent();
	    	sb.append(opponent);
	    	sb.append(": ");
	    	String scoreThem = theGame.getScoreThem() == null ? "0" : theGame.getScoreThem().toString();
	    	sb.append(scoreThem);
	    	sb.append("</span>");
	    	sb.append("</div>");
    	sb.append("</div>");
    	
    	if(thePhotoUrl != null) {
        	sb.append("<div style='height:10px;'></div>");
        	sb.append("<div style='margin: auto;'><img src='" + thePhotoUrl + "' border='0' alt='Game Photo'></div>");
        	sb.append("<div style='height:10px;'></div>");
    	}
    	
    	buildStandardEmailSignature(sb, null, theTeam.getTwitterScreenName(), theOneUseToken);
    	return sb.toString();
    }
    

    public static String getEventDeletedEmailBody(String theMemberFullName, String theUserFullName, String theOneUseToken, String theTeamName, 
    		                      String theTwitterScreenName, Game theGame, Practice thePractice) {
    	String eventTypeStr = Utility.getEventType(theGame, thePractice);
    	StringBuffer sb = new StringBuffer();
    	buildStandardEmailHeader(sb);
    	
    	sb.append("<div style='margin-bottom:15px;'>");
    	sb.append(theMemberFullName + ", ");
    	sb.append("</div>");
    	
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
    	sb.append("</div>");
    	
    	buildStandardEmailSignature(sb, null, theTwitterScreenName, theOneUseToken);
    	return sb.toString();
    }

    public static String getEventUpdatedEmailBody(String theMemberFullName, String theUserFullName, String theOneUseToken, String theTeamName, 
         String theTwitterScreenName, Game theGame, Practice thePractice, String theNotificationMessage, Boolean theUpdateAllLocations) {
    	String eventTypeStr = Utility.getEventType(theGame, thePractice);
    	StringBuffer sb = new StringBuffer();
    	buildStandardEmailHeader(sb);
    	
    	sb.append("<div style='margin-bottom:15px;'>");
    	sb.append(theMemberFullName + ", ");
    	sb.append("</div>");
    	
    	if(theUpdateAllLocations) {
    		sb.append("The location of all ");
    		sb.append(eventTypeStr);
    		sb.append("s has been changed to ");
    		
    	} else {
        	sb.append("The ");
        	sb.append(eventTypeStr);
        	sb.append(" originally scheduled for ");
        	String startDateStr = theGame != null ? theGame.getEventLocalStartDate() : thePractice.getEventLocalStartDate();
        	sb.append(startDateStr);
        	sb.append(" for team '");
        	sb.append(theTeamName);
        	sb.append("' has been udpated as follows: ");
    	}
    	sb.append(theNotificationMessage);
    	sb.append(".");
    	sb.append("</div>");
    	
    	buildStandardEmailSignature(sb, null, theTwitterScreenName, theOneUseToken);
    	buildStandardEmailSignature(sb, theTwitterScreenName);
    	return sb.toString();
    }
    
    private static void buildStandardEmailHeader(StringBuffer sb) {
    	sb.append("<html>");
    	sb.append("<head></head>");
    	sb.append("<body>");
    	
    	sb.append("<div><img src='" + RteamApplication.BASE_URL_WITH_SLASH + "desktop/images/rteamEmailLogo.png' width='155' height='46' border='0' alt='rTeam Logo'></div>");
    	sb.append("<div style='height:5px;'></div>");
    	
    	sb.append("<div style='padding:20px; border-radius:10px; -o-border-radius:10px; -icab-border-radius:10px; -khtml-border-radius:10px; ");
    	sb.append("-moz-border-radius:10px; -webkit-border-radius:10px; background-color: #ccc; border: 1px solid #000; width:85%;'>");
    }
    
    private static void buildStandardEmailSignature(StringBuffer sb, String theTwitterScreenName) {
    	buildStandardEmailSignature(sb, null, theTwitterScreenName, null);
    }
    
    private static void buildStandardEmailSignature(StringBuffer sb, String theSenderName, String theTwitterScreenName) {
    	buildStandardEmailSignature(sb, theSenderName, theTwitterScreenName, null);
    }
   
    private static void buildStandardEmailSignature(StringBuffer sb, String theSenderName, String theTwitterScreenName, String theOneUseToken) {
    	sb.append("<div style='margin-top:30px; font-size:12px;'>");
    	sb.append("Regards,");
    	sb.append("</div>");
    	sb.append("<div style='margin-top:1px; font-size:12px;'>");
    	if(theSenderName == null) {
        	sb.append("(automated message from rTeam)");
    	} else {
    		sb.append(theSenderName);
    	}
    	sb.append("</div>");
    	
    	if(theTwitterScreenName != null && theTwitterScreenName.length() > 0) {
    		sb.append("<div style='margin-top:12px; font-size:12px;'>");
    		sb.append("Follow the team on Twitter: ");
    		sb.append("<span style='color:blue;'>");
    		String twitterUrl = "http://www.twitter.com/" + theTwitterScreenName;
    		sb.append("<a href='");
    		sb.append(twitterUrl);
    		sb.append("'>");
    		sb.append("@");
    		sb.append(theTwitterScreenName);
    		sb.append("</a>");
    		sb.append("</span>");
    		sb.append("</div>");
    	
    		sb.append("<div style='margin-top:1px; font-size:12px;'>");
    		sb.append("Visit us at: ");
    		sb.append("<span style='color:blue;'>");
    		String rteamUrl = "http://www.rteam.com";
    		sb.append("<a href='");
    		sb.append(rteamUrl);
    		sb.append("'>");
    		sb.append("rTeam.com");
    		sb.append("</a>");
    		sb.append("</span>");
    		sb.append("</div>");
        	
    		sb.append("<div style='margin-top:1px; font-size:8px;'>");
    		sb.append("<span style='color:white;'>");
    		if(theOneUseToken != null) {
        		sb.append(RteamApplication.EMAIL_START_TOKEN_MARKER);
        		sb.append(theOneUseToken);
        		sb.append(RteamApplication.EMAIL_END_TOKEN_MARKER);
    		}
    		sb.append("</span>");
    		sb.append("</div>");
    	}
    	
    	sb.append("</body>");
    	sb.append("</html>");
    }

}
