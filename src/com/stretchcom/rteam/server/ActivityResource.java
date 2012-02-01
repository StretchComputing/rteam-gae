package com.stretchcom.rteam.server;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;

import org.apache.commons.codec.binary.Base64;
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
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
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
public class ActivityResource extends ServerResource {  
	//private static final Logger log = Logger.getLogger(ActivityResource.class.getName());
	private RskyboxClient log = new RskyboxClient(this);
	
	String teamId;
	String activityId;
	String media;
	
    @Override  
    protected void doInit() throws ResourceException {  
    	log.info("API requested with URL: " + this.getReference().toString());
    	
        // Get the "teamId" attribute value taken from the URI template /team/{teamId} 
        this.teamId = (String)getRequest().getAttributes().get("teamId"); 
        log.debug("ActivityResource:doInit() - teamId = " + this.teamId);
        if(this.teamId != null) {
            this.teamId = Reference.decode(this.teamId);
            log.debug("ActivityResource:doInit() - decoded teamId = " + this.teamId);
        }
        
        this.media = (String)getRequest().getAttributes().get("media"); 
        log.debug("ActivityResource:doInit() - media = " + this.media);
        if(this.media != null) {
            this.media = Reference.decode(this.media);
            log.debug("ActivityResource:doInit() - decoded media = " + this.media);
        }

        this.activityId = (String)getRequest().getAttributes().get("activityId"); 
        log.debug("ActivityResource:doInit() - activityId = " + this.activityId);
        if(this.activityId != null) {
            this.activityId = Reference.decode(this.activityId);
            log.debug("ActivityResource:doInit() - decoded activityId = " + this.activityId);
        } 
    }  
    
    // Handles 'Update activity' API  
    @Put 
    public JsonRepresentation updateActivity(Representation entity) {
    	JSONObject jsonReturn = new JSONObject();
    	log.debug("updateActivity(@Put) entered ..... ");
		EntityManager em = EMF.get().createEntityManager();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		User currentUser = null;
        try {
    		currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				log.error("ActivityResource:updateAttendees:currentUser", "user could not be retrieved from Request attributes");
    			return Utility.apiError(null);
    		} 

    		//::BUSINESSRULE:: user must be network authenticated to update an activity
    		else if(!currentUser.getIsNetworkAuthenticated()) {
    			return Utility.apiError(ApiStatusCode.USER_NOT_NETWORK_AUTHENTICATED);
    		}
    		// teamId is required
    		else if(this.teamId == null || this.teamId.length() == 0) {
    			return Utility.apiError(ApiStatusCode.TEAM_ID_REQUIRED);
    		}
    		// activityId is required
    		else if(this.activityId == null || this.activityId.length() == 0) {
    			return Utility.apiError(ApiStatusCode.ACTIVITY_ID_REQUIRED);
    		}
    		else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
    			return Utility.apiError(ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM);
        	}
    		
    		Team team = null;
    		EntityManager emTeam = EMF.get().createEntityManager();
			try {
				team = (Team)emTeam.createNamedQuery("Team.getByKey")
					.setParameter("key", KeyFactory.stringToKey(this.teamId))
					.getSingleResult();
				log.debug("team retrieved = " + team.getTeamName());
			} catch (NoResultException e) {
				return Utility.apiError(ApiStatusCode.TEAM_NOT_FOUND);
			} catch (NonUniqueResultException e) {
				log.exception("UserResource:udateUser:NonUniqueResultException", "two teams have the same key", e);
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				return Utility.apiError(null);
			} finally {
				emTeam.close();
			}

			Activity activity = null;
			Activity parentActivity = null;
			try {
				activity = (Activity)em.createNamedQuery("Activity.getByKey")
					.setParameter("key", KeyFactory.stringToKey(this.activityId))
					.getSingleResult();
				log.debug("activity retrieved successfully. LIKE count = " + activity.getNumberOfLikeVotes() + " DISLIKE count = " + activity.getNumberOfDislikeVotes());
				
				if(activity.getParentActivityId() != null) {
					parentActivity = (Activity)em.createNamedQuery("Activity.getByKey")
							.setParameter("key", KeyFactory.stringToKey(activity.getParentActivityId()))
							.getSingleResult();
						log.debug("parent activity retrieved successfully");
				}
			} catch (NoResultException e) {
				return Utility.apiError(ApiStatusCode.ACTIVITY_NOT_FOUND);
			} catch (NonUniqueResultException e) {
				log.exception("UserResource:udateUser:NonUniqueResultException2", "two or more activities have same key", e);
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				return Utility.apiError(null);
			}
			
			JsonRepresentation jsonRep = new JsonRepresentation(entity);
			JSONObject json = jsonRep.getJsonObject();
			
