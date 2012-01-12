package com.stretchcom.rteam.server;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import com.google.appengine.api.datastore.Key;

@Entity
@NamedQueries({
    @NamedQuery(
    		name="EventDetails.getByKeys",
    		query="SELECT ed FROM EventDetails ed WHERE ed.key = :keys"
    ),
    @NamedQuery(
    		name="EventDetails.getByKey",
    		query="SELECT ed FROM EventDetails ed WHERE ed.key = :key"
    ),
})
public class EventDetails {
	private static final Logger log = Logger.getLogger(EventDetails.class.getName());
	
	private Date createdGmtDate;
	private String timeZone;
	private String eventId;
	private String eventType;
	private Integer scoreUs;   // for game only
	private Integer scoreThem; // for game only
	private Integer interval;  // for game only

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Key key;

	@Basic
	private List<String> activityIds;

	public EventDetails() {
    	
    }

    public Key getKey() {
        return key;
    }
	
	public Date getCreatedGmtDate() {
		return createdGmtDate;
	}

	public void setCreatedGmtDate(Date createdGmtDate) {
		this.createdGmtDate = createdGmtDate;
	}
	
	public void setStartDate(String theDate, String theTimeZone) {
		
	}
	
	// Convenience function.
	// Return created date in "local" timezone (i.e. timezone date was originally set with)
	public String getEventLocalCreatedDate() {
		return _getEventLocalCreatedDate(this.createdGmtDate);
	}
	
	private String _getEventLocalCreatedDate(Date theCreatedDate) {
		String localDateStr = null;
		if(theCreatedDate != null && this.timeZone != null) {
			TimeZone tz = GMT.getTimeZone(this.timeZone);
			if(tz != null) {
				localDateStr = GMT.convertToSimpleLocalDate(theCreatedDate, tz);
			}
		}
		return localDateStr;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public Integer getInterval() {
		return interval;
	}

	public void setInterval(Integer interval) {
		this.interval = interval;
	}

	public Integer getScoreThem() {
		return scoreThem;
	}

	public void setScoreThem(Integer scoreThem) {
		this.scoreThem = scoreThem;
	}

	public Integer getScoreUs() {
		return scoreUs;
	}

	public void setScoreUs(Integer scoreUs) {
		this.scoreUs = scoreUs;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public List<String> getActivityIds() {
		return activityIds;
	}

	public void setActivityIds(List<String> activityIds) {
		this.activityIds = activityIds;
	}
}
