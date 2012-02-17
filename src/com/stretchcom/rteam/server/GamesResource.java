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
import org.restlet.data.MediaType;
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
 * Resource that manages a list of games. 
 *  
 */  
public class GamesResource extends ServerResource {  
	//private static final Logger log = Logger.getLogger(GamesResource.class.getName());
	private RskyboxClient log = new RskyboxClient(this);
  
    // The sequence of characters that identifies the resource.
    String teamId;
    String timeZoneStr;
    String multiple;
    
    @Override  
    protected void doInit() throws ResourceException {  
    	log.info("API requested with URL: " + this.getReference().toString());
    	
        // Get the "teamId" attribute value taken from the URI template /team/{teamId} 
        this.teamId = (String)getRequest().getAttributes().get("teamId"); 
        log.debug("GamesResource:doInit() - teamName = " + this.teamId);
        if(this.teamId != null) {
            this.teamId = Reference.decode(this.teamId);
            log.debug("GamesResource:doInit() - decoded teamName = " + this.teamId);
        }
   
        this.timeZoneStr = (String)getRequest().getAttributes().get("timeZone"); 
        log.debug("GamesResource:doInit() - timeZone = " + this.timeZoneStr);
        if(this.timeZoneStr != null) {
            this.timeZoneStr = Reference.decode(this.timeZoneStr);
            log.debug("GamesResource:doInit() - decoded timeZone = " + this.timeZoneStr);
        }
   
        this.multiple = (String)getRequest().getAttributes().get("multiple"); 
        log.debug("GamesResource:doInit() - multiple = " + this.multiple);
        if(this.multiple != null) {
            this.multiple = Reference.decode(this.multiple);
            log.debug("GamesResource:doInit() - decoded multiple = " + this.multiple);
        }
    }  
    
