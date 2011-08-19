package com.stretchcom.rteam.server;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
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
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
  
/** 
 * Resource that manages a list of practices. 
 *  
 */  
public class PracticesResource extends ServerResource {  
	private static final Logger log = Logger.getLogger(PracticesResource.class.getName());
  
    // The sequence of characters that identifies the resource.
    String teamId;
    String timeZoneStr;
    String eventType;
    String happening;
    String multiple;
    
    @Override  
    protected void doInit() throws ResourceException {  
        // Get the "teamId" attribute value taken from the URI template /team/{teamId} 
        this.teamId = (String)getRequest().getAttributes().get("teamId"); 
        log.info("PracticesResource:doInit() - teamName = " + this.teamId);
        if(this.teamId != null) {
            this.teamId = Reference.decode(this.teamId);
            log.info("PracticesResource:doInit() - decoded teamName = " + this.teamId);
        }
   
        this.timeZoneStr = (String)getRequest().getAttributes().get("timeZone"); 
        log.info("PracticesResource:doInit() - timeZone = " + this.timeZoneStr);
        if(this.timeZoneStr != null) {
            this.timeZoneStr = Reference.decode(this.timeZoneStr);
            log.info("PracticesResource:doInit() - decoded timeZone = " + this.timeZoneStr);
        }
        
        this.multiple = (String)getRequest().getAttributes().get("multiple"); 
        log.info("GamesResource:doInit() - multiple = " + this.multiple);
        if(this.multiple != null) {
            this.multiple = Reference.decode(this.multiple);
            log.info("GamesResource:doInit() - decoded multiple = " + this.multiple);
        }
        
		Form form = getRequest().getResourceRef().getQueryAsForm();
		for (Parameter parameter : form) {
			log.info("parameter " + parameter.getName() + " = " + parameter.getValue());
			if(parameter.getName().equals("eventType"))  {
				this.eventType = (String)parameter.getValue();
				this.eventType = Reference.decode(this.eventType);
				log.info("PracticeResource:doInit() - decoded eventType = " + this.eventType);
			} else if(parameter.getName().equals("happening"))  {
				this.happening = (String)parameter.getValue();
				this.happening = Reference.decode(this.happening);
				log.info("PracticeResource:doInit() - decoded happening = " + this.happening);
			}
		}
    }  
    
