package com.stretchcom.rteam.server;
	
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
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
import org.restlet.data.MediaType;
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
public class GameResource extends ServerResource {
	//private static final Logger log = Logger.getLogger(GameResource.class.getName());
	private RskyboxClient log = new RskyboxClient(this);

    // The sequence of characters that identifies the resource.
    String teamId;
    String gameId;
    String timeZoneStr;
    String voteType;
    
    @Override  
    protected void doInit() throws ResourceException {  
    	log.info("API requested with URL: " + this.getReference().toString());
    	
        // attribute values taken from the URI template /team/{teamId}/game/{gameId}/{timeZone}
    	
        this.teamId = (String)getRequest().getAttributes().get("teamId"); 
        log.debug("GameResource:doInit() - teamId = " + this.teamId);
        if(this.teamId != null) {
            this.teamId = Reference.decode(this.teamId);
            log.debug("GameResource:doInit() - decoded teamId = " + this.teamId);
        }
   
        this.gameId = (String)getRequest().getAttributes().get("gameId"); 
        log.debug("GameResource:doInit() - gameId = " + this.gameId);
        if(this.gameId != null) {
            this.gameId = Reference.decode(this.gameId);
            log.debug("GameResource:doInit() - decoded gameId = " + this.gameId);
        }
        
        this.timeZoneStr = (String)getRequest().getAttributes().get("timeZone"); 
        log.debug("GamesResource:doInit() - timeZone = " + this.timeZoneStr);
        if(this.timeZoneStr != null) {
            this.timeZoneStr = Reference.decode(this.timeZoneStr);
            log.debug("GameResource:doInit() - decoded timeZone = " + this.timeZoneStr);
        }
        
        this.voteType = (String)getRequest().getAttributes().get("voteType"); 
        log.debug("GamesResource:doInit() - voteType = " + this.voteType);
        if(this.voteType != null) {
            this.voteType = Reference.decode(this.voteType);
            log.debug("GameResource:doInit() - decoded voteType = " + this.voteType);
        }
    }  

    // Handles 'Get game info' API
    // Handles 'Get game vote tallies' API
    @Get("json")
    public JsonRepresentation getGameInfo(Variant variant) {
        log.debug("GameResource:toJson() entered");
        JSONObject jsonReturn = new JSONObject();
		EntityManager em = EMF.get().createEntityManager();

		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		try {
    		User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
     		TimeZone tz = GMT.getTimeZone(this.timeZoneStr);
    		if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
    			log.error("GameResource:getGameInfo:currentUser", "user could not be retrieved from Request attributes!!");
    		}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			
			if(voteType == null) {
				///////////////////////////
				// 'Get game info' API only
				///////////////////////////
				if(this.teamId == null || this.teamId.length() == 0 ||
			        	   this.gameId == null || this.gameId.length() == 0 ||
			        	   this.timeZoneStr == null || this.timeZoneStr.length() == 0) {
	    			apiStatus = ApiStatusCode.TEAM_ID_GAME_ID_AND_TIME_ZONE_REQUIRED;
				} else if(tz == null) {
	    			apiStatus = ApiStatusCode.INVALID_TIME_ZONE_PARAMETER;
	    		}
			} else {
    			///////////////////////////////////
    			// 'Get game vote tallies' API only
    			///////////////////////////////////
				if(this.teamId == null || this.teamId.length() == 0 || this.gameId == null || this.gameId.length() == 0) {
	    			apiStatus = ApiStatusCode.TEAM_ID_AND_GAME_ID_REQUIRED;
				} else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
					apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
					log.debug(apiStatus);
	        	} else 	if(!GameVote.isVoteTypeValid(this.voteType)) {
					apiStatus = ApiStatusCode.INVALID_VOTE_TYPE_PARAMETER;
					log.debug(apiStatus);
				}
			}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
        	
    		Key gameKey = KeyFactory.stringToKey(this.gameId);
    		Game game = null;
    		try {
        		game = (Game)em.createNamedQuery("Game.getByKey")
    				.setParameter("key", gameKey)
    				.getSingleResult();
        		log.debug("game retrieved = " + game.getDescription());
    		} catch (NoResultException e) {
            	log.debug("no result exception, game not found");
            	apiStatus = ApiStatusCode.GAME_NOT_FOUND;
    		} catch (NonUniqueResultException e) {
    			log.exception("GameResource:getGameInfo:NonUniqueResultException1", "two or more games have same key", e);
            	this.setStatus(Status.SERVER_ERROR_INTERNAL);
    		}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
    		
