package com.stretchcom.rteam.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.ManyToOne;
import javax.persistence.FetchType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

@Entity
@NamedQueries({
    @NamedQuery(
    		name="Game.getByKeys",
    		query="SELECT g FROM Game g WHERE g.key = :keys"
    ),
    @NamedQuery(
    		name="Game.getByKey",
    		query="SELECT g FROM Game g WHERE g.key = :key"
    ),
    @NamedQuery(
    		name="Game.getByTeam",
    		query="SELECT g FROM Game g WHERE g.teams = :teamKey"
    ),
    @NamedQuery(
    		name="Game.getByTeamAndStartDateRange",
    		query="SELECT g FROM Game g WHERE g.teams = :teamKey" + " AND " +
    				"g.eventGmtStartDate >= :startDate" + " AND " +
    				"g.eventGmtStartDate <= :endDate"
    ),
})
public class Game implements Cloneable {
	private static final Logger log = Logger.getLogger(Game.class.getName());
	
	//constants
	public static final String GAME = "game";
	
	public static final String OPEN_POLL_STATUS = "open";
	public static final String CLOSED_POLL_STATUS = "closed";
	
	public static final Integer GAME_CANCELED_INTERVAL = -4;
	public static final Integer GAME_IN_PROGRESS_INTERVAL = -3;
	public static final Integer GAME_IN_OVERTIME_INTERVAL = -2;
	public static final Integer GAME_OVER_INTERVAL = -1;
	public static final Integer GAME_NOT_STARTED_YET_INTERVAL = 0;
	
	// ::IMPORTANT:: when adding properties, don't forget to update clone() method
	private Date eventGmtStartDate;
	private Date eventGmtEndDate;
	private String timeZone;
	private String description;
	private String opponent;
	private Key gameLocation; // key to the Location entity holding the game location. Used 'key' and not 'locationId' because never sent to client
	private Double latitude; // local copy of team location data maintained in Location entity.
	private Double longitude; // local copy of team location data maintained in Location entity.
	private String locationName; // local copy of team location data maintained in Location entity.
	private Integer scoreUs;
	private Integer scoreThem;
	private Integer interval; // game interval: inning, quarter, period, etc.
	private String eventType;
	private Boolean attendanceTaken;
	private String mvpMemberId;
	private String mvpDisplayName; //could be full name, email address, phone number -- depending on what was entered
	private String pollStatus = OPEN_POLL_STATUS; // open for voting as soon as game is created. Later may wait until game starts.

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Key key;
    
    @Basic
    private Set<Key> teams = new HashSet<Key>();

	public Game() {
    	
    }
	
	// Key property is NOT cloned, so clone cannot be used to access entity
	public Object clone() {
        Game obj = new Game();
        if(this.eventGmtStartDate != null) obj.eventGmtStartDate = (Date)this.eventGmtStartDate.clone();
        if(this.eventGmtEndDate != null) obj.eventGmtEndDate = (Date)this.eventGmtEndDate.clone();
        obj.timeZone = this.timeZone;
        obj.description = this.description;
        obj.opponent = this.opponent;
        obj.gameLocation = this.gameLocation;
        obj.latitude = this.latitude;
        obj.longitude = this.longitude;
        obj.locationName = this.locationName;
        obj.eventType = this.eventType;
        obj.scoreUs = this.scoreUs;
        obj.scoreThem = this.scoreThem;
        obj.interval = this.interval;
		return obj;
    }  
    
	@Transient
	private Date previousEventGmtStartDate;

	// used in client-side tests
    public Game(Date eventGmtStartDate, Date eventGmtEndDate) {
    	this.eventGmtStartDate = eventGmtStartDate;
    	this.eventGmtEndDate = eventGmtEndDate;
    }

    public Key getKey() {
        return key;
    }
    
    public Set<Key> getTeams() {
		return teams;
	}

