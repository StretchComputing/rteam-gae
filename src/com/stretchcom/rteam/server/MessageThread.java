package com.stretchcom.rteam.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

@Entity
@NamedQueries({
    @NamedQuery(
    		name="MessageThread.getByKey",
    		query="SELECT mt FROM MessageThread mt WHERE mt.key = :key"
    ),
    @NamedQuery(
    		name="MessageThread.getBySenderUserId",
    		query="SELECT mt FROM MessageThread mt WHERE mt.senderUserId = :senderUserId"
    ),
    @NamedQuery(
    		name="MessageThread.getBySenderUserIdAndTeamId",
    		query="SELECT mt FROM MessageThread mt WHERE " +
    		      "mt.senderUserId = :senderUserId" + " AND " +
    		      "mt.teamId = :teamId"
    ),
    @NamedQuery(
    		name="MessageThread.getBySenderUserIdAndEventIdAndEventType",
    		query="SELECT mt FROM MessageThread mt WHERE " +
    				"mt.eventId = :eventId" + " AND " +
    				"mt.isGame = :isGame" + " AND " +
    				"mt.senderUserId = :senderUserId"
    ),
    @NamedQuery(
    		name="MessageThread.getBySenderUserIdAndEventIdAndEventTypeAndTeamId",
    		query="SELECT mt FROM MessageThread mt WHERE " +
    				"mt.eventId = :eventId" + " AND " +
    				"mt.isGame = :isGame" + " AND " +
    				"mt.senderUserId = :senderUserId" + " AND " +
    				"mt.teamId = :teamId"
    ),
    @NamedQuery(
    		name="MessageThread.getBySenderUserIdAndStatus",
    		query="SELECT mt FROM MessageThread mt WHERE " +
    		      "mt.senderUserId = :senderUserId" + " AND " +
    		      "mt.status = :status"
   ),
   @NamedQuery(
   		name="MessageThread.getBySenderUserIdAndStatusAndTeamId",
   		query="SELECT mt FROM MessageThread mt WHERE " +
   		      "mt.senderUserId = :senderUserId" + " AND " +
   		      "mt.status = :status" + " AND " +
   		      "mt.teamId = :teamId"
  ),
   @NamedQuery(
   		name="MessageThread.getBySenderUserIdAndEventIdAndEventTypeAndStatus",
   		query="SELECT mt FROM MessageThread mt WHERE " +
   				"mt.eventId = :eventId" + " AND " +
   				"mt.isGame = :isGame" + " AND " +
   				"mt.senderUserId = :senderUserId" + " AND " +
   				"mt.status = :status"
   ),
   @NamedQuery(
	   		name="MessageThread.getBySenderUserIdAndEventIdAndEventTypeAndStatusAndTeamId",
	   		query="SELECT mt FROM MessageThread mt WHERE " +
	   				"mt.eventId = :eventId" + " AND " +
	   				"mt.isGame = :isGame" + " AND " +
	   				"mt.senderUserId = :senderUserId" + " AND " +
	   				"mt.status = :status" + " AND " +
	   				"mt.teamId = :teamId"
	   ),
	@NamedQuery(
    		name="MessageThread.getOldActiveThru",
    		query="SELECT mt FROM MessageThread mt WHERE " + 
    				"mt.activeThruGmtDate < :currentDate"  + " AND " +
    				"mt.status = :status"
      ),
  	@NamedQuery(
    		name="MessageThread.getByStatus",
    		query="SELECT mt FROM MessageThread mt WHERE " + 
    				"mt.status = :status"
      ),
})
public class MessageThread {
	private static final Logger log = Logger.getLogger(MessageThread.class.getName());
	
	//constants
	public static final String NONE_TYPE = "none";
	public static final String PLAIN_TYPE = "plain";
	public static final String CONFIRMED_TYPE = "confirm";
	public static final String POLL_TYPE = "poll";
	public static final String WHO_IS_COMING_TYPE = "whoiscoming";
	public static final String USER_ACTIVATION_TYPE = "user activation";
	public static final String MEMBER_ACTIVATION_TYPE = "member activation";
	public static final String EMAIL_UPDATE_CONFIRMATION_TYPE = "email update confirmation";
	public static final String MEMBER_DELETE_CONFIRMATION_TYPE = "member delete confirmation";
	public static final String GAME_DELETED_TYPE = "game deleted";
	public static final String PRACTICE_DELETED_TYPE = "practice deleted";
	