    		if(voteType == null) {
    			//////////////////////
    		    // 'Get game info' API
    			//////////////////////
            	jsonReturn.put("description", game.getDescription());
            	jsonReturn.put("startDate", GMT.convertToLocalDate(game.getEventGmtStartDate(), tz));
            	
            	Date endDate = game.getEventGmtEndDate();
            	if(endDate != null) jsonReturn.put("endDate", GMT.convertToLocalDate(endDate, tz));
            	
            	String opponent = game.getOpponent();
            	if(opponent != null ) jsonReturn.put("opponent", opponent);
            	
            	Double latitude = game.getLatitude();
            	if(latitude != null ) jsonReturn.put("latitude", latitude);
            	
            	Double longitude = game.getLongitude();
            	if(longitude != null ) jsonReturn.put("longitude", longitude);
            	
            	String location = game.getLocationName();
            	if(location != null ) jsonReturn.put("location", location);
            	
            	Integer scoreUs = game.getScoreUs() == null ? 0 : game.getScoreUs();
            	jsonReturn.put("scoreUs", scoreUs);
            	
            	Integer scoreThem = game.getScoreThem() == null ? 0 : game.getScoreThem();
            	jsonReturn.put("scoreThem", scoreThem);
            	
            	Integer interval = game.getInterval() == null ? 0 : game.getInterval();
            	jsonReturn.put("interval", interval);
            	
            	// if not set, default pollStatus to OPEN_POLL_STATUSs
            	String pollStatus = game.getPollStatus() == null ? Game.OPEN_POLL_STATUS : game.getPollStatus();
            	jsonReturn.put("pollStatus", pollStatus);
            	
            	String mvpMemberId = game.getMvpMemberId();
            	if(mvpMemberId != null ) jsonReturn.put("mvpMemberId", mvpMemberId);
            	
            	String mvpDisplayName = game.getMvpDisplayName();
            	if(mvpDisplayName != null ) jsonReturn.put("mvpDisplayName", mvpDisplayName);
    		} else {
    			//////////////////////////////
    		    // 'Get game vote tallies' API
    			//////////////////////////////
    			
    			/////////////////////////////////////////////////////
    			// Get the team and construct the non-fan member list
    			/////////////////////////////////////////////////////
        		Team team = null;
				List<Member> members = new ArrayList<Member>();
    			try {
        			team = (Team)em.createNamedQuery("Team.getByKey")
    					.setParameter("key", KeyFactory.stringToKey(this.teamId))
    					.getSingleResult();
            		log.debug("team retrieved = " + team.getTeamName());
            		
    				// filter out the fans
    				for(Member m : team.getMembers()) {
    					if(!m.isFan()) {
    						members.add(m);
    					}
    				}
    				log.debug("number of non-fan members = " + members.size());
        		} catch (NoResultException e) {
                	log.debug("no result exception, team not found");
                	apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
        		} catch (NonUniqueResultException e) {
        			log.exception("GameResource:getGameInfo:NonUniqueResultException2", "two or more teams have same key", e);
                	this.setStatus(Status.SERVER_ERROR_INTERNAL);
        		}
    			
    			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
    				jsonReturn.put("apiStatus", apiStatus);
    				return new JsonRepresentation(jsonReturn);
    			}
        		