    // Handles 'Create a new game' API
	// Handles 'Create multiple new games' API 
    @Post  
    public JsonRepresentation createGame(Representation entity) {
    	JSONObject jsonReturn = new JSONObject();
    	log.debug("createGame(@Post) entered ..... ");
		EntityManager em = EMF.get().createEntityManager();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_CREATED);
		List<Game> games = new ArrayList<Game>();
		User currentUser = null;
        try {
        	///////////////////////////////////
        	// Parameter Handling for both APIs
        	///////////////////////////////////
    		currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				log.error("GamesResource:createGame:currentUser", "user could not be retrieved from Request attributes!!");
    		}
			// teamId is required
			else if(this.teamId == null || this.teamId.length() == 0) {
				apiStatus = ApiStatusCode.TEAM_ID_REQUIRED;
				log.debug("invalid team ID");
			} else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
				log.debug(apiStatus);
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
			log.debug("team retrieved = " + team.getTeamName());
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
				//::SPECIAL_CASE:: even if not network authenticated allow game create if user is the team creator
				if(team.isCreator(currentUser.getEmailAddress())) {
					isCoordinator = true;
				}
			}
			
			if(!isCoordinator) {
				apiStatus = ApiStatusCode.USER_NOT_A_COORDINATOR;
				jsonReturn.put("apiStatus", apiStatus);
				log.debug(apiStatus);
				return new JsonRepresentation(jsonReturn);
			}

    		JsonRepresentation jsonRep = new JsonRepresentation(entity);
			JSONObject json = jsonRep.getJsonObject();
			log.debug("received json object = " + json.toString());
			
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
				//////////////////////////
			    // 'Create a new game' API
				//////////////////////////
				Game game = new Game();
				games.add(game);
				game.setTimeZone(timeZoneStr);
				apiStatus = parseGameFromJson(json, game, timeZoneStr);
			
				if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_CREATED)) {
					jsonReturn.put("apiStatus", apiStatus);
					return new JsonRepresentation(jsonReturn);
				}
			} else {
				//////////////////////////////////
				// 'Create multiple new games' API
				//////////////////////////////////
				if(json.has("games")) {
					JSONArray gamesJsonArray = json.getJSONArray("games");
					int arraySize = gamesJsonArray.length();
					log.debug("games json array length = " + arraySize);
					for(int i=0; i<arraySize; i++) {
						Game game = new Game();
						games.add(game);
						JSONObject gameJsonObj = gamesJsonArray.getJSONObject(i);
						game.setTimeZone(timeZoneStr);
						apiStatus = parseGameFromJson(gameJsonObj, game, timeZoneStr);
					
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
			
			for(Game g : games) {
				em.getTransaction().begin();
				g.setTeams(teams);
			    em.persist(g);
			    em.getTransaction().commit();
			}
		    
			String baseUri = this.getRequest().getHostRef().getIdentifier();
			if(this.multiple == null) {
				//////////////////////////
			    // 'Create a new game' API
				//////////////////////////
				Game game = games.get(0);
			    String keyWebStr = KeyFactory.keyToString(game.getKey());
			    log.debug("game " + game.getDescription() + " with key " + keyWebStr + " created successfully");
			    
				this.getResponse().setLocationRef(baseUri + "/" + keyWebStr);
				jsonReturn.put("gameId", keyWebStr);
			} else {
				//////////////////////////////////
				// 'Create multiple new games' API
				//////////////////////////////////
				JSONArray gameIdsJsonArray = new JSONArray();
				for(Game g : games) {
				    String keyWebStr = KeyFactory.keyToString(g.getKey());
				    log.debug("game " + g.getDescription() + " with key " + keyWebStr + " created successfully");
				    gameIdsJsonArray.put(keyWebStr);
				}
				this.getResponse().setLocationRef(baseUri + "/");
				jsonReturn.put("gameIds", gameIdsJsonArray);
			}

			//::BUSINESS_RULE:: coordinator must be network authenticated for a notification to be sent
			if(isCoordinatorNetworkAuthenticated && notificationType != null && !notificationType.equalsIgnoreCase(MessageThread.NONE_TYPE)) {
				PubHub.sendNewEventMessage(members, currentUser, team, games, null, notificationType);
			}
		} catch (IOException e) {
			log.exception("GamesResource:createGame:IOException", "", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (JSONException e) {
			log.exception("GamesResource:createGame:JSONException", "", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (NoResultException e) {
        	log.debug("no result exception, team not found");
        	apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
		} catch (NonUniqueResultException e) {
			log.exception("GamesResource:createGame:NonUniqueResultException", "", e);
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
			log.exception("GamesResource:createGame:JSONException2", "", e);
		}
		return new JsonRepresentation(jsonReturn);
    }
    
    private String parseGameFromJson(JSONObject theJson, Game theGame, String theTimeZoneStr)  throws JSONException {
    	TimeZone tz = GMT.getTimeZone(theTimeZoneStr);
		if(tz == null) {
			return ApiStatusCode.INVALID_TIME_ZONE_PARAMETER;
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
			theGame.setEventGmtStartDate(gmtStartDate);
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
				theGame.setEventGmtEndDate(gmtEndDate);
			}
		}
		
		if(theJson.has("description")) {
			String description = theJson.getString("description");
			theGame.setDescription(description);
		}
		
		if(theJson.has("opponent")) {
			String opponent = theJson.getString("opponent");
			theGame.setOpponent(opponent);
		}
		
		if(theJson.has("latitude")) {
			String latitudeStr = theJson.getString("latitude");
			try {
				Double latitude = new Double(latitudeStr);
				theGame.setLatitude(latitude);
			} catch (NumberFormatException e) {
				return ApiStatusCode.INVALID_LATITUDE_PARAMETER;
			}
		}
		
		if(theJson.has("longitude")) {
			String longitudeStr = theJson.getString("longitude");
			try {
				Double longitude = new Double(longitudeStr);
				theGame.setLongitude(longitude);
			} catch (NumberFormatException e) {
				return ApiStatusCode.INVALID_LONGITUDE_PARAMETER;
			}
		}
		
		if(theJson.has("location")) {
			String location = theJson.getString("location");
			theGame.setLocationName(location);
		}
		
		return ApiStatusCode.SUCCESS;
    }
    
    // Handles 'Get game list for a specified team' API
    // Handles 'Get game list for all user teams' API
    @Get("json")
    public JsonRepresentation getGameList(Variant variant) {
        log.debug("GamesResource:getGameList() entered");
        JSONObject jsonReturn = new JSONObject();
		EntityManager em = EMF.get().createEntityManager();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		try {
    		User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
     		
    		if(this.teamId != null && this.timeZoneStr == null) {
    			// TODO -- have browser send timezone for user requesting rScoreboard
    			this.timeZoneStr = "America/Chicago";
    		}
    		TimeZone tz = GMT.getTimeZone(this.timeZoneStr);
    		
    		if(this.teamId == null && currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
    			log.error("GamesResource:getGameList:currentUser", "user could not be retrieved from Request attributes!!");
    		} else if(this.teamId == null && tz == null) {
    			apiStatus = ApiStatusCode.INVALID_TIME_ZONE_PARAMETER;
    		}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}

			List<Game> games = null;
    		List<Team> teams = null;
    		if(this.teamId != null) {
    			// --------------------------------------------------------------
				// This is the 'Get game list for a specified team' API call
    			// --------------------------------------------------------------
    			log.debug("This is the 'Get game list for a specified team' API call");
			
    			// teamId is required
    			if(this.teamId == null || this.teamId.length() == 0) {
    				apiStatus = ApiStatusCode.TEAM_ID_REQUIRED;
    				log.debug("Team ID not provided and it is required");
    			} 
    			else if(currentUser != null && !currentUser.isUserMemberOfTeam(this.teamId)) {
    				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
    				log.debug(apiStatus);
            	}
    			
    			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
    				jsonReturn.put("apiStatus", apiStatus);
    				return new JsonRepresentation(jsonReturn);
    			}
    			
    			String teamId = null;
    			if(this.teamId.length() <= UrlShort.MAX_ID_SIZE) {
    				Team team = (Team)em.createNamedQuery("Team.getByPageUrl")
    						.setParameter("pageUrl", this.teamId)
    						.getSingleResult();
    				teamId = KeyFactory.keyToString(team.getKey());
    			} else {
    				teamId = this.teamId;
    			}

    			games = (List<Game>)em.createNamedQuery("Game.getByTeam")
    						.setParameter("teamKey", KeyFactory.stringToKey(teamId))
    						.getResultList();
    			log.debug("getGameList(): number of games found = " + games.size());
			} else {
    			// --------------------------------------------------------------
				// This is the 'Get game list for all user teams' API call
    			// --------------------------------------------------------------
    			log.debug("This is the 'Get game list for all user teams' API call");
    			
        		User user = (User)em.createNamedQuery("User.getByKey")
					.setParameter("key", currentUser.getKey())
					.getSingleResult();
	    		List<Key> teamKeys = user.getTeams();
	    		log.debug("number of user teams = " + teamKeys.size());
    		
	    		teams = new ArrayList<Team>();
	    		if(teamKeys.size() > 0) {
	        		teams = (List<Team>)em.createQuery("select from " + Team.class.getName() + " where key = :keys")
	    	    	.setParameter("keys", teamKeys)
	    	    	.getResultList();
	    		}

	    		games = new ArrayList<Game>();
	    		for(Key tk : teamKeys) {
	    			List<Game> moreGames = (List<Game>)em.createNamedQuery("Game.getByTeam")
						.setParameter("teamKey", tk)
						.getResultList();
	    			games.addAll(moreGames);
	    		}
	    		log.debug("getGameList(): number of games found = " + games.size());
			}
    		
			JSONArray jsonArray = new JSONArray();
			Date endDate = null;
			for(Game g : games) {
				JSONObject jsonGameObj = new JSONObject();
    			if(this.teamId == null) {
    				// no team specified, so games returned are for all teams
    				Team t = getTeamFromList(g, teams);
    				if(t != null) {
        				jsonGameObj.put("teamId", KeyFactory.keyToString(t.getKey()));
        				jsonGameObj.put("teamName", t.getTeamName());
        				jsonGameObj.put("sport", t.getSport());
        				
        				// Need to determine what role this user plays on this team.  Since a user can be associated with
        				// multiple memberships (due to guardian), give precedence to coordinator, then member, then fan
        				String participantRole = null;
        				if(currentUser != null) {
            				for(Member m : t.getMembers()) {
            					if(m.isUserParticipant(currentUser)) {
            						if(participantRole == null || participantRole.equalsIgnoreCase(Member.FAN_ROLE) || 
            								(m.isCoordinator() && !participantRole.equalsIgnoreCase(Member.CREATOR_PARTICIPANT)) ) {
            							participantRole = m.getParticipantRole();
            						}
            					}
            				}
        				}
        				
        				if(participantRole != null) {
        					jsonGameObj.put("participantRole", participantRole);
        				} else if(participantRole == null && currentUser != null){
        	    			log.error("GamesResource:getGameList:currentUser", "User's participant role could not be found on team = " + t.getTeamName());
        				}
    				} else {
    	    			log.error("GamesResource:getGameList:currentUser", "could not find team associated with the game");
    				}
    			}
				jsonGameObj.put("gameId", KeyFactory.keyToString(g.getKey()));
				jsonGameObj.put("description", g.getDescription());
				jsonGameObj.put("startDate", GMT.convertToLocalDate(g.getEventGmtStartDate(), tz));
				jsonGameObj.put("opponent", g.getOpponent());
				jsonGameObj.put("latitude", g.getLatitude());
				jsonGameObj.put("longitude", g.getLongitude());
				if(g.getLocationName() != null) jsonGameObj.put("location", g.getLocationName());
				if(g.getMvpDisplayName() != null) jsonGameObj.put("mvpDisplayName", g.getMvpDisplayName());
	        	
	        	Integer scoreUs = g.getScoreUs() == null ? 0 : g.getScoreUs();
	        	jsonGameObj.put("scoreUs", scoreUs);
	        	
	        	Integer scoreThem = g.getScoreThem() == null ? 0 : g.getScoreThem();
	        	jsonGameObj.put("scoreThem", scoreThem);
	        	
	        	Integer interval = g.getInterval() == null ? 0 : g.getInterval();
	        	jsonGameObj.put("interval", interval);
	        	
				jsonArray.put(jsonGameObj);
			}
			jsonReturn.put("games", jsonArray);
		} catch (JSONException e) {
			log.exception("GamesResource:getGameList:JSONException", "could not find team associated with the game", e);
		} catch (NoResultException e) {
        	log.debug("no result exception, user not found");
        	apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
		} catch (NonUniqueResultException e) {
			log.exception("GamesResource:getGameList:JSONException", "two or more users have same key", e);
        	this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} finally {
			em.close();
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("GamesResource:getGameList:JSONException2", "could not find team associated with the game", e);
		}
        return new JsonRepresentation(jsonReturn);
    }
    
    private Team getTeamFromList(Game theGame, List<Team> theTeams) {
    	if(theGame == null || theTeams == null) {
    		return null;
    	}
    	
    	Set<Key> teamKeys = theGame.getTeams();
    	for(Key k : teamKeys) {
        	for(Team t : theTeams) {
        		if(t.getKey().equals(k)) {
        			return t;
        		}
        	}
    	}
    	return null;
    }
    
}  