	public static final String DRAFT_STATUS = "draft";
	public static final String ACTIVE_STATUS = "active";
	public static final String FINALIZED_STATUS = "finalized";
	public static final String ARCHIVED_STATUS = "archived";
	public static final String ALL_STATUS = "all";  //used for queries only
	
	public static final String INBOX_MESSAGE_GROUP = "inbox";
	public static final String OUTBOX_MESSAGE_GROUP = "outbox";
	public static final String ALL_MESSAGE_GROUP = "all";
	
	public static final String YES_WHO_IS_COMING_CHOICE = "yes";
	public static final String NO_WHO_IS_COMING_CHOICE = "no";
	public static final String MAYBE_WHO_IS_COMING_CHOICE = "maybe";

	public static final int MAX_NUMBER_OF_POLL_CHOICES = 5;
	public static final int MAX_NUMBER_OF_AUTO_ARCHIVE_DAY_COUNT = 30;
	
	private String subject;
	private Text message;
	private String followupMessage;
	private String type;
	private String status;
	private String eventId;         // soft schema: either a game key or a practice key
	private Date eventGmtStartDate;
	private Boolean isGame;
    private String teamId;          // soft schema: use id because team object is never used so @ManyToOne is overkill
    private String teamName;
	private String senderUserId;    // soft schema: use id because user object is never used so @ManyToOne is overkill
	private Date createdGmtDate;
	private Date finalizedGmtDate;
	private Date activeThruGmtDate;  // Active thru this date.  Member specific.
	private int numOfRecipients;
	private List<String> memberIdsThatReplied; // core of mechanism to only count replies once for families that share team membership
	private Boolean isAlert;
	private Boolean isPublic;  // applies to polls. Public polls results can be viewed by all. Private poll results by sender only.
	private Boolean includeEntireTeam;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Key key;
	
    @OneToMany(mappedBy = "messageThread", cascade = CascadeType.ALL)
    private List<Recipient> recipients = new ArrayList<Recipient>();
	
	@Basic
	private List<String> pollChoices;

	public Key getKey() {
        return key;
    }

	public List<Recipient> getRecipients() {
		return recipients;
	}

