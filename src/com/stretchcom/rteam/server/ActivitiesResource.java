package com.stretchcom.rteam.server;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.Transform;

 
/** 
 * Resource that manages a members of a team. 
 *  
 */  
public class ActivitiesResource extends ServerResource {  
	//private static final Logger log = Logger.getLogger(ActivitiesResource.class.getName());
	private RskyboxClient log = new RskyboxClient(this);
	
	private static final Integer DEFAULT_MAX_COUNT  = 45;
	private static final Integer MAX_MAX_COUNT  = 200;
	private static final Integer NUM_OF_DAYS_IN_ACTIVITY_SEARCH  = 30;
	private static final Integer MAX_NUM_ACTIVITY_SEARCHES  = 12;
	
	private static final Long ONE_MINUTE_IN_MILLI_SECONDS = 60000L;
	
	String teamId;
	String timeZoneStr;
	String refreshFirstStr;
	String newOnlyStr;
	String maxCountStr;
	String maxCacheIdStr;
	String mostCurrentDateStr;
	String totalNumberOfDaysStr;
	String userVote;
	String includeDetailsStr;
	String detailsStr;
	String activityIdsStr;
	String eventIdStr;
	String eventTypeStr;
	String mediaOnlyStr;
  
    @Override  
    protected void doInit() throws ResourceException {
    	log.info("API requested with URL: " + this.getReference().toString());
    	
        // Get the "teamId" attribute value taken from the URI template /team/{teamId} 
        this.teamId = (String)getRequest().getAttributes().get("teamId"); 
        log.debug("ActivitiesResource:doInit() - teamId = " + this.teamId);
        if(this.teamId != null) {
            this.teamId = Reference.decode(this.teamId);
            log.debug("ActivitysResource:doInit() - decoded teamId = " + this.teamId);
        }
        
        this.timeZoneStr = (String)getRequest().getAttributes().get("timeZone"); 
        log.debug("ActivitiesResource:doInit() - timeZone = " + this.timeZoneStr);
        if(this.timeZoneStr != null) {
            this.timeZoneStr = Reference.decode(this.timeZoneStr);
            log.debug("ActivitiesResource:doInit() - decoded timeZone = " + this.timeZoneStr);
        }
        
        this.detailsStr = (String)getRequest().getAttributes().get("details"); 
        log.debug("ActivitiesResource:doInit() - details = " + this.detailsStr);
        if(this.detailsStr != null) {
            this.detailsStr = Reference.decode(this.detailsStr);
            log.debug("ActivitiesResource:doInit() - decoded details = " + this.detailsStr);
        }
        
        this.userVote = (String)getRequest().getAttributes().get("userVote"); 
        log.debug("ActivitiesResource:doInit() - userVote = " + this.userVote);
        if(this.userVote != null) {
            this.userVote = Reference.decode(this.userVote);
            log.debug("ActivitiesResource:doInit() - decoded userVote = " + this.userVote);
        }
        
		Form form = getRequest().getResourceRef().getQueryAsForm();
		for (Parameter parameter : form) {
			log.debug("parameter " + parameter.getName() + " = " + parameter.getValue());
			if(parameter.getName().equals("refreshFirst")) {
				this.refreshFirstStr = (String)parameter.getValue();
				this.refreshFirstStr = Reference.decode(this.refreshFirstStr);
				log.debug("ActivitiesResource:doInit() - decoded refreshFirstStr = " + this.refreshFirstStr);
			} else if(parameter.getName().equals("newOnly")) {
				this.newOnlyStr = (String)parameter.getValue();
				this.newOnlyStr = Reference.decode(this.newOnlyStr);
				log.debug("ActivitiesResource:doInit() - decoded newOnlyStr = " + this.newOnlyStr);
			} else if(parameter.getName().equals("maxCount")) {
				this.maxCountStr = (String)parameter.getValue();
				this.maxCountStr = Reference.decode(this.maxCountStr);
				log.debug("ActivitiesResource:doInit() - decoded maxCountStr = " + this.maxCountStr);
			} else if(parameter.getName().equals("maxCacheId")) {
				this.maxCacheIdStr = (String)parameter.getValue();
				this.maxCacheIdStr = Reference.decode(this.maxCacheIdStr);
				log.debug("ActivitiesResource:doInit() - decoded maxCacheIdStr = " + this.maxCacheIdStr);
			} else if(parameter.getName().equals("mostCurrentDate")) {
				this.mostCurrentDateStr = (String)parameter.getValue();
				this.mostCurrentDateStr = Reference.decode(this.mostCurrentDateStr);
				log.debug("ActivitiesResource:doInit() - decoded mostCurrentDateStr = " + this.mostCurrentDateStr);
			} else if(parameter.getName().equals("totalNumberOfDays")) {
				this.totalNumberOfDaysStr = (String)parameter.getValue();
				this.totalNumberOfDaysStr = Reference.decode(this.totalNumberOfDaysStr);
				log.debug("ActivitiesResource:doInit() - decoded totalNumberOfDaysStr = " + this.totalNumberOfDaysStr);
			} else if(parameter.getName().equals("includeDetails")) {
				this.includeDetailsStr = (String)parameter.getValue();
				this.includeDetailsStr = Reference.decode(this.includeDetailsStr);
				log.debug("ActivitiesResource:doInit() - decoded includeDetails = " + this.includeDetailsStr);
			} else if(parameter.getName().equals("activityIds")) {
				this.activityIdsStr = (String)parameter.getValue();
				this.activityIdsStr = Reference.decode(this.activityIdsStr);
				log.debug("ActivitiesResource:doInit() - decoded activityIdsStr = " + this.activityIdsStr);
			} else if(parameter.getName().equals("eventId")) {
				this.eventIdStr = (String)parameter.getValue();
				this.eventIdStr = Reference.decode(this.eventIdStr);
				log.debug("ActivitiesResource:doInit() - decoded eventIdStr = " + this.eventIdStr);
			} else if(parameter.getName().equals("eventType")) {
				this.eventTypeStr = (String)parameter.getValue();
				this.eventTypeStr = Reference.decode(this.eventTypeStr);
				log.debug("ActivitiesResource:doInit() - decoded eventTypeStr = " + this.eventTypeStr);
			}  else if(parameter.getName().equals("mediaOnly")) {
				this.mediaOnlyStr = (String)parameter.getValue();
				this.mediaOnlyStr = Reference.decode(this.mediaOnlyStr);
				log.debug("ActivitiesResource:doInit() - decoded mediaOnlyStr = " + this.mediaOnlyStr);
			}
		}
    }  
    
    
    // Handles 'Create a new activity' API  
    // Handles 'Get Status of Activities for User' API (but most is delegated to another method)
    @Post  
    public JsonRepresentation createActivity(Representation entity) {
    	JSONObject jsonReturn = new JSONObject();
    	log.debug("createActivity(@Post) entered ..... ");
		EntityManager em = EMF.get().createEntityManager();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_CREATED);
		User currentUser = null;
        try {
    		currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				log.error("ActivitiesResource:createActivity:currentUser", "error converting json representation into a JSON object");
				return Utility.apiError(null);
    		}
    		//::BUSINESSRULE:: user must be network authenticated to send a message
    		else if(!currentUser.getIsNetworkAuthenticated()) {
    			return Utility.apiError(ApiStatusCode.USER_NOT_NETWORK_AUTHENTICATED);
    		}
    		// teamId is required if this is 'Create a new activity' API
    		else if(this.userVote == null) {
        		if(this.teamId == null || this.teamId.length() == 0) {
        			return Utility.apiError(ApiStatusCode.TEAM_ID_REQUIRED);
        		} else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
        			return Utility.apiError(ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM);
            	}
    		}
			
    		JsonRepresentation jsonRep = new JsonRepresentation(entity);
			JSONObject json = jsonRep.getJsonObject();

			//////////////////////////////////////////
    		// 'Get Status of Activities for User' API
    		//////////////////////////////////////////
    		if(this.userVote != null) {
    			// remainder of processing for this API is delegated to another method
    			getStatusOfActivitiesForUser(currentUser, json, jsonReturn);
    			return new JsonRepresentation(jsonReturn);
    		}
    		
			Team team = (Team)em.createNamedQuery("Team.getByKey")
				.setParameter("key", KeyFactory.stringToKey(this.teamId))
				.getSingleResult();
			log.debug("team retrieved = " + team.getTeamName());
			
			String statusUpdate = null;
			if(json.has("statusUpdate")) {
				statusUpdate = json.getString("statusUpdate");
			}
			
			String photoBase64 = null;
			if(json.has("photo")) {
				photoBase64 = json.getString("photo");
			}
			
			Boolean isPortrait = null;
			if(json.has("isPortrait")) {
				isPortrait = json.getBoolean("isPortrait");
				log.debug("json isPortrait = " + isPortrait);
			}

			String videoBase64 = null;
			if(json.has("video")) {
				videoBase64 = json.getString("video");
			}

			String parentActivityId = null;
			if(json.has("parentActivityId")) {
				parentActivityId = json.getString("parentActivityId");
			}
			
			String eventIdStr = null;
			if(json.has("eventId")) {
				eventIdStr = json.getString("eventId");
			}

			String eventTypeStr = null;
			if(json.has("eventType")) {
				eventTypeStr = json.getString("eventType");
			}
			
			String gameDay = null;
			if(json.has("gameDay")) {
				gameDay = json.getString("gameDay");
			}
			
			// TODO: for Android and rScoreboard, default the time zone for now if not provided
			// Time zone only used if eventId not provided
			String timeZoneStr = "America/Chicago";
			TimeZone tz = null;
			if(eventIdStr == null) {
				if(json.has("timeZone")) {
	    			timeZoneStr = json.getString("timeZone");
				}
		    	tz = GMT.getTimeZone(timeZoneStr);
				if(tz == null) {
					return Utility.apiError(ApiStatusCode.INVALID_TIME_ZONE_PARAMETER);
				}
			}
			
			// Enforce Rules
			List eventInfo = null;
			if((statusUpdate == null || statusUpdate.length() == 0) && (photoBase64 == null || photoBase64.length() == 0)) {
				return Utility.apiError(ApiStatusCode.STATUS_UPDATE_OR_PHOTO_REQUIRED);
			} else if(statusUpdate != null && statusUpdate.length() > TwitterClient.MAX_TWITTER_CHARACTER_COUNT){
				return Utility.apiError(ApiStatusCode.INVALID_STATUS_UPDATE_MAX_SIZE_EXCEEDED);
			} else if(videoBase64 != null && photoBase64 == null) {
				return Utility.apiError(ApiStatusCode.VIDEO_AND_PHOTO_MUST_BE_SPECIFIED_TOGETHER);
			} else if(photoBase64 != null && isPortrait == null) {
				return Utility.apiError(ApiStatusCode.IS_PORTRAIT_AND_PHOTO_MUST_BE_SPECIFIED_TOGETHER);
			} else if(eventIdStr != null) {
				if(eventTypeStr == null) {
					return Utility.apiError(ApiStatusCode.EVENT_ID_AND_EVENT_TYPE_MUST_BE_SPECIFIED_TOGETHER);
				}
				eventInfo = Practice.getEventInfo(eventIdStr, eventTypeStr);
				if(eventInfo == null || eventInfo.size() == 0) {
					return Utility.apiError(ApiStatusCode.EVENT_NOT_FOUND);
				}
			}
			
			Activity parentActivity = null;
			if(parentActivityId != null) {
				try {
					parentActivity = (Activity)em.createNamedQuery("Activity.getByKey")
							.setParameter("key", KeyFactory.stringToKey(parentActivityId))
							.getSingleResult();
					log.debug("parent activity retrieved successfully");
				
					if(parentActivity.getParentActivityId() != null) {
						// parent activity is itself a reply -- not allowed!
						return Utility.apiError(ApiStatusCode.INVALID_ACTIVITY_ID_PARAMETER);
					}
				} catch (NoResultException e) {
					return Utility.apiError(ApiStatusCode.ACTIVITY_NOT_FOUND);
				} catch (NonUniqueResultException e) {
					log.exception("ActivityResource:createActivity:NonUniqueResultException", "", e);
		        	this.setStatus(Status.SERVER_ERROR_INTERNAL);
		        	return Utility.apiError(null);
				}
			}
			
			if(statusUpdate == null) {statusUpdate = "";}
			
			///////////////////////////////////////////////////////////////////
			// Cache the activity post whether the team is using Twitter or not
			///////////////////////////////////////////////////////////////////
			// abbreviate only if necessary
			if(statusUpdate.length() > TwitterClient.MAX_TWITTER_CHARACTER_COUNT) {
				statusUpdate = Language.abbreviate(statusUpdate);
			}
				
			Activity newActivity = new Activity();
			newActivity.setText(statusUpdate);
			newActivity.setCreatedGmtDate(new Date());
			newActivity.setTeamId(this.teamId);
			newActivity.setTeamName(team.getTeamName());
			newActivity.setSport(team.getSport());
			newActivity.setContributor(currentUser.getFullName());
			newActivity.setUserId(KeyFactory.keyToString(currentUser.getKey()));
			newActivity.setParentActivityId(parentActivityId);
			if(parentActivityId != null) newActivity.setIsReply(true);
			
			// associate this activity with an event if appropriate
			List<Game> todaysGames = null;
			Boolean gameDayChecked = false;
			if(eventIdStr != null) {
				newActivity.setEventId(eventIdStr);
				newActivity.setEventType(eventTypeStr);
				newActivity.setEventStartGmtDate((Date)(eventInfo.get(0)));
				newActivity.setEventDescription((String)(eventInfo.get(3)));
			} else {
				// see if today is GAMEDAY
				// TODO eventually support other types of events beside Game
				todaysGames = Game.getTodaysGames(team, tz);
				gameDayChecked = true;
				if(todaysGames.size() > 0) {
					// for now, just associate with the first game in the list
					Game todayGame = todaysGames.get(0);
					newActivity.setEventId(KeyFactory.keyToString(todayGame.getKey()));
					newActivity.setEventType(Practice.GAME_EVENT_TYPE);
					newActivity.setEventStartGmtDate(todayGame.getEventGmtStartDate());
					newActivity.setEventDescription(todayGame.getDescription());
				}
			}
			
			// cacheId held in team is the last used.
			Long cacheId = team.getNewestCacheId() + 1;
			newActivity.setCacheId(cacheId);
			team.setNewestCacheId(cacheId);
			
			// Only send activity to Twitter if team is using Twitter
			twitter4j.Status twitterStatus = null;
			if(team.getUseTwitter()) {
				// TODO once twitter API supports the meta data, then user name will not have to be inserted into update
				// Poster's name is added to update before sending to twitter
				if(statusUpdate.length() == 0) {
					statusUpdate = currentUser.getFullName() + " shared a photo fr loc " + TF.getPassword();
				} else {
					statusUpdate = currentUser.getDisplayName() + " post: " + statusUpdate;
				}

				// truncate if necessary
				if(statusUpdate.length() > TwitterClient.MAX_TWITTER_CHARACTER_COUNT) {
					statusUpdate = statusUpdate.substring(0, TwitterClient.MAX_TWITTER_CHARACTER_COUNT - 2) + "..";
				}

				Long parentTwitterId = null;
				if(parentActivity != null) {
					parentTwitterId = parentActivity.getTwitterId();
				}
				twitterStatus = TwitterClient.updateStatus(statusUpdate, parentTwitterId, team.getTwitterAccessToken(), team.getTwitterAccessTokenSecret());
				
				// if Twitter update failed, log error, but continue because activity post will be stored by rTeam
				if(twitterStatus == null) {
					log.debug("Twitter update failed, but continuing on ...");
					//apiStatus = ApiStatusCode.TWITTER_ERROR;
				} else {
					newActivity.setTwitterId(twitterStatus.getId());
					// if posted to twitter, match the exact twitter date
					newActivity.setCreatedGmtDate(twitterStatus.getCreatedAt());
				}
			}
			
			EntityManager emActivity = EMF.get().createEntityManager();
			try {
				if(photoBase64 != null) {
					// decode the base64 encoding to create the thumb nail
					byte[] rawPhoto = Base64.decodeBase64(photoBase64);
					ImagesService imagesService = ImagesServiceFactory.getImagesService();
					Image oldImage = ImagesServiceFactory.makeImage(rawPhoto);
					
					int tnWidth = isPortrait == true ? Activity.THUMB_NAIL_SHORT_SIDE : Activity.THUMB_NAIL_LONG_SIDE;
					int tnHeight = isPortrait == true ? Activity.THUMB_NAIL_LONG_SIDE : Activity.THUMB_NAIL_SHORT_SIDE;
					Transform resize = ImagesServiceFactory.makeResize(tnWidth, tnHeight);
					Image newImage = imagesService.applyTransform(resize, oldImage);
					String thumbNailBase64 = Base64.encodeBase64String(newImage.getImageData());
					newActivity.setThumbNailBase64(thumbNailBase64);
					newActivity.setPhotoBase64(photoBase64);
					if(videoBase64 != null) newActivity.setVideoBase64(videoBase64);
					newActivity.updateMediaCount();
				}
				
				emActivity.persist(newActivity);
				log.debug("new Activity was successfully persisted");
			} catch(Exception e) {
				log.exception("ActivitiesResource:createActivity:Exception", "", e);
			} finally {
				emActivity.close();
			}
			
			if(gameDay != null) {
				// check if team has a game scheduled today
				if(!gameDayChecked) {
					todaysGames = Game.getTodaysGames(team, tz);
				}
				if(todaysGames.size() == 0) {
					// need to create a game
					log.debug("no GameDay game, so creating one ...");
					Game newGameDayGame = Game.createGameDayGame(team, timeZoneStr);

					//::BUSINESS_RULE:: coordinator must be network authenticated for a notification to be sent
					if(currentUser.getIsNetworkAuthenticated()) {
						List<Game> games = new ArrayList<Game>();
						games.add(newGameDayGame);
						PubHub.sendNewEventMessage(team.getMembers(), currentUser, team, games, null, MessageThread.PLAIN_TYPE);
					}
				}
			}
		} catch (IOException e) {
			log.exception("ActivitiesResource:createActivity:IOException", "error extracting JSON object from Post", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (JSONException e) {
			log.exception("ActivitiesResource:createActivity:JSONException", "error converting json representation into a JSON object", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (NoResultException e) {
			log.exception("ActivitiesResource:createActivity:NoResultException", "team not found", e);
			apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
		} catch (NonUniqueResultException e) {
			log.exception("ActivitiesResource:createActivity:NonUniqueResultException", "two or more teams have same team id", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} finally {
		    em.close();
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("ActivitiesResource:createActivity:JSONException2", "error converting json representation into a JSON object", e);
		}
		return new JsonRepresentation(jsonReturn);
    }
    
    
    
    // Implementation Notes:
    // ---------------------
    // newestTwitterId: Only used by Twitter refresh code. Used as a marker to remember the last Twitter update retrieved during
    //                  a refresh. Since direct Twitter posts are still supported, refreshes must walk through all twitter posts
    //                  to see if any of them have not been cached yet by rTeam. Most of course will already be cached, but not
    //                  direct Twitter posts. This variable allows the refresh code to request only Twitter posts that have not
    //                  yet examined for direct Twitter posts that haven't been cached yet by rTeam.
    //
    // newestCacheId:   Stores the last used cache ID.  To add a new activity to the cache, this variable is incremented before
    //                  it is used. Any code that adds activities to the cache must updated the team.newestCacheId. This includes
    //                  Create Activity API code and the refresh code in Get Activities APIs below.
    //
    // lastTwitterRefresh:  A time stamp store in Team entity to remember when the last Twitter refresh was done. Current
    //                      algorithm will only allow one Twitter refresh every 60 seconds.
    //
    // teamNewestCacheIds:  The user entity hold a newest cached ID for each of the user's teams.  Used to determine if a user
    //                      has new activity.

    // Handles 'Get activities for a team' API  
    // Handles 'Get activities for all teams' API
    // Handles 'Get activities details' API
    @Get  
    public JsonRepresentation getActivities(Variant variant) {
    	log.debug("getActivities(@Get) entered ..... ");
    	JSONObject jsonReturn = new JSONObject();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		User currentUser = null;
		TimeZone tz = null;
		// teamId is provided only if getting activities for a single team. 
		boolean isGetActivitiesForAllTeamsApi = this.teamId == null;
        try {
    		currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);

    		if(this.teamId != null && currentUser == null) {
    			// TODO -- have browser send timezone for user requesting rScoreboard
    			this.timeZoneStr = "America/Chicago";
    		}
    		
    		//::BUSINESSRULE:: user must be network authenticated to get activities
    		if(currentUser != null && !currentUser.getIsNetworkAuthenticated()) {
				return Utility.apiError(ApiStatusCode.USER_NOT_NETWORK_AUTHENTICATED);
    		}
    		//::BUSINESSRULE:: user must be a member of the team, if teamId was specified
    		else if(this.teamId != null && currentUser != null && !currentUser.isUserMemberOfTeam(this.teamId)) {
				return Utility.apiError(ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM);
        	}
    		// timeZone check 
    		else if(this.timeZoneStr == null || this.timeZoneStr.length() == 0) {
				return Utility.apiError(ApiStatusCode.TIME_ZONE_REQUIRED);
    		} else {
    			tz = GMT.getTimeZone(this.timeZoneStr);
    			if(tz == null) {
    				return Utility.apiError(ApiStatusCode.INVALID_TIME_ZONE_PARAMETER);
    			}
    		}
    		
    		// handle 'Get activities details' API
    		if(this.activityIdsStr != null) {
    			return handleActivitiesDetailsApi(tz, currentUser);
    		}
			
    		//////////////////////////////////////
			// verify and default input parameters
    		//////////////////////////////////////
			boolean refreshFirst = false;
			if(this.refreshFirstStr != null) {
				if(refreshFirstStr.equalsIgnoreCase("true")) {
					refreshFirst = true;
				} else if(refreshFirstStr.equalsIgnoreCase("false")) {
					refreshFirst = false;
				} else {
    				return Utility.apiError(ApiStatusCode.INVALID_REFRESH_FIRST_PARAMETER);
				}
			}
			
			boolean newOnly = false;
			if(this.newOnlyStr != null) {
				if(newOnlyStr.equalsIgnoreCase("true")) {
					newOnly = true;
				} else if(newOnlyStr.equalsIgnoreCase("false")) {
					newOnly = false;
				} else {
    				return Utility.apiError(ApiStatusCode.INVALID_NEW_ONLY_PARAMETER);
				}
			}
			
			int maxCount = DEFAULT_MAX_COUNT;
			if(maxCountStr != null) {
				try {
					maxCount = new Integer(maxCountStr);
					if(maxCount > MAX_MAX_COUNT) maxCount = MAX_MAX_COUNT;
				} catch (NumberFormatException e) {
    				return Utility.apiError(ApiStatusCode.INVALID_MAX_COUNT_PARAMETER);
				}
			}
			
			Boolean mediaOnly = false;
			if(mediaOnlyStr != null && mediaOnlyStr.equalsIgnoreCase("true")) {
				mediaOnly = true;
			}
			
			///////////////////////////////////
			// some parameters are API specific
			///////////////////////////////////
			Long maxCacheId = null;
			Date mostCurrentDate = null;
			Integer totalNumberOfDays = null;
			if(isGetActivitiesForAllTeamsApi) {
				////////////////////////////////////////////////////////
				// Get Activity for All Teams: Parameters and Validation
				////////////////////////////////////////////////////////
				if(mostCurrentDateStr != null) {
					mostCurrentDate = GMT.convertToGmtDate(mostCurrentDateStr, false, tz);
					if(mostCurrentDate == null) {
	    				return Utility.apiError(ApiStatusCode.INVALID_MOST_CURRENT_DATE_PARAMETER);
					}
				}
				
				if(this.totalNumberOfDaysStr != null) {
					try {
						totalNumberOfDays = new Integer(this.totalNumberOfDaysStr);
					} catch (NumberFormatException e) {
	    				return Utility.apiError(ApiStatusCode.INVALID_TOTAL_NUMBER_OF_DAYS_PARAMETER);
					}
				}
				
				//::BUSINESSRULE:: if newOnly=true, then refreshFirst must also be true
				if(newOnly && !refreshFirst) {
    				return Utility.apiError(ApiStatusCode.REFRESH_FIRST_AND_NEW_ONLY_MUST_BE_SPECIFIED_TOGETHER);
				}
			
				//::BUSINESSRULE:: if newOnly=false, date interval must be specified
				if(!newOnly && mostCurrentDate == null) {
    				return Utility.apiError(ApiStatusCode.DATE_INTERVAL_REQUIRED);
				}

				//::BUSINESSRULE:: newOnly=true is mutually exclusive with date interval
				if(newOnly && (mostCurrentDate != null || totalNumberOfDays != null)) {
    				return Utility.apiError(ApiStatusCode.NEW_ONLY_AND_DATE_INTERVAL_MUTUALLY_EXCLUSIVE);
				}
			} else {
				///////////////////////////////////////////////////////////////////////
				// Get Activity for a Single, Specified Team: Parameters and Validation
				///////////////////////////////////////////////////////////////////////
				if(maxCacheIdStr != null) {
					try {
						maxCacheId = new Long(maxCacheIdStr);
					} catch (NumberFormatException e) {
	    				return Utility.apiError(ApiStatusCode.INVALID_MAX_CACHE_ID_PARAMETER);
					}
				}
				
				//::BUSINESSRULE:: refreshFirst=true is mutually exclusive with maxCacheId
				if(refreshFirst && maxCacheId != null) {
    				return Utility.apiError(ApiStatusCode.REFRESH_FIRST_AND_MAX_CACHE_ID_MUTUALLY_EXCLUSIVE);
				} else if(eventIdStr != null) {
					if(eventTypeStr == null) {
						return Utility.apiError(ApiStatusCode.EVENT_ID_AND_EVENT_TYPE_MUST_BE_SPECIFIED_TOGETHER);
					}
					
					List eventInfo = Practice.getEventInfo(eventIdStr, eventTypeStr);
					if(eventInfo == null || eventInfo.size() == 0) {
						return Utility.apiError(ApiStatusCode.EVENT_NOT_FOUND);
					}
				}
			}
			
			List<Team> teams = new ArrayList<Team>();
			List<Key> teamKeys = null;
			EntityManager em = EMF.get().createEntityManager();
			if(isGetActivitiesForAllTeamsApi) {
				teamKeys = currentUser.getTeams();
				if(teamKeys.size() > 0) {
					//::JPA_BUG:: ?? if I get all teams by passing  in a list of keys, the list of teams is not in same order as keys!!!!
//	    			List<Team> teams = (List<Team>) em.createQuery("select from " + Team.class.getName() + " where key = :keys")
//						.setParameter("keys", teamKeys)
//						.getResultList();
					
					for(Key tk : teamKeys) {
						Team aTeam = null;
						try {
							 aTeam = (Team)em.createNamedQuery("Team.getByKey")
								.setParameter("key", tk)
								.getSingleResult();
						} catch(Exception e) {
							log.exception("ActivitiesResource:getActivities:Exception", "Could not find team with the team key", e);
						}
						if(aTeam != null) {teams.add(aTeam);}
					}
					log.debug("number of teams retrieved for current user = " + teams.size());
				} else {
					log.debug("user has no teams");
				}
			} else {
				try {
	    			String tempTeamId = null;
	    			Team team = null;
	    			if(this.teamId.length() <= UrlShort.MAX_ID_SIZE) {
	    				team = (Team)em.createNamedQuery("Team.getByPageUrl")
	    						.setParameter("pageUrl", this.teamId)
	    						.getSingleResult();
	    				tempTeamId = KeyFactory.keyToString(team.getKey());
	    			} else {
	    				tempTeamId = this.teamId;
	    			}
					
 					team = (Team)em.createNamedQuery("Team.getByKey")
 						.setParameter("key", KeyFactory.stringToKey(tempTeamId))
 						.getSingleResult();
 					log.debug("team retrieved = " + team.getTeamName());
 					teams.add(team);
 				} catch (NoResultException e) {
    				return Utility.apiError(ApiStatusCode.TEAM_NOT_FOUND);
 				} catch (NonUniqueResultException e) {
					log.exception("ActivitiesResource:getActivities:NonUniqueResultException", "two teams have the same key", e);
    				return Utility.apiError(null);
 				}
			}
    		
			List<Activity> allTeamsRequestedActivities = new ArrayList<Activity>();
			Boolean mustMatchMaxCount = false;
			Boolean searchMaxedOut = false;
			int numOfSearches = 0;
			Date leastCurrentDate = null;
			
			// setup to possible go through the 'teams' for loop multiple times. Only done if this isGetActivitiesForAllTeamsApi 
			if(isGetActivitiesForAllTeamsApi) {
				if(totalNumberOfDays == null) {
					log.debug("activity query is being driven by maxCount, not totalNumberOfDays");
					totalNumberOfDays = NUM_OF_DAYS_IN_ACTIVITY_SEARCH;
					mustMatchMaxCount = true;
				} 
				leastCurrentDate = GMT.subtractDaysFromDate(mostCurrentDate, totalNumberOfDays-1);
			}
			
			// Only loop through 'teams' for loop if 'mustMatchMaxCount is true
			while(true) { //###################################################################################################################################
				// 'teams' will either be one team if only getting activity for a single team or all the user's teams if that's what was requested
				for(Team userTeam : teams) { //****************************************************************************************************************
					Boolean teamUsesTwitter = userTeam.getUseTwitter() != null && userTeam.getUseTwitter();
					
					List<Activity> teamRequestedActivities = null;
					Long cacheId = userTeam.getNewestCacheId();
					
					////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
					// If a refresh has been requested and team uses Twitter, get the latest activities from Twitter and store in cache
					////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
					if(teamUsesTwitter && refreshFirst) {
						log.debug("refreshFirst is true - updating the cache");
						
						if(newOnly) {teamRequestedActivities = new ArrayList<Activity>();}

						// Twitter refresh is done at most once per minute, so see if the refresh has been done in the last minute.
						Date lastRefreshDate = userTeam.getLastTwitterRefresh();
						Long howLongSinceLastRefresh = null;
						Date now = new Date();
						if(lastRefreshDate != null) {
							howLongSinceLastRefresh = now.getTime() - lastRefreshDate.getTime();
							log.debug("howLongSinceLastRefresh = " + howLongSinceLastRefresh);
						} else {
							log.debug("lastTwitterRefresh in User null, so refresh will proceed"); 
						}
						
						if(lastRefreshDate == null || (howLongSinceLastRefresh > ONE_MINUTE_IN_MILLI_SECONDS)) {
							log.debug("has been over a minute so do a Twitter refresh");
							Long newestTwitterId = userTeam.getNewestTwitterId();
							List<Activity> twitterActivities = TwitterClient.getTeamActivities(userTeam, newestTwitterId);
							if(twitterActivities == null) {
			    				return Utility.apiError(ApiStatusCode.TWITTER_ERROR);
							} 
							
							// because twitterActivities doesn't have the cacheId set, this will get sorted by twitterId
							Collections.sort(twitterActivities);
							
							////////////////////////////////////////////////////////////////////////////////////////
							// persist the activities retrieved from Twitter that aren't already stored in the cache
							////////////////////////////////////////////////////////////////////////////////////////
							int requestedActivityCount = maxCount;

							Long largestTwitterId = newestTwitterId;
							log.debug("before processing activities, newestTwitterId = " + newestTwitterId);
							EntityManager em0 = EMF.get().createEntityManager();
							try {
								for(Activity a: twitterActivities) {
									em0.getTransaction().begin();
									try {
										Activity precachedActivity = (Activity)em0.createNamedQuery("Activity.getByTwitterId")
											.setParameter("twitterId", a.getTwitterId())
											.getSingleResult();
										// if already cached, there is no work to do ...
									} catch (NoResultException e) {
						            	// not an error - we have found a Twitter update that was a direct post to Twitter
										log.debug("uncached activity retrieved with twitter ID = " + a.getTwitterId());
										if(newOnly && requestedActivityCount != 0) {
											teamRequestedActivities.add(a);
											requestedActivityCount--;
										}
										
										cacheId += 1;
										a.setCacheId(cacheId);
										a.setContributor(RteamApplication.TWITTER_POST);
										em0.persist(a);
										if(a.getTwitterId() > largestTwitterId) {largestTwitterId = a.getTwitterId();}
						    		} catch (NonUniqueResultException e) {
										log.exception("ActivitiesResource:getActivities:NonUniqueResultException2", "two or more activities have the same twitter ID", e);
						    		}
									em0.getTransaction().commit();
								}
								log.debug("after processing activities, largestTwitterId = " + largestTwitterId);
								newestTwitterId = largestTwitterId;
							} finally {
								em0.close();
							}
							
							// Update team in a separate transaction
							// at this point, newestTwitterId holds the largest, most recent Twitter Id
							// at this point, cachId holds the largest, most recent cache Id
							EntityManager em2 = EMF.get().createEntityManager();
							try {
								em2.getTransaction().begin();
								Team teamInTransaction = (Team)em2.createNamedQuery("Team.getByKey")
									.setParameter("key", userTeam.getKey())
									.getSingleResult();
								log.debug("team2 retrieved = " + teamInTransaction.getTeamName());
								
								// update the activity IDs
								teamInTransaction.setNewestCacheId(cacheId);
								teamInTransaction.setNewestTwitterId(newestTwitterId);
								teamInTransaction.setLastTwitterRefresh(new Date());
								em2.getTransaction().commit();
							} catch(Exception e) {
								log.exception("ActivitiesResource:getActivities:Exception2", "Could not find team using teamKey from User entity", e);
								// no matter what, the teamsWithPossibleUpdates team list MUST be complete if this is a refresh!
							} finally {
							    if (em2.getTransaction().isActive()) {
							        em2.getTransaction().rollback();
							    }
							    em2.close();
							}
						}
					} // end of if(teamUsesTwitter && refreshFirst)
					
					// If this team uses Twitter and this is a refreshFirst with newOnly, then teamRequestedActivities 
					// already initialized in the code above.
					if(!(teamUsesTwitter && refreshFirst && newOnly)) {
						if(refreshFirst && newOnly) {
							// teamUsesTwitter must be false. If no Twitter, then a refreshFirst-newOnly request has no work to do,
							// but must create a teamRequestedActivities for code below, so make the empty list.
							teamRequestedActivities = new ArrayList<Activity>();
						} else {
							// To get into this leg of code, newOnly must be FALSE
							
							//////////////////////////////////////////////////////////////
							// Build the teamRequestedActivities list from the local cache
							//////////////////////////////////////////////////////////////
							EntityManager em3 = EMF.get().createEntityManager();
							try {
								log.debug("getting activities from the cache ...");
								//////////////////////////////////////////////////////////////////////
								// return activities from cache (which may include some new stuff too)
								//////////////////////////////////////////////////////////////////////
								if(isGetActivitiesForAllTeamsApi) {
									if(mediaOnly) {
										teamRequestedActivities = (List<Activity>)em3.createNamedQuery("Activity.getByTeamIdAndUpperAndLowerCreatedDatesAndMediaOnly")
												.setParameter("teamId", KeyFactory.keyToString(userTeam.getKey()))
												.setParameter("mostCurrentDate", GMT.setTimeToEndOfTheDay(mostCurrentDate))
												.setParameter("leastCurrentDate", GMT.setTimeToTheBeginningOfTheDay(leastCurrentDate))
												.getResultList();
									} else {
										teamRequestedActivities = (List<Activity>)em3.createNamedQuery("Activity.getByTeamIdAndUpperAndLowerCreatedDates")
												.setParameter("teamId", KeyFactory.keyToString(userTeam.getKey()))
												.setParameter("mostCurrentDate", GMT.setTimeToEndOfTheDay(mostCurrentDate))
												.setParameter("leastCurrentDate", GMT.setTimeToTheBeginningOfTheDay(leastCurrentDate))
												.getResultList();
									}
								} else {
									/////////////////////////////////////
									// get activities for specified event
									/////////////////////////////////////
									if(this.eventIdStr != null) {
										teamRequestedActivities = (List<Activity>)em3.createNamedQuery("Activity.getByEventIdAndEventType")
												.setParameter("eventId", this.eventIdStr)
												.setParameter("eventType", this.eventTypeStr)
												.getResultList();
									} else {
										//////////////////////////////////
										// get activities by cacheId range
										//////////////////////////////////
										Long upperCacheId = null; // non-inclusive, upper cache ID used in activity query
										Long lowerCacheId = null; // 
										if(maxCacheId != null) {
											// typically used to request activities that are not the most recent
											if(maxCacheId > cacheId + 1) {maxCacheId = cacheId + 1;}
											upperCacheId = maxCacheId;
										} else {
											// the most recent activities are being requested
											// make upper cache ID large enough so newest item in cache will be returned
											upperCacheId = cacheId + 1; 
										}
										lowerCacheId = upperCacheId - maxCount;
										// number of available activities might be less than maxCount
										if(lowerCacheId < 0) {lowerCacheId = 0L;}
										
										teamRequestedActivities = (List<Activity>)em3.createNamedQuery("Activity.getByTeamIdAndUpperAndLowerCacheIds")
											.setParameter("teamId", KeyFactory.keyToString(userTeam.getKey()))
											.setParameter("upperCacheId", upperCacheId)
											.setParameter("lowerCacheId", lowerCacheId)
											.getResultList();
									}
								}
								log.debug("number of teamRequestedActivities found = " + teamRequestedActivities.size());
							} catch(Exception e) {
								log.exception("ActivitiesResource:getActivities:Exception3", "Failed in getting Activity from cache", e);
								this.setStatus(Status.SERVER_ERROR_INTERNAL);
			    				return Utility.apiError(null);
							} finally {
								em3.close();
							}
						}
					}
					
					////////////////////////////////////////////////////////////////////////
					// for each activity found, set the participant role inside the activity
					////////////////////////////////////////////////////////////////////////
					if(currentUser != null) {
						String participantRole = null;
	    				for(Member m : userTeam.getMembers()) {
	    					if(m.isUserParticipant(currentUser)) {
	    						if(participantRole == null || participantRole.equalsIgnoreCase(Member.FAN_ROLE) || 
	    								(m.isCoordinator() && !participantRole.equalsIgnoreCase(Member.CREATOR_PARTICIPANT)) ) {
	    							participantRole = m.getParticipantRole();
	    						}
	    					}
	    				}
	    				for(Activity tra : teamRequestedActivities) {
	    					tra.setParticipantRole(participantRole);
	    				}
					}
					
					allTeamsRequestedActivities.addAll(teamRequestedActivities);
				} // end of for(Team userTeam : teams) ********************************************************************************************************
				
				numOfSearches++;
				log.debug("query for activity, number of queries = " + numOfSearches);
				if(numOfSearches > MAX_NUM_ACTIVITY_SEARCHES) {
					searchMaxedOut = true;
					break;  // out of the while(true) loop
				}
				if(!mustMatchMaxCount || allTeamsRequestedActivities.size() >= maxCount) {
					break;  // out of the while(true) loop
				}
				mostCurrentDate = GMT.subtractDaysFromDate(mostCurrentDate, totalNumberOfDays);
				leastCurrentDate = GMT.subtractDaysFromDate(mostCurrentDate, totalNumberOfDays-1);
			} // end of the while(true) loop #####################################################################################################################
			log.debug("number of allTeamsRequestedActivities found = " + allTeamsRequestedActivities.size());
			
			/////////////////////////////////////////////////////////////////////////////////////////////
			// Update newestCacheIds in User Entity. newestCacheIds are used to determine if a user
			// has any new activity.  All the user's teams that were updated by this API call need
			// to be updated.  Note that the team.newestCacheId may not have been updated in this
			// API but rather by the Create Activity API, but it doesn't matter.  Any user team
			// that has had activity retrieved by this API call should the associated user.newestCacheId
			// updated.
			/////////////////////////////////////////////////////////////////////////////////////////////
			if(currentUser != null && teams.size() > 0) {
	        	EntityManager em4= EMF.get().createEntityManager();
	        	try {
	        		em4.getTransaction().begin();
	        		// re-get the user inside this transaction
					User withinTransactionUser = (User)em4.createNamedQuery("User.getByKey")
						.setParameter("key", currentUser.getKey())
						.getSingleResult();
					log.debug("updating newCachIds for User = " + withinTransactionUser.getFullName() + ". Number of updated teams = " + teams.size());
					
					List<Long> newestCacheIds = withinTransactionUser.getTeamNewestCacheIds();
					// If a teamId was specified in the API call, then only ONE TEAM will be in the teamsWithPossibleUpdates list
					// so we must find the 'matching' team in the User's team list and update just that one team.
					if(teams.size() == 1) {
						Team specifiedTeam = teams.get(0);
						int index = 0;
						for(Key teamKey : withinTransactionUser.getTeams()) {
							if(teamKey.equals(specifiedTeam.getKey())) {
								newestCacheIds.set(index, specifiedTeam.getNewestCacheId());
								log.debug("updating cacheID for specified team = " + specifiedTeam.getTeamName());
								break;
							}
							index++;
						}
					} else {
						// ALL the User's team could have been updated. In this case, the code above guarantees that
						// the teamsWithPossibleUpdates list will contain ALL the user's teams -- both updated
						// teams and teams not updated. For simplicity, just completely rebuild newestCacheIds list.
						newestCacheIds = new ArrayList<Long>();
						for(Team t : teams) {
							// even if Activity not active for this team, getNewestCacheId() guaranteed to return 0L
							log.debug("updating cacheID for team = " + t.getTeamName());
							newestCacheIds.add(t.getNewestCacheId());
						}
					}
					
					withinTransactionUser.setTeamNewestCacheIds(newestCacheIds);
					em4.getTransaction().commit();
	        	} catch (NoResultException e) {
					log.exception("ActivitiesResource:getActivities:NoResultException", "Failed in getting Activity from cache", e);
	    		} catch (NonUniqueResultException e) {
					log.exception("ActivitiesResource:getActivities:NonUniqueResultException3", "two or more users have same key", e);
	    		} finally {
	    		    if (em4.getTransaction().isActive()) {
	    		    	em4.getTransaction().rollback();
	    		    }
	    		    em4.close();
	    		}
			}
			
			// because cacheId has been set, this will get sorted by created date - reverse chronological order
			Collections.sort(allTeamsRequestedActivities);
			
			if(isGetActivitiesForAllTeamsApi) {
				// enforce the maxCount for Date Interval algorithm
				if(allTeamsRequestedActivities.size() > maxCount) {
					// activity list needs to be truncated, but truncation only happens on full day boundaries
					Date dateBoundary = allTeamsRequestedActivities.get(maxCount-1).getCreatedGmtDate();
					dateBoundary = GMT.setTimeToTheBeginningOfTheDay(dateBoundary);
					log.debug("Activity list exceeded max size of " + maxCount + ". List size = " + allTeamsRequestedActivities.size() + " Date boundary = " + dateBoundary.toString());
					
					// find the index of the first activity with a date greater than the boundary date
					Integer truncateIndex = null;
					for(int searchIndex = maxCount; searchIndex<allTeamsRequestedActivities.size(); searchIndex++) {
						Date activityDate = allTeamsRequestedActivities.get(searchIndex).getCreatedGmtDate();
						activityDate = GMT.setTimeToTheBeginningOfTheDay(activityDate);
						if(activityDate.before(dateBoundary)) {
							truncateIndex = searchIndex;
							log.debug("truncate index found and = " + truncateIndex);
							break;
						}
					}
					
					// possible that no activity exceeded the boundary date, so modify activity list only if appropriate
					if(truncateIndex != null) {
						// for subList call, first index is inclusive and second is exclusive
						allTeamsRequestedActivities = allTeamsRequestedActivities.subList(0, truncateIndex);
						log.debug("Activity list truncated. New list size = " + allTeamsRequestedActivities.size());
					}
				}
			}
			
			// Package requested activities into JSON
			JSONArray jsonActivitiesArray = new JSONArray();
			Boolean includeDetails = Utility.s2b(this.includeDetailsStr, true); // default value is true
			for(Activity a : allTeamsRequestedActivities) {
				JSONObject jsonActivityObj = new JSONObject();
				jsonActivityObj.put("activityId", KeyFactory.keyToString(a.getKey()));
				jsonActivityObj.put("text", a.getText());
				// TODO is the Twitter Date returned GMT? -- if not fix this code
				jsonActivityObj.put("createdDate", GMT.convertToLocalDate(a.getCreatedGmtDate(), tz));
				if(isGetActivitiesForAllTeamsApi) {
					jsonActivityObj.put("teamId", a.getTeamId());
					jsonActivityObj.put("teamName", a.getTeamName());
					jsonActivityObj.put("sport", a.getSport());
					jsonActivityObj.put("participantRole", a.getParticipantRole());
				}
				jsonActivityObj.put("cacheId", a.getCacheId());
				jsonActivityObj.put("numberOfLikeVotes", a.getNumberOfLikeVotes());
				jsonActivityObj.put("numberOfDislikeVotes", a.getNumberOfDislikeVotes());
				if(a.getThumbNailBase64() != null) {
					if(includeDetails) {jsonActivityObj.put("thumbNail",a.getThumbNailBase64());}
					Boolean isVideo = a.getVideoBase64() == null ? false : true;
					jsonActivityObj.put("isVideo", isVideo);
				}
				Boolean useTwitterRet = a.getTwitterId() == null ? false : true;
				jsonActivityObj.put("useTwitter", useTwitterRet);
				
				// just set poster to the team name for legacy posting that were done before the poster started being returned
				String poster = a.getContributor() == null ? a.getTeamName() : a.getContributor();
				jsonActivityObj.put("poster", poster);
				
				Boolean isCurrentUser = true;
				String userId = a.getUserId();
				if(userId == null || (currentUser != null && !KeyFactory.keyToString(currentUser.getKey()).equals(userId))  ) {
					isCurrentUser = false;
				}
				jsonActivityObj.put("isCurrentUser", isCurrentUser);
				
				if(a.getEventId() != null) jsonActivityObj.put("eventId", a.getEventId());
				if(a.getEventType() != null) jsonActivityObj.put("eventType", a.getEventType());
				if(a.getEventStartGmtDate() != null) jsonActivityObj.put("eventStartDate", GMT.convertToLocalDate(a.getEventStartGmtDate(), tz));
				if(a.getEventDescription() != null) jsonActivityObj.put("eventDescription", a.getEventDescription());
				
				jsonActivitiesArray.put(jsonActivityObj);
			}
			log.debug("JSON object built successfully");
			jsonReturn.put("activities", jsonActivitiesArray);
			if(mustMatchMaxCount) jsonReturn.put("noMoreActivities", searchMaxedOut);
        } catch (JSONException e) {
			log.exception("ActivitiesResource:getActivities:JSONException", "error converting json representation into a JSON object", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (NoResultException e) {
			log.exception("ActivitiesResource:getActivities:NoResultException2", "team not found", e);
			apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
		} catch (NonUniqueResultException e) {
			log.exception("ActivitiesResource:getActivities:NonUniqueResultException2", "two or more teams have same team id", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} finally {
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("ActivitiesResource:getActivities:JSONException2", "error converting json representation into a JSON object", e);
		}
		return new JsonRepresentation(jsonReturn);
    }
    
    // theJsonReturn: out parameter - JSON object that will be returned by the calling routine
    private void getStatusOfActivitiesForUser(User theCurrentUser, JSONObject theJsonInput, JSONObject theJsonReturn) {
    	EntityManager em = EMF.get().createEntityManager();
    	String apiStatus = ApiStatusCode.SUCCESS;
		String currentUserId = KeyFactory.keyToString(theCurrentUser.getKey());
		
		try {
			List<String> activityIds = new ArrayList<String>();
			if(theJsonInput.has("activities")) {
				JSONArray activityIdsJsonArray = theJsonInput.getJSONArray("activities");
				int activityIdsJsonArraySize = activityIdsJsonArray.length();
				log.debug("activityIds json array length = " + activityIdsJsonArraySize);
				for(int i=0; i<activityIdsJsonArraySize; i++) {
					JSONObject activityIdJsonObj = activityIdsJsonArray.getJSONObject(i);
					activityIds.add(activityIdJsonObj.getString("activityId"));
				}
			}
			
	    	// Activity IDs are required.
	    	if(activityIds.size() == 0) {
	    		apiStatus = ApiStatusCode.ACTIVITY_IDS_REQUIRED;
	    	} else {
				ActivityVote activityVote = null;
				// Package requested activities into JSON
				JSONArray jsonActivitiesArray = new JSONArray();
				for(String activityId : activityIds) {
					JSONObject jsonActivityObj = new JSONObject();
					jsonActivityObj.put("activityId", activityId);
					String activityStatus = ActivityVote.NONE_STATUS;
					try {
						activityVote = (ActivityVote)em.createNamedQuery("ActivityVote.getByActivityIdAndUserId")
							.setParameter("activityId", activityId)
							.setParameter("userId", currentUserId)
							.getSingleResult();
						log.debug("activityVote retrieved successfully. Current status = " + activityVote.getStatus());
						activityStatus = activityVote.getStatus();
					} catch (NoResultException e) {
						// Not an error - actually, it's one of the expected results
					} catch (NonUniqueResultException e) {
						log.exception("ActivitiesResource:getStatusOfActivitiesForUser:JNonUniqueResultException", "error converting json representation into a JSON object", e);
						this.setStatus(Status.SERVER_ERROR_INTERNAL);
					}
					jsonActivityObj.put("vote", activityStatus);
					jsonActivitiesArray.put(jsonActivityObj);
				}
				log.debug("JSON object built successfully");
				theJsonReturn.put("activities", jsonActivitiesArray);
	    	}
		} catch (JSONException e) {
			log.exception("ActivitiesResource:getStatusOfActivitiesForUser:JSONException", "error converting json representation into a JSON object", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} finally {
		    em.close();
		}
    	
		try {
			theJsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("ActivitiesResource:getStatusOfActivitiesForUser:JSONException2", "error converting json representation into a JSON object", e);
		}
    	return;
    }
    
    private JsonRepresentation handleActivitiesDetailsApi(TimeZone tz, User theCurrentUser)  throws JSONException {
    	EntityManager em = EMF.get().createEntityManager();
    	JSONObject jsonReturn = new JSONObject();
    	List<String> activityIds = parseActivityIds();
    	
    	try {
    		JSONArray jsonActivitiesArray = new JSONArray();
    		for(String aId : activityIds) {
    			log.info("processing activityId = " + aId);
    			Activity activity = Activity.getActivity(aId, em);
    			List<Activity> replies = Activity.getReplies(aId, em);
    			
    			JSONObject jsonActivityObj = new JSONObject();
    			jsonActivityObj.put("activityId", aId);
    			jsonActivityObj.put("thumbNail", activity.getThumbNailBase64());
    			
    			JSONArray jsonRepliesArray = new JSONArray();
    			for(Activity reply : replies) {
    				JSONObject jsonReplyObj = new JSONObject();
    				
    				jsonReplyObj.put("activityId", KeyFactory.keyToString(reply.getKey()));
    				jsonReplyObj.put("text", reply.getText());
    				jsonReplyObj.put("createdDate", GMT.convertToLocalDate(reply.getCreatedGmtDate(), tz));
    				jsonReplyObj.put("teamId", reply.getTeamId());
    				jsonReplyObj.put("teamName", reply.getTeamName());
    				jsonReplyObj.put("cacheId", reply.getCacheId());
    				jsonReplyObj.put("numberOfLikeVotes", reply.getNumberOfLikeVotes());
    				jsonReplyObj.put("numberOfDislikeVotes", reply.getNumberOfDislikeVotes());
    				if(reply.getThumbNailBase64() != null) {
    					jsonReplyObj.put("thumbNail",reply.getThumbNailBase64());
    					Boolean isVideo = reply.getVideoBase64() == null ? false : true;
    					jsonReplyObj.put("isVideo", isVideo);
    				}
    				Boolean useTwitterRet = reply.getTwitterId() == null ? false : true;
    				jsonReplyObj.put("useTwitter", useTwitterRet);
    				
    				// just set poster to the team name for legacy posting that were done before the poster started being returned
    				String poster = reply.getContributor() == null ? reply.getTeamName() : reply.getContributor();
    				jsonReplyObj.put("poster", poster);
    				
    				Boolean isCurrentUser = true;
    				String userId = reply.getUserId();
    				if(userId == null || !KeyFactory.keyToString(theCurrentUser.getKey()).equals(userId)) {
    					isCurrentUser = false;
    				}
    				jsonReplyObj.put("isCurrentUser", isCurrentUser);
    				jsonRepliesArray.put(jsonReplyObj);
    			}
    			jsonActivityObj.put("replies", jsonRepliesArray);
    			jsonActivitiesArray.put(jsonActivityObj);
    		}
    		jsonReturn.put("activities", jsonActivitiesArray);
        	jsonReturn.put("apiStatus", ApiStatusCode.SUCCESS);
    	} finally {
		    em.close();
		}
    	
    	return new JsonRepresentation(jsonReturn);
    }
    
    private List<String> parseActivityIds() {
    	List<String> activityIds = new ArrayList<String>();
    	log.info("parsing activityIds = " + this.activityIdsStr);
		StringTokenizer st = new StringTokenizer(this.activityIdsStr, ",");
		while (st.hasMoreTokens()) {
			activityIds.add(st.nextToken());
		}
		return activityIds;
    }
}  