	public void setTeams(Set<Key> teams) {
		this.teams = teams;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public Date getEventGmtStartDate() {
		return eventGmtStartDate;
	}

	public void setEventGmtStartDate(Date eventGmtStartDate) {
		this.eventGmtStartDate = eventGmtStartDate;
	}
	
	public void setStartDate(String theDate, String theTimeZone) {
		
	}
	
	// Convenience function.
	// Return start date in "local" timezone (i.e. timezone date was originally set with)
	public String getEventLocalStartDate() {
		return _getEventLocalStartDate(this.eventGmtStartDate);
	}
	
	public String getOriginalEventLocalStartDate() {
		Date startDate = this.previousEventGmtStartDate != null ? this.previousEventGmtStartDate : this.eventGmtStartDate;
		return _getEventLocalStartDate(startDate);
	}
	
	private String _getEventLocalStartDate(Date theStartDate) {
		String localDateStr = null;
		if(theStartDate != null && this.timeZone != null) {
			TimeZone tz = GMT.getTimeZone(this.timeZone);
			if(tz != null) {
				localDateStr = GMT.convertToSimpleLocalDate(theStartDate, tz);
			}
		}
		return localDateStr;
	}
	
	public Date getEventGmtEndDate() {
		return eventGmtEndDate;
	}

	public void setEventGmtEndDate(Date eventGmtEndDate) {
		this.eventGmtEndDate = eventGmtEndDate;
	}

	// Convenience function.
	// Return end date in "local" timezone (i.e. timezone date was originally set with)
	public String getEventLocalEndDate() {
		String localDateStr = null;
		if(this.eventGmtEndDate != null && this.timeZone != null) {
			TimeZone tz = GMT.getTimeZone(this.timeZone);
			if(tz != null) {
				localDateStr = GMT.convertToSimpleLocalDate(this.eventGmtEndDate, tz);
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

	public String getOpponent() {
		return opponent;
	}

	public void setOpponent(String opponent) {
		this.opponent = opponent;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}


	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
	
	public static Boolean isNotificationTypeValid(String theNotificationType) {
		if(theNotificationType.equalsIgnoreCase(MessageThread.NONE_TYPE)  || 
		   theNotificationType.equalsIgnoreCase(MessageThread.PLAIN_TYPE) ||
		   theNotificationType.equalsIgnoreCase(MessageThread.CONFIRMED_TYPE)    ) {
			return true;
		}
		return false;
	}
	
	public static Boolean isPollStatusValid(String thePollStatus) {
		if(thePollStatus.equalsIgnoreCase(OPEN_POLL_STATUS)  || 
				thePollStatus.equalsIgnoreCase(CLOSED_POLL_STATUS)    ) {
			return true;
		}
		return false;
	}

	public String getLocationName() {
		return locationName;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	public Key getGameLocation() {
		return gameLocation;
	}

	public void setGameLocation(Key gameLocation) {
		this.gameLocation = gameLocation;
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

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}
	
	public Date getPreviousEventGmtStartDate() {
		return previousEventGmtStartDate;
	}

	public void setPreviousEventGmtStartDate(Date previousEventGmtStartDate) {
		this.previousEventGmtStartDate = previousEventGmtStartDate;
	}
	
	public Boolean getAttendanceTaken() {
		return attendanceTaken;
	}

	public void setAttendanceTaken(Boolean attendanceTaken) {
		this.attendanceTaken = attendanceTaken;
	}

	public String getPollStatus() {
		return pollStatus;
	}

	public void setPollStatus(String pollStatus) {
		this.pollStatus = pollStatus;
	}

	public String getMvpDisplayName() {
		return mvpDisplayName;
	}

	public void setMvpDisplayName(String mvpDisplayName) {
		this.mvpDisplayName = mvpDisplayName;
	}

	public String getMvpMemberId() {
		return mvpMemberId;
	}

	public void setMvpMemberId(String mvpMemberId) {
		this.mvpMemberId = mvpMemberId;
	}
	
	public static void updateAllLocations(Team theTeam, Game theGame) {
		EntityManager em = EMF.get().createEntityManager();
		try {
			List<Game> games = (List<Game>)em.createNamedQuery("Game.getByTeam")
				.setParameter("teamKey", theTeam.getKey())
				.getResultList();
			
			for(Game g : games) {
				// no need to update if it was the game that was passed in (which was passed in because it was already updated)
				if(theGame.getKey().equals(g.getKey())) {continue;}
				
    	    	em.getTransaction().begin();
    	    	g.setLatitude(theGame.getLatitude());
    	    	g.setLongitude(theGame.getLongitude());
    	    	g.setLocationName(theGame.getLocationName());
    			em.getTransaction().commit();
			}
			int numOfGamesUpdated = games.size() - 1;
			log.info("number of games with location fields update = " + numOfGamesUpdated);
		} catch (Exception e) {
			log.severe("updateAllLocations(): game could not be retrieved using team key");
		} finally {
			em.close();
		}
	}
	
}
