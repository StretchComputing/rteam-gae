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

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

@Entity
@NamedQueries({
    @NamedQuery(
    		name="ActivityVote.getByKey",
    		query="SELECT av FROM ActivityVote av WHERE av.key = :key"
    ),
    @NamedQuery(
    		name="ActivityVote.getByActivityIdAndUserId",
    		query="SELECT av FROM ActivityVote av WHERE av.activityId = :activityId" + " AND " +
    		      "av.userId = :userId"
    ),
})
public class ActivityVote {
	//constants
	public static final String LIKE_STATUS = "like";
	public static final String DISLIKE_STATUS = "dislike";
	public static final String NONE_STATUS = "none";
	
    private String userId;  // must be a user to vote
	private String teamId;
	private String activityId;
	private Date createdGmtDate;
	private Date lastUpdatedGmtDate;
	private String status;
	private Boolean isMemberVote;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Key key;

	public Key getKey() {
        return key;
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
	
	public Date getLastUpdatedGmtDate() {
		return lastUpdatedGmtDate;
	}

	public void setLastUpdatedGmtDate(Date lastUpdatedGmtDate) {
		this.lastUpdatedGmtDate = lastUpdatedGmtDate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
	public static Boolean isStatusValid(String theStatusValue) {
		if(theStatusValue.equalsIgnoreCase(DISLIKE_STATUS) || theStatusValue.equalsIgnoreCase(LIKE_STATUS)) {
			return true;
		}
		return false;
	}
	
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getActivityId() {
		return activityId;
	}

	public void setActivityId(String activityId) {
		this.activityId = activityId;
	}

	public Boolean getIsMemberVote() {
		return isMemberVote;
	}

	public void setIsMemberVote(Boolean isMemberVote) {
		this.isMemberVote = isMemberVote;
	}


}
