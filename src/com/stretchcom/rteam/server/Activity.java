package com.stretchcom.rteam.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

@Entity
@NamedQueries({
    @NamedQuery(
    		name="Activity.getByKey",
    		query="SELECT a FROM Activity a WHERE a.key = :key"
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
    				"a.cacheId >= :lowerCacheId ORDER BY a.cacheId DESC"
    ),
    @NamedQuery(
    		name="Activity.getByTeamIdAndUpperAndLowerCreatedDates",
    		query="SELECT a FROM Activity a WHERE a.teamId = :teamId" + " AND " + 
    				"a.createdGmtDate <= :mostCurrentDate"  + " AND " +
    				"a.createdGmtDate >= :leastCurrentDate ORDER BY a.createdGmtDate DESC"
    ),
})
public class Activity implements Comparable<Activity> {
	
	private Long twitterId;
	private Long cacheId;    // rTeam cache ID that is always SEQUENTIAL
	private String text;
    private String teamId;          
    private String teamName;
	private Date createdGmtDate;
	private Long numberOfLikeVotes;
	private Long numberOfDislikeVotes;
	private Text photoBase64;
	private Text thumbNailBase64;
	private Text videoBase64;

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
		this.thumbNailBase64 = new Text(new String(thumbNailBase64));
	}

	public void setThumbNailBase64(String thumbNailBase64) {
		this.thumbNailBase64 = new Text(thumbNailBase64);
	}

	public String getPhotoBase64() {
		return this.photoBase64 == null? null : this.photoBase64.getValue();
	}

	public void setPhotoBase64(String photo) {
		this.photoBase64 = new Text(photo);
	}

	public String getVideoBase64() {
		return this.videoBase64 == null? null : this.videoBase64.getValue();
	}

	public void setVideoBase64(String videoBase64) {
		this.videoBase64 = new Text(videoBase64);
	}
	
}
