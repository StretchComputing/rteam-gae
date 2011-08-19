package com.stretchcom.rteam.server;
	
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ServerResource;
import org.restlet.resource.ResourceException;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

/**
 * @author joepwro
 */
public class PracticeResource extends ServerResource {
	private static final Logger log = Logger.getLogger(PracticeResource.class.getName());

    // The sequence of characters that identifies the resource.
    String teamId;
    String practiceId;
    String timeZoneStr;
    
    @Override  
    protected void doInit() throws ResourceException {  
        // attribute values taken from the URI template /team/{teamId}/practice/{practiceId}/{timeZone}
    	
        this.teamId = (String)getRequest().getAttributes().get("teamId"); 
        log.info("PracticeResource:doInit() - teamId = " + this.teamId);
        if(this.teamId != null) {
            this.teamId = Reference.decode(this.teamId);
            log.info("PracticeResource:doInit() - decoded teamId = " + this.teamId);
        }
   
        this.practiceId = (String)getRequest().getAttributes().get("practiceId"); 
        log.info("PracticeResource:doInit() - practiceId = " + this.practiceId);
        if(this.practiceId != null) {
            this.practiceId = Reference.decode(this.practiceId);
            log.info("PracticeResource:doInit() - decoded practiceId = " + this.practiceId);
        }
        
        this.timeZoneStr = (String)getRequest().getAttributes().get("timeZone"); 
        log.info("PracticesResource:doInit() - timeZone = " + this.timeZoneStr);
        if(this.timeZoneStr != null) {
            this.timeZoneStr = Reference.decode(this.timeZoneStr);
            log.info("PracticeResource:doInit() - decoded timeZone = " + this.timeZoneStr);
        }
    }  

    // Handles 'Get practice info' API
    @Get("json")
    public JsonRepresentation getPracticeInfo(Variant variant) {
        log.info("PracticeResource:toJson() entered");
        JSONObject jsonReturn = new JSONObject();
		EntityManager em = EMF.get().createEntityManager();

		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		try {
    		User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
     		TimeZone tz = GMT.getTimeZone(this.timeZoneStr);
    		if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
    			log.severe("user could not be retrieved from Request attributes!!");
    		} else if(this.teamId == null || this.teamId.length() == 0 ||
		        	   this.practiceId == null || this.practiceId.length() == 0 ||
		        	   this.timeZoneStr == null || this.timeZoneStr.length() == 0) {
    			apiStatus = ApiStatusCode.TEAM_ID_PRACTICE_ID_AND_TIME_ZONE_REQUIRED;
			} else if(tz == null) {
    			apiStatus = ApiStatusCode.INVALID_TIME_ZONE_PARAMETER;
    		} 
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}

    		Key practiceKey = KeyFactory.stringToKey(this.practiceId);
    		Practice practice = (Practice)em.createNamedQuery("Practice.getByKey")
    			.setParameter("key", practiceKey)
    			.getSingleResult();
    		log.info("practice retrieved = " + practice.getDescription());
        	
        	jsonReturn.put("eventType", practice.getEventType());
        	jsonReturn.put("startDate", GMT.convertToLocalDate(practice.getEventGmtStartDate(), tz));
        	
        	String eventName = practice.getEventName();
        	if(eventName != null) jsonReturn.put("eventName", practice.getEventName());
        	
        	String description = practice.getDescription();
        	if(description != null) jsonReturn.put("description", practice.getDescription());

        	Date endDate = practice.getEventGmtEndDate();
        	if(endDate != null) jsonReturn.put("endDate", GMT.convertToLocalDate(endDate, tz));
        	
        	String opponent = practice.getOpponent();
        	if(opponent != null ) jsonReturn.put("opponent", opponent);
        	
        	Double latitude = practice.getLatitude();
        	if(latitude != null ) jsonReturn.put("latitude", latitude);
        	
        	Double longitude = practice.getLongitude();
        	if(longitude != null ) jsonReturn.put("longitude", longitude);
        	