    // Handles 'Create a new practice' API
	// Handles 'Create multiple new practices' API 
    @Post  
    public JsonRepresentation createPractice(Representation entity) {
    	JSONObject jsonReturn = new JSONObject();
    	log.info("createPractice(@Post) entered ..... ");
		EntityManager em = EMF.get().createEntityManager();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_CREATED);
		List<Practice> practices = new ArrayList<Practice>();
		User currentUser = null;
        try {
        	///////////////////////////////////
        	// Parameter Handling for both APIs
        	///////////////////////////////////
    		currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				log.severe("user could not be retrieved from Request attributes!!");
    		}
			// teamId is required
			else if(this.teamId == null || this.teamId.length() == 0) {
				apiStatus = ApiStatusCode.TEAM_ID_REQUIRED;
				log.info("invalid team ID");
			} else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
				log.info(apiStatus);
        	}
    		
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_CREATED)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			
    		// need to get membership of current user to check if user is a coordinator
			///////////////////////////////////////////////////////////////////////////
			Team team = (Team)em.createNamedQuery("Team.getByKey")
				.setParameter("key", KeyFactory.stringToKey(this.teamId))
				.getSingleResult();
			log.info("team retrieved = " + team.getTeamName());
			// need to access members before closing transaction since it's lazy init and used after tran closed
			team.getMembers();
			
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
				//::SPECIAL_CASE:: even if not network authenticated allow practice create if user is the team creator
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

    		JsonRepresentation jsonRep = new JsonRepresentation(entity);
			JSONObject json = jsonRep.toJsonObject();
			log.info("received json object = " + json.toString());
			
			String timeZoneStr = null;
			if(json.has("timeZone")) {
    			timeZoneStr = json.getString("timeZone");
			} else {
				apiStatus = ApiStatusCode.TIME_ZONE_REQUIRED;
			}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_CREATED)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
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
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_CREATED)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			////////////////////////////////////////////////////////////////////////////////////////////////////////////
			// end of Common parameter handling
			////////////////////////////////////////////////////////////////////////////////////////////////////////////
			
			
			if(this.multiple == null) {
				//////////////////////////////
			    // 'Create a new practice' API
				//////////////////////////////
				Practice practice = new Practice();
				practices.add(practice);
				practice.setTimeZone(timeZoneStr);
				apiStatus = parsePracticeFromJson(json, practice, timeZoneStr);
			
				if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_CREATED)) {
					jsonReturn.put("apiStatus", apiStatus);
					return new JsonRepresentation(jsonReturn);
				}
			} else {
				//////////////////////////////////////
				// 'Create multiple new practices' API
				//////////////////////////////////////
				if(json.has("practices")) {
					JSONArray practicesJsonArray = json.getJSONArray("practices");
					int arraySize = practicesJsonArray.length();
					log.info("practices json array length = " + arraySize);
					for(int i=0; i<arraySize; i++) {
						Practice practice = new Practice();
						practices.add(practice);
						JSONObject practiceJsonObj = practicesJsonArray.getJSONObject(i);
						practice.setTimeZone(timeZoneStr);
						apiStatus = parsePracticeFromJson(practiceJsonObj, practice, timeZoneStr);
					
						if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_CREATED)) {
							jsonReturn.put("apiStatus", apiStatus);
							return new JsonRepresentation(jsonReturn);
						}
					}
				}
			}
			
			// teamMates does not include the currentUser since currentUser should NOT receive notification
			List<Member> members = team.getTeamMates(currentUser);
			HashSet<Key> teams = new  HashSet<Key>();
			teams.add(KeyFactory.stringToKey(this.teamId));
			
			for(Practice p : practices) {
				em.getTransaction().begin();
				p.setTeams(teams);
			    em.persist(p);
			    em.getTransaction().commit();
			}
		    
			String baseUri = this.getRequest().getHostRef().getIdentifier();
			if(this.multiple == null) {
				//////////////////////////////
			    // 'Create a new practice' API
				//////////////////////////////
				Practice practice = practices.get(0);
			    String keyWebStr = KeyFactory.keyToString(practice.getKey());
			    log.info("practice " + practice.getDescription() + " with key " + keyWebStr + " created successfully");
			    
				this.getResponse().setLocationRef(baseUri + "/" + keyWebStr);
				jsonReturn.put("practiceId", keyWebStr);
			} else {
				//////////////////////////////////////
				// 'Create multiple new practices' API
				//////////////////////////////////////
				JSONArray practiceIdsJsonArray = new JSONArray();
				for(Practice p : practices) {
				    String keyWebStr = KeyFactory.keyToString(p.getKey());
				    log.info("practice " + p.getDescription() + " with key " + keyWebStr + " created successfully");
				    practiceIdsJsonArray.put(keyWebStr);
				}
				this.getResponse().setLocationRef(baseUri + "/");
				jsonReturn.put("practiceIds", practiceIdsJsonArray);
			}

			//::BUSINESS_RULE:: coordinator must be network authenticated for a notification to be sent
			if(isCoordinatorNetworkAuthenticated && notificationType != null && !notificationType.equalsIgnoreCase(MessageThread.NONE_TYPE)) {
				PubHub.sendNewEventMessage(members, currentUser, team, null, practices, notificationType);
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
        	log.info("no result exception, team not found");
        	apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
		} catch (NonUniqueResultException e) {
			log.severe("should never happen - two or more teams have same key");
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
    
    private String parsePracticeFromJson(JSONObject theJson, Practice thePractice, String theTimeZoneStr)  throws JSONException {
    	TimeZone tz = GMT.getTimeZone(theTimeZoneStr);
		if(tz == null) {
			return ApiStatusCode.INVALID_TIME_ZONE_PARAMETER;
		}
		
		if(theJson.has("eventType")) {
			String eventTypeStr = theJson.getString("eventType");
			if(!eventTypeStr.equalsIgnoreCase(Practice.PRACTICE) && !eventTypeStr.equalsIgnoreCase(Practice.GENERIC_EVENT_TYPE)) {
				return ApiStatusCode.INVALID_EVENT_TYPE_PARAMETER;
			}
			thePractice.setEventType(eventTypeStr);
		} else {
			return ApiStatusCode.EVENT_TYPE_REQUIRED;
		}
		
		if(theJson.has("eventName")) {
			String eventName = theJson.getString("eventName");
			thePractice.setEventName(eventName);
		}
					
		if(theJson.has("startDate")) {
			String startDateStr = theJson.getString("startDate");
			if(startDateStr == null || startDateStr.trim().length() == 0) {
				return ApiStatusCode.START_DATE_REQUIRED;
			}
			Date gmtStartDate = GMT.convertToGmtDate(startDateStr, tz);
			if(gmtStartDate == null) {
				return ApiStatusCode.INVALID_START_DATE_PARAMETER;
			}
			thePractice.setEventGmtStartDate(gmtStartDate);
		} else {
			return ApiStatusCode.START_DATE_REQUIRED;
		}
		
		if(theJson.has("endDate")) {
			String endDateStr = theJson.getString("endDate");
			if(endDateStr != null || endDateStr.trim().length() != 0) {
				Date gmtEndDate = GMT.convertToGmtDate(endDateStr, tz);
				if(gmtEndDate == null) {
					return ApiStatusCode.INVALID_END_DATE_PARAMETER;
				}
				thePractice.setEventGmtEndDate(gmtEndDate);
			}
		}
		
		if(theJson.has("description")) {
			String description = theJson.getString("description");
			thePractice.setDescription(description);
		}
		
		if(theJson.has("opponent")) {
			String opponent = theJson.getString("opponent");
			thePractice.setOpponent(opponent);
		}
		
		if(theJson.has("latitude")) {
			String latitudeStr = theJson.getString("latitude");
			try {
				Double latitude = new Double(latitudeStr);
				thePractice.setLatitude(latitude);
			} catch (NumberFormatException e) {
				return ApiStatusCode.INVALID_LATITUDE_PARAMETER;
			}
		}
		
		if(theJson.has("longitude")) {
			String longitudeStr = theJson.getString("longitude");
			try {
				Double longitude = new Double(longitudeStr);
				thePractice.setLongitude(longitude);
			} catch (NumberFormatException e) {
				return ApiStatusCode.INVALID_LONGITUDE_PARAMETER;
			}
		}
		
		if(theJson.has("location")) {
			String location = theJson.getString("location");
			thePractice.setLocationName(location);
		}
		
		// by default, practice is not canceled
		thePractice.setIsCanceled(false);
		
		return ApiStatusCode.SUCCESS;
    }
    
    // Handles 'Get list of practices for a specified team' API
    // Handles 'Get list of practices for all user teams' API
    @Get("json")
    public JsonRepresentation getPracticeList(Variant variant) {
        log.info("PracticesResource:getPracticeList() entered");
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
    		} else if(tz == null) {
    			apiStatus = ApiStatusCode.INVALID_TIME_ZONE_PARAMETER;
    		}
			
			if(apiStatus.equals(ApiStatusCode.SUCCESS) && this.eventType != null) {
				this.eventType = this.eventType.toLowerCase();
				if(!this.eventType.equalsIgnoreCase(Practice.PRACTICE) &&
				   !this.eventType.equalsIgnoreCase(Practice.GENERIC_EVENT_TYPE) &&
				   !this.eventType.equalsIgnoreCase(Practice.ALL_EVENT_TYPE)) {
					apiStatus = ApiStatusCode.INVALID_EVENT_TYPE_PARAMETER;
				}
			} else {
				// default to 'practice'
				this.eventType = Practice.PRACTICE;
			}
			
			if(apiStatus.equals(ApiStatusCode.SUCCESS) && this.happening != null) {
				if(!this.happening.equalsIgnoreCase("now")) {
					apiStatus = ApiStatusCode.INVALID_HAPPENING_PARAMETER;
				}
			}			

			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			
    		List<Practice> practices = null;
    		List todayGames = null;
    		List todayPractices = null;
    		List tomorrowGames = null;
    		List tomorrowPractices = null;
    		List<Game> games = null;
    		List<Team> teams = null;
    		
    		if(this.teamId != null) {
    			// --------------------------------------------------------------
				// This is the 'Get practice list for a specified team' API call
    			// --------------------------------------------------------------
    			log.info("This is the 'Get practice list for a specified team' API call");
			
    			// teamId is required
    			if(this.teamId == null || this.teamId.length() == 0) {
    				apiStatus = ApiStatusCode.TEAM_ID_REQUIRED;
    				log.info("Team ID not provided and it is required");
    			} 
    			else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
    				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
    				log.info(apiStatus);
            	}
    			
    			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
    				jsonReturn.put("apiStatus", apiStatus);
    				return new JsonRepresentation(jsonReturn);
    			}

    			
    			if(this.eventType.equals(Practice.ALL_EVENT_TYPE)) {
        			practices = (List<Practice>)em.createNamedQuery("Practice.getByTeam")
					.setParameter("teamKey", KeyFactory.stringToKey(this.teamId))
					.getResultList();
    			} else {
        			practices = (List<Practice>)em.createNamedQuery("Practice.getByTeamAndEventType")
					.setParameter("teamKey", KeyFactory.stringToKey(this.teamId))
					.setParameter("eventType", this.eventType)
					.getResultList();
    			}
    			log.info("getPracticeList(): number of practices found = " + practices.size());
    		} else {
    			// --------------------------------------------------------------
				// This is the 'Get practice list for all user teams' API call
    			// --------------------------------------------------------------
    			log.info("This is the 'Get practice list for all user teams' API call");
    			
        		User user = (User)em.createNamedQuery("User.getByKey")
					.setParameter("key", currentUser.getKey())
					.getSingleResult();
	    		List<Key> teamKeys = user.getTeams();
	    		log.info("number of user teams = " + teamKeys.size());
    		
	    		teams = new ArrayList<Team>();
	    		if(teamKeys.size() > 0) {
	        		teams = (List<Team>)em.createQuery("select from " + Team.class.getName() + " where key = :keys")
	    	    	.setParameter("keys", teamKeys)
	    	    	.getResultList();
	    		}
	    		
	    		practices = new ArrayList<Practice>();
	    		if(this.happening != null) {
	        		games = new ArrayList<Game>();
	    			todayGames = new ArrayList();
	    			tomorrowGames = new ArrayList();
	    			todayPractices = new ArrayList();
	    			tomorrowPractices = new ArrayList();

	        		// for 'happening', need to get all events for today and tomorrow.
	    			List<Date> todayDates = GMT.getTodayBeginAndEndDates(tz);
	    			List<Date> tomorrowDates = GMT.getTomorrowBeginAndEndDates(tz);
		    		for(Key tk : teamKeys) {
		    			List<Practice> morePractices = null;
		    			// get all practices for today and tomorrow - will sort them out in code later
		    			morePractices = (List<Practice>)em.createNamedQuery("Practice.getByTeamAndStartDateRange")
							.setParameter("teamKey", tk)
							.setParameter("startDate", todayDates.get(0))
							.setParameter("endDate", tomorrowDates.get(1))
							.getResultList();
		    			practices.addAll(morePractices);
		    			
		    			List<Game> moreGames = null;
		    			// get all games for today and tomorrow - will sort them out in code later
		    			moreGames = (List<Game>)em.createNamedQuery("Game.getByTeamAndStartDateRange")
							.setParameter("teamKey", tk)
							.setParameter("startDate", todayDates.get(0))
							.setParameter("endDate", tomorrowDates.get(1))
							.getResultList();
		    			games.addAll(moreGames);
		    		}
		    		log.info("getPracticeList(): number of practices found = " + practices.size());
		    		log.info("getPracticeList(): number of games found = " + games.size());
		    		
		    		// split the 'today' and 'tomorrow' games/practices
		    		for(Practice p : practices) {
		    			if( (p.getEventGmtStartDate().equals(todayDates.get(0)) || p.getEventGmtStartDate().after(todayDates.get(0))) && 
		    					(p.getEventGmtStartDate().equals(todayDates.get(1)) || p.getEventGmtStartDate().before(todayDates.get(1)))) {
		    				todayPractices.add(p);
		    			}
		    			else if( (p.getEventGmtStartDate().equals(tomorrowDates.get(0)) || p.getEventGmtStartDate().after(tomorrowDates.get(0))) && 
		    					(p.getEventGmtStartDate().equals(tomorrowDates.get(1)) || p.getEventGmtStartDate().before(tomorrowDates.get(1)))) {
		    				tomorrowPractices.add(p);
		    			} else {
		    				log.severe("practice could not be mapped into Today's events or Tomorrow's events");
		    			}
		    		}
		    		for(Game g : games) {
		    			g.setEventType(Practice.GAME_EVENT_TYPE);
		    			if( (g.getEventGmtStartDate().equals(todayDates.get(0)) || g.getEventGmtStartDate().after(todayDates.get(0))) && 
		    					(g.getEventGmtStartDate().equals(todayDates.get(1)) || g.getEventGmtStartDate().before(todayDates.get(1)))) {
		    				todayGames.add(g);
		    			}
		    			else if( (g.getEventGmtStartDate().equals(tomorrowDates.get(0)) || g.getEventGmtStartDate().after(tomorrowDates.get(0))) && 
		    					(g.getEventGmtStartDate().equals(tomorrowDates.get(1)) || g.getEventGmtStartDate().before(tomorrowDates.get(1)))) {
		    				tomorrowGames.add(g);
		    			} else {
		    				log.severe("game could not be mapped into Today's events or Tomorrow's events");
		    			}
		    		}
	    		} else {
		    		for(Key tk : teamKeys) {
		    			List<Practice> morePractices = null;
		    			if(this.eventType.equals(Practice.ALL_EVENT_TYPE)) {
			    			morePractices = (List<Practice>)em.createNamedQuery("Practice.getByTeam")
							.setParameter("teamKey", tk)
							.getResultList();
		    			} else {
			    			morePractices = (List<Practice>)em.createNamedQuery("Practice.getByTeamAndEventType")
							.setParameter("teamKey", tk)
							.setParameter("eventType", this.eventType)
							.getResultList();
		    			}
		    			practices.addAll(morePractices);
		    		}
		    		log.info("getPracticeList(): number of practices found = " + practices.size());
	    		}

    		}
			
    		if(this.happening != null) {
        		JSONArray jsonTodayArray = new JSONArray();
        		buildPracticeJsonArray(teams, todayPractices, currentUser, tz, "eventId", jsonTodayArray);
        		buildGameJsonArray(teams, todayGames, currentUser, tz, "eventId", jsonTodayArray);
        		jsonReturn.put("today", jsonTodayArray);
        		
        		JSONArray jsonTomorrowArray = new JSONArray();
        		buildPracticeJsonArray(teams, tomorrowPractices, currentUser, tz, "eventId", jsonTomorrowArray);
        		buildGameJsonArray(teams, tomorrowGames, currentUser, tz, "eventId", jsonTomorrowArray);
        		jsonReturn.put("tomorrow", jsonTomorrowArray);
    		} else {
        		JSONArray jsonArray = new JSONArray();
        		buildPracticeJsonArray(teams, practices, currentUser, tz, "practiceId", jsonArray);
        		jsonReturn.put("practices", jsonArray);
    		}
		} catch (JSONException e) {
			log.severe("error converting json representation into a JSON object");
			e.printStackTrace();
		} catch (NoResultException e) {
        	log.info("no result exception, user not found");
        	apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
		} catch (NonUniqueResultException e) {
			log.severe("should never happen - two or more users have same key");
			e.printStackTrace();
        	this.setStatus(Status.SERVER_ERROR_INTERNAL);
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
    
    private void buildPracticeJsonArray(List<Team> theTeams, List<Practice> thePractices, User theUser, 
    		TimeZone theTimeZone, String theEventIdName, JSONArray theJsonArray) throws JSONException {
		for(Practice p : thePractices) {
			JSONObject jsonPracticeObj = new JSONObject();
			if(this.teamId == null) {
				// no team specified, so practices returned are for all teams
				Set<Key> teamKeys = p.getTeams();
				Team t = getTeamFromList(teamKeys, theTeams);
				if(t != null) {
					jsonPracticeObj.put("teamId", KeyFactory.keyToString(t.getKey()));
					jsonPracticeObj.put("teamName", t.getTeamName());
					if(t.getSport() != null) jsonPracticeObj.put("sport", t.getSport());
					
					// Need to determine what role this user plays on this team.  Since a user can be associated with
					// multiple memberships (due to guardian), give precedence to coordinator, then member, then fan
					String participantRole = null;
					for(Member m : t.getMembers()) {
						if(m.isUserParticipant(theUser)) {
							if(participantRole == null || participantRole.equalsIgnoreCase(Member.FAN_ROLE) || 
									(m.isCoordinator() && !participantRole.equalsIgnoreCase(Member.CREATOR_PARTICIPANT)) ) {
								participantRole = m.getParticipantRole();
							}
						}
					}
					
					if(participantRole != null) {
						jsonPracticeObj.put("participantRole", participantRole);
					} else {
						log.severe("User's partipant role could not be found on team = " + t.getTeamName());
					}
				} else {
					log.severe("could not find team associated with the practice");
				}
			}
			//TODO  what happens when a put is done and the practice attribute is null?
			jsonPracticeObj.put(theEventIdName, KeyFactory.keyToString(p.getKey()));
			jsonPracticeObj.put("description", p.getDescription());
			jsonPracticeObj.put("startDate", GMT.convertToLocalDate(p.getEventGmtStartDate(), theTimeZone));
			jsonPracticeObj.put("latitude", p.getLatitude());
			jsonPracticeObj.put("longitude", p.getLongitude());
			jsonPracticeObj.put("eventType", p.getEventType());
			jsonPracticeObj.put("eventName", p.getEventName());
			jsonPracticeObj.put("opponent", p.getOpponent());
			jsonPracticeObj.put("isCanceled", p.getIsCanceled());
			theJsonArray.put(jsonPracticeObj);
		}
    }
    
    private void buildGameJsonArray(List<Team> theTeams, List<Game> theGames, User theUser, 
    		TimeZone theTimeZone, String theEventIdName, JSONArray theJsonArray) throws JSONException {
		for(Game g : theGames) {
			JSONObject jsonGameObj = new JSONObject();
			if(this.teamId == null) {
				// no team specified, so games returned are for all teams
				Set<Key> teamKeys = g.getTeams();
				Team t = getTeamFromList(teamKeys, theTeams);
				if(t != null) {
					jsonGameObj.put("teamId", KeyFactory.keyToString(t.getKey()));
					jsonGameObj.put("teamName", t.getTeamName());
					if(t.getSport() != null) jsonGameObj.put("sport", t.getSport());
					
					// Need to determine what role this user plays on this team.  Since a user can be associated with
					// multiple memberships (due to guardian), give precedence to coordinator, then member, then fan
					String participantRole = null;
					for(Member m : t.getMembers()) {
						if(m.isUserParticipant(theUser)) {
							if(participantRole == null || participantRole.equalsIgnoreCase(Member.FAN_ROLE) || 
									(m.isCoordinator() && !participantRole.equalsIgnoreCase(Member.CREATOR_PARTICIPANT)) ) {
								participantRole = m.getParticipantRole();
							}
						}
					}
					
					if(participantRole != null) {
						jsonGameObj.put("participantRole", participantRole);
					} else {
						log.severe("User's partipant role could not be found on team = " + t.getTeamName());
					}
				} else {
					log.severe("could not find team associated with the game");
				}
			}
			jsonGameObj.put(theEventIdName, KeyFactory.keyToString(g.getKey()));
			jsonGameObj.put("description", g.getDescription());
			jsonGameObj.put("startDate", GMT.convertToLocalDate(g.getEventGmtStartDate(), theTimeZone));
			jsonGameObj.put("latitude", g.getLatitude());
			jsonGameObj.put("longitude", g.getLongitude());
			jsonGameObj.put("eventType", g.getEventType());
			jsonGameObj.put("eventName", g.getDescription());
			jsonGameObj.put("opponent", g.getOpponent());
			Integer interval = g.getInterval() == null ? 0 : g.getInterval();
			jsonGameObj.put("interval", interval);
			Boolean isCanceled = interval.equals(Game.GAME_CANCELED_INTERVAL) ? true : false;
			jsonGameObj.put("isCanceled", isCanceled);
        	Integer scoreUs = g.getScoreUs() == null ? 0 : g.getScoreUs();
        	jsonGameObj.put("scoreUs", scoreUs);
        	Integer scoreThem = g.getScoreThem() == null ? 0 : g.getScoreThem();
        	jsonGameObj.put("scoreThem", scoreThem);
			theJsonArray.put(jsonGameObj);
		}
    }
    
    private Team getTeamFromList(Set<Key> theTeamKeys, List<Team> theTeams) {
    	if(theTeamKeys == null || theTeams == null) {
    		return null;
    	}
    	
    	for(Key k : theTeamKeys) {
        	for(Team t : theTeams) {
        		if(t.getKey().equals(k)) {
        			return t;
        		}
        	}
    	}
    	return null;
    }
}  