			String vote = null;
			if(json.has("vote")) {
				vote = json.getString("vote");
			}
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
			
			// Enforce Rules, and process request
			if(vote != null) {
				if(!ActivityVote.isStatusValid(vote)) {
					return Utility.apiError(ApiStatusCode.INVALID_VOTE_PARAMETER);
				}
				
				apiStatus = handleVoteUpdate(currentUser, vote, activity, jsonReturn);
				if(apiStatus == null || !apiStatus.equalsIgnoreCase(ApiStatusCode.SUCCESS)) {
					return Utility.apiError(apiStatus);
				}
			} else if(statusUpdate != null || photoBase64 != null) {
				String posterUserId = activity.getUserId();
				if(posterUserId == null || !posterUserId.equals(KeyFactory.keyToString(currentUser.getKey()))) {
					return Utility.apiError(ApiStatusCode.USER_NOT_POSTER);
				} else if(statusUpdate != null && statusUpdate.length() > TwitterClient.MAX_TWITTER_CHARACTER_COUNT){
					return Utility.apiError(ApiStatusCode.INVALID_STATUS_UPDATE_MAX_SIZE_EXCEEDED);
				} else if(videoBase64 != null && photoBase64 == null) {
					return Utility.apiError(ApiStatusCode.VIDEO_AND_PHOTO_MUST_BE_SPECIFIED_TOGETHER);
				} else if(photoBase64 != null && photoBase64.length() > 0 && isPortrait == null) {
					return Utility.apiError(ApiStatusCode.IS_PORTRAIT_AND_PHOTO_MUST_BE_SPECIFIED_TOGETHER);
				}
				
				apiStatus = handleStatusUpdate(statusUpdate, photoBase64, isPortrait, videoBase64, activity, jsonReturn);
				if(apiStatus == null || !apiStatus.equalsIgnoreCase(ApiStatusCode.SUCCESS)) {
					return Utility.apiError(apiStatus);
				}
			} else {
				return Utility.apiError(ApiStatusCode.VOTE_STATUS_UPDATE_OR_PHOTO_REQUIRED);
			}
			
