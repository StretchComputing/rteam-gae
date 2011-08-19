package com.stretchcom.rteam.server;
	
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
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
import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.Transform;
import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.google.appengine.api.labs.taskqueue.TaskOptions.Method;
import com.google.appengine.repackaged.com.google.common.util.Base64;
import com.google.appengine.repackaged.com.google.common.util.Base64DecoderException;

/**
 * @author joepwro
 */
public class TeamResource extends ServerResource {
	private static final Logger log = Logger.getLogger(TeamResource.class.getName());

    // The sequence of characters that identifies the resource.
    String teamId;  
    String oneUseToken;
    String oauthVerifier;
    String includePhoto;
    
    @Override  
    protected void doInit() throws ResourceException {  
        // Get the "teamId" attribute value taken from the URI template /team/{teamId} 
        this.teamId = (String)getRequest().getAttributes().get("teamId"); 
        log.info("UserResource:doInit() - teamId = " + this.teamId);
        if(this.teamId != null) {
            this.teamId = Reference.decode(this.teamId);
            log.info("UserResource:doInit() - decoded teamName = " + this.teamId);
        }
        
		Form form = getRequest().getResourceRef().getQueryAsForm();
		for (Parameter parameter : form) {
			log.info("parameter " + parameter.getName() + " = " + parameter.getValue());
			if(parameter.getName().equals("oneUseToken")) {
				this.oneUseToken = (String)parameter.getValue();
				this.oneUseToken = Reference.decode(this.oneUseToken);
				log.info("TeamResource:doInit() - decoded oneUseToken = " + this.oneUseToken);
			} else if(parameter.getName().equals("oauthVerifier")) {
				this.oauthVerifier = (String)parameter.getValue();
				this.oauthVerifier = Reference.decode(this.oauthVerifier);
				log.info("TeamResource:doInit() - decoded oauthVerifier = " + this.oauthVerifier);
			} else if(parameter.getName().equals("includePhoto"))  {
				this.includePhoto = (String)parameter.getValue();
				this.includePhoto = Reference.decode(this.includePhoto);
				log.info("TeamResource:doInit() - decoded includePhoto = " + this.includePhoto);
			}
		}
    }  

    // Handles 'Get team info' API
    @Get("json")
    public JsonRepresentation getTeamInfo(Variant variant) {
        log.info("TeamResource:toJson() entered");
        JSONObject jsonReturn = new JSONObject();
		EntityManager em = EMF.get().createEntityManager();

		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
        User currentUser = null;
        try {
    		currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
    			log.severe("user could not be retrieved from Request attributes!!");
    		} else if(this.teamId == null || this.teamId.length() == 0) {
        		log.info("TeamResource:toJson() teamId null or zero length");
				apiStatus = ApiStatusCode.TEAM_ID_REQUIRED;
        	} else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
				log.info(apiStatus);
        	}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
    		
