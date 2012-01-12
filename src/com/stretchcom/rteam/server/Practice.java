package com.stretchcom.rteam.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
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
import javax.persistence.Transient;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

@Entity
@NamedQueries({
    @NamedQuery(
    		name="Practice.getByKeys",
    		query="SELECT p FROM Practice p WHERE p.key = :keys"
    ),
    @NamedQuery(
    		name="Practice.getByKey",
    		query="SELECT p FROM Practice p WHERE p.key = :key"
    ),
    @NamedQuery(
    		name="Practice.getByTeam",
    		query="SELECT p FROM Practice p WHERE p.teams = :teamKey"
    ),
    @NamedQuery(
    		name="Practice.getByTeamAndEventType",
    		query="SELECT p FROM Practice p WHERE p.teams = :teamKey" + " AND " +
    		      "p.eventType = :eventType"
    ),
    @NamedQuery(
    		name="Practice.getByTeamAndStartDateRange",
    		query="SELECT p FROM Practice p WHERE p.teams = :teamKey" + " AND " +
    				"p.eventGmtStartDate >= :startDate" + " AND " +
    				"p.eventGmtStartDate <= :endDate"
    ),
})
public class Practice implements Cloneable {
	private static final Logger log = Logger.getLogger(Practice.class.getName());
	
	//constants
	public static final String PRACTICE = "practice";
	public static final String GENERIC_EVENT_TYPE = "generic";
	public static final String GAME_EVENT_TYPE = "game";
	public static final String PRACTICE_EVENT_TYPE = "practice";
	public static final String ALL_EVENT_TYPE = "all";
	
	// ::IMPORTANT:: when adding properties, don't forget to update clone() method
	private Date eventGmtStartDate;
	private Date eventGmtEndDate;
	private String timeZone;
	private String description;
	private String opponent;
	private Key practiceLocation; // key to the Location entity holding the game location. Use 'key' and not 'locationId' because never sent to client
	private Double latitude; // local copy of team location data maintained in Location entity.
	private Double longitude; // local copy of team location data maintained in Location entity.
	private String locationName; // local copy of team location data maintained in Location entity.
	private String eventType;  // either 'practice' or 'generic'
	private String eventName;
	private Boolean attendanceTaken;
	private Boolean isCanceled;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Key key;
    
    @Basic
    private Set<Key> teams = new HashSet<Key>();

	public Practice() {
    	
    }
	
	// Key property is NOT cloned, so clone cannot be used to access entity
	public Object clone() {
        Practice obj = new Practice();
        if(this.eventGmtStartDate != null) obj.eventGmtStartDate = (Date)this.eventGmtStartDate.clone();
        if(this.eventGmtEndDate != null) obj.eventGmtEndDate = (Date)this.eventGmtEndDate.clone();
        obj.timeZone = this.timeZone;
        obj.description = this.description;
        obj.opponent = this.opponent;
        obj.practiceLocation = this.practiceLocation;
        obj.latitude = this.latitude;
        obj.longitude = this.longitude;
        obj.locationName = this.locationName;
        obj.eventType = this.eventType;
        obj.eventName = this.eventName;
		return obj;
    }  
	
	// "touches" all properties to make sure they have been accessed (like within a transaction)
	public void touch() {
        Date d = (Date)this.eventGmtStartDate;
        d = (Date)this.eventGmtEndDate;
        String s = this.timeZone;
        s = this.description;
        s = this.opponent;
        Key z = this.practiceLocation;
        Double db = this.latitude;
        db = this.longitude;
        s = this.locationName;
        s = this.eventType;
        s = this.eventName;
    }  
	
	@Transient
	private Date previousEventGmtStartDate;