         	// if status was modified AND team is using Twitter, then delete Twitter status
        	if(statusUpdate != null && team.getUseTwitter()) {
        		Long parentTwitterId = null;
        		if(parentActivity != null) {
        			parentTwitterId = parentActivity.getTwitterId();
        		}
            	twitter4j.Status twitterStatus = TwitterClient.modifyStatus(activity.getTwitterId(), parentTwitterId, statusUpdate, team.getTwitterAccessToken(), team.getTwitterAccessTokenSecret());
    			// if Twitter modify failed, log error, but continue because activity post will be stored by rTeam
    			if(twitterStatus == null) {
    				log.debug("Twitter modify failed, but continuing on ...");
    				//apiStatus = ApiStatusCode.TWITTER_ERROR;
    			} else {
					activity.setTwitterId(twitterStatus.getId());
					// if posted to twitter, match the exact twitter date
					activity.setUpdatedGmtDate(twitterStatus.getCreatedAt());
				}
        	}
		} catch (IOException e) {
			log.exception("UserResource:udateUser:IOException", "error extracting JSON object from Post", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (JSONException e) {
			log.exception("UserResource:udateUser:JSONException", "", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} finally {
		    em.close();
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("UserResource:udateUser:JSONException2", "error converting json representation into a JSON object", e);
		}
		return new JsonRepresentation(jsonReturn);
    }
    
    // Handles 'Get activity photo' API 
    // Handles 'Get activity video' API 
    @Get("json")
    public JsonRepresentation getActivity(Variant variant) {
        log.debug("ActivityResource:getActivity() entered");
        JSONObject jsonReturn = new JSONObject();
		EntityManager em = EMF.get().createEntityManager();

		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
        Team team = null;
		try {
	    	User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
			if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				log.error("UserResource:getActivity:currentUser", "user could not be retrieved from Request attributes");
				return Utility.apiError(null);
			}
			// teamId is required
			else if(this.teamId == null || this.teamId.length() == 0) {
				return Utility.apiError(ApiStatusCode.TEAM_ID_REQUIRED);
			} else {
				try {
    	    		team = (Team)em.createNamedQuery("Team.getByKey")
						.setParameter("key", KeyFactory.stringToKey(this.teamId))
						.getSingleResult();
        			
    	    		if(!currentUser.isUserMemberOfTeam(this.teamId)) {
    	    			return Utility.apiError(ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM);
                	}
				} catch (NoResultException e) {
					return Utility.apiError(ApiStatusCode.TEAM_NOT_FOUND);
				} 
			}
			
			if(this.activityId == null) {
				return Utility.apiError(ApiStatusCode.ACTIVITY_ID_REQUIRED);
			} else if(!currentUser.getIsNetworkAuthenticated()) {
				return Utility.apiError(ApiStatusCode.USER_NOT_NETWORK_AUTHENTICATED);
			}

    		Activity activity = (Activity)em.createNamedQuery("Activity.getByKey")
    			.setParameter("key", KeyFactory.stringToKey(this.activityId))
    			.getSingleResult();
    		log.debug("activity retrieved = " + activity.getText());
    		
    		if(this.media != null && this.media.equalsIgnoreCase("photo")) {
        		String photoBase64 = activity.getPhotoBase64();
        		if(photoBase64 != null) {
            		jsonReturn.put("photo", activity.getPhotoBase64());
        		}
    		} else if(this.media != null && this.media.equalsIgnoreCase("video")) {
        		String videoBase64 = activity.getVideoBase64();
        		if(videoBase64 != null) {
            		jsonReturn.put("video", activity.getVideoBase64());
        		}
    		}
        } catch (NoResultException e) {
        	apiStatus = ApiStatusCode.ACTIVITY_NOT_FOUND;
        	log.debug("no result exception, activity not found");
		} catch (JSONException e) {
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
			log.exception("UserResource:getActivity:JSONException", "error converting json representation into a JSON object", e);
		} catch (NonUniqueResultException e) {
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
			log.exception("UserResource:getActivity:NonUniqueResultException", "two or more activities have same ID", e);
		} finally {
			em.close();
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("UserResource:getActivity:JSONException2", "error converting json representation into a JSON object", e);
		}
        return new JsonRepresentation(jsonReturn);
    }
    
    // Handles 'Delete Activity' API
    @Delete
    public JsonRepresentation remove() {
    	log.debug("ActivityResource:remove() entered");
    	EntityManager em = EMF.get().createEntityManager();
    	JSONObject jsonReturn = new JSONObject();
    	
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
        try {
    		User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				log.error("ActivityResource:remove:currentUser", "");
				return Utility.apiError(null);
    		}
    		// teamId and activity are required
    		else if(this.teamId == null || this.teamId.length() == 0 ||
		        	this.activityId == null || this.activityId.length() == 0 ) {
    			return Utility.apiError(ApiStatusCode.TEAM_ID_AND_ACTIVITY_ID_REQUIRED);
			} 
			// must be a member of the team
			else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
    			return Utility.apiError(ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM);
        	}
			
			Team team = (Team)em.createNamedQuery("Team.getByKey")
				.setParameter("key", KeyFactory.stringToKey(this.teamId))
				.getSingleResult();
    		log.debug("team retrieved = " + team.getTeamName());

			Activity activity = null;
			activity = (Activity)em.createNamedQuery("Activity.getByKey")
					.setParameter("key", KeyFactory.stringToKey(this.activityId))
					.getSingleResult();
			log.debug("activity retrieved successfully");
			
			// Rule: only Poster can delete an activity
			String posterUserId = activity.getUserId();
			if(posterUserId == null || !posterUserId.equals(KeyFactory.keyToString(currentUser.getKey()))) {
				return Utility.apiError(ApiStatusCode.USER_NOT_POSTER);
			}
			
         	// if team uses Twitter, then delete Twitter status
        	if(team.getUseTwitter()) {
            	twitter4j.Status twitterStatus = TwitterClient.destroyStatus(activity.getTwitterId(), team.getTwitterAccessToken(), team.getTwitterAccessTokenSecret());
    			// if Twitter update failed, log error, but continue because activity post will be stored by rTeam
    			if(twitterStatus == null) {
    				log.error("ActivityResource:remove:twitterStatus", "Twitter destroy failed, but continuing on ...");
    				//apiStatus = ApiStatusCode.TWITTER_ERROR;
    			}
        	}

        	em.remove(activity);
        } catch (NoResultException e) {
        	log.debug("no result exception, activity not found");
        	apiStatus = ApiStatusCode.ACTIVITY_NOT_FOUND;
		} catch (NonUniqueResultException e) {
			log.exception("ActivityResource:remove:NonUniqueResultException", "", e);
        	this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} finally {
		    em.close();
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("ActivityResource:remove:JSONException2", "", e);
		}
        return new JsonRepresentation(jsonReturn);
    }
    
    private String handleVoteUpdate(User currentUser, String vote, Activity activity, JSONObject jsonReturn) throws JSONException {
		////////////////////////////////////////
		// First, create/update the activityVote
		////////////////////////////////////////
		EntityManager emActivityVote = EMF.get().createEntityManager();
		String oldStatus = null;
		Boolean isActivityVoteNew = false;
		String currentUserId = KeyFactory.keyToString(currentUser.getKey());
		try {
			ActivityVote activityVote = null;
			try {
				activityVote = (ActivityVote)emActivityVote.createNamedQuery("ActivityVote.getByActivityIdAndUserId")
					.setParameter("activityId", this.activityId)
					.setParameter("userId", currentUserId)
					.getSingleResult();
				log.debug("activityVote retrieved successfully. Current status = " + activityVote.getStatus());
			} catch (NoResultException e) {
				// Not an error - actually, it's the expected result
				log.debug("activityVote not found");
			} catch (NonUniqueResultException e) {
				log.exception("UserResource:udateUser:NonUniqueResultException3", "two or more activityVote have same key", e);
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				return null;
			} 
		
			if(activityVote == null) {
				// no activityVote yet, so create one
				activityVote = new ActivityVote();
				activityVote.setActivityId(this.activityId);
				activityVote.setCreatedGmtDate(new Date());
				activityVote.setTeamId(this.teamId);
				activityVote.setUserId(currentUserId);
				isActivityVoteNew = true;
				// TODO set isMember
			} else {
				// activityVote is being updated
				activityVote.setLastUpdatedGmtDate(new Date());
				oldStatus = activityVote.getStatus();
			}
			
			activityVote.setStatus(vote);
			log.debug("activityVote successfully updated to vote = " + vote);
			
			if(isActivityVoteNew) {
				emActivityVote.persist(activityVote);
			}
		} finally {
			emActivityVote.close();
		}
		
		////////////////////////////////////////////
		// Now, update the vote count(s) in Activity
		////////////////////////////////////////////
		if(oldStatus == null) {
			// this is a new vote
			if(vote.equalsIgnoreCase(ActivityVote.DISLIKE_STATUS)) {
				activity.incrementNumberOfDislikeVotes();
			} else {
				activity.incrementNumberOfLikeVotes();
			}
		} else {
			// vote is being changed, so must decrement original vote and increment new vote
			if(oldStatus.equalsIgnoreCase(ActivityVote.DISLIKE_STATUS) &&
			   vote.equalsIgnoreCase(ActivityVote.LIKE_STATUS))    {
				activity.decrementNumberOfDislikeVotes();
				activity.incrementNumberOfLikeVotes();
			}
			else if(oldStatus.equalsIgnoreCase(ActivityVote.LIKE_STATUS) &&
					vote.equalsIgnoreCase(ActivityVote.DISLIKE_STATUS))    {
				activity.decrementNumberOfLikeVotes();
				activity.incrementNumberOfDislikeVotes();
			}
		}
		
		jsonReturn.put("numberOfLikeVotes", activity.getNumberOfLikeVotes());
		jsonReturn.put("numberOfDislikeVotes", activity.getNumberOfDislikeVotes());
		
		return ApiStatusCode.SUCCESS;
    }
    
    private String handleStatusUpdate(String statusUpdate, String photoBase64, Boolean isPortrait, String videoBase64, Activity activity, JSONObject jsonReturn) {
    	
       	/////////////////////////////////////////////////////////////////////////////////
    	// Only provided fields are updated. If a field is null, then it was NOT provided
    	// If an empty string is provided, the field is cleared (i.e set to null)
    	/////////////////////////////////////////////////////////////////////////////////
    	log.debug("handleStatusUpdate() entered");
    	activity.setUpdatedGmtDate(new Date());
    	
    	// TODO update Twitter too if used
    	if(statusUpdate != null) {
    		if(statusUpdate.length() == 0) {statusUpdate = null;}
    		activity.setText(statusUpdate);
    	}
    	
		if(photoBase64 != null) {
			if(photoBase64.length() == 0) {
				photoBase64 = null;
				activity.setThumbNailBase64((String)null);
			} else {
				// decode the base64 encoding to create the thumb nail
				byte[] rawPhoto = Base64.decodeBase64(photoBase64);
				ImagesService imagesService = ImagesServiceFactory.getImagesService();
				Image oldImage = ImagesServiceFactory.makeImage(rawPhoto);
				
				int tnWidth = isPortrait == true ? Activity.THUMB_NAIL_SHORT_SIDE : Activity.THUMB_NAIL_LONG_SIDE;
				int tnHeight = isPortrait == true ? Activity.THUMB_NAIL_LONG_SIDE : Activity.THUMB_NAIL_SHORT_SIDE;
				Transform resize = ImagesServiceFactory.makeResize(tnWidth, tnHeight);
				Image newImage = imagesService.applyTransform(resize, oldImage);
				String thumbNailBase64 = Base64.encodeBase64String(newImage.getImageData());
				activity.setThumbNailBase64(thumbNailBase64);
			}
			activity.setPhotoBase64(photoBase64);
		}

		if(videoBase64 != null) {
			if(videoBase64.length() == 0) {videoBase64 = null;}
			activity.setVideoBase64(videoBase64);
		}
		
    	return ApiStatusCode.SUCCESS;
    }
}  
