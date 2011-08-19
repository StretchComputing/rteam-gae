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
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
  
/** 
 * Resource that manages a members of a team. 
 *  
 */  
public class ActivityResource extends ServerResource {  
	private static final Logger log = Logger.getLogger(ActivityResource.class.getName());
	
	String teamId;
	String activityId;
	String media;
	
    @Override  
    protected void doInit() throws ResourceException {  
        // Get the "teamId" attribute value taken from the URI template /team/{teamId} 
        this.teamId = (String)getRequest().getAttributes().get("teamId"); 
        log.info("ActivityResource:doInit() - teamId = " + this.teamId);
        if(this.teamId != null) {
            this.teamId = Reference.decode(this.teamId);
            log.info("ActivityResource:doInit() - decoded teamId = " + this.teamId);
        }
        
        this.media = (String)getRequest().getAttributes().get("media"); 
        log.info("ActivityResource:doInit() - media = " + this.media);
        if(this.media != null) {
            this.media = Reference.decode(this.media);
            log.info("ActivityResource:doInit() - decoded media = " + this.media);
        }

        this.activityId = (String)getRequest().getAttributes().get("activityId"); 
        log.info("ActivityResource:doInit() - activityId = " + this.activityId);
        if(this.activityId != null) {
            this.activityId = Reference.decode(this.activityId);
            log.info("ActivityResource:doInit() - decoded activityId = " + this.activityId);
        } 
    }  
    
    // Handles 'Update activity' API  
    @Put 
    public JsonRepresentation updateActivity(Representation entity) {
    	JSONObject jsonReturn = new JSONObject();
    	log.info("updateActivity(@Put) entered ..... ");
		EntityManager em = EMF.get().createEntityManager();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		User currentUser = null;
        try {
    		currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
    			log.severe("user could not be retrieved from Request attributes!!");
    		}
    		//::BUSINESSRULE:: user must be network authenticated to update an activity
    		else if(!currentUser.getIsNetworkAuthenticated()) {
    			apiStatus = ApiStatusCode.USER_NOT_NETWORK_AUTHENTICATED;
    		}
    		// teamId is required
    		else if(this.teamId == null || this.teamId.length() == 0) {
				apiStatus = ApiStatusCode.TEAM_ID_REQUIRED;
    		}
    		// activityId is required
    		else if(this.activityId == null || this.activityId.length() == 0) {
				apiStatus = ApiStatusCode.ACTIVITY_ID_REQUIRED;
    		}
    		else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
				log.info(apiStatus);
        	}
    		
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
    		
			EntityManager emTeam = EMF.get().createEntityManager();
			try {
				Team team = (Team)emTeam.createNamedQuery("Team.getByKey")
					.setParameter("key", KeyFactory.stringToKey(this.teamId))
					.getSingleResult();
				log.info("team retrieved = " + team.getTeamName());
			} catch (NoResultException e) {
				apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
				log.info("invalid team id");
			} catch (NonUniqueResultException e) {
				log.severe("should never happen - two teams have the same key");
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
			} finally {
				emTeam.close();
			}
    		
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}

			Activity activity = null;
			try {
				activity = (Activity)em.createNamedQuery("Activity.getByKey")
					.setParameter("key", KeyFactory.stringToKey(this.activityId))
					.getSingleResult();
				log.info("activity retrieved successfully. LIKE count = " + activity.getNumberOfLikeVotes() + " DISLIKE count = " + activity.getNumberOfDislikeVotes());
			} catch (NoResultException e) {
				apiStatus = ApiStatusCode.ACTIVITY_NOT_FOUND;
				log.info("invalid team id");
			} catch (NonUniqueResultException e) {
				log.severe("should never happen - two or more activities have same key");
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
			}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			
			JsonRepresentation jsonRep = new JsonRepresentation(entity);
			JSONObject json = jsonRep.toJsonObject();
			
			String vote = null;
			if(json.has("vote")) {
				vote = json.getString("vote");
			}
			
			// Enforce Business Rules
			if(vote == null || vote.length() == 0) {
				apiStatus = ApiStatusCode.VOTE_REQUIRED;
				log.info("required vote field missing");
			} else if(!ActivityVote.isStatusValid(vote)) {
				apiStatus = ApiStatusCode.INVALID_VOTE_PARAMETER;
				log.info("invalid vote parameter");
			}
    		
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			
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
					activityVote = (ActivityVote)em.createNamedQuery("ActivityVote.getByActivityIdAndUserId")
						.setParameter("activityId", this.activityId)
						.setParameter("userId", currentUserId)
						.getSingleResult();
					log.info("activityVote retrieved successfully. Current status = " + activityVote.getStatus());
				} catch (NoResultException e) {
					// Not an error - actually, it's the expected result
					log.info("activityVote not found");
				} catch (NonUniqueResultException e) {
					log.severe("should never happen - two or more activityVotes have same activity id and user id");
					this.setStatus(Status.SERVER_ERROR_INTERNAL);
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
				log.info("activityVote successfully updated to vote = " + vote);
				
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
		} catch (IOException e) {
			log.severe("error extracting JSON object from Post");
			e.printStackTrace();
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (JSONException e) {
			log.severe("error converting json representation into a JSON object");
			e.printStackTrace();
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} finally {
		    em.close();
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.severe("error creating JSON return object");
			e.printStackTrace();
		}
		return new JsonRepresentation(jsonReturn);
    }
    
    // Handles 'Get activity photo' API 
    // Handles 'Get activity video' API 
    @Get("json")
    public JsonRepresentation getActivity(Variant variant) {
        log.info("ActivityResource:getActivity() entered");
        JSONObject jsonReturn = new JSONObject();
		EntityManager em = EMF.get().createEntityManager();

		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
        Team team = null;
		try {
	    	User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
			if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				log.severe("user could not be retrieved from Request attributes!!");
			}
			// teamId is required
			else if(this.teamId == null || this.teamId.length() == 0) {
 				apiStatus = ApiStatusCode.TEAM_ID_REQUIRED;
				log.info("invalid team ID");
			} else {
				try {
    	    		team = (Team)em.createNamedQuery("Team.getByKey")
						.setParameter("key", KeyFactory.stringToKey(this.teamId))
						.getSingleResult();
        			
    	    		if(!currentUser.isUserMemberOfTeam(this.teamId)) {
        				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
        				log.info(apiStatus);
                	}
				} catch (NoResultException e) {
		        	apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
		        	log.info("no result exception, team not found");
				} 
			}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			
			if(this.activityId == null) {
				apiStatus = ApiStatusCode.ACTIVITY_ID_REQUIRED;
			} else if(!currentUser.getIsNetworkAuthenticated()) {
				apiStatus = ApiStatusCode.USER_NOT_NETWORK_AUTHENTICATED;
			}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}

    		Activity activity = (Activity)em.createNamedQuery("Activity.getByKey")
    			.setParameter("key", KeyFactory.stringToKey(this.activityId))
    			.getSingleResult();
    		log.info("activity retrieved = " + activity.getText());
    		
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
        	log.info("no result exception, activity not found");
		} catch (JSONException e) {
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
			log.severe("error building JSON object");
		} catch (NonUniqueResultException e) {
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
			log.severe("should never happen - two or more activities have same ID");
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
}  