	// used in client-side tests
    public Practice(Date eventGmtStartDate, Date eventGmtEndDate) {
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
	
	public void setStartDate(String theDate, String theTimeZone) {
		
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

	public String getLocationName() {
		return locationName;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	public Key getPracticeLocation() {
		return practiceLocation;
	}

	public void setPracticeLocation(Key practiceLocation) {
		this.practiceLocation = practiceLocation;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}
	
	public Date getPreviousEventGmtStartDate() {
		return previousEventGmtStartDate;
	}

	public void setPreviousEventGmtStartDate(Date previousEventGmtStartDate) {
		this.previousEventGmtStartDate = previousEventGmtStartDate;
	}
	
	public static Boolean isEventTypeValid(String theEventType) {
		if(theEventType.equalsIgnoreCase(Practice.GAME_EVENT_TYPE) || 
		   theEventType.equalsIgnoreCase(Practice.PRACTICE_EVENT_TYPE) || 
		   theEventType.equalsIgnoreCase(Practice.GENERIC_EVENT_TYPE)) {
			return true;
		}
		return false;
	}

	public Boolean getAttendanceTaken() {
		return attendanceTaken;
	}

	public void setAttendanceTaken(Boolean attendanceTaken) {
		this.attendanceTaken = attendanceTaken;
	}
    
    public static Date getEventDate(String theEventIdStr, String theEventTypeStr) {
    	EntityManager em = EMF.get().createEntityManager();
    	
    	try {
    		Key eventKey = KeyFactory.stringToKey(theEventIdStr);
    		if(theEventTypeStr.equalsIgnoreCase(Game.GAME)) {
        		Game game = (Game)em.createNamedQuery("Game.getByKey")
					.setParameter("key", eventKey)
					.getSingleResult();
        		return game.getEventGmtStartDate();
    		} else {
        		Practice practice = (Practice)em.createNamedQuery("Practice.getByKey")
					.setParameter("key", eventKey)
					.getSingleResult();
        		return practice.getEventGmtStartDate();
    		}
    	} catch (NoResultException e) {
        	// not a error - eventID passed in via API is bad. Null will be returned below.
		} catch (NonUniqueResultException e) {
			log.severe("should never happen - two or more games/practices have same key");
		} finally {
		    em.close();
		}
		
		return null;
    }
    
    // return event (game or practice) info including:
    // info[0]: GMT start date of type Date
    // info[1]: Local start date converted using event's time zone of type String
    // info[2]: event name (ony applies to practice, for game always an empty string
    public static List getEventInfo(String theEventIdStr, String theEventTypeStr) {
    	EntityManager em = EMF.get().createEntityManager();
    	List info = new ArrayList();
    	try {
    		Key eventKey = KeyFactory.stringToKey(theEventIdStr);
    		if(theEventTypeStr.equalsIgnoreCase(Game.GAME)) {
        		Game game = (Game)em.createNamedQuery("Game.getByKey")
					.setParameter("key", eventKey)
					.getSingleResult();
        		
        		info.add(game.getEventGmtStartDate());
        		info.add(game.getEventLocalStartDate());
        		info.add("");
    		} else {
        		Practice practice = (Practice)em.createNamedQuery("Practice.getByKey")
					.setParameter("key", eventKey)
					.getSingleResult();
        		
        		info.add(practice.getEventGmtStartDate());
        		info.add(practice.getEventLocalStartDate());
        		info.add(practice.getEventName());
    		}
    	} catch (NoResultException e) {
        	// not a error - eventID passed in via API is bad. Null will be returned below.
		} catch (NonUniqueResultException e) {
			log.severe("should never happen - two or more games/practices have same key");
		} finally {
		    em.close();
		}
		
		return info;
    }
	
	public static void updateAllLocations(Team theTeam, Practice thePractice) {
		EntityManager em = EMF.get().createEntityManager();
		try {
			List<Practice> practices = (List<Practice>)em.createNamedQuery("Practice.getByTeam")
				.setParameter("teamKey", theTeam.getKey())
				.getResultList();
			
			for(Practice p : practices) {
				// no need to update if it was the practice that was passed in (which was passed in because it was already updated)
				if(thePractice.getKey().equals(p.getKey())) {continue;}
				
    	    	em.getTransaction().begin();
    	    	p.setLatitude(thePractice.getLatitude());
    	    	p.setLongitude(thePractice.getLongitude());
    	    	p.setOpponent(thePractice.getOpponent());
    			em.getTransaction().commit();
			}
			int numOfPracticesUpdated = practices.size() - 1;
			log.info("number of practices with location fields update = " + numOfPracticesUpdated);
		} catch (Exception e) {
			log.severe("updateAllLocations(): practice could not be retrieved using team key");
		} finally {
			em.close();
		}
	}

	public Boolean getIsCanceled() {
		return isCanceled;
	}

	public void setIsCanceled(Boolean isCanceled) {
		this.isCanceled = isCanceled;
	}
	
}