    		else {
    			Key teamKey = KeyFactory.stringToKey(this.teamId);
        		Team team = (Team)em.createNamedQuery("Team.getByKey")
        			.setParameter("key", teamKey)
        			.getSingleResult();
        		log.info("team retrieved = " + team.getTeamName());
            	
            	// always return the required fields
    			jsonReturn.put("teamName", team.getTeamName());
    			jsonReturn.put("description", team.getDescription());
    			
    			// conditionally return the optional fields
    			if(team.getLeagueName() != null && team.getLeagueName().length() > 0)
    				jsonReturn.put("leagueName", team.getLeagueName());
    			if(team.getSport() != null && team.getSport().length() > 0) 
    				jsonReturn.put("sport", team.getSport());
    			
    			Boolean useTwitter = team.getUseTwitter() == null ? false : team.getUseTwitter();
    			jsonReturn.put("useTwitter", useTwitter);
    			
    			if(team.getSiteUrl() != null && team.getSiteUrl().length() > 0)
    				jsonReturn.put("teamSiteUrl", team.getSiteUrl());
    			if(team.getPageUrl() != null && team.getPageUrl().length() > 0)
    				jsonReturn.put("teamPageUrl", team.getPageUrl());
    			if(team.getGender() != null && team.getGender().length() > 0)
    				jsonReturn.put("gender", team.getGender());
    			if(team.getCity() != null && team.getCity().length() > 0)
    				jsonReturn.put("city", team.getCity());
    			if(team.getState() != null && team.getState().length() > 0)
    				jsonReturn.put("state", team.getState());
            	
				Boolean photoReturned = false;
            	if(this.includePhoto != null && this.includePhoto.equalsIgnoreCase("true")) {
		    		String photoBase64 = team.getPhotoBase64();
		    		if(photoBase64 != null) {
		        		jsonReturn.put("photo", team.getPhotoBase64());
		        		photoReturned = true;
		    		}
				} 
            	
        		// if the photo was not returned, then attempt to return the thumb nail
            	if(!photoReturned && team.getThumbNailBase64() != null) {
            		jsonReturn.put("thumbNail", team.getThumbNailBase64());
            	}
        	}
        } catch (NoResultException e) {
        	log.info("no result exception, team not found");
        	apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
		} catch (JSONException e) {
			log.severe("error building JSON object");
			e.printStackTrace();
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
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
    
    
    // Handles 'Delete team' API
    //::BUSINESSRULE:: Once created, no one user can delete a team. Instead, the user can only remove themselves from the team. If
    //                 the user is the only team coordinator, all other members who are also users are made coordinators and are
    //                 sent an email indication indicating such. If no other members are users, a special notification is sent to
    //                 all members informing them that team will not be able to operate normally until one or more members
    //                 registers and becomes a user.
    @Delete
    public JsonRepresentation remove() {
    	log.info("TeamResource:remove() entered");
    	EntityManager em = EMF.get().createEntityManager();
    	JSONObject jsonReturn = new JSONObject();
    	
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		try {
	    	User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
    			log.severe("user could not be retrieved from Request attributes!!");
    		} else if(this.teamId == null || this.teamId.length() == 0) {
        		log.info("TeamResource:toJson() teamId null or zero length");
				apiStatus = ApiStatusCode.TEAM_ID_REQUIRED;
        	} else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
				log.info(apiStatus);
        	}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
	    	
			Boolean wasMemberRemoved = false;
			Key memberRemovedKey = null;
			em.getTransaction().begin();
            try {
        		Team team = (Team)em.createNamedQuery("Team.getByKey")
					.setParameter("key", KeyFactory.stringToKey(this.teamId))
					.getSingleResult();
        		log.info("team retrieved = " + team.getTeamName());
        		
        		//::TODO refactor and build fan and non-fan list via queries. Relevant if #fans in team gets very large
        		//-----------------------------------------------------------------------------------------------------
        		// remove the member that is associated with this user
        		Member memberToRemove = null;
        		List<Member> fanParticipants = new ArrayList<Member>();
        		List<Member> memberParticipants = new ArrayList<Member>();
        		List<Member> memberParticipantsThatAreUsers = new ArrayList<Member>();
        		List<Member> memberParticipantsThatAreUsersAndCoordinators = new ArrayList<Member>();
        		List<Member> members = team.getMembers();
        		
        		//***********************************************************************
        		// lots of problems below
        		// in general, can't match user to member unless both are NA. If true - update API in Wave
        		// can't just match the user's EA against primary EA in member -- any person in membership could be requesting the delete
        		// when checking if member is a User, can't just look at primary UserId - need to look at all user IDs.
        		//
        		// PRIORITY TODO
        		
        		
        		for(Member m : members) {
        			if(m.getEmailAddress().equalsIgnoreCase(currentUser.getEmailAddress())) {
        				memberToRemove = m;
        			} else {
        				// only add member to fan or nonFan lists if member is NOT being removed
            			if(m.isFan()) {
            				fanParticipants.add(m);
            			} else {
            				memberParticipants.add(m);
            				if(m.getUserId() != null) {
            					memberParticipantsThatAreUsers.add(m);
            					if(m.isCoordinator()) {
            						memberParticipantsThatAreUsersAndCoordinators.add(m);
            					}
            				}
            			}
        			}
        		}
        		
        		if(memberToRemove != null) {
        			memberRemovedKey = memberToRemove.getKey();
        			members.remove(memberToRemove);
        			wasMemberRemoved = true;
        		} else {
        			log.severe("should never happen: user did not have membership on this team");
        		}
        		
        		boolean shouldRemoveTeam = false;
        		boolean notifyMemberUsersTheyAreNowCoordinators = false;
        		boolean notifymemberParticipantsThereAreNoMoreCoordinators = false;
        		boolean notifyFansTeamHasBeenDisbanded = false;
        		if(members.size() == 0) {
        			// absolutely no one left on the team, so delete the team
        			shouldRemoveTeam = true;
        		}
        		else if(memberParticipants.size() == 0) {
        			// no nonFan members left, notify fans team has been disbanded
        			notifyFansTeamHasBeenDisbanded = true;
        		} else if(memberParticipantsThatAreUsers.size() > 0 && memberParticipantsThatAreUsersAndCoordinators.size() == 0 ) {
        			// there are users, but no more coordinators
        			// make all members-users coordinators
        			for(Member mu : memberParticipantsThatAreUsers) {
        				mu.setParticipantRole(Member.COORDINATOR_PARTICIPANT);
        			}
        			// notify member-users that they are now coordinators
        			notifyMemberUsersTheyAreNowCoordinators = true;
        		} else if(memberParticipantsThatAreUsers.size() == 0) {
        			// there aren't any member-users. Notify all member participants (not fans) that there are no coordinators
        			notifymemberParticipantsThereAreNoMoreCoordinators = true;
        		}
    		
			    if(shouldRemoveTeam) {
			    	em.remove(team);
			    }
        		em.getTransaction().commit();
       		
        		// send notifications after closing transaction
        		///////////////////////////////////////////////
        		if(notifyMemberUsersTheyAreNowCoordinators) {
        			log.info("sendNewCoordinatorMessage to " + memberParticipantsThatAreUsers.size() + " member participant users.");
        			PubHub.sendNewCoordinatorMessage(memberParticipantsThatAreUsers, team);
        		} else if(notifymemberParticipantsThereAreNoMoreCoordinators) {
        			log.info("sendNoCoordinatorMessage to " + memberParticipants.size() + " member participants.");
        			PubHub.sendNoCoordinatorMessage(memberParticipants, team);
        		} else if(notifyFansTeamHasBeenDisbanded) {
        			log.info("sendTeamInactiveMessage to " + fanParticipants.size() + " fans.");
        			PubHub.sendTeamInactiveMessage(fanParticipants, team);
        		}
            } catch (NoResultException e) {
            	log.info("no result exception, team not found");
            	apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
    		} finally {
    		    if (em.getTransaction().isActive()) {
    		        em.getTransaction().rollback();
    		    }
    		    em.close();
    		}
    		
    		// TODO this could be done later via a Task Queue
    		//////////////////////////////////////////////////////////////////
    		// Update recipients of messageThreads outside of any transaction.
    		//////////////////////////////////////////////////////////////////
    		// If a member was deleted, then all the the member's messages for this team must be archived. If they are not
    		// archived, they will continue to show up when a user views all their messages for all teams and they won't
    		// be able to delete them.
    		if(wasMemberRemoved) {
    			EntityManager emMessages = EMF.get().createEntityManager();
    	        try {
    	        	// First remove messages from the User's inbox
    	        	List<Recipient> nonarchivedInboxMessages = (List<Recipient>)emMessages.createNamedQuery("Recipient.getByMemberIdAndEmailAddressAndNotStatus")
    					.setParameter("memberId", KeyFactory.keyToString(memberRemovedKey))
    					.setParameter("emailAddress", currentUser.getEmailAddress())
    					.setParameter("status", Recipient.ARCHIVED_STATUS)
    					.getResultList();
        			log.info("archiving " + nonarchivedInboxMessages.size() + " user's inbox messages for the team they are no longer part of");
    	        	
    	        	for(Recipient r : nonarchivedInboxMessages) {
    	        		r.setWasViewed(true);
    	        		r.setStatus(Recipient.ARCHIVED_STATUS);
    	        	}
    	        	
    	        	// Next, remove messages from the User's outbox -- for THIS TEAM
    	        	List<MessageThread> nonarchivedOutboxMessages = (List<MessageThread>)emMessages.createNamedQuery("MessageThread.getBySenderUserIdAndStatusAndTeamId")
						.setParameter("senderUserId", KeyFactory.keyToString(currentUser.getKey()))
						.setParameter("status", MessageThread.ACTIVE_STATUS)
						.setParameter("teamId", this.teamId)
						.getResultList();
    	        	log.info("archiving " + nonarchivedOutboxMessages.size() + " user's outbox messages for the team they are no longer part of");
    	        	for(MessageThread mt : nonarchivedOutboxMessages) {
    	        		// finalize polls, archive the rest
    	        		if(mt.getType().equalsIgnoreCase(MessageThread.POLL_TYPE)) {
    	        			mt.setStatus(MessageThread.FINALIZED_STATUS);
    	        		} else {
        	        		mt.setStatus(MessageThread.ARCHIVED_STATUS);
    	        		}
    	        	}
    	        	
    	        } catch (Exception e) {
    				log.severe("Should never happen: error getting nonarchivedMessages - exeception = " + e.getMessage());
    			} finally {
    				emMessages.close();
    			}
    		}

			// update User in a separate transaction - don't think User and Team are in the same Entity Group
			log.info("adding Team to user team list");
			EntityManager em2 = EMF.get().createEntityManager();
			em2.getTransaction().begin();
	        try {
        		// currentUser was retrieved during authentication outside the transaction. Get it again inside the transaction
        		// so that is is managed by the entityManager. (is there a better way to do this?)
    			currentUser = (User)em2.createNamedQuery("User.getByKey")
        			.setParameter("key", currentUser.getKey())
        			.getSingleResult();
        		List<Key> teamKeys = currentUser.getTeams();
        		for(Key k : teamKeys) {
        			if(k.equals(KeyFactory.stringToKey(this.teamId))) {
        				log.info("removing user from team with ID = " + KeyFactory.keyToString(k));
        				currentUser.removeTeam(k);
        				break;
        			}
        		}
	    		em2.getTransaction().commit();
	        } catch (NoResultException e) {
				log.severe("user not found");
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (NonUniqueResultException e) {
				log.severe("should never happen - two or more users have same key");
				e.printStackTrace();
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
			} finally {
			    if (em2.getTransaction().isActive()) {
			        em2.getTransaction().rollback();
			    }
			    em2.close();
			}
		} catch (JSONException e) {
			log.severe("error converting json representation into a JSON object");
			e.printStackTrace();
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		}
		
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.severe("error converting json representation into a JSON object");
			e.printStackTrace();
		}
		return new JsonRepresentation(jsonReturn);
    }
    
    // Handles 'Update team' API
    // Handles 'Twitter Callback Confirmation' API (for GWT)
    @Put 
    public JsonRepresentation updateTeam(Representation entity) {
    	log.info("updateTeam(@Put) entered ..... ");
		EntityManager em = EMF.get().createEntityManager();
		JSONObject jsonReturn = new JSONObject();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		String notificationMessage = "";
		em.getTransaction().begin();
        try {
        	if(this.oneUseToken != null) {
    			// --------------------------------------------------
    			// This is the 'Get Twitter Confirmation' API call
    			// --------------------------------------------------
        		log.info("this is 'Get Twitter Confirmation' API handling");
        		
        		Team team = null;
        		try {
        			team = (Team)em.createNamedQuery("Team.getByOneUseTokenAndTokenStatus")
        				.setParameter("oneUseToken", this.oneUseToken)
        				.setParameter("oneUseTokenStatus", Recipient.NEW_TOKEN_STATUS)
        				.getSingleResult();
        			log.info("updateTeam(): team found");
        			
        			team.setOneUseTokenStatus(Team.USED_TOKEN_STATUS);
        			
        			// get the twitter access token
        			///////////////////////////////
        			// create out parameter
        			List<String> accessTokenInfo = new ArrayList<String>();
        			boolean accessTokenRetrieved = TwitterClient.getAccessToken(team.getTwitterRequestToken(), team.getTwitterRequestTokenSecret(), 
        																		this.oauthVerifier, accessTokenInfo);
        			if(accessTokenRetrieved) {
        				team.setTwitterAccessToken(accessTokenInfo.get(0));
        				team.setTwitterAccessTokenSecret(accessTokenInfo.get(1));
        				team.setTwitterScreenName(accessTokenInfo.get(2));
        				// Twitter has been fully authorized. Only now is the team truly using Twitter ....
        				team.setUseTwitter(true);
    					
    					// schedule task that will post recent Activity to Twitter
    					scheduleSynchActivityWithTwitterTask(KeyFactory.keyToString(team.getKey()));
    					log.info("after scheduleSynchActivityWithTwitterTask() call");
        			} else {
        				apiStatus = ApiStatusCode.TWITTER_ERROR;
        			}

        			em.getTransaction().commit();
        		} catch (NoResultException e) {
        			// Not an error - twitter callback is not longer active (though not sure why this would happen)
        			apiStatus = ApiStatusCode.TWITTER_CALLBACK_TOKEN_NO_LONGER_ACTIVE;
        		} catch (NonUniqueResultException e) {
        			log.severe("updateMessageThread(): should never happen - two or more teams have same oneUseToken");
        			this.setStatus(Status.SERVER_ERROR_INTERNAL);
        		}  // "finally" to clean up entity manager is done by the outer try/catch block
        		
    			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
    				jsonReturn.put("apiStatus", apiStatus);
    				return new JsonRepresentation(jsonReturn);
    			}

        	} else {
    			// --------------------------------------------------
    			// This is the 'Update team' API call
    			// --------------------------------------------------
        		log.info("this is 'Update team' API handling");
        		
            	User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    			if(currentUser == null) {
    				this.setStatus(Status.SERVER_ERROR_INTERNAL);
    				log.severe("user could not be retrieved from Request attributes!!");
    			}
    			// teamId is required
    			else if(this.teamId == null || this.teamId.length() == 0) {
    				apiStatus = ApiStatusCode.TEAM_ID_REQUIRED;
    				log.info("invalid team ID");
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
    			List<Member> members = team.getTeamMates(currentUser);
    			
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
    	
    			JsonRepresentation jsonRep = new JsonRepresentation(entity);
    			JSONObject json = jsonRep.toJsonObject();
    			log.info("received json object = " + json.toString());
    			
    			// if new field is empty, original value is not updated.
    			if(json.has("teamName")) {
    				String oldTeamName = team.getTeamName() != null ? team.getTeamName() : "";
    				String teamName = json.getString("teamName");
    				if(teamName.length() > 0) team.setTeamName(teamName);
    				if(!oldTeamName.equalsIgnoreCase(teamName)) {
    					String nameUpdateMessage = Utility.getModMessage("Team Name", oldTeamName, teamName);
    					notificationMessage = notificationMessage + " " + nameUpdateMessage;
    				}
    			}
    			
    			// if new field is empty, original value is not updated.
    			if(json.has("description")) {
    				String oldDescription = team.getDescription() != null ? team.getDescription() : "";
    				String description = json.getString("description");
    				if(description.length() > 0) team.setDescription(description);
    				if(!oldDescription.equalsIgnoreCase(description)) {
    					String descriptionUpdateMessage = Utility.getModMessage("Team Description", oldDescription, description);
    					notificationMessage = notificationMessage + descriptionUpdateMessage;
    				}
    			}
    			
    			if(json.has("leagueName")) {
    				String leagueName = json.getString("leagueName");
    				team.setLeagueName(leagueName);
    			}
    			
    			// no action taken if not specified
    			boolean twitterAuthorizationInitiated = false;
    			boolean twitterDisassociationInitiated = false;
    			if(json.has("useTwitter")) {
    				Boolean useTwitter = json.getBoolean("useTwitter");
    				
    				//::BUSINESS_RULE:: must be NA to set up a Twitter account
    				if(useTwitter && !currentUser.getIsNetworkAuthenticated()) {
						log.info("user must be network authenticated to connect to a Twitter account");
						jsonReturn.put("apiStatus", ApiStatusCode.USER_NOT_NETWORK_AUTHENTICATED);
						return new JsonRepresentation(jsonReturn);
    				}
    				
    				Boolean oldUseTwitter = team.getUseTwitter();
    				if(oldUseTwitter == null) oldUseTwitter = false;
    				// going from 'false' to 'true'
    				if(!oldUseTwitter && useTwitter) {
    					// get the twitter request token so twitter authorization token can be returned.
    					twitterAuthorizationInitiated = true;
    					team.setOneUseToken(TF.get());
    					team.setOneUseTokenStatus(Team.NEW_TOKEN_STATUS);
    					// create ArrayList to use as 'out' parameter
    					List<String> twitterRequestInfo = new ArrayList<String>();
    					boolean twitterCallSuccessful = TwitterClient.getRequestToken(team.getOneUseToken(), twitterRequestInfo);
    					if(twitterCallSuccessful) {
    						team.setTwitterAuthorizationUrl(twitterRequestInfo.get(0));
    						team.setTwitterRequestToken(twitterRequestInfo.get(1));
    						team.setTwitterRequestTokenSecret(twitterRequestInfo.get(2));
    					} else {
    						// twitter call failed
    						// TODO ?create a separate twitter status so failing twitter doesn't fail the entire create Team API?
    						jsonReturn.put("apiStatus", ApiStatusCode.TWITTER_ERROR);
    						return new JsonRepresentation(jsonReturn);
    					}
    				}
    				// going from 'true' to 'false'
    				else if(oldUseTwitter && !useTwitter) {
    					// this deactivates twitter activity
    					team.setTwitterAccessToken(null);
    					team.setTwitterAccessTokenSecret(null);
    					team.setTwitterAuthorizationUrl(null);
    					team.setTwitterRequestToken(null);
    					team.setTwitterRequestTokenSecret(null);
    					team.setTwitterScreenName(null);
    					team.setNewestTwitterId(null);
    					//team.setNewestCacheId(null);
    					twitterDisassociationInitiated = true;
    				}
    				// even if useTwitter being requested, not persisted as 'true' until Twitter authorization is complete
    				team.setUseTwitter(false);
    			}
    			
    			if(json.has("teamSiteUrl")) {
    				String oldTeamSiteUrl = team.getSiteUrl() != null ? team.getSiteUrl() : "";
    				String teamSiteUrl = json.getString("teamSiteUrl");
    				team.setSiteUrl(teamSiteUrl);
    				if(!oldTeamSiteUrl.equalsIgnoreCase(teamSiteUrl)) {
    					String siteUrlUpdateMessage = Utility.getModMessage("Team Site", oldTeamSiteUrl, teamSiteUrl);
    					notificationMessage = notificationMessage + " " + siteUrlUpdateMessage;
    				}
    			}
    			
    			if(json.has("gender")) {
    				String gender = json.getString("gender");
    				team.setGender(gender);
    			}
    			
    			if(json.has("sport")) {
    				String oldSport = team.getSport() != null ? team.getSport() : "";
    				String sport = json.getString("sport");
    				team.setSport(sport);
    				if(!oldSport.equalsIgnoreCase(sport)) {
    					String sportUpdateMessage = Utility.getModMessage("Team Sport", oldSport, sport);
    					notificationMessage = notificationMessage + " " + sportUpdateMessage;
    				}
    			}
				
				Boolean isPortrait = null;
				if(json.has("isPortrait")) {
					isPortrait = json.getBoolean("isPortrait");
					log.info("json isPortrait = " + isPortrait);
				}
    			
    			if(json.has("photo")) {
					if(isPortrait == null) {
						jsonReturn.put("apiStatus", ApiStatusCode.IS_PORTRAIT_AND_PHOTO_MUST_BE_SPECIFIED_TOGETHER);
						return new JsonRepresentation(jsonReturn);
					}
					
    				String photoBase64 = json.getString("photo");
    				try {
    					String oldPhoto = team.getPhotoBase64() == null ? "" : team.getPhotoBase64();
    					
    					// decode the base64 encoding to create the thumb nail
    					byte[] rawPhoto = Base64.decode(photoBase64);
    					ImagesService imagesService = ImagesServiceFactory.getImagesService();
    					Image oldImage = ImagesServiceFactory.makeImage(rawPhoto);
    					
    					// start by defaulting to landscape
    					int tnWidth = Team.THUMB_NAIL_LONG_SIDE;
						int tnHeight = Team.THUMB_NAIL_SHORT_SIDE;
						if(isPortrait) {
							// If the team photo is portrait orientation, a square image using the SHORT_SIZE dimension
							// is returned. Need to crop the image before resizing it to prevent distortion. Assumption: 
							// aspect ratio of portrait image is 3 x 4
							//makeCrop(double leftX, double topY, double rightX, double bottomY)
							Transform crop = ImagesServiceFactory.makeCrop(0.0, 0.0, 1.0, 0.75);
							oldImage = imagesService.applyTransform(crop, oldImage);
	    					tnWidth = Team.THUMB_NAIL_SHORT_SIDE;
							tnHeight = Team.THUMB_NAIL_SHORT_SIDE;
						}
						Transform resize = ImagesServiceFactory.makeResize(tnWidth, tnHeight);
    					Image newImage = imagesService.applyTransform(resize, oldImage);
    					String thumbNailBase64 = Base64.encode(newImage.getImageData());
    					
    					team.setThumbNailBase64(thumbNailBase64);
    					team.setPhotoBase64(photoBase64);
    					
    					String teamUpdateMessage = "Team Photo was updated";
    					if(oldPhoto.length() == 0 && photoBase64.length() > 0) {
    						teamUpdateMessage = "Team Photo was added";
    					} else if(oldPhoto.length() > 0 && photoBase64.length() == 0) {
    						teamUpdateMessage = "Team Photo was removed";
    					}
    					log.info("modMessage = " + teamUpdateMessage);
    					notificationMessage = notificationMessage + " " + teamUpdateMessage;
    				} catch(Base64DecoderException e) {
    					apiStatus = ApiStatusCode.INVALID_PHOTO_PARAMETER;
    				}
    			}
    		    
    			// TODO for test only. Remove somehow for production
    			if(json.has("twitterAccessToken")) {
    				String twitterAccessToken = json.getString("twitterAccessToken");
    				team.setTwitterAccessToken(twitterAccessToken);
    			}
    		    
    			// TODO for test only. Remove somehow for production
    			if(json.has("twitterAccessTokenSecret")) {
    				String twitterAccessTokenSecret = json.getString("twitterAccessTokenSecret");
    				team.setTwitterAccessTokenSecret(twitterAccessTokenSecret);
    			}
    		    
    			// TODO for test only. Remove somehow for production
    			if(json.has("newestTwitterId")) {
    				Long newestTwitterId = json.getLong("newestTwitterId");
    				team.setNewestTwitterId(newestTwitterId);
    			}
    		    
    			// TODO for test only. Remove somehow for production
    			if(json.has("newestCacheId")) {
    				Long newestCacheId = json.getLong("newestCacheId");
    				team.setNewestCacheId(newestCacheId);
    			}
    		    
    			// TODO for test only. Remove somehow for production
    			if(json.has("useTwitterForced")) {
    				Boolean useTwitterForced = json.getBoolean("useTwitterForced");
    				team.setUseTwitter(useTwitterForced);
    			}

    			em.getTransaction().commit();
    			
    			//::BUSINESS_RULE:: coordinator must be network authenticated for a notification to be sent
    			if(isCoordinatorNetworkAuthenticated && notificationMessage.length() > 0) {
    				PubHub.sendTeamInfoUpdateMessage(members, notificationMessage, team);
    			}
    			
    			if(twitterAuthorizationInitiated) {
    				jsonReturn.put("twitterAuthorizationUrl", team.getTwitterAuthorizationUrl());
    			}
//--------------------------------------------------------------------------------------------------------------------------------------
// commented out on 3/26/2011
// remove this after rTeam Release 2 is in app store
//
//    			if(twitterDisassociationInitiated) {
//    				///////////////////////////////////////////////////////////////////
//    				// Must update User -- zero out the teamNewestCacheId for this team
//    				///////////////////////////////////////////////////////////////////
//    				EntityManager em2 = EMF.get().createEntityManager();
//    				em2.getTransaction().begin();
//    		        try {
//    	        		// currentUser was retrieved during authentication outside the transaction. Get it again inside the transaction
//    	        		// so that is is managed by the entityManager.
//    	    			currentUser = (User)em2.createNamedQuery("User.getByKey")
//    	        			.setParameter("key", currentUser.getKey())
//    	        			.getSingleResult();
//    	    			currentUser.resetNewestCacheId(this.teamId);
//    		    		em2.getTransaction().commit();
//    		        } catch (NoResultException e) {
//    					log.severe("user not found");
//    					this.setStatus(Status.SERVER_ERROR_INTERNAL);
//    				} catch (NonUniqueResultException e) {
//    					log.severe("should never happen - two or more users have same key");
//    					e.printStackTrace();
//    					this.setStatus(Status.SERVER_ERROR_INTERNAL);
//    				} finally {
//    				    if (em2.getTransaction().isActive()) {
//    				        em2.getTransaction().rollback();
//    				    }
//    				    em2.close();
//    				}
//    				
//    				////////////////////////////////////////////
//    				// Remove all team activity via a Task Queue
//    				////////////////////////////////////////////
//    				scheduleRemoveTeamActivityTask(this.teamId);
//    			}
//--------------------------------------------------------------------------------------------------------------------------------------

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
        	apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
			log.info(apiStatus);
		} catch (NonUniqueResultException e) {
			log.severe("should never happen - two or more teams have same team name");
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
    
    private void scheduleRemoveTeamActivityTask(String theTeamId) {
		// URL "/removeTeamActivityTask" is routed to ActivityTaskServlet in web.xml
		// not calling name() to name the task, so the GAE will assign a unique name that has not been used in 7 days (see book)
		// method defaults to POST, but decided to be explicit here
		// PRIORITY TODO need to somehow secure this URL. Book uses web.xml <security-constraint> but not sure why it restricts the
		//               URL to task queues (I see how it restricts it to admins)
		TaskOptions taskOptions = TaskOptions.Builder.url("/removeTeamActivityTask")
				.method(Method.POST)
				.param("teamId", theTeamId);
		Queue queue = QueueFactory.getQueue("removeTeamActivity"); // "removeTeamActivity" queue is defined in WEB-INF/queue.xml
		queue.add(taskOptions);
    }
    
    @SuppressWarnings("deprecation")
	private void scheduleSynchActivityWithTwitterTask(String theTeamId) {
		// URL "/synchActivityWithTwitterTask" is routed to SynchActivityWithTwitterTaskServlet in web.xml
		// not calling name() to name the task, so the GAE will assign a unique name that has not been used in 7 days (see book)
		// method defaults to POST, but decided to be explicit here
		// PRIORITY TODO need to somehow secure this URL. Book uses web.xml <security-constraint> but not sure why it restricts the
		//               URL to task queues (I see how it restricts it to admins)
		TaskOptions taskOptions = TaskOptions.Builder.url("/synchActivityWithTwitterTask")
				.method(Method.POST)
				.param("teamId", theTeamId);
		Queue queue = QueueFactory.getQueue("synchActivityWithTwitter"); // "synchActivityWithTwitter" queue is defined in WEB-INF/queue.xml
		queue.add(taskOptions);
    }
}