	public void setRecipients(List<Recipient> recipients) {
		this.recipients = recipients;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
    public List<String> getPollChoices() {
		return pollChoices;
	}

	public void setPollChoices(List<String> pollChoices) {
		this.pollChoices = pollChoices;
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

	public String getMessage() {
		return message == null? null : message.getValue();
	}

	public void setMessage(String message) {
		this.message = new Text(message);
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

    public String getTeamId() {
		return teamId;
	}

	public void setTeamId(String teamId) {
		this.teamId = teamId;
	}

	public String getSenderUserId() {
		return senderUserId;
	}

	public void setSenderUserId(String senderUserId) {
		this.senderUserId = senderUserId;
	}

	public Date getCreatedGmtDate() {
		return createdGmtDate;
	}

	public void setCreatedGmtDate(Date createdGmtDate) {
		this.createdGmtDate = createdGmtDate;
	}
	
	public Date getFinalizedGmtDate() {
		return finalizedGmtDate;
	}

	public void setFinalizedGmtDate(Date finalizedGmtDate) {
		this.finalizedGmtDate = finalizedGmtDate;
	}

	public String getFollowupMessage() {
		return followupMessage;
	}

	public void setFollowupMessage(String followupMessage) {
		this.followupMessage = followupMessage;
	}

	public int getNumOfRecipients() {
		return numOfRecipients;
	}

	public void setNumOfRecipients(int numOfRecipients) {
		this.numOfRecipients = numOfRecipients;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
	public Boolean getIsAlert() {
		return isAlert;
	}

	public void setIsAlert(Boolean isAlert) {
		this.isAlert = isAlert;
	}
	
	public String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	public List<String> getMemberIdsThatReplied() {
		return memberIdsThatReplied;
	}

	public void setMemberIdsThatReplied(List<String> memberIdsThatReplied) {
		this.memberIdsThatReplied = memberIdsThatReplied;
	}
	
	public void handleMemberReply(String theMemberId) {
		if(this.memberIdsThatReplied == null) {
			this.memberIdsThatReplied = new ArrayList<String>();
		}
		
		if(!this.memberIdsThatReplied.contains(theMemberId)) {
			this.memberIdsThatReplied.add(theMemberId);
		}
	}
	
	public void addMemberIdThatReplied(String theMemberId) {
		if(this.memberIdsThatReplied == null) {
			this.memberIdsThatReplied = new ArrayList<String>();
		}
		// only add the member ID if it is not already held in the list
		if(!this.memberIdsThatReplied.contains(theMemberId)) {
			this.memberIdsThatReplied.add(theMemberId);
		}
	}

	public Boolean getIsPublic() {
		return isPublic;
	}

	public void setIsPublic(Boolean isPublic) {
		this.isPublic = isPublic;
	}
	
	public Boolean getIncludeEntireTeam() {
		return includeEntireTeam;
	}

	public void setIncludeEntireTeam(Boolean includeEntireTeam) {
		this.includeEntireTeam = includeEntireTeam;
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
	
	public Map<String, Integer> getPollResults() {
		Map<String, Integer> pollResults = new HashMap<String, Integer>();
		for(Recipient r : this.recipients) {
			String reply = r.getReply();
			if(reply != null) {
				// assumes the reply is already equal to one of the poll choices
				Integer choiceCount = pollResults.get(reply);
				if(choiceCount == null) {
					// count doesn't exist yet for this poll choice so create it and set it to one
					Integer newCount = new Integer(1);
					pollResults.put(reply, newCount);
				} else {
					choiceCount++;
				}
			}
		}
		return pollResults;
	}
	

	
	////////////////////
	// STATIC METHODS //
	////////////////////
    
	// theDateAdjustment: can be negative or positive. Never results in an ActiveThruGmtDate before before 24 hours from now.
	public static void upateUserActiveThruGmtDate(User theUser, Integer theDateAdjustment) {
    	EntityManager emMessageThread = EMF.get().createEntityManager();
    	
    	try {
    		List<MessageThread> messageThreads = (List<MessageThread>)emMessageThread.createNamedQuery("MessageThread.getBySenderUserIdAndStatus")
				.setParameter("senderUserId", KeyFactory.keyToString(theUser.getKey()))
				.setParameter("status", MessageThread.ACTIVE_STATUS)
				.getResultList();
    		
    		for(MessageThread mt : messageThreads) {
    			emMessageThread.getTransaction().begin();
        		Date activeThruGmtDate = mt.getActiveThruGmtDate();
        		if(activeThruGmtDate == null) {activeThruGmtDate = new Date();}
        		
        		// the following line could subtract days in adjustment is negative
        		activeThruGmtDate = GMT.addDaysToDate(activeThruGmtDate, theDateAdjustment);
        		if(theDateAdjustment < 0 ) {activeThruGmtDate = GMT.setToFutureDate(activeThruGmtDate);}
        		mt.setActiveThruGmtDate(activeThruGmtDate);
        		emMessageThread.getTransaction().commit();
    		}
    		log.info("ActiveThruGmtDate adjusted: messageThread count = " + messageThreads.size());
    		
    	} catch (Exception e) {
    		log.severe("Query MessageThread.getBySenderUserIdAndStatus failed");
    	} finally {
    		emMessageThread.close();
    	}
    	return;
    }
	
	public static Boolean isConfirmOrPoll(String theType) {
		if(theType == null) {return false;}
		
		if(theType.equalsIgnoreCase(CONFIRMED_TYPE) || theType.equalsIgnoreCase(POLL_TYPE) || theType.equalsIgnoreCase(WHO_IS_COMING_TYPE)) {
			return true;
		}
		return false;
	}
	
	public Boolean isPoll() {
		if(this.type == null) {return false;}
		
		if(this.type.equalsIgnoreCase(POLL_TYPE) || this.type.equalsIgnoreCase(WHO_IS_COMING_TYPE)) {
			return true;
		}
		return false;
	}
	
	public static Boolean isPoll(String theType) {
		if(theType == null) {return false;}
		
		if(theType.equalsIgnoreCase(POLL_TYPE) || theType.equalsIgnoreCase(WHO_IS_COMING_TYPE)) {
			return true;
		}
		return false;
	}
	
	public Boolean isConfirm() {
		if(this.type == null) {return false;}
		
		if(this.type.equalsIgnoreCase(CONFIRMED_TYPE)) {
			return true;
		}
		return false;
	}
	
	
	public static Boolean isConfirm(String theType) {
		if(theType == null) {return false;}
		
		if(theType.equalsIgnoreCase(CONFIRMED_TYPE)) {
			return true;
		}
		return false;
	}
}
