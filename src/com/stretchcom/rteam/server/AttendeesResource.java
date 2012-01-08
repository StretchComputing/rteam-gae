package com.stretchcom.rteam.server;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
  
/** 
 * Resource that manages a list of games. 
 *  
 */  
public class AttendeesResource extends ServerResource {  
	private static final Logger log = Logger.getLogger(AttendeesResource.class.getName());
  
    // The sequence of characters that identifies the resource.
    String teamId;
    String eventId;
    String eventType;
    String memberId;
    String startDateStr;
    String endDateStr;
    String timeZoneStr;
    
    @Override  
    protected void doInit() throws ResourceException {  
		
		Form form = getRequest().getResourceRef().getQueryAsForm();
		for (Parameter parameter : form) {
			log.info("parameter " + parameter.getName() + " = " + parameter.getValue());
			if(parameter.getName().equals("teamId"))  {
				this.teamId = (String)parameter.getValue();
				this.teamId = Reference.decode(this.teamId);
				log.info("AttendeesResource:doInit() - decoded teamId = " + this.teamId);
			} else if(parameter.getName().equals("eventId")) {
				this.eventId = (String)parameter.getValue();
				this.eventId = Reference.decode(this.eventId);
				log.info("AttendeesResource:doInit() - decoded eventId = " + this.eventId);
			} else if(parameter.getName().equals("eventType")) {
				this.eventType = (String)parameter.getValue();
				this.eventType = Reference.decode(this.eventType);
				log.info("AttendeesResource:doInit() - decoded eventType = " + this.eventType);
			} else if(parameter.getName().equals("memberId")) {
				this.memberId = (String)parameter.getValue();
				this.memberId = Reference.decode(this.memberId);
				log.info("AttendeesResource:doInit() - decoded memberId = " + this.memberId);
			} else if(parameter.getName().equals("startDate")) {
				this.startDateStr = (String)parameter.getValue();
				this.startDateStr = Reference.decode(this.startDateStr);
				log.info("AttendeesResource:doInit() - decoded startDate = " + this.startDateStr);
			} else if(parameter.getName().equals("endDate")) {
				this.endDateStr = (String)parameter.getValue();
				this.endDateStr = Reference.decode(this.endDateStr);
				log.info("AttendeesResource:doInit() - decoded endDate = " + this.endDateStr);
			} else if(parameter.getName().equals("timeZone")) {
				this.timeZoneStr = (String)parameter.getValue();
				this.timeZoneStr = Reference.decode(this.timeZoneStr);
				log.info("AttendeesResource:doInit() - decoded timeZone = " + this.timeZoneStr);
			}
		}
}  
    
    // Handles 'Update attendees' API  
    @Put
    public JsonRepresentation updateAttendees(Representation entity) {
    	JSONObject jsonReturn = new JSONObject();
    	log.info("updateAttendees(@Put) entered ..... ");
		EntityManager em = EMF.get().createEntityManager();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		Attendee attendee = null;
		User currentUser = null;
        try {
    		currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
    			log.severe("user could not be retrieved from Request attributes!!");
    			return Utility.apiError(null);
    		} 
    		
			JsonRepresentation jsonRep = new JsonRepresentation(entity);
			JSONObject json = jsonRep.toJsonObject();
			
			String teamIdStr = null;
			if(json.has("teamId")) {
				teamIdStr = json.getString("teamId");
			}
			
			String eventIdStr = null;
			if(json.has("eventId")) {
				eventIdStr = json.getString("eventId");
			}
			
			String eventType = null;
			if(json.has("eventType")) {
				eventType = json.getString("eventType").toLowerCase();
			}
			
			// if new field is empty, field is cleared.
			JSONArray attendeesJsonArray = null;
			List<String> memberIds = new ArrayList<String>();
			List<String> statuses = new ArrayList<String>();
			List<String> preGameStatuses = new ArrayList<String>();
			int attendeeArraySize = 0;
			if(json.has("attendees")) {
				attendeesJsonArray = json.getJSONArray("attendees");
				attendeeArraySize = attendeesJsonArray.length();
				log.info("attendee json array length = " + attendeeArraySize);
				for(int i=0; i<attendeeArraySize; i++) {
					JSONObject attendeeJsonObj = attendeesJsonArray.getJSONObject(i);
					memberIds.add(attendeeJsonObj.getString("memberId"));
					if(attendeeJsonObj.has("present")) {
						statuses.add(attendeeJsonObj.getString("present"));
						log.info("attendee status sent = " + attendeeJsonObj.getString("present"));
					}
					if(attendeeJsonObj.has("preGameStatus")) {
						preGameStatuses.add(attendeeJsonObj.getString("preGameStatus"));
						log.info("attendee pre-game status sent = " + attendeeJsonObj.getString("preGameStatus"));
					}
				}
			}

			// all JSON fields are required
			Team team = null;
			if(teamIdStr == null || eventIdStr ==  null || eventType == null || attendeesJsonArray == null) {
				return Utility.apiError(ApiStatusCode.ALL_PARAMETERS_REQUIRED);
			}
			else if(!Practice.isEventTypeValid(eventType)) {
				return Utility.apiError(ApiStatusCode.INVALID_EVENT_TYPE_PARAMETER);
			}
			else if(!currentUser.isUserMemberOfTeam(teamIdStr)) {
				return Utility.apiError(ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM);
        	}
			
    		team = (Team)em.createNamedQuery("Team.getByKey")
				.setParameter("key", KeyFactory.stringToKey(teamIdStr))
				.getSingleResult();
			log.info("team retrieved = " + team.getTeamName());
			
   			Boolean isNetworkedAuthenticatedCoordinator = false;
   			List<Member> memberships = null;
			if(currentUser.getIsNetworkAuthenticated()) {
				log.info("user is network authenticated");
				memberships = Member.getMemberShipsWithEmailAddress(currentUser.getEmailAddress(), team);
				for(Member m : memberships) {
    	    		if(m.isCoordinator()) {
    	    			isNetworkedAuthenticatedCoordinator = true;
    	    			break;
    	    		}
				}
			}
			
			// a non-coordinator can update their own attendance, but nobody else's
			Boolean onlyMemberIdBelongsToCurrentUser = false;
			if(memberships != null && memberIds.size() == 1) {
				log.info("only one member ID specified -- Who's coming response");
				for(Member m : memberships) {
					if(memberIds.get(0).equals(KeyFactory.keyToString(m.getKey()))) {
						onlyMemberIdBelongsToCurrentUser = true;
						log.info("onlyMemberIdBelongsToCurrentUser is TRUE");
						break;
					}
				}
			}
			
			//::BUSINESSRULE:: for FULL update access, user must be the team creator or network authenticated coordinator to update attendees
			Boolean isCreator = team.isCreator(currentUser.getEmailAddress());
			if(!isCreator && !isNetworkedAuthenticatedCoordinator && !onlyMemberIdBelongsToCurrentUser) {
				return Utility.apiError(ApiStatusCode.USER_NOT_CREATOR_NOR_NETWORK_AUTHENTICATED_COORDINATOR);
			} 

			Date eventDate = null;
			String eventName = null;
			Boolean attendanceTaken = null;
			Practice practice = null;
			Game game = null;
			if(eventType.equalsIgnoreCase(Practice.PRACTICE_EVENT_TYPE) || eventType.equalsIgnoreCase(Practice.GENERIC_EVENT_TYPE)) {
        		Key practiceKey = KeyFactory.stringToKey(eventIdStr);
        		practice = (Practice)em.createNamedQuery("Practice.getByKey")
        			.setParameter("key", practiceKey)
        			.getSingleResult();
        		eventDate = practice.getEventGmtStartDate();
        		eventName = practice.getEventName();
        		attendanceTaken = practice.getAttendanceTaken();
			}
			else {
        		Key gameKey = KeyFactory.stringToKey(eventIdStr);
        		game = (Game)em.createNamedQuery("Game.getByKey")
        			.setParameter("key", gameKey)
        			.getSingleResult();
        		eventDate = game.getEventGmtStartDate();
        		attendanceTaken = game.getAttendanceTaken();
			}
			
			List<Attendee> attendeesPresent = new ArrayList<Attendee>();
			for(int i=0; i<attendeeArraySize; i++) {
				boolean isPresent = false;
				String preGameStatus = Attendee.NO_REPLY_STATUS;
				String memberId =  memberIds.get(i);
				
				// process only if "present" specified for all attendees
				if(statuses.size() == attendeeArraySize && statuses.get(i).equalsIgnoreCase(Attendee.PRESENT)) {
					isPresent = true;
				}
				
				// process only if "preGameStatus" specified for all attendees
				if(preGameStatuses.size() == attendeeArraySize) {
					preGameStatus = preGameStatuses.get(i);
				}
				
				//::TODO a better way than separate transactions for each Attendee????
				em.getTransaction().begin();
				try {
					attendee = (Attendee)em.createNamedQuery("Attendee.getByEventIdAndEventTypeAndMember")
						.setParameter("eventId", eventIdStr)
						.setParameter("eventType", eventType)
						.setParameter("memberId", memberId)
						.getSingleResult();
				} catch(NoResultException e) {
					attendee = new Attendee();
					attendee.setEventId(eventIdStr);
					attendee.setEventType(eventType);
					attendee.setMemberId(memberId);
					attendee.setTeamId(teamIdStr);
					attendee.setEventGmtDate(eventDate);
					attendee.setEventName(eventName);
					em.persist(attendee);
				}
				
				// process only if "present" specified for all attendees
				if(statuses.size() == attendeeArraySize) {
					attendee.setIsPresent(isPresent);
					if(isPresent) {
						attendeesPresent.add(attendee);
					}
				}
				
				// process only if "preGameStatus" specified for all attendees
				if(preGameStatuses.size() == attendeeArraySize) {
					attendee.setPreGameStatus(preGameStatus);
				}
				
				em.getTransaction().commit();
			}
			
			// if this is the first time GAME-TIME attendance was take for this event, then post to team activity
			if(attendeesPresent.size() > 0 && (attendanceTaken == null || !attendanceTaken) ){
				// need to build a member list from the attendee list because the member display info is needed for the post
				EntityManager emMembers = EMF.get().createEntityManager();
				
				List<Key> memberKeys = new ArrayList<Key>();
				for(Attendee a : attendeesPresent) {
					memberKeys.add(KeyFactory.stringToKey(a.getMemberId()));
				}
				log.info("Number of members found that are in attendance = " + memberKeys.size());
				
				List<Member> membersPresent = new ArrayList<Member>();
	    		if(memberKeys.size() > 0) {
	    			try {
		    			membersPresent = (List<Member>)emMembers.createQuery("select from " + Member.class.getName() + " where key = :keys")
		    	    		.setParameter("keys", memberKeys)
		    	    		.getResultList();
		    			log.info("number of member entities found = " + membersPresent.size());
	    			} catch(Exception e) {
	    				log.severe("Exception retrieving members via keys. Message = " + e.getMessage());
	    			}
	    			finally {
	    				emMembers.close();
	    			}
	    		}
				
			    PubHub.postAttendanceActivity(team, membersPresent, currentUser, game, practice);
			    
			    // update the attendanceTaken flag in game/practice in a separate transaction
			    EntityManager emEvent = EMF.get().createEntityManager();
				emEvent.getTransaction().begin();
				try {
					if(game != null) {
						Game managedGame = (Game)emEvent.createNamedQuery("Game.getByKey")
							.setParameter("key", game.getKey())
							.getSingleResult();
						
						managedGame.setAttendanceTaken(true);
					} else if(practice != null) {
						Practice managedPractice = (Practice)emEvent.createNamedQuery("Practice.getByKey")
							.setParameter("key", practice.getKey())
							.getSingleResult();
						
						managedPractice.setAttendanceTaken(true);
					}

					emEvent.getTransaction().commit();
				} catch(NoResultException e) {
					log.severe("error accessing the game/practice");
				} catch (NonUniqueResultException e) {
					log.severe("should never happen - two games/practices have the same key");
					e.printStackTrace();
				} finally {
					emEvent.close();
				}
			}
			
			// if a single member attendance was updated from the who's coming, then check if one or more polls were sent
			// out to this member and update the status of the recipient
			if(onlyMemberIdBelongsToCurrentUser) {
				Recipient.updateWhoIsComingPollForMember(eventIdStr, memberIds.get(0), preGameStatuses.get(0));
			}
			
		} catch (IOException e) {
			log.severe("error extracting JSON object from Put");
			e.printStackTrace();
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (JSONException e) {
			log.severe("error converting json representation into a JSON object");
			e.printStackTrace();
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (NonUniqueResultException e) {
			log.severe("should never happen - two entities matched query criteria");
			e.printStackTrace();
		} finally {
		    if (em.getTransaction().isActive()) {
		        em.getTransaction().rollback();
		    }
		    em.close();
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.severe("error converting json representation into a JSON object");
			e.printStackTrace();
		}
		return new JsonRepresentation(jsonReturn);
    }
    
    // Handles 'Get attendees' API
    @Get("json")
    public JsonRepresentation getAttendees(Variant variant) {
        log.info("AttendeesResource:getAttendees() entered");
        JSONObject jsonReturn = new JSONObject();
		EntityManager em = EMF.get().createEntityManager();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		try {
			TimeZone tz = null;
			if(this.timeZoneStr != null) {
				tz = GMT.getTimeZone(this.timeZoneStr);
			}
			// enforce the parameter rules
			if(this.eventId != null && this.eventType == null) {
				apiStatus = ApiStatusCode.EVENT_ID_AND_EVENT_TYPE_MUST_BE_SPECIFIED_TOGETHER;
			} else if(this.eventId != null && this.memberId != null) {
				apiStatus = ApiStatusCode.EVENT_ID_AND_MEMBER_ID_MUTUALLY_EXCLUSIVE;
			} else if(this.memberId != null && this.timeZoneStr == null) {
				apiStatus = ApiStatusCode.TIME_ZONE_AND_MEMBER_ID_MUST_BE_SPECIFIED_TOGETHER;
			} else if((this.startDateStr != null || this.endDateStr != null) && this.memberId == null) {
				apiStatus = ApiStatusCode.MEMBER_ID_AND_DATES_MUST_BE_SPECIFIED_TOGETHER;
			} else if(this.eventId == null && this.memberId == null)  {
				apiStatus = ApiStatusCode.EITHER_EVENT_ID_AND_MEMBER_ID_REQUIRED;
			} else if(this.timeZoneStr != null && tz == null) {
				apiStatus = ApiStatusCode.INVALID_TIME_ZONE_PARAMETER;
			} else if(this.eventType != null && !Practice.isEventTypeValid(this.eventType)) {
				apiStatus = ApiStatusCode.INVALID_EVENT_TYPE_PARAMETER;
			}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				log.info(apiStatus);
				return new JsonRepresentation(jsonReturn);
			}

			List<Attendee> attendees = null;
			boolean isEventSearch = true;
			if(this.eventId != null) {
				// attendees for a specific event has been requested
				attendees = (List<Attendee>)em.createNamedQuery("Attendee.getByEventIdAndEventType")
					.setParameter("eventId", this.eventId)
					.setParameter("eventType", this.eventType)
					.getResultList();
				log.info("AttendeesResource.getAttendees(): getting " + attendees.size() + " Attendees for specified event");
			} else {
				isEventSearch = false;
				// attendance for a specific member has been requested
				Date startDate = null;
				Date endDate = null;
				if(this.startDateStr != null) {
					startDate = GMT.convertToGmtDate(this.startDateStr, tz);
				}
				if(this.endDateStr != null) {
					endDate = GMT.convertToGmtDate(this.endDateStr, tz);
				}
				
				if(startDate != null && endDate != null) {
					if(this.eventType == null) {
						attendees = (List<Attendee>)em.createNamedQuery("Attendee.getByMemberIdAndStartAndEndDates")
						.setParameter("memberId", this.memberId)
						.setParameter("startDate", startDate)
						.setParameter("endDate", endDate)
						.getResultList();
						log.info("AttendeesResource.getAttendees(): getting " + attendees.size() + " Attendees for member between start and end date");
					} else {
						attendees = (List<Attendee>)em.createNamedQuery("Attendee.getByMemberIdAndStartAndEndDatesAndEventType")
						.setParameter("memberId", this.memberId)
						.setParameter("startDate", startDate)
						.setParameter("endDate", endDate)
						.setParameter("eventType", this.eventType)
						.getResultList();
						log.info("AttendeesResource.getAttendees(): getting " + attendees.size() + " Attendees for member between start and end date. eventType = " + this.eventType);
					}
				} else if(startDate == null && endDate == null) {
					if(this.eventType == null) {
						attendees = (List<Attendee>)em.createNamedQuery("Attendee.getByMemberId")
						.setParameter("memberId", this.memberId)
						.getResultList();
						log.info("AttendeesResource.getAttendees(): getting " + attendees.size() + " Attendees for member");
					} else {
						attendees = (List<Attendee>)em.createNamedQuery("Attendee.getByMemberIdAndEventType")
						.setParameter("memberId", this.memberId)
						.setParameter("eventType", this.eventType)
						.getResultList();
						log.info("AttendeesResource.getAttendees(): getting " + attendees.size() + " Attendees for member. eventType = " + this.eventType);
					}
				} else if(startDate != null) {
					if(this.eventType == null) {
						attendees = (List<Attendee>)em.createNamedQuery("Attendee.getByMemberIdAndStartDate")
						.setParameter("memberId", this.memberId)
						.setParameter("startDate", startDate)
						.getResultList();
						log.info("AttendeesResource.getAttendees(): getting " + attendees.size() + " Attendees for member");
					} else {
						attendees = (List<Attendee>)em.createNamedQuery("Attendee.getByMemberIdAndStartDateAndEventType")
						.setParameter("memberId", this.memberId)
						.setParameter("startDate", startDate)
						.setParameter("eventType", this.eventType)
						.getResultList();
						log.info("AttendeesResource.getAttendees(): getting " + attendees.size() + " Attendees for member. eventType = " + this.eventType);
					}
				log.info("AttendeesResource.getAttendees(): getting " + attendees.size() + " Attendees for member after start date");
				} else if(endDate != null) {
					if(this.eventType == null) {
						attendees = (List<Attendee>)em.createNamedQuery("Attendee.getByMemberIdAndEndDate")
						.setParameter("memberId", this.memberId)
						.setParameter("endDate", endDate)
						.getResultList();
						log.info("AttendeesResource.getAttendees(): getting " + attendees.size() + " Attendees for member before end date");
					} else {
						attendees = (List<Attendee>)em.createNamedQuery("Attendee.getByMemberIdAndEndDateAndEventType")
						.setParameter("memberId", this.memberId)
						.setParameter("endDate", endDate)
						.setParameter("eventType", this.eventType)
						.getResultList();
						log.info("AttendeesResource.getAttendees(): getting " + attendees.size() + " Attendees for member before end date. eventType = " + this.eventType);
					}
				}
			}
    		JSONArray jsonAttendeesArray = new JSONArray();
    		for(Attendee a : attendees) {
    			JSONObject jsonAttendeeObj = new JSONObject();
    			jsonAttendeeObj.put("teamId", this.teamId);
    			
    			if(a.getIsPresent() != null) {
        			jsonAttendeeObj.put("present", a.getIsPresent() ? Attendee.PRESENT : Attendee.NOT_PRESENT);
    			}
    			jsonAttendeeObj.put("preGameStatus", a.getPreGameStatus());
    			
    			if(isEventSearch) {
    				jsonAttendeeObj.put("memberId", a.getMemberId());
    			} else {
    				String eventType = a.getEventType();
    				jsonAttendeeObj.put("eventId", a.getEventId());
    				jsonAttendeeObj.put("eventType", eventType);
    				jsonAttendeeObj.put("eventDate", GMT.convertToLocalDate(a.getEventGmtDate(), tz));
    				if(eventType.equalsIgnoreCase(Practice.GENERIC_EVENT_TYPE)) {
    					String eventName = a.getEventName() == null ? "" : a.getEventName();
    					jsonAttendeeObj.put("eventName", eventName);
    				}
    			}
    			jsonAttendeesArray.put(jsonAttendeeObj);
    		}
    		jsonReturn.put("attendees", jsonAttendeesArray);
		} catch (Exception e) {
			log.log(Level.SEVERE, "exception thrown: message = " + e.getMessage());
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
			e.printStackTrace();
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.severe("error converting json representation into a JSON object");
			e.printStackTrace();
		}
        return new JsonRepresentation(jsonReturn);
    }
    
}  
