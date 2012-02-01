package com.stretchcom.rteam.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.restlet.data.Status;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

@Entity
@NamedQueries({
    @NamedQuery(
    		name="Activity.getByUpperAndLowerCreatedDates",
    		query="SELECT a FROM Activity a" + " WHERE " +
    				"a.createdGmtDate <= :mostCurrentDate" + " AND " +
    				 "a.createdGmtDate >= :leastCurrentDate"
    ),
    @NamedQuery(
    		name="Activity.getByKey",
    		query="SELECT a FROM Activity a WHERE a.key = :key"
    ),
    @NamedQuery(
    		name="Activity.getByParentActivityId",
    		query="SELECT a FROM Activity a WHERE a.parentActivityId = :parentActivityId"
    ),
    @NamedQuery(
    		name="Activity.getByTeamId",
    		query="SELECT a FROM Activity a WHERE a.teamId = :teamId"
    ),
    @NamedQuery(
    		name="Activity.getByTwitterId",
    		query="SELECT a FROM Activity a WHERE a.twitterId = :twitterId"
    ),
    @NamedQuery(
    		name="Activity.getByTeamIdAndUpperAndLowerCacheIds",
    		query="SELECT a FROM Activity a WHERE a.teamId = :teamId" + " AND " +
    				"a.cacheId < :upperCacheId" + " AND " +
    				"a.cacheId >= :lowerCacheId"  + " AND " +
    				"a.isReply = FALSE ORDER BY a.cacheId DESC"
    ),
    @NamedQuery(
    		name="Activity.getByTeamIdAndUpperAndLowerCreatedDates",
    		query="SELECT a FROM Activity a WHERE a.teamId = :teamId" + " AND " + 
    				"a.createdGmtDate <= :mostCurrentDate"  + " AND " +
    				"a.createdGmtDate >= :leastCurrentDate" + " AND " +
    				"a.isReply = FALSE ORDER BY a.createdGmtDate DESC"
    ),
    @NamedQuery(
    		name="Activity.getByEventIdAndEventTypeWithPhoto",
    		query="SELECT a FROM Activity a WHERE a.eventId = :eventId AND a.eventType = :eventType AND photoBase64 <> NULL"
    ),
})
public class Activity implements Comparable<Activity> {
	//private static final Logger log = Logger.getLogger(Activity.class.getName());
	private static RskyboxClient log = new RskyboxClient();
	
	public static final Integer THUMB_NAIL_SHORT_SIDE  = 60;
	public static final Integer THUMB_NAIL_LONG_SIDE  = 80;
	
	private Long twitterId;
	private Long cacheId;    // rTeam cache ID that is always SEQUENTIAL
	private String text;
    private String teamId;          
    private String teamName;
	private Date createdGmtDate;
	private Date updatedGmtDate;
	private Long numberOfLikeVotes;
	private Long numberOfDislikeVotes;
	private Text photoBase64;
	private Text thumbNailBase64;
	private Text videoBase64;
	private String eventId;
	private String eventType;
	private String eventDetailsId; // not sure I want to keep this -- maybe too much overhead to set this
	private String userId; // user ID of the poster (if it was a user and not an auto post)
	private String parentActivityId;  // if present, activity is a 'reply'.
	private Boolean isReply = false;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Key key;
	
	@Basic
	private List<String> contributors;

	public Key getKey() {
        return key;
    }
	
    public List<String> getContributors() {
		return contributors;
	}

	public void setContributors(List<String> contributors) {
		this.contributors = contributors;
	}
	
    // returns the first contributor in the contributor list if any; null otherwise
	public String getContributor() {
		if(this.contributors == null || this.contributors.size() == 0) {
			return null;
		}
		return this.contributors.get(0);
	}

	// add specified contributor to the list
	public void setContributor(String theContributor) {
		if(this.contributors == null) {
			// create the list since it doesn't yet exist
			this.contributors = new ArrayList<String>();
		}
		this.contributors.add(theContributor);
	}

	public Long getTwitterId() {
		return twitterId;
	}

	public void setTwitterId(Long twitterId) {
		this.twitterId = twitterId;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

    public String getTeamId() {
		return teamId;
	}

	public void setTeamId(String teamId) {
		this.teamId = teamId;
	}

	public Date getCreatedGmtDate() {
		return createdGmtDate;
	}

	public void setCreatedGmtDate(Date createdGmtDate) {
		this.createdGmtDate = createdGmtDate;
	}
	
	public String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}
	
	public Long getCacheId() {
		return cacheId;
	}

	public void setCacheId(Long cacheId) {
		this.cacheId = cacheId;
	}

	
	// TODO is this too much of a hack?
	// Sorting on this class is done two different ways:
	// 1. if the cacheId has not yet be set, then sorting is done on the twitterId
	// 2. if cacheId has been set, the sorting is done via createdGmtDate in reverse chronological order
	public int compareTo(Activity aThat) {
		final int BEFORE = -1;
		final int EQUAL = 0;
		final int AFTER = 1;

		if(this == aThat)
			return EQUAL;
		if (cacheId == null) {
			return this.twitterId.compareTo(aThat.twitterId);
		} else {
			// logic is reverse since sorting is done in reverse chronological order
			if (this.createdGmtDate.before(aThat.createdGmtDate)) {
				return AFTER;
			} else if (this.createdGmtDate.after(aThat.createdGmtDate)) {
				return BEFORE;
			} else {
				return EQUAL;
			}
		}
	}
	
	public Date getUpdatedGmtDate() {
		return updatedGmtDate;
	}

	public void setUpdatedGmtDate(Date updatedGmtDate) {
		this.updatedGmtDate = updatedGmtDate;
	}