    			/////////////////////////////////////
    			// Get the Game Tally for each member
    			/////////////////////////////////////
    			List<GameVote> gameTallies = null;
    			try {
        			gameTallies = (List<GameVote>)em.createNamedQuery("GameVote.getByGameIdAndVoteTypeAndIsTally")
        				.setParameter("gameId", this.gameId)
        				.setParameter("voteType", this.voteType)
        				.setParameter("isTally", GameVote.YES_TALLY)
        				.getResultList();
        			log.debug("number of gameTallies = " + gameTallies.size());
        		} catch (Exception e) {
        			log.exception("GameResource:getGameInfo:Exception2", "retrieving gameTallies", e);
        			this.setStatus(Status.SERVER_ERROR_INTERNAL);
        		}    		
    			
    			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
    				jsonReturn.put("apiStatus", apiStatus);
    				return new JsonRepresentation(jsonReturn);
    			}
    			
    			JSONArray memberTalliesJsonArray = new JSONArray();
    			for(Member m : members) {
    				JSONObject memberTallyJsonObject = new JSONObject();
    				memberTallyJsonObject.put("memberId", KeyFactory.keyToString(m.getKey()));
    				memberTallyJsonObject.put("memberName", m.getDisplayName());
    				GameVote memberTally = getMemberTally(gameTallies, m);
    				if(memberTally == null) {
    					// tally doesn't exist, so member has zero votes so far
    					memberTallyJsonObject.put("voteCount", 0);
    				} else {
    					memberTallyJsonObject.put("voteCount", memberTally.getVoteCount());
    				}
    				memberTalliesJsonArray.put(memberTallyJsonObject);
    			}
    			jsonReturn.put("members", memberTalliesJsonArray);
        	}
    		
			//////////////////////////////////
			// Get the user's vote if it exist
			//////////////////////////////////
			try {
    			GameVote gameVote = (GameVote)em.createNamedQuery("GameVote.getByUserIdAndGameIdAndVoteType")
    				.setParameter("userId", KeyFactory.keyToString(currentUser.getKey()))
    				.setParameter("gameId", this.gameId)
    				.setParameter("voteType", this.voteType)
    				.getSingleResult();
    			
    			jsonReturn.put("vote", gameVote.getMemberId());
    		} catch (NoResultException e) {
    			// this is not an error - user has not voted yet for this game
    			// nothing returned in JSON object if user hasn't voted yet.
    		} catch (NonUniqueResultException e) {
    			log.exception("GameResource:getGameInfo:NonUniqueResultException3", "two or more games have same key", e);
    			this.setStatus(Status.SERVER_ERROR_INTERNAL);
    		}    		
		} catch (JSONException e) {
			log.exception("GameResource:getGameInfo:JSONException1", "", e);
		} finally {
			em.close();
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("GameResource:getGameInfo:JSONException2", "", e);
		}
        return new JsonRepresentation(jsonReturn);
    }
    
    private GameVote getMemberTally(List<GameVote> theGameTallies, Member theMember) {
    	for(GameVote gv : theGameTallies) {
    		if(gv.getMemberId().equals(KeyFactory.keyToString(theMember.getKey()))) {
    			return gv;
    		}
    	}
    	return null;
    }

    // Handles 'Delete Game' API
    @Delete
    public JsonRepresentation remove() {
    	log.debug("GameResource:remove() entered");
    	EntityManager em = EMF.get().createEntityManager();
    	JSONObject jsonReturn = new JSONObject();
    	
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
        try {
    		User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				log.error("GameResource:remove:currentUser", "");
    		}
    		// teamId and gameId are required
    		else if(this.teamId == null || this.teamId.length() == 0 ||
		        	   this.gameId == null || this.gameId.length() == 0 ) {
        		apiStatus = ApiStatusCode.TEAM_ID_AND_GAME_ID_REQUIRED;
			} 
			// must be a member of the team
			else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
				log.debug(apiStatus);
        	}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
        	
    		Team team = (Team)em.createNamedQuery("Team.getByKey")
				.setParameter("key", KeyFactory.stringToKey(this.teamId))
				.getSingleResult();
			log.debug("team retrieved = " + team.getTeamName());
			// need to access members before closing transaction since it's lazy init and used after tran closed
			team.getMembers();
			
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
				log.debug(apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
        	
	    	em.getTransaction().begin();
    		Key gameKey = KeyFactory.stringToKey(this.gameId);
    		Game game = (Game)em.createNamedQuery("Game.getByKey")
    			.setParameter("key", gameKey)
    			.getSingleResult();
    		log.debug("game retrieved = " + game.getDescription());
    		
    		// TODO make sure game is associated with the specified team
        	
    		// BUG WORKAROUND:: after commit(), game.getEventType() is NULL
    		Game gamePubHubClone = (Game)game.clone();
        	em.remove(game);
        	em.getTransaction().commit();
        	
        	//::BUSINESS_RULE notification only sent if user/coordinator is network authenticated
        	if(currentUser.getIsNetworkAuthenticated()) {
            	// send notification message to let everyone (members and fans) know that the game has been deleted
            	PubHub.sendEventDeletedMessage(team.getTeamMates(currentUser), currentUser, gamePubHubClone, null, team);
        	}
        	
        } catch (JSONException e) {
			log.exception("GameResource:remove:JSONException1", "", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (NoResultException e) {
        	log.debug("no result exception, team or game not found");
        	apiStatus = ApiStatusCode.GAME_NOT_FOUND;
		} catch (NonUniqueResultException e) {
			log.exception("GameResource:remove:NonUniqueResultException", "", e);
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
			log.exception("GameResource:remove:JSONException2", "", e);
		}
        return new JsonRepresentation(jsonReturn);
    }

    // Handles 'Update Game' API
    // Handles 'Cast Game Vote' API
    @Put 
    public JsonRepresentation updateGame(Representation entity) {
    	JSONObject jsonReturn = new JSONObject();
    	log.debug("updateGame(@Put) entered ..... ");
		EntityManager em = EMF.get().createEntityManager();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		String notificationMessage = "";
        try {
    		User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				log.error("GameResource:updateGame:currentUser", "");
    		}
    		// teamId and gameId are required
    		else if(this.teamId == null || this.teamId.length() == 0 ||
		        	   this.gameId == null || this.gameId.length() == 0 ) {
        		apiStatus = ApiStatusCode.TEAM_ID_AND_GAME_ID_REQUIRED;
			} 
			// must be a member of the team
			else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
				log.debug(apiStatus);
        	}
				
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			
			Team team = null;
			List<Member> members = null;
			try {
	    		team = (Team)em.createNamedQuery("Team.getByKey")
					.setParameter("key", KeyFactory.stringToKey(this.teamId))
					.getSingleResult();
	    		log.debug("team retrieved = " + team.getTeamName());
	    		
	    		// need to access members before closing transaction since it's lazy init and used after tran closed
	    		members = team.getMembers();
			} catch (NoResultException e) {
				log.debug("team not found");
				apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
			} catch (NonUniqueResultException e) {
				log.exception("GameResource:updateGame:NonUniqueResultException", "", e);
			}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
        	
			Boolean isCoordinator = false;
			Boolean isCoordinatorNetworkAuthenticated = false;
			if(this.voteType == null) {
				/////////////////////////
				// 'Update Game' API only
				/////////////////////////
				
				////////////////////////////////////////////////////////////////////////////////////////
	   			//::BUSINESS_RULE:: User must be the team creator or a network authenticated coordinator
				////////////////////////////////////////////////////////////////////////////////////////
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
			} else {
				////////////////////////////
				// 'Cast Game Vote' API Only
				////////////////////////////
				if(!GameVote.isVoteTypeValid(this.voteType)) {
					apiStatus = ApiStatusCode.INVALID_VOTE_TYPE_PARAMETER;
					jsonReturn.put("apiStatus", apiStatus);
					log.debug(apiStatus);
					return new JsonRepresentation(jsonReturn);
				}
			}
			
			String notificationType = "";
			Game game = null;
			String gameMvp = null;
			String updateAllStr = null;
			String gameJustCompletedStr = "false";
			try {
	    		Key gameKey = KeyFactory.stringToKey(this.gameId);
	    		game = (Game)em.createNamedQuery("Game.getByKey")
	    			.setParameter("key", gameKey)
	    			.getSingleResult();
	    		log.debug("game retrieved = " + game.getDescription());

				JsonRepresentation jsonRep = new JsonRepresentation(entity);
				JSONObject json = jsonRep.getJsonObject();
				log.debug("received json object = " + json.toString());
				
				List<String> notificationInfo = new ArrayList<String>();
				if(this.voteType == null) {
					////////////////////
					// 'Update Game' API
					////////////////////
					apiStatus = handleJsonForUpdateGameApi(json, game, notificationInfo);
					if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
						jsonReturn.put("apiStatus", apiStatus);
						return new JsonRepresentation(jsonReturn);
					}
					
					notificationType = notificationInfo.get(0);
					notificationMessage = notificationInfo.get(1);
					gameMvp = notificationInfo.get(2);
					updateAllStr = notificationInfo.get(3);
					gameJustCompletedStr = notificationInfo.get(4);
				} else {
					///////////////////////
					// 'Cast Game Vote' API
					///////////////////////
					String memberId = null;
					if(json.has("memberId")) {
						memberId = json.getString("memberId");
					} else {
						apiStatus = ApiStatusCode.MEMBER_ID_REQUIRED;
					}

					if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
						jsonReturn.put("apiStatus", apiStatus);
						return new JsonRepresentation(jsonReturn);
					}
					
					// ::BUSINESS_RULE:: verify that the member is on the team and is not a fan
					Member memberSpecified = null;
					for(Member m : members) {
						String memId = KeyFactory.keyToString(m.getKey());
						if(memId.equals(memberId)) {
							memberSpecified = m;
							break;
						}
					}
					
					if(memberSpecified == null) {
						apiStatus = ApiStatusCode.MEMBER_NOT_FOUND;
					} else if(memberSpecified.isFan()) {
						apiStatus = ApiStatusCode.MEMBER_CANNOT_BE_A_FAN;
					}

					if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
						jsonReturn.put("apiStatus", apiStatus);
						return new JsonRepresentation(jsonReturn);
					}
					
					GameVote.castUserVote(currentUser, memberId, game, voteType, team);
				}

			    log.debug("game " + game.getDescription() + " updated successfully");
			} catch (NoResultException e) {
				log.debug("game not found");
				apiStatus = ApiStatusCode.GAME_NOT_FOUND;
			} catch (NonUniqueResultException e) {
				log.exception("GameResource:updateGame:NonUniqueResultException2", "", e);
			}

		    
			if(this.voteType == null) {
				////////////////////
				// 'Update Game' API
				////////////////////
				
				Boolean updateAllLocations = (updateAllStr != null && updateAllStr.equalsIgnoreCase("true")) ? true : false;
			    
			    //::BUSINESS_RULE:: coordinator must be network authenticated for a notification to be sent
	        	if(isCoordinatorNetworkAuthenticated && notificationMessage.length() > 0 &&
	        			notificationType != null && !notificationType.equalsIgnoreCase(MessageThread.NONE_TYPE)) {
	            	// send notification message to let everyone (members and fans) know that the game info has been updated
	            	PubHub.sendEventUpdatedMessage(team.getTeamMates(currentUser), currentUser, game, null, notificationMessage, team, updateAllLocations);
	        	}
	        	
				// if end of game, post score to activity
	        	//log.debug("game interval = " + game.getInterval());
	        	if(gameJustCompletedStr.equalsIgnoreCase("true")) {
	        		//log.debug("Game over, post the score to Twitter");
	        		//PubHub.postScoreActivity(team, game, game.getScoreUs(), game.getScoreThem());
	        		PubHub.sendGameSummaryMessage(members, team, game);
				}
	        	
	        	if(gameMvp != null && gameMvp.length() > 0) {
	        		PubHub.postGameMvpActivity(team, game, gameMvp);
	        	}
	        	
	        	if(updateAllLocations) {
	        		Game.updateAllLocations(team, game);
	        	}
			}
		} catch (IOException e) {
			log.exception("GameResource:updateGame:IOException", "", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (JSONException e) {
			log.exception("GameResource:updateGame:JSONException1", "", e);
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
			log.exception("GameResource:updateGame:JSONException2", "", e);
		}
		return new JsonRepresentation(jsonReturn);
    }
    
    // theGame: out parameter that stores all the game info parsed out by this method
    // theNotificationInfo: out parameter that hold notificationType and notificationMessage
    private String handleJsonForUpdateGameApi(JSONObject theJson, Game theGame, List<String> theNotificationInfo)  throws JSONException {
    	String notificationMessage = "";
    	Boolean pollHasBeenClosed = false;
    	Boolean pollReopened = false;
    	
		// if new field is empty, original value is not updated.
		String timeZoneStr = null;
		if(theJson.has("timeZone")) {
			timeZoneStr = theJson.getString("timeZone");
		}
		
		String startDateStr = null;
		if(theJson.has("startDate")) {
			startDateStr = theJson.getString("startDate");
		}
		
		String endDateStr = null;
		if(theJson.has("endDate")) {
			endDateStr = theJson.getString("endDate");
		}
		
		String description = null;
		if(theJson.has("description")) {
			description = theJson.getString("description");
		}
		
		String opponent = null;
		if(theJson.has("opponent")) {
			opponent = theJson.getString("opponent");
		}
		
		String latitudeStr = null;
		if(theJson.has("latitude")) {
			latitudeStr = theJson.getString("latitude");
		}
		
		String longitudeStr = null;
		if(theJson.has("longitude")) {
			longitudeStr = theJson.getString("longitude");
		}
		
		String location = null;
		if(theJson.has("location")) {
			location = theJson.getString("location");
		}
		
		Boolean updateAll = null;
		if(theJson.has("updateAll")) {
			try {
				updateAll = theJson.getBoolean("updateAll");
			} catch (JSONException e) {
				return ApiStatusCode.INVALID_UPDATE_ALL_PARAMETER;
			}
		}
		
		String scoreUsStr = null;
		if(theJson.has("scoreUs")) {
			scoreUsStr = theJson.getString("scoreUs");
		}
		
		String scoreThemStr = null;
		if(theJson.has("scoreThem")) {
			scoreThemStr = theJson.getString("scoreThem");
		}
		
		String intervalStr = null;
		if(theJson.has("interval")) {
			intervalStr = theJson.getString("interval");
		}
		
		String notificationType = MessageThread.PLAIN_TYPE;
		if(theJson.has("notificationType")) {
			notificationType = theJson.getString("notificationType");
			//::BUSINESS_RULE default is PLAIN notification
			if(notificationType.length() == 0) {
				notificationType = MessageThread.PLAIN_TYPE;
			} else if(!Game.isNotificationTypeValid(notificationType)) {
				return ApiStatusCode.INVALID_NOTIFICATION_TYPE_PARAMETER;
			}
		}
		
		String pollStatus = null;
		if(theJson.has("pollStatus")) {
			pollStatus = theJson.getString("pollStatus");
		}

		TimeZone tz = null;
		Date gmtStartDate = null;
		Date gmtEndDate = null;
		Double latitude = null;
		Double longitude = null;
		Integer scoreUs = null;
		Integer scoreThem = null;
		Integer interval = null;
		// Time Zone, Start Date and Description are required
		if(timeZoneStr == null && (startDateStr != null || endDateStr != null)) {
			return ApiStatusCode.TIME_ZONE_AND_DATES_MUST_BE_SPECIFIED_TOGETHER;
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
				return ApiStatusCode.INVALID_TIME_ZONE_PARAMETER;
			} else {
				if(startDateStr != null) {
					gmtStartDate = GMT.convertToGmtDate(startDateStr, tz);
					if(gmtStartDate == null) {
						return ApiStatusCode.INVALID_START_DATE_PARAMETER;
					}
				}
				
				if(endDateStr != null) {
					gmtEndDate = GMT.convertToGmtDate(endDateStr, tz);
					if(gmtEndDate == null) {
						return ApiStatusCode.INVALID_END_DATE_PARAMETER;
					}
				}
			}
			
			if(latitudeStr != null && latitudeStr.length() > 0) {
				try {
					latitude = new Double(latitudeStr);
				} catch (NumberFormatException e) {
					return ApiStatusCode.INVALID_LATITUDE_PARAMETER;
				}
			}
			
			if(longitudeStr != null && longitudeStr.length() > 0) {
				try {
					longitude = new Double(longitudeStr);
				} catch (NumberFormatException e) {
					return ApiStatusCode.INVALID_LONGITUDE_PARAMETER;
				}
			}
			
			if(scoreUsStr != null && scoreUsStr.length() > 0) {
				try {
					scoreUs = new Integer(scoreUsStr);
				} catch (NumberFormatException e) {
					return ApiStatusCode.INVALID_SCORE_US_PARAMETER;
				}
			}
			
			if(scoreThemStr != null && scoreThemStr.length() > 0) {
				try {
					scoreThem = new Integer(scoreThemStr);
				} catch (NumberFormatException e) {
					return ApiStatusCode.INVALID_SCORE_THEM_PARAMETER;
				}
			}
			
			if(intervalStr != null && intervalStr.length() > 0) {
				try {
					interval = new Integer(intervalStr);
				} catch (NumberFormatException e) {
					return ApiStatusCode.INVALID_INTERVAL_PARAMETER;
				}
			}
			
			if(pollStatus != null) {
				if(!Game.isPollStatusValid(pollStatus)) {
					return ApiStatusCode.INVALID_POLL_STATUS_PARAMETER;
				}
			}
		}

		// if new field is empty, original value is not updated.
		if(gmtStartDate != null) {
			Date oldStartDate = theGame.getEventGmtStartDate();
			if(oldStartDate == null || !oldStartDate.equals(gmtStartDate)) {
				theGame.setPreviousEventGmtStartDate(oldStartDate);
				String oldStartDateStr = "";
				if(oldStartDate != null) {oldStartDateStr = GMT.convertToSimpleLocalDate(oldStartDate, tz);}
				String simpleStartDateStr = GMT.convertToSimpleLocalDate(gmtStartDate, tz);
				String startDateUpdateMessage = Utility.getModMessage("Start Date", oldStartDateStr, simpleStartDateStr);
				log.debug(startDateUpdateMessage);
				notificationMessage = notificationMessage + " " + startDateUpdateMessage;
			}
			theGame.setEventGmtStartDate(gmtStartDate);
		}
		
		// if new field is empty, field is cleared.
		if(gmtEndDate != null) {
			Date oldEndDate = theGame.getEventGmtEndDate();
			if(oldEndDate == null || !oldEndDate.equals(gmtEndDate)) {
				String oldEndDateStr = "";
				if(oldEndDate != null) {oldEndDateStr = GMT.convertToSimpleLocalDate(oldEndDate, tz);}
				String simpleEndDateStr = GMT.convertToSimpleLocalDate(gmtEndDate, tz);
				String endDateUpdateMessage = Utility.getModMessage("End Date", oldEndDateStr, simpleEndDateStr);
				log.debug(endDateUpdateMessage);
				notificationMessage = notificationMessage + " " + endDateUpdateMessage;
			}
			theGame.setEventGmtEndDate(gmtEndDate);
		}
		
		// if new field is empty, original value is not updated.
		if(timeZoneStr != null && timeZoneStr.length() > 0) {
			String oldTimeZoneStr = theGame.getTimeZone() != null ? theGame.getTimeZone() : "";
			if(!timeZoneStr.equalsIgnoreCase(oldTimeZoneStr)) {
				String timeZoneUpdateMessage = Utility.getModMessage("Time Zone", oldTimeZoneStr, timeZoneStr);
				log.debug(timeZoneUpdateMessage);
				// TODO not even sure if it makes sense to change a timezone. Certainly can't include in notify since it varies.
				//notificationMessage = notificationMessage + " " + timeZoneUpdateMessage;
			}
			theGame.setTimeZone(timeZoneStr);
		}
		
		
		// if new field is empty, original value is not updated.
		if(description != null && description.length() > 0) {
			String oldDescription = theGame.getDescription() != null ? theGame.getDescription() : "";
			if(!description.equalsIgnoreCase(oldDescription)) {
				String descriptionMessage = Utility.getModMessage("Description", oldDescription, description); 
				log.debug(descriptionMessage);
				notificationMessage = notificationMessage + " " + descriptionMessage;
			}
			theGame.setDescription(description);
		}
		
		// if new field is empty, field is cleared.
		boolean wasLatitudeUpdated = false;
		if(latitudeStr != null) {
			String oldLatitudeStr = theGame.getLatitude() != null ? theGame.getLatitude().toString() : "";
			if(!latitudeStr.equalsIgnoreCase(oldLatitudeStr)) {
				// TODO send something more meaningful in the notification message
				//String latitudeUpdateMessage = "Latitude has been changed from " + oldLatitudeStr + " to " + latitudeStr;
				//notificationMessage = notificationMessage + " " + latitudeUpdateMessage;
				wasLatitudeUpdated = true;
			}
			theGame.setLatitude(latitude);
		}
		
		
		// if new field is empty, field is cleared.
		boolean wasLongitudeUpdated = false;
		if(longitudeStr != null) {
			String oldLongitudeStr = theGame.getLongitude() != null ? theGame.getLongitude().toString() : "";
			if(!longitudeStr.equalsIgnoreCase(oldLongitudeStr)) {
				// TODO send something more meaningful in the notification message
				//String longitudeUpdateMessage = "Longitude has been changed from " + oldLongitudeStr + " to " + longitudeStr;
				//notificationMessage = notificationMessage + " " + longitudeUpdateMessage;
				wasLongitudeUpdated = true;
			}
			theGame.setLongitude(longitude);
		}
		
		if(wasLatitudeUpdated || wasLongitudeUpdated) {
			String locationUpdatemessage = "Location was updated";
			log.debug(locationUpdatemessage);
			notificationMessage = notificationMessage + " " + locationUpdatemessage;
		}
		
		// if new field is empty, field is cleared.
		if(location != null) {
			String oldLocation = theGame.getLocationName() != null ? theGame.getLocationName() : "";
			if(!location.equalsIgnoreCase(oldLocation)) {
				String locationUpdateMessage = null;
				if(updateAll != null && updateAll) {
					// for this situation, the upstream code expects only the new location to be in the notification message
					locationUpdateMessage = location;
				} else {
					locationUpdateMessage = Utility.getModMessage("Location", oldLocation, location);
				}
				String spacer = "";
				if(notificationMessage.length() > 0) {
					spacer = " ";
				}
				notificationMessage = notificationMessage + spacer + locationUpdateMessage;
			}
			theGame.setLocationName(location);
		}
		
		// if new field is empty, field is cleared.
		if(opponent != null) {
			String oldOpponent = theGame.getOpponent() != null ? theGame.getOpponent() : "";
			if(!opponent.equalsIgnoreCase(oldOpponent)) {
				String opponentMessage = Utility.getModMessage("Opponent", oldOpponent, opponent);
				log.debug(opponentMessage);
				notificationMessage = notificationMessage + " " + opponentMessage;
			}
			theGame.setOpponent(opponent);
		}
		
		if(scoreUs != null) {
			theGame.setScoreUs(scoreUs);
		}
		
		if(scoreThem != null) {
			theGame.setScoreThem(scoreThem);
		}
		
		Boolean gameJustCompleted = false;
		if(interval != null) {
			if(interval.equals(Game.GAME_OVER_INTERVAL)) {
				Integer oldGameInterval = theGame.getInterval();
				if(oldGameInterval == null || !oldGameInterval.equals(interval)) {
					gameJustCompleted = true;
				}
			} else if(interval.equals(Game.GAME_CANCELED_INTERVAL)) {
				
			}
			theGame.setInterval(interval);
		}
		
		if(pollStatus != null) {
			String oldPollStatus = theGame.getPollStatus();
			if(pollStatus.equalsIgnoreCase(Game.CLOSED_POLL_STATUS) && !pollStatus.equalsIgnoreCase(oldPollStatus)) {
				pollHasBeenClosed = true;
			} else if(pollStatus.equalsIgnoreCase(Game.OPEN_POLL_STATUS) && !pollStatus.equalsIgnoreCase(oldPollStatus)) {
				pollReopened = true;
			}
			theGame.setPollStatus(pollStatus);
		}
		
    	// if the voting "poll" has closed, then set the MVP inside the Game entity (if there is one)
		String gameMvp = "";
    	if(pollHasBeenClosed) {
    		String mvpMemberId = GameVote.getMvp(theGame);
    		if(mvpMemberId != null) {
    			Member member = Member.getMember(mvpMemberId);
    			if(member != null) {
    				gameMvp = member.getDisplayName();
    				theGame.setMvpDisplayName(gameMvp);
    				theGame.setMvpMemberId(KeyFactory.keyToString(member.getKey()));
    				log.debug("game updated with MVP -- displayName = " + gameMvp);
    			}
    		}
    	} else if(pollReopened) {
			theGame.setMvpDisplayName(null);
			theGame.setMvpMemberId(null);
    	}
		
		// init out parameter and then return success - since we made it to the end
		theNotificationInfo.add(notificationType);
		theNotificationInfo.add(notificationMessage);
		theNotificationInfo.add(gameMvp);
		String updateAllStr = (updateAll == null || updateAll.equals(false)) ? "false" : "true";
		theNotificationInfo.add(updateAllStr);
		String gameJustCompletedStr = gameJustCompleted ? "true" : "false";
		theNotificationInfo.add(gameJustCompletedStr);
		return ApiStatusCode.SUCCESS;
    }
}