        	Boolean isCanceled = practice.getIsCanceled();
        	if(isCanceled != null ) jsonReturn.put("isCanceled", isCanceled);
        } catch (NoResultException e) {
        	log.info("no result exception, practice not found");
        	apiStatus = ApiStatusCode.PRACTICE_NOT_FOUND;
		} catch (NonUniqueResultException e) {
			log.severe("should never happen - two or more practices have same key");
			e.printStackTrace();
        	this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (JSONException e) {
			log.severe("error building JSON object");
			e.printStackTrace();
		} finally {
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

    // Handles 'Delete Game' API
    @Delete
    public JsonRepresentation remove() {
    	log.info("PracticeResource:remove() entered");
    	EntityManager em = EMF.get().createEntityManager();
    	JSONObject jsonReturn = new JSONObject();
    	
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
        try {
    		User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
    			log.severe("user could not be retrieved from Request attributes!!");
    		}
    		// teamId and gameId are required
    		else if(this.teamId == null || this.teamId.length() == 0 ||
		        	   this.practiceId == null || this.practiceId.length() == 0 ) {
        		apiStatus = ApiStatusCode.TEAM_ID_AND_PRACTICE_ID_REQUIRED;
			} 
			// must be a member of the team
			else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
				log.info(apiStatus);
        	}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
        	
    		Team team = (Team)em.createNamedQuery("Team.getByKey")
				.setParameter("key", KeyFactory.stringToKey(this.teamId))
				.getSingleResult();
			log.info("team retrieved = " + team.getTeamName());
			
			// TODO use query instead of walking through entire members list.  See MemberResource for example of query.
			List<Member> members = team.getMembers();
			
   			Boolean isCoordinator = false;
			if(currentUser.getIsNetworkAuthenticated()) {
				List<Member> memberships = Member.getMemberShipsWithEmailAddress(currentUser.getEmailAddress(), team);
				for(Member m : memberships) {
    	    		if(m.isCoordinator()) {
    	    			isCoordinator = true;
    	    			break;
    	    		}
				}
			}
			
			//::BUSINESSRULE:: user must be a coordinator to update team info
			if(!isCoordinator) {
				apiStatus = ApiStatusCode.USER_NOT_A_COORDINATOR;
				jsonReturn.put("apiStatus", apiStatus);
				log.info(apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
        	
	    	em.getTransaction().begin();
	    	
    		Key practiceKey = KeyFactory.stringToKey(this.practiceId);
    		Practice practice = (Practice)em.createNamedQuery("Practice.getByKey")
    			.setParameter("key", practiceKey)
    			.getSingleResult();
    		log.info("practice retrieved = " + practice.getDescription());
        	
    		// BUG WORKAROUND:: after commit(), practice.getEventType() is NULL
    		Practice practicePubHubClone = (Practice)practice.clone();
    		//log.info("before remove, practice.getEventType() = " + practice.getEventType());
    		em.remove(practice);
    		//log.info("after remove, practice.getEventType() = " + practice.getEventType());
        	em.getTransaction().commit();
        	//log.info("after commit(), practice.getEventType() = " + practice.getEventType());
	    	
    		// TODO make sure practice is associated with the specified team
        	
        	//::BUSINESS_RULE notification only sent if user/coordinator is network authenticated
        	if(currentUser.getIsNetworkAuthenticated()) {
            	// send notification message to let everyone (members and fans) know that the game has been deleted
            	PubHub.sendEventDeletedMessage(team.getTeamMates(currentUser), currentUser, null, practicePubHubClone, team);
        	}
        	
        } catch (JSONException e) {
			log.severe("error converting json representation into a JSON object");
			e.printStackTrace();
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (NoResultException e) {
        	log.info("no result exception, team or practice not found");
        	apiStatus = ApiStatusCode.PRACTICE_NOT_FOUND;
		} catch (NonUniqueResultException e) {
			log.severe("should never happen - two or more teams or practices have same key");
			e.printStackTrace();
        	this.setStatus(Status.SERVER_ERROR_INTERNAL);
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

    // Handles 'Update Practice' API
    @Put 
    public JsonRepresentation updatePractice(Representation entity) {
    	JSONObject jsonReturn = new JSONObject();
    	log.info("updatePractice(@Put) entered ..... ");
		EntityManager em = EMF.get().createEntityManager();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		String notificationMessage = "";
        try {
    		User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
    			log.severe("user could not be retrieved from Request attributes!!");
    		}
    		// teamId and gameId are required
    		else if(this.teamId == null || this.teamId.length() == 0 ||
		        	   this.practiceId == null || this.practiceId.length() == 0 ) {
        		apiStatus = ApiStatusCode.TEAM_ID_AND_PRACTICE_ID_REQUIRED;
			} 
			// must be a member of the team
			else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
				log.info(apiStatus);
        	}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
        	
    		Team team = (Team)em.createNamedQuery("Team.getByKey")
				.setParameter("key", KeyFactory.stringToKey(this.teamId))
				.getSingleResult();
			log.info("team retrieved = " + team.getTeamName());
			
			// TODO use query instead of walking through entire members list.  See MemberResource for example of query.
			List<Member> members = team.getMembers();
			
			////////////////////////////////////////////////////////////////////////////////////////
   			//::BUSINESS_RULE:: User must be the team creator or a network authenticated coordinator
			////////////////////////////////////////////////////////////////////////////////////////
			Boolean isCoordinator = false;
			Boolean isCoordinatorNetworkAuthenticated = false;
			if(currentUser.getIsNetworkAuthenticated()) {
				List<Member> memberships = Member.getMemberShipsWithEmailAddress(currentUser.getEmailAddress(), team);
				for(Member m : memberships) {
    	    		if(m.isCoordinator()) {
    	    			isCoordinator = true;
    	    			isCoordinatorNetworkAuthenticated = true;
    	    			break;
    	    		}
				}
			} else {
				//::SPECIAL_CASE:: even if not network authenticated allow if user is the team creator
				if(team.isCreator(currentUser.getEmailAddress())) {
					isCoordinator = true;
				}
			}
			
			if(!isCoordinator) {
				apiStatus = ApiStatusCode.USER_NOT_A_COORDINATOR;
				jsonReturn.put("apiStatus", apiStatus);
				log.info(apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
        	
			em.getTransaction().begin();
    		Key practiceKey = KeyFactory.stringToKey(this.practiceId);
    		Practice practice = (Practice)em.createNamedQuery("Practice.getByKey")
    			.setParameter("key", practiceKey)
    			.getSingleResult();
    		log.info("practice retrieved = " + practice.getDescription());

			JsonRepresentation jsonRep = new JsonRepresentation(entity);
			JSONObject json = jsonRep.toJsonObject();
			log.info("received json object = " + json.toString());
			
			// if new field is empty, original value is not updated.
			String timeZoneStr = null;
			if(json.has("timeZone")) {
    			timeZoneStr = json.getString("timeZone");
			}
			
			String startDateStr = null;
			if(json.has("startDate")) {
				startDateStr = json.getString("startDate");
			}
			
			String endDateStr = null;
			if(json.has("endDate")) {
				endDateStr = json.getString("endDate");
			}
			
			String description = null;
			if(json.has("description")) {
				description = json.getString("description");
			}
			
			String opponent = null;
			if(json.has("opponent")) {
				opponent = json.getString("opponent");
			}
			
			String latitudeStr = null;
			if(json.has("latitude")) {
    			latitudeStr = json.getString("latitude");
			}
			
			String longitudeStr = null;
			if(json.has("longitude")) {
				longitudeStr = json.getString("longitude");
			}
			
			Boolean updateAll = false;
			if(json.has("updateAll")) {
				try {
					updateAll = json.getBoolean("updateAll");
				} catch (JSONException e) {
					apiStatus = ApiStatusCode.INVALID_UPDATE_ALL_PARAMETER;
				}
			}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}

			String eventName = null;
			if(json.has("eventName")) {
				eventName = json.getString("eventName");
			}
			
			String notificationType = MessageThread.PLAIN_TYPE;
			if(json.has("notificationType")) {
				notificationType = json.getString("notificationType");
				//::BUSINESS_RULE default is PLAIN notification
				if(notificationType.length() == 0) {
					notificationType = MessageThread.PLAIN_TYPE;
				} else if(!Game.isNotificationTypeValid(notificationType)) {
					apiStatus = ApiStatusCode.INVALID_NOTIFICATION_TYPE_PARAMETER;
				}
			}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			
			Boolean isCanceled = null;
			if(json.has("isCanceled")) {
				try {
					isCanceled = json.getBoolean("isCanceled");
				} catch (JSONException e) {
					apiStatus = ApiStatusCode.INVALID_IS_CANCELED_PARAMETER;
				}
			}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}

			TimeZone tz = null;
			Date gmtStartDate = null;
			Date gmtEndDate = null;
			Double latitude = null;
			Double longitude = null;
			// Time Zone, Start Date and Description are required
			if(timeZoneStr == null && (startDateStr != null || endDateStr != null)) {
				apiStatus = ApiStatusCode.TIME_ZONE_AND_DATES_MUST_BE_SPECIFIED_TOGETHER;
			}
			else {
				boolean isTimeZoneInvalid = false;
				if(timeZoneStr != null) {
					tz = GMT.getTimeZone(timeZoneStr);
					if(tz ==  null) {
						isTimeZoneInvalid = true;
					}
				}
				
				if(isTimeZoneInvalid) {
					apiStatus = ApiStatusCode.INVALID_TIME_ZONE_PARAMETER;
				} else {
					if(startDateStr != null) {
    					gmtStartDate = GMT.convertToGmtDate(startDateStr, tz);
    					if(gmtStartDate == null) {
    						apiStatus = ApiStatusCode.INVALID_START_DATE_PARAMETER;
    					}
					}
					
					if(apiStatus.equals(ApiStatusCode.SUCCESS) && endDateStr != null) {
						gmtEndDate = GMT.convertToGmtDate(endDateStr, tz);
    					if(gmtEndDate == null) {
    						apiStatus = ApiStatusCode.INVALID_START_DATE_PARAMETER;
    					}
					}
				}
				
				if(apiStatus.equals(ApiStatusCode.SUCCESS) && latitudeStr != null && latitudeStr.length() > 0) {
					try {
						latitude = new Double(latitudeStr);
					} catch (NumberFormatException e) {
						apiStatus = ApiStatusCode.INVALID_LATITUDE_PARAMETER;
					}
				}
				
				if(apiStatus.equals(ApiStatusCode.SUCCESS) && longitudeStr != null && longitudeStr.length() > 0) {
					try {
						longitude = new Double(longitudeStr);
					} catch (NumberFormatException e) {
						apiStatus = ApiStatusCode.INVALID_LONGITUDE_PARAMETER;
					}
				}
				
			}
		    
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}

			// if new field is empty, original value is not updated.
			if(gmtStartDate != null) {
				Date oldStartDate = practice.getEventGmtStartDate();
				if(oldStartDate == null || !oldStartDate.equals(gmtStartDate)) {
					practice.setPreviousEventGmtStartDate(oldStartDate);
					String oldStartDateStr = "";
					if(oldStartDate != null) {oldStartDateStr = GMT.convertToSimpleLocalDate(oldStartDate, tz);}
					String simpleStartDateStr = GMT.convertToSimpleLocalDate(gmtStartDate, tz);
					String startDateUpdateMessage = Utility.getModMessage("Start Date", oldStartDateStr, simpleStartDateStr);
					log.info(startDateUpdateMessage);
					notificationMessage = notificationMessage + " " + startDateUpdateMessage;
				}
				practice.setEventGmtStartDate(gmtStartDate);
			}
			
			// if new field is empty, field is cleared.
			if(gmtEndDate != null) {
				Date oldEndDate = practice.getEventGmtEndDate();
				if(oldEndDate == null || !oldEndDate.equals(gmtEndDate)) {
					String oldEndDateStr = "";
					if(oldEndDate != null) {oldEndDateStr = GMT.convertToSimpleLocalDate(oldEndDate, tz);}
					String simpleEndDateStr = GMT.convertToSimpleLocalDate(gmtEndDate, tz);
					String endDateUpdateMessage = Utility.getModMessage("End Date", oldEndDateStr, simpleEndDateStr);
					log.info(endDateUpdateMessage);
					notificationMessage = notificationMessage + " " + endDateUpdateMessage;
				}
				practice.setEventGmtEndDate(gmtEndDate);
			}
			
			// if new field is empty, original value is not updated.
			if(timeZoneStr != null && timeZoneStr.length() > 0) {
				String oldTimeZoneStr = practice.getTimeZone() != null ? practice.getTimeZone() : "";
				if(!timeZoneStr.equalsIgnoreCase(oldTimeZoneStr)) {
					String timeZoneUpdateMessage = "Time Zone has been changed from " + oldTimeZoneStr + " to " + timeZoneStr;
					// TODO not even sure if it makes sense to change a timezone. Certainly can't include in notify since it varies.
					//notificationMessage = notificationMessage + " " + timeZoneUpdateMessage;
				}
				practice.setTimeZone(timeZoneStr);
			}
			
			
			// if new field is empty, original value is not updated.
			if(description != null && description.length() > 0) {
				String oldDescription = practice.getDescription() != null ? practice.getDescription() : "";
				if(!description.equalsIgnoreCase(oldDescription)) {
					String descriptionMessage = "Description has been changed from " + oldDescription + " to " + description;
					notificationMessage = notificationMessage + " " + descriptionMessage;
				}
				practice.setDescription(description);
			}
			
			// if new field is empty, original value is not updated.
			if(eventName != null && eventName.length() > 0) {
				String oldEventName = practice.getEventName() != null ? practice.getEventName() : "";
				if(!eventName.equalsIgnoreCase(oldEventName)) {
					String eventNameMessage = "Event Name has been changed from " + oldEventName + " to " + eventName;
					notificationMessage = notificationMessage + " " + eventNameMessage;
				}
				practice.setEventName(eventName);
			}
			
			// if new field is empty, field is cleared.
			boolean wasLatitudeUpdated = false;
			if(latitudeStr != null) {
				String oldLatitudeStr = practice.getLatitude() != null ? practice.getLatitude().toString() : "";
				if(!latitudeStr.equalsIgnoreCase(oldLatitudeStr)) {
					// TODO send something more meaningful in the notification message
					String latitudeUpdateMessage = "Latitude has been changed from " + oldLatitudeStr + " to " + latitudeStr;
					//notificationMessage = notificationMessage + " " + latitudeUpdateMessage;
					wasLatitudeUpdated = true;
				}
				practice.setLatitude(latitude);
			}
			
			
			// if new field is empty, field is cleared.
			boolean wasLongitudeUpdated = false;
			if(longitudeStr != null) {
				String oldLongitudeStr = practice.getLongitude() != null ? practice.getLongitude().toString() : "";
				if(!longitudeStr.equalsIgnoreCase(oldLongitudeStr)) {
					// TODO send something more meaningful in the notification message
					String longitudeUpdateMessage = "Longitude has been changed from " + oldLongitudeStr + " to " + longitudeStr;
					//notificationMessage = notificationMessage + " " + longitudeUpdateMessage;
					wasLongitudeUpdated = true;
				}
				practice.setLongitude(longitude);
			}
			
			if(wasLatitudeUpdated || wasLongitudeUpdated) {
				String locationUpdatemessage = "Location was updated";
				log.info(locationUpdatemessage);
				notificationMessage = notificationMessage + " " + locationUpdatemessage;
			}
			
			// if new field is empty, field is cleared.
			if(opponent != null) {
				String oldOpponent = practice.getOpponent() != null ? practice.getOpponent() : "";
				if(!opponent.equalsIgnoreCase(oldOpponent)) {
					String opponentMessage = null;
					if(updateAll) {
						// for this situation, the upstream code expects only the new location to be in the notification message
						opponentMessage = opponent;
					} else {
						opponentMessage = "Location has been changed from " + oldOpponent + " to " + opponent;
					}
					String spacer = "";
					if(notificationMessage.length() > 0) {
						spacer = " ";
					}
					notificationMessage = notificationMessage + spacer + opponentMessage;
				}
				practice.setOpponent(opponent);
			}
			
			if(isCanceled != null) {
				Boolean oldIsCanceled = practice.getIsCanceled();
				// oldIsCanceled can equal NULL because practices could have been created before isCanceled feature added
				if(oldIsCanceled ==  null || !oldIsCanceled.equals(isCanceled)) {
					String isCanceledUpdateMessage = "Canceled";
					notificationMessage = notificationMessage + " " + isCanceledUpdateMessage;
				}
				practice.setIsCanceled(isCanceled);
			}
			
		    em.getTransaction().commit();
		    log.info("practice " + practice.getDescription() + " updated successfully");
		    
			//::BUSINESS_RULE:: coordinator must be network authenticated for a notification to be sent
        	if(isCoordinatorNetworkAuthenticated && notificationMessage.length() > 0 &&
        			notificationType != null && !notificationType.equalsIgnoreCase(MessageThread.NONE_TYPE)) {
            	// send notification message to let everyone (members and fans) know that the practice info has been updated
            	PubHub.sendEventUpdatedMessage(team.getTeamMates(currentUser), currentUser, null, practice, notificationMessage, team, updateAll);
        	}
        	
        	if(updateAll) {
        		Practice.updateAllLocations(team, practice);
        	}
        	
		} catch (IOException e) {
			log.severe("error extracting JSON object from Post");
			e.printStackTrace();
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (JSONException e) {
			log.severe("error converting json representation into a JSON object");
			e.printStackTrace();
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (NoResultException e) {
			log.severe("team not found");
		} catch (NonUniqueResultException e) {
			log.severe("should never happen - two or more teams have same team name");
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
}