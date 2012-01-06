package com.stretchcom.rteam.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.Basic;
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

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;

@Entity
@NamedQueries({
    @NamedQuery(
    		name="Attendee.getByKey",
    		query="SELECT a FROM Attendee a WHERE a.key = :key"
    ),
    @NamedQuery(
    		name="Attendee.getByEventIdAndEventTypeAndMember",
    		query="SELECT a FROM Attendee a WHERE " +
    				"a.eventId = :eventId" + " AND " +
    				"a.eventType = :eventType" + " AND " +
    				"a.memberId = :memberId"
    ),
    @NamedQuery(
    		name="Attendee.getByEventIdAndEventType",
    		query="SELECT a FROM Attendee a WHERE " +
    				"a.eventId = :eventId" + " AND " +
    				"a.eventType = :eventType"
    ),
    @NamedQuery(
    		name="Attendee.getByMemberIdAndStartAndEndDates",
    		query="SELECT a FROM Attendee a WHERE " +
    				"a.memberId = :memberId" + " AND " +
    				"a.eventGmtDate >= :startDate" + " AND " +
    				"a.eventGmtDate <= :endDate"
    ),
    @NamedQuery(
    		name="Attendee.getByMemberIdAndStartAndEndDatesAndEventType",
    		query="SELECT a FROM Attendee a WHERE " +
    				"a.memberId = :memberId" + " AND " +
    				"a.eventGmtDate >= :startDate" + " AND " +
    				"a.eventGmtDate <= :endDate" + " AND " +
    				"a.eventType = :eventType"
    ),
    @NamedQuery(
    		name="Attendee.getByMemberIdAndStartDate",
    		query="SELECT a FROM Attendee a WHERE " +
    				"a.memberId = :memberId" + " AND " +
    				"a.eventGmtDate >= :startDate"
    ),
    @NamedQuery(
    		name="Attendee.getByMemberIdAndStartDateAndEventType",
    		query="SELECT a FROM Attendee a WHERE " +
    				"a.memberId = :memberId" + " AND " +
    				"a.eventGmtDate >= :startDate" + " AND " +
    				"a.eventType = :eventType"
    ),
    @NamedQuery(
    		name="Attendee.getByMemberIdAndEndDate",
    		query="SELECT a FROM Attendee a WHERE " +
    				"a.memberId = :memberId" + " AND " +
    				"a.eventGmtDate <= :endDate"
    ),
    @NamedQuery(
    		name="Attendee.getByMemberIdAndEndDateAndEventType",
    		query="SELECT a FROM Attendee a WHERE " +
    				"a.memberId = :memberId" + " AND " +
    				"a.eventGmtDate <= :endDate" + " AND " +
    				"a.eventType = :eventType"
    ),
    @NamedQuery(
    		name="Attendee.getByMemberId",
    		query="SELECT a FROM Attendee a WHERE " +
    				"a.memberId = :memberId"
    ),
    @NamedQuery(
    		name="Attendee.getByMemberIdAndEventType",
    		query="SELECT a FROM Attendee a WHERE " +
    				"a.memberId = :memberId" + " AND " +
    				"a.eventType = :eventType"
    ),
})
/*
 * Getters never return null, but rather, empty Strings, empty Lists, etc.
 */
public class Attendee {
	private static final Logger log = Logger.getLogger(Attendee.class.getName());
	
	// isPresent constants
	public static final String PRESENT = "yes";
	public static final String NOT_PRESENT = "no";
	
	// preGameStatus constants	
	public static final String YES_STATUS = "yes";
	public static final String NO_STATUS = "no";
	public static final String MAYBE_STATUS = "maybe";
	public static final String NO_REPLY_STATUS = "noreply";

	private Boolean isPresent;
	private String eventId;  // soft schema: either a game key or a practice key
	private String eventType;
	private Date eventGmtDate;
	private String eventName; // only applies to generic events right now
	private String teamId;    // soft schema: use id because team object is never used so @ManyToOne is overkill
    private String memberId;  // soft schema: use id because member object is never used so @ManyToOne is overkill
    private String preGameStatus = NO_REPLY_STATUS;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Key key;

	public String getTeamId() {
		return teamId;
	}

	public void setTeamId(String teamId) {
		this.teamId = teamId;
	}

    // used in client-side tests
    public Attendee() {
    }

    public Key getKey() {
        return key;
    }

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

    public String getMemberId() {
		return memberId;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}

	public String getPreGameStatus() {
		// guarantee not null and then no data migration required when this field was added
		return preGameStatus == null ? Attendee.NO_REPLY_STATUS : preGameStatus;
	}

	public void setPreGameStatus(String preGameStatus) {
		this.preGameStatus = preGameStatus;
	}
	
	public Date getEventGmtDate() {
		return eventGmtDate;
	}

	public void setEventGmtDate(Date eventGmtDate) {
		this.eventGmtDate = eventGmtDate;
	}

	public Boolean getIsPresent() {
		return isPresent;
	}

	public void setIsPresent(Boolean isPresent) {
		this.isPresent = isPresent;
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

	public static void updatePreGameAttendance(String theEventIdStr, String theEventType, String theMemberId, String theTeamId,
			                                   Date theEventDate, String theEventName, String theReply) {
    	EntityManager em = EMF.get().createEntityManager();
		em.getTransaction().begin();
		Attendee attendee = null;
		try {
			try {
				attendee = (Attendee)em.createNamedQuery("Attendee.getByEventIdAndEventTypeAndMember")
					.setParameter("eventId", theEventIdStr)
					.setParameter("eventType", theEventType)
					.setParameter("memberId", theMemberId)
					.getSingleResult();
			} catch(NoResultException e) {
				attendee = new Attendee();
				attendee.setEventId(theEventIdStr);
				attendee.setEventType(theEventType);
				attendee.setMemberId(theMemberId);
				attendee.setTeamId(theTeamId);
				attendee.setEventGmtDate(theEventDate);
				attendee.setEventName(theEventName);
			}
			
			attendee.setPreGameStatus(theReply);
			
			em.persist(attendee);
		    em.getTransaction().commit();
		} catch (Exception e) {
			log.severe("updatePreGameAttendance(): exception = " + e.getMessage());
		} finally {
			em.close();
		}
	}
}