	public Long getNumberOfLikeVotes() {
		if(this.numberOfLikeVotes == null) {
			return 0L;
		}
		return this.numberOfLikeVotes;
	}

	public void setNumberOfLikeVotes(Long numberOfLikeVotes) {
		this.numberOfLikeVotes = numberOfLikeVotes;
	}
	
	public void incrementNumberOfLikeVotes() {
		if(this.numberOfLikeVotes == null) {
			this.numberOfLikeVotes = 0L;
		}
		this.numberOfLikeVotes++;
	}
	
	public void decrementNumberOfLikeVotes() {
		if(this.numberOfLikeVotes == null || this.numberOfLikeVotes.equals(0L)) {
			return;
		}
		this.numberOfLikeVotes--;
	}

	public Long getNumberOfDislikeVotes() {
		if(this.numberOfDislikeVotes == null) {
			return 0L;
		}
		return numberOfDislikeVotes;
	}

	public void setNumberOfDislikeVotes(Long numberOfDislikeVotes) {
		this.numberOfDislikeVotes = numberOfDislikeVotes;
	}
	
	public void incrementNumberOfDislikeVotes() {
		if(this.numberOfDislikeVotes == null) {
			this.numberOfDislikeVotes = 0L;
		}
		this.numberOfDislikeVotes++;
	}
	
	public void decrementNumberOfDislikeVotes() {
		if(this.numberOfDislikeVotes == null || this.numberOfDislikeVotes.equals(0L)) {
			return;
		}
		this.numberOfDislikeVotes--;
	}
	
	public String getThumbNailBase64() {
		return this.thumbNailBase64 == null? null : this.thumbNailBase64.getValue();
	}

	public void setThumbNailBase64(byte[] thumbNailBase64) {
		if(thumbNailBase64 == null) {
			this.thumbNailBase64 = null;
		} else {
			this.thumbNailBase64 = new Text(new String(thumbNailBase64));
		}
	}

	public void setThumbNailBase64(String thumbNailBase64) {
		if(thumbNailBase64 == null) {
			this.thumbNailBase64 = null;
		} else {
			this.thumbNailBase64 = new Text(thumbNailBase64);
		}
	}

	public String getPhotoBase64() {
		return this.photoBase64 == null? null : this.photoBase64.getValue();
	}

	public void setPhotoBase64(String photo) {
		if(photo == null) {
			this.photoBase64 = null;
		} else {
			this.photoBase64 = new Text(photo);
		}
	}

	public String getVideoBase64() {
		return this.videoBase64 == null? null : this.videoBase64.getValue();
	}

	public void setVideoBase64(String videoBase64) {
		if(videoBase64 == null) {
			this.videoBase64 = null;
		} else {
			this.videoBase64 = new Text(videoBase64);
		}
	}
	
	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public String getEventDetailsId() {
		return eventDetailsId;
	}

	public void setEventDetailsId(String eventDetailsId) {
		this.eventDetailsId = eventDetailsId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getParentActivityId() {
		return parentActivityId;
	}

	public void setParentActivityId(String parentActivityId) {
		this.parentActivityId = parentActivityId;
	}

	public Boolean getIsReply() {
		return isReply;
	}

	public void setIsReply(Boolean isReply) {
		this.isReply = isReply;
	}
	
	// returns activity ID of an activity associated with event that has a photo. If more than one, photo chosen randomly.
	// returns null if no activity associated with event or no activity that is associated with the event has a photo
	public static String getActivityIdOfEventPhoto(String theEventId, String theEventType) {
		String activityIdWithPhoto = null;
		
    	EntityManager em = EMF.get().createEntityManager();
    	try {
			List<Activity> activitiesWithPhotos = (List<Activity>)em.createNamedQuery("Activity.getByEventIdAndEventTypeWithPhoto")
				.setParameter("eventId", theEventId)
				.setParameter("eventType", theEventType)
				.getResultList();
			if(activitiesWithPhotos.size() > 0) {
				// TODO for now, just pick the first one returned. Later make it random or use some other algorithm
				activityIdWithPhoto = KeyFactory.keyToString(activitiesWithPhotos.get(0).getKey());
			} else {
				log.error("Activity:getActivityIdOfEventPhoto:removeMe", "REMOVE THE TEST CODE IN Activity.getActivityIdOfEventPhotof() THAT IS ALWAYS RETURNING AN ACTIVITY ID");
				return "aglydGVhbXRlc3RyEQsSCEFjdGl2aXR5GJfuvgEM";
			}
    	} catch (Exception e) {
			log.exception("Activity:getActivityIdOfEventPhoto:Exception", "", e);
		} finally {
		    em.close();
		}

    	return activityIdWithPhoto;
	}
	
	public static Activity getActivity(String theActivityId, EntityManager theEm) {
    	Activity activity = null;
    	try {
			activity = (Activity)theEm.createNamedQuery("Activity.getByKey")
				.setParameter("key", KeyFactory.stringToKey(theActivityId))
				.getSingleResult();
    	} catch (NoResultException e) {
			log.exception("Activity:getActivity:NoResultException", "activity not found", e);
		} catch (NonUniqueResultException e) {
			log.exception("Activity:getActivity:NonUniqueResultException", "two or more activities have same key", e);
		}
    	return activity;
	}
	
	public static List<Activity> getReplies(String theActivityId, EntityManager theEm) {
		List<Activity> replies = null;
    	try {
    		replies = (List<Activity>)theEm.createNamedQuery("Activity.getByParentActivityId")
				.setParameter("parentActivityId", theActivityId)
				.getResultList();
    	} catch (Exception e) {
			log.exception("Activity:getReplies:Exception", "", e);
		}
    	return replies;
	}
}
