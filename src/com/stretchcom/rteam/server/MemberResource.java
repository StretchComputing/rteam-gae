package com.stretchcom.rteam.server;
	
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
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
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
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
 * @author joepwro
 */
public class MemberResource extends ServerResource {
	//private static final Logger log = Logger.getLogger(MemberResource.class.getName());
	private RskyboxClient log = new RskyboxClient(this);
	
	private static final String ACCEPT_DELETE = "accept";
	private static final String REJECT_DELETE = "reject";

    // The sequence of characters that identifies the resource.
    String teamId;
    String memberId;
    String emailAddress;
    String oneUseToken;
    String decision;
    String includePhoto;
    
    @Override  
    protected void doInit() throws ResourceException {  
    	log.info("API requested with URL: " + this.getReference().toString());
    	
        // Get the "teamId" attribute value taken from the URI template /team/{teamId} 
        this.teamId = (String)getRequest().getAttributes().get("teamId"); 
        log.debug("MemberResource:doInit() - teamId = " + this.teamId);
        if(this.teamId != null) {
            this.teamId = Reference.decode(this.teamId);
            log.debug("MemberResource:doInit() - decoded teamId = " + this.teamId);
        }
   
        // Get the "memberId" attribute value taken from the URI template /team/{teamId} 
        this.memberId = (String)getRequest().getAttributes().get("memberId"); 
        log.debug("MemberResource:doInit() - memberId = " + this.memberId);
        if(this.memberId != null) {
            this.memberId = Reference.decode(this.memberId);
            log.debug("MemberResource:doInit() - decoded memberId = " + this.memberId);
        }
    
		// same method is used to handle both 'Get Member Info' and 'Get Membership Status'
        // for 'Get Membership Status', the email address comes in as query parameters
        Form form = getRequest().getResourceRef().getQueryAsForm();
		for (Parameter parameter : form) {
			log.debug("parameter " + parameter.getName() + " = " + parameter.getValue());
			if(parameter.getName().equals("emailAddress"))  {
				this.emailAddress = (String)parameter.getValue();
				this.emailAddress = Reference.decode(this.emailAddress);
				this.emailAddress = this.emailAddress.toLowerCase();
				log.debug("UserResource:doInit() - decoded emailAddress = " + this.emailAddress);
			} else if(parameter.getName().equals("oneUseToken")) {
				this.oneUseToken = (String)parameter.getValue();
				this.oneUseToken = Reference.decode(this.oneUseToken);
				log.debug("MemberResource:doInit() - decoded oneUseToken = " + this.oneUseToken);
			} else if(parameter.getName().equals("decision")) {
				this.decision = (String)parameter.getValue();
				this.decision = Reference.decode(this.decision);
				log.debug("MemberResource:doInit() - decoded decision = " + this.decision);
			} else if(parameter.getName().equals("includePhoto"))  {
				this.includePhoto = (String)parameter.getValue();
				this.includePhoto = Reference.decode(this.includePhoto);
				log.debug("UserResource:doInit() - decoded includePhoto = " + this.includePhoto);
			}

		}
    }  

    // Handles 'Get member info' API 
	// Handles 'Get membership status' API
    @Get("json")
    public JsonRepresentation getMemberInfo(Variant variant) {
        log.debug("MemberResource:toJson() entered");
        JSONObject jsonReturn = new JSONObject();
		EntityManager em = EMF.get().createEntityManager();

		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
        Team team = null;
		try {
        	if(this.emailAddress != null) {
    			// --------------------------------------------
				// This is the 'Get Membership Status' API call
    			// --------------------------------------------
        		
        		// Only return memberships that are NA.  User won't be able to bind to them later if there not NA.
        		List<Member>  members= (List<Member> ) em.createNamedQuery("Member.getByNetworkAuthenticatedEmailAddress")
							.setParameter("emailAddress", this.emailAddress)
				 			.getResultList();
        		
        		String firstName = "";
        		String lastName = "";
        		for(Member m : members){
        			String memberFirstName = m.getFirstNameByEmailAddress(this.emailAddress);
        			if(firstName.length() == 0 && memberFirstName != null && memberFirstName.length() > 0) {
        				firstName = memberFirstName;
        			}
        			
        			String memberLastName = m.getLastNameByEmailAddress(this.emailAddress);
        			if(lastName.length() == 0 && memberLastName != null && memberLastName.length() > 0) {
        				lastName = memberLastName;
        			}
        		}
				
				jsonReturn.put("numberOfTeams", members.size());
				jsonReturn.put("firstName", firstName);
				jsonReturn.put("lastName", lastName);
        	} else {
    			// --------------------------------------------
				// This is the 'Get Member Info' API call
    			// --------------------------------------------

    	    	User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    			if(currentUser == null) {
    				this.setStatus(Status.SERVER_ERROR_INTERNAL);
    				log.error("MemberResource:getMemberInfo:currentUser", "user could not be retrieved from Request attributes!!");
    			}
    			// teamId is required
    			else if(this.teamId == null || this.teamId.length() == 0) {
     				apiStatus = ApiStatusCode.TEAM_ID_REQUIRED;
    				log.debug("invalid team ID");
    			} else {
    				try {
        	    		team = (Team)em.createNamedQuery("Team.getByKey")
    						.setParameter("key", KeyFactory.stringToKey(this.teamId))
    						.getSingleResult();
            			
        	    		if(!currentUser.isUserMemberOfTeam(this.teamId)) {
            				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
            				log.debug(apiStatus);
                    	}
    				} catch (NoResultException e) {
    		        	apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
    		        	log.debug("no result exception, team not found");
    				} 
    				
    			}
    			
    			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
    				jsonReturn.put("apiStatus", apiStatus);
    				return new JsonRepresentation(jsonReturn);
    			}
    			
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

        		Key memberKey = KeyFactory.stringToKey(this.memberId);
        		Member member = (Member)em.createNamedQuery("Member.getByKey")
        			.setParameter("key", memberKey)
        			.getSingleResult();
        		log.debug("member retrieved = " + member.getFullName());
            	
            	jsonReturn.put("firstName", member.getFirstName());
            	jsonReturn.put("lastName", member.getLastName());
            	//::BUSINESS_RULE email addresses only returned to coordinators
            	if(isCoordinator && member.getEmailAddress() != null) jsonReturn.put("emailAddress", member.getEmailAddress());
            	if(member.getJerseyNumber() != null) jsonReturn.put("jerseyNumber", member.getJerseyNumber());
            	jsonReturn.put("isNetworkAuthenticated", member.getIsNetworkAuthenticated());
            	jsonReturn.put("isSmsConfirmed", member.isPhoneNumberSmsConfirmed(member.getPhoneNumber()));
            	jsonReturn.put("isEmailConfirmed", member.getIsEmailAddressNetworkAuthenticated());
            	jsonReturn.put("participantRole", member.getParticipantRole());
            	if(member.getPhoneNumber() != null) jsonReturn.put("phoneNumber", member.getPhoneNumber());
            	
            	JSONArray jsonArray = new JSONArray();
				List<Guardian> guardians = member.getGuardians();
    			for(Guardian g : guardians) {
    				JSONObject jsonGuardianObj = new JSONObject();
    				jsonGuardianObj.put("key", g.getKey());
        			//::BUSINESS_RULE email addresses only returned to coordinators
                	if(isCoordinator) {
        				jsonGuardianObj.put("emailAddress", g.getEmailAddress());
                	}
    				jsonGuardianObj.put("firstName", g.getFirstName());
    				jsonGuardianObj.put("lastName", g.getLastName());
    				jsonGuardianObj.put("phoneNumber", g.getPhoneNumber());
    				jsonGuardianObj.put("isNetworkAuthenticated", g.getIsNetworkAuthenticated());
    				jsonGuardianObj.put("isSmsConfirmed", g.getHasBeenSmsConfirmed());
    				jsonGuardianObj.put("isEmailConfirmed", g.getIsEmailAddressNetworkAuthenticated());
    				jsonArray.put(jsonGuardianObj);
    			}
    			jsonReturn.put("guardians", jsonArray);
            	
            	JSONArray roleJsonArray = new JSONArray();
            	List<String> roles = member.getRoles();
            	log.debug("number of roles = " + roles.size());
            	for(String r : roles) {
            		roleJsonArray.put(r);
            		log.debug("role sent by client = " + r);
            	}
            	jsonReturn.put("roles", roleJsonArray);
            	if(member.getGender() != null) jsonReturn.put("gender", member.getGender());
            	if(member.getAge() != null) jsonReturn.put("age", member.getAge());
            	if(member.getStreetAddress() != null) jsonReturn.put("streetAddress", member.getStreetAddress());
            	if(member.getCity() != null) jsonReturn.put("city", member.getCity());
            	if(member.getState() != null) jsonReturn.put("state", member.getState());
            	if(member.getZipcode() != null) jsonReturn.put("zipcode", member.getZipcode());
            	
				Boolean photoReturned = false;
            	if(this.includePhoto != null && this.includePhoto.equalsIgnoreCase("true")) {
		    		String photoBase64 = member.getPhotoBase64();
		    		if(photoBase64 != null) {
		        		jsonReturn.put("photo", member.getPhotoBase64());
		        		photoReturned = true;
		    		}
				} 
            	
        		// if the photo was not returned, then attempt to return the thumb nail
            	if(!photoReturned && member.getThumbNailBase64() != null) {
            		jsonReturn.put("thumbNail", member.getThumbNailBase64());
            	}
        	}
        } catch (NoResultException e) {
        	apiStatus = ApiStatusCode.MEMBER_NOT_FOUND;
        	log.debug("no result exception, member not found");
		} catch (JSONException e) {
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
			log.exception("MemberResource:getMemberInfo:JSONException", "", e);
		} catch (NonUniqueResultException e) {
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
			log.exception("MemberResource:getMemberInfo:NonUniqueResultException", "two or more members have same ID", e);
		} finally {
			em.close();
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("MemberResource:getMemberInfo:JSONException", "", e);
		}
        return new JsonRepresentation(jsonReturn);
    }
    
    
    // Handles 'Delete member' API 
    // Handles 'Delete member confirmation' API 
    @Delete
    public JsonRepresentation remove() {
    	log.debug("MemberResource:remove() entered");
    	EntityManager em = EMF.get().createEntityManager();
    	EntityTransaction txn = em.getTransaction();
    	JSONObject jsonReturn = new JSONObject();
    	
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
    	Team team = null;
        try {
        	if(this.oneUseToken != null) {
    			// -------------------------------------------------
    			// This is the 'Delete member confirmation' API call
    			// -------------------------------------------------
    			boolean shouldDeleteMember = false;
    			String memberToDeleteId = null;
        		try {
        			txn.begin();
        			Recipient recipient = (Recipient)em.createNamedQuery("Recipient.getByOneUseTokenAndTokenStatus")
        				.setParameter("oneUseToken", this.oneUseToken)
        				.setParameter("oneUseTokenStatus", Recipient.NEW_TOKEN_STATUS)
        				.getSingleResult();
        			log.debug("remove(): recipient found");
        			
        			// this is a "first" response wins scenario.  So we need to mark all recipients that are part of this
        			// message thread as no longer active.
        			List<Recipient> recipients = recipient.getMessageThread().getRecipients();
        			for(Recipient r : recipients) {
        				r.setOneUseTokenStatus(Recipient.USED_TOKEN_STATUS);
        			}
        			
        			// determine if the membership should be deleted or not
        			if(this.decision.equalsIgnoreCase(ACCEPT_DELETE)) {
        				shouldDeleteMember = true;
        				memberToDeleteId = recipient.getMemberId();
        			}
        			
        			txn.commit();
        		} catch (NoResultException e) {
        			// Not an error - multiple people associated with the same membership may respond. Or, the same user may click
        			// on the link in the email multiple times.  In either case, it is inactive after first response.
        			apiStatus = ApiStatusCode.DELETE_MEMBER_CONFIRMATION_LINK_NO_LONGER_ACTIVE;
        		} catch (NonUniqueResultException e) {
        			log.exception("MemberResource:remove:NonUniqueResultException1", "two or more recipients have same oneUseToken", e);
        			this.setStatus(Status.SERVER_ERROR_INTERNAL);
        		}
        		
        		// in another transaction, delete the member if necessary
        		if(shouldDeleteMember) {
        			EntityManager em2 = EMF.get().createEntityManager();
        	    	try {
        	    		em2.getTransaction().begin();
        				Key memberKey = KeyFactory.stringToKey(memberToDeleteId);
        				Member memberToRemove = (Member)em2.createNamedQuery("Member.getByKey")
        	    			.setParameter("key", memberKey)
        	    			.getSingleResult();
        	    		log.debug("member being removed = " + memberToRemove.getFullName());
        	    		
                    	jsonReturn.put("firstName", memberToRemove.getFirstName());
            			jsonReturn.put("lastName", memberToRemove.getLastName());
            			jsonReturn.put("emailAddress", memberToRemove.getEmailAddress());
            			
            			em2.remove(memberToRemove);
        	    		em2.getTransaction().commit();
        	    	} catch (NoResultException e) {
        				this.setStatus(Status.SERVER_ERROR_INTERNAL);
            			log.exception("MemberResource:remove:NoResultException1", "member could not be found using memberId stored in recipient entity", e);
        			} catch (NonUniqueResultException e) {
        				this.setStatus(Status.SERVER_ERROR_INTERNAL);
            			log.exception("MemberResource:remove:NonUniqueResultException2", "two or more members have same member ID", e);
        			} finally {
            		    if (em2.getTransaction().isActive()) {
            		    	em2.getTransaction().rollback();
            		    }
            		    em2.close();
            		}
        		}
        	} else {
    			// -------------------------------------
    			// This is the 'Delete member' API call
    			// -------------------------------------
    	    	User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    			if(currentUser == null) {
    				this.setStatus(Status.SERVER_ERROR_INTERNAL);
    				log.error("MemberResource:remove:currentUser", "user could not be retrieved from Request attributes!!");
    			}
    			// teamId is required
    			else if(this.teamId == null || this.teamId.length() == 0) {
    				apiStatus = ApiStatusCode.TEAM_ID_REQUIRED;
    				log.debug("invalid team ID");
    			} else {
    				try {
        	    		team = (Team)em.createNamedQuery("Team.getByKey")
    						.setParameter("key", KeyFactory.stringToKey(this.teamId))
    						.getSingleResult();
            			
        	    		if(!currentUser.isUserMemberOfTeam(this.teamId)) {
            				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
            				log.debug(apiStatus);
                    	}
    				} catch (NoResultException e) {
    		        	apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
    		        	log.debug("no result exception, team not found");
    				} catch (NonUniqueResultException e) {
    					this.setStatus(Status.SERVER_ERROR_INTERNAL);
            			log.exception("MemberResource:remove:NonUniqueResultException3", "two or more teams have same ID", e);
    				}
    			}
    			
    			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
    				jsonReturn.put("apiStatus", apiStatus);
    				return new JsonRepresentation(jsonReturn);
    			}
    			
    			Member currentUserCoordinatorMembership = null;
    			if(currentUser.getIsNetworkAuthenticated()) {
    				List<Member> memberships =  Member.getMemberShipsWithEmailAddress(currentUser.getEmailAddress(), team);
    				Boolean isCoordinator = false;
    				for(Member m : memberships) {
        	    		if(m.isCoordinator()) {
        	    			isCoordinator = true;
        	    			currentUserCoordinatorMembership = m;
        	    			break;
        	    		}
    				}
    				if(!isCoordinator) {
    					apiStatus = ApiStatusCode.USER_NOT_A_COORDINATOR;
    				}
    			} else {
        			apiStatus = ApiStatusCode.USER_NOT_NETWORK_AUTHENTICATED;
        			log.debug("user must be network authenticated");
    			}
            	
    			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
    				jsonReturn.put("apiStatus", apiStatus);
    				return new JsonRepresentation(jsonReturn);
    			}
    			
    	    	txn.begin();
    	    	Member memberToRemove = null;
    	    	try {
    				Key memberKey = KeyFactory.stringToKey(this.memberId);
    				memberToRemove = (Member)em.createNamedQuery("Member.getByKey")
    	    			.setParameter("key", memberKey)
    	    			.getSingleResult();
    	    		log.debug("member to be removed = " + memberToRemove.getFullName());
    	    	} catch (NoResultException e) {
    	        	apiStatus = ApiStatusCode.MEMBER_NOT_FOUND;
    	        	log.debug("no result exception, member user not found");
    			} catch (NonUniqueResultException e) {
    				this.setStatus(Status.SERVER_ERROR_INTERNAL);
        			log.exception("MemberResource:remove:NonUniqueResultException4", "two or more members have same member ID", e);
    			}
        		
        		//::BUSINESS_RULE:: Deleting a team creator is a request. Creator sent an email and given option to not allow.
        		Boolean creatorDeleteRequested = false;
        		if(memberToRemove != null) {
        			if(currentUserCoordinatorMembership.getKey().equals(memberToRemove.getKey())) {
        				apiStatus = ApiStatusCode.USER_CANNOT_DELETE_SELF;
        				log.debug("user cannot delete himself/herself");
        			} else if(memberToRemove.isCreator() && memberToRemove.getIsNetworkAuthenticated()) {
        				creatorDeleteRequested = true;
        			} 
        		} 
        		
    			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
    				jsonReturn.put("apiStatus", apiStatus);
    				return new JsonRepresentation(jsonReturn);
    			}

    			boolean wasMemberRemoved = false;
    			boolean wasMemberMarkedForDeletion = false;
    			if(creatorDeleteRequested) {
    				memberToRemove.setIsMarkedForDeletion(true);
    				memberToRemove.setMarkedForDeletionOn(new Date());
    				memberToRemove.setMarkedForDeletionRequester(currentUserCoordinatorMembership.getFullName());
    				wasMemberMarkedForDeletion = true;
    				// Call getTeam() before transaction finishes below. Team needed after transaction and getting
    				// error unless team is first accessed now.
    				memberToRemove.getTeam();
    	        	log.debug("coordinator delete requested. Confirmation message will be sent to coordinator.");
    			} else {
    				em.remove(memberToRemove);
    	        	log.debug("member deleted successfully");
    	        	if(memberToRemove == null) {
    	        		log.debug("after remove(), memberToRemove is now NULL");
    	        	} else {
    	        		log.debug("after remove(), memberToRemove is NOT NULL");
    	        	}
    	        	wasMemberRemoved = true;
    			}
    			txn.commit();
    			
    			// only send message after transaction is committed
    			if(wasMemberMarkedForDeletion) {
    				PubHub.sendDeleteConfirmationMessage(memberToRemove);
    			}
    			
    			// For all individuals that were part of the membership removed, find the associated User if it exists
    			// and remove the team from the user.
    			if(wasMemberRemoved) {
    				EntityManager em2 = EMF.get().createEntityManager();
    		        try {
    					// tried to update all users in a single transaction, but got the following error:
    	        		// "can't operate on multiple entity groups in a single transaction"
    	        		// alternate solution. Create a separate transaction for each user deleted
    		        	List<String> allMemberEmailAddresses = memberToRemove.getNetworkAuthenticatedActiveEmailAddresses();
    		        	for(String ea : allMemberEmailAddresses) {
    			        	try {
    							em2.getTransaction().begin();
    							User userOfMemberRemoved = (User)em.createNamedQuery("User.getByEmailAddressAndIsNetworkAuthenticated")
    								.setParameter("emailAddress", ea)
    								.setParameter("isNetworkAuthenticated", true)
    								.getSingleResult();
    	    		
    							userOfMemberRemoved.removeTeam(memberToRemove.getTeam().getKey());
    							em2.getTransaction().commit();
    			        	} catch (NoResultException e) {
    				        	// THIS IS NOT AN ERROR -- there may be no user entity associated with the member.
    						} finally {
    						    if (em2.getTransaction().isActive()) {
    						        em2.getTransaction().rollback();
    						    }
    						}
    		        	}
    		        } catch (NonUniqueResultException e) {
            			log.exception("MemberResource:remove:NonUniqueResultException5", "two or more users have same key", e);
    				} finally {
    				    if (em2.getTransaction().isActive()) {
    				        em2.getTransaction().rollback();
    				    }
    				    em2.close();
    				}
    			}
        	}
        } catch (JSONException e) {
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
			log.exception("MemberResource:remove:JSONException1", "", e);
		} finally {
		    if (txn.isActive()) {
		    	txn.rollback();
		    }
		    em.close();
		}
    
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("MemberResource:remove:JSONException2", "", e);
		}
        return new JsonRepresentation(jsonReturn);
    }

    //Handles 'Update Member' API 
    @Put 
    public JsonRepresentation updateMember(Representation entity) {
    	JSONObject jsonReturn = new JSONObject();
    	log.debug("updateMember(@Put) entered ..... ");
		EntityManager em = EMF.get().createEntityManager();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		Boolean isEmailUpdated = false;
        List<UserMemberInfo> potentialAssociatedIndividuals = new ArrayList<UserMemberInfo>();
        UserMemberInfo primaryIndividual = new UserMemberInfo();
		Team team = null;
        try {
	    	User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
			if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				log.error("MemberResource:updateMember:currentUser", "user could not be retrieved from Request attributes!!");
			}
			// teamId is required
			else if(this.teamId == null || this.teamId.length() == 0) {
				apiStatus = ApiStatusCode.TEAM_ID_REQUIRED;
				log.debug("invalid team ID");
			}
			//::BUSINESS_RULE: current user must be a member of the team 
			else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
				log.debug(apiStatus);
        	}
			else {
				try {
    	    		team = (Team)em.createNamedQuery("Team.getByKey")
						.setParameter("key", KeyFactory.stringToKey(this.teamId))
						.getSingleResult();
        			
				} catch (NoResultException e) {
		        	apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
		        	log.debug("no result exception, team not found");
				} catch (NonUniqueResultException e) {
					this.setStatus(Status.SERVER_ERROR_INTERNAL);
					log.exception("MemberResource:updateMember:NonUniqueResultException", "", e);
				}
			}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			
			//::BUSINESSRULE:: user must be the team creator or network authenticated to create a new member.
			Boolean isCreator = team.isCreator(currentUser.getEmailAddress());
			if(!isCreator && !currentUser.getIsNetworkAuthenticated()) {
				apiStatus = ApiStatusCode.USER_NOT_CREATOR_NOR_NETWORK_AUTHENTICATED;
				log.debug("user is not the team creator nor network authenticated");
			} 
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}

   			Boolean isCoordinator = false;
			List<Member> naMemberships = Member.getMemberShipsWithEmailAddress(currentUser.getEmailAddress(), team);
   			if(isCreator) {
   				// creator is by definition a coordinator too
   				isCoordinator = true;
   			} else {
				for(Member m : naMemberships) {
    	    		if(m.isCoordinator()) {
    	    			isCoordinator = true;
    	    			break;
    	    		}
				}
   			}

			em.getTransaction().begin();
			Member memberBeingModified = null;
	    	try {
				Key memberKey = KeyFactory.stringToKey(this.memberId);
				memberBeingModified = (Member)em.createNamedQuery("Member.getByKey")
	    			.setParameter("key", memberKey)
	    			.getSingleResult();
	    		log.debug("member retrieved = " + memberBeingModified.getFullName());
	    	} catch (NoResultException e) {
	        	apiStatus = ApiStatusCode.MEMBER_NOT_FOUND;
	        	log.debug("no result exception, member to modify not found");
			} catch (NonUniqueResultException e) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				log.exception("MemberResource:updateMember:NonUniqueResultException2", "", e);
			}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
    		
			// isMemberOwner is true if primary member (i.e. not a guardian)
			Boolean isMemberOwner = false;
			for(Member m : naMemberships) {
	    		if(m.getKey().equals(memberBeingModified.getKey())) {
	    			isMemberOwner = true;
	    			break;
	    		}
			}

    		//::BUSINESS_RULE  current user must either be a coordinator on the team or one of the "owners" of the member object
    		if(isCoordinator == false && isMemberOwner == false) {
	        	apiStatus = ApiStatusCode.USER_NOT_A_COORDINATOR_NOR_OWNING_MEMBER;
	        	log.debug("user must either be a coordinator on the team or one of the 'owners' of the member object");
    		}
        	
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}

			JsonRepresentation jsonRep = new JsonRepresentation(entity);
			JSONObject json = jsonRep.toJsonObject();
			log.debug("received json object = " + json.toString());
			
			// record the changes as we go along so notification message can show details of the updated
			List<String> modificationMessages = new ArrayList<String>();
			
			// if new field is empty, field is cleared.
			if(json.has("firstName")) {
				String firstName = json.getString("firstName");
				String oldFirstName = memberBeingModified.getFirstName() == null ? "" : memberBeingModified.getFirstName();
				if(!firstName.equalsIgnoreCase(oldFirstName)) {
					memberBeingModified.setFirstName(firstName);
					modificationMessages.add(Utility.getModMessage("First Name", oldFirstName, firstName));
				}
			}
			
			// if new field is empty, field is cleared.
			if(json.has("lastName")) {
				String lastName = json.getString("lastName");
				String oldLastName = memberBeingModified.getLastName() == null ? "" : memberBeingModified.getLastName();
				if(!lastName.equalsIgnoreCase(oldLastName)) {
					memberBeingModified.setLastName(lastName);
					modificationMessages.add(Utility.getModMessage("Last Name", oldLastName, lastName));
				}
			}
			
			// if new field is empty, field is cleared.
			if(json.has("emailAddress")) {
				// ::BUSINESS_RULE:: (known: user is either creator, NA coordinator or owning primary member)
				// Don't allow the change if:
				//---------------------------
				//    - the email address is associated with a user
				//    - the email address is not unique among primary members of this team
				//    - the email address is not unique within the member (i.e. does not conflict with guardian email addresses)
				// Allow the change if:
				//---------------------
				//    - old email address was not specified so creator, NA coordinator or owning primary member can change
				//    - user is the team coordinator ('isCoordinator' true for creator and NA coordinator)
				String newEmailAddress = json.getString("emailAddress");
				
				String oldEmailAddress = (memberBeingModified.getEmailAddress() == null || memberBeingModified.getEmailAddress().length() == 0) ? Utility.NOT_SPECIFIED : memberBeingModified.getEmailAddress();
				newEmailAddress = (newEmailAddress == null || newEmailAddress.trim().length() == 0) ? Utility.NOT_SPECIFIED : newEmailAddress;
				if(!newEmailAddress.equalsIgnoreCase(oldEmailAddress)) {
					// ::BUSINESS_RULE:: nobody can change the NA email address of a member already associated with a user
					if(memberBeingModified.getIsEmailAddressNetworkAuthenticated() && memberBeingModified.hasAssociatedUser()) {
						jsonReturn.put("apiStatus", ApiStatusCode.EMAIL_ADDRESS_CAN_NOT_BE_UPDATED);
						return new JsonRepresentation(jsonReturn);
					} else if(!newEmailAddress.equalsIgnoreCase(Utility.NOT_SPECIFIED) && !team.isNewPrimaryEmailAddressAcceptable(newEmailAddress)) {
						jsonReturn.put("apiStatus", ApiStatusCode.MEMBER_EMAIL_ADDRESS_NOT_UNIQUE);
						return new JsonRepresentation(jsonReturn);
					} else if(!newEmailAddress.equalsIgnoreCase(Utility.NOT_SPECIFIED) && !memberBeingModified.isNewPrimaryEmailAddressAcceptable(newEmailAddress)) {
						jsonReturn.put("apiStatus", ApiStatusCode.MEMBER_EMAIL_ADDRESS_NOT_UNIQUE);
						return new JsonRepresentation(jsonReturn);
					} else {
						if(oldEmailAddress.equalsIgnoreCase(Utility.NOT_SPECIFIED) || isCoordinator) {
							String newEmailAddressValue = newEmailAddress.equalsIgnoreCase(Utility.NOT_SPECIFIED) ? null : newEmailAddress;
							memberBeingModified.setEmailAddress(newEmailAddressValue);
							
							// if old email address was specified and was network authenticated, then un-network authenticated
							if(!oldEmailAddress.equalsIgnoreCase(Utility.NOT_SPECIFIED) && memberBeingModified.getIsEmailAddressNetworkAuthenticated()) {
								memberBeingModified.unNetworkAuthenticateEmailAddress(oldEmailAddress);
							}
							
							modificationMessages.add(Utility.getModMessage("Email Address", oldEmailAddress, newEmailAddress));
							
							// if the email address has not been cleared, then queue up the email for member notification later
							if(!newEmailAddress.equalsIgnoreCase(Utility.NOT_SPECIFIED)) {
								isEmailUpdated = true;
								primaryIndividual.setEmailAddress(newEmailAddress);
							}
						} else {
							jsonReturn.put("apiStatus", ApiStatusCode.EMAIL_ADDRESS_CAN_NOT_BE_UPDATED);
							return new JsonRepresentation(jsonReturn);
						}
					}
				}
			}

			// if new field is empty, field is cleared.
			if(json.has("jerseyNumber")) {
				String jerseyNumber = json.getString("jerseyNumber");
				String oldJerseyNumber = memberBeingModified.getJerseyNumber() == null ? "" : memberBeingModified.getJerseyNumber();
				if(!jerseyNumber.equalsIgnoreCase(oldJerseyNumber)) {
					memberBeingModified.setJerseyNumber(jerseyNumber);
					modificationMessages.add(Utility.getModMessage("Jersey Number", oldJerseyNumber, jerseyNumber));
				}
			}
			
			// Deprecated (only used by original iPhone version)
			// if new field is empty, field is cleared.
			if(json.has("thumbNail")) {
				String thumbNail = json.getString("thumbNail");
				String oldThumbNail = memberBeingModified.getThumbNailBase64() == null ? "" : memberBeingModified.getThumbNailBase64();
				memberBeingModified.setThumbNailBase64(thumbNail);
				String modMessage = "Member Photo was updated";
				if(oldThumbNail.length() == 0 && thumbNail.length() > 0) {
					modMessage = "Member Photo was added";
				} else if(oldThumbNail.length() > 0 && thumbNail.length() == 0) {
					modMessage = "Member Photo was removed";
				}
				log.debug("modMessage = " + modMessage);
				modificationMessages.add(modMessage);
			}
			
			Boolean isPortrait = null;
			if(json.has("isPortrait")) {
				isPortrait = json.getBoolean("isPortrait");
				log.debug("json isPortrait = " + isPortrait);
			}

			if(json.has("photo")) {
				if(isPortrait == null) {
					jsonReturn.put("apiStatus", ApiStatusCode.IS_PORTRAIT_AND_PHOTO_MUST_BE_SPECIFIED_TOGETHER);
					return new JsonRepresentation(jsonReturn);
				}
				
				String photoBase64 = json.getString("photo");
				try {
					String oldPhoto = memberBeingModified.getPhotoBase64() == null ? "" : memberBeingModified.getPhotoBase64();
					
					// decode the base64 encoding to create the thumb nail
					byte[] rawPhoto = Base64.decodeBase64(photoBase64);
					ImagesService imagesService = ImagesServiceFactory.getImagesService();
					Image oldImage = ImagesServiceFactory.makeImage(rawPhoto);
					
					int tnWidth = isPortrait == true ? User.THUMB_NAIL_SHORT_SIDE : User.THUMB_NAIL_LONG_SIDE;
					int tnHeight = isPortrait == true ? User.THUMB_NAIL_LONG_SIDE : User.THUMB_NAIL_SHORT_SIDE;
					Transform resize = ImagesServiceFactory.makeResize(tnWidth, tnHeight);
					Image newImage = imagesService.applyTransform(resize, oldImage);
					String thumbNailBase64 = Base64.encodeBase64String(newImage.getImageData());
					
					memberBeingModified.setThumbNailBase64(thumbNailBase64);
					memberBeingModified.setPhotoBase64(photoBase64);
					
					String modMessage = "Member Photo was updated";
					if(oldPhoto.length() == 0 && photoBase64.length() > 0) {
						modMessage = "Member Photo was added";
					} else if(oldPhoto.length() > 0 && photoBase64.length() == 0) {
						modMessage = "Member Photo was removed";
					}
					log.debug("modMessage = " + modMessage);
					modificationMessages.add(modMessage);
				} catch(Exception e) {
					apiStatus = ApiStatusCode.INVALID_PHOTO_PARAMETER;
				}
			}
			
			if(json.has("thumbNail") && json.has("photo")) {
				apiStatus = ApiStatusCode.PHOTO_AND_THUMBNAIL_MUTUALLY_EXCLUSIVE;
			}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			
			if(json.has("guardians")) {
				JSONArray guardianJsonArray = json.getJSONArray("guardians");
				int arraySize = guardianJsonArray.length();
				List<Guardian> guardians = new ArrayList<Guardian>();
				List<String> guardianEmailAddresses = new ArrayList<String>();
				List<String> guardianPhoneNumbers = new ArrayList<String>();
				
				///////////////////////////////////////
				// Build Guardian Objects
				// - fields not present are set to null
				///////////////////////////////////////
				for(int i=0; i<arraySize; i++) {
					JSONObject guardianJsonObj = guardianJsonArray.getJSONObject(i);
					Guardian guardian = new Guardian();

					String guardianKey = null;
					if(guardianJsonObj.has("key")) {
						guardianKey = guardianJsonObj.getString("key");
					}
					guardian.setKey(guardianKey);

					String guardianEmailAddress = null;
					if(guardianJsonObj.has("emailAddress")) {
						guardianEmailAddress = guardianJsonObj.getString("emailAddress");
					}
					guardian.setEmailAddress(guardianEmailAddress);
					
					String guardianFirstName = null;
					if(guardianJsonObj.has("firstName")) {
						guardianFirstName = guardianJsonObj.getString("firstName");
					}
					guardian.setFirstName(guardianFirstName);
					
					String guardianLastName = null;
					if(guardianJsonObj.has("lastName")) {
						guardianLastName = guardianJsonObj.getString("lastName");
					}
					guardian.setLastName(guardianLastName);
					
					String guardianPhoneNumber = null;
					if(guardianJsonObj.has("phoneNumber")) {
						guardianPhoneNumber = guardianJsonObj.getString("phoneNumber");
						guardianPhoneNumber = Utility.extractAllDigits(guardianPhoneNumber);
						guardianPhoneNumber = Utility.stripLeadingOneIfPresent(guardianPhoneNumber);
					}
					guardian.setPhoneNumber(guardianPhoneNumber);
					
					guardians.add(guardian);
				}
				
				for(Guardian updatedGuardian : guardians) {
					UserMemberInfo guardianIndividual = new UserMemberInfo();
					guardianIndividual.setIsGuardian(true);

					// if no key is provided, then this guardian is being added
					if(updatedGuardian.getKey() == null || updatedGuardian.getKey().length() == 0) {
						log.debug("Guardian being added");
						///////////////////////
						// Guardian Being Added
						///////////////////////
						// verify the email address is unique for this member
						if(!memberBeingModified.isNewGuardianEmailAddressAcceptable(null, updatedGuardian.getEmailAddress())) {
							jsonReturn.put("apiStatus", ApiStatusCode.GUARDIAN_EMAIL_ADDRESS_NOT_UNIQUE);
							return new JsonRepresentation(jsonReturn);
						}

						modificationMessages.add("Guardian " + updatedGuardian.getFullName() + " added for member " + memberBeingModified.getFullName());
						memberBeingModified.addGuardian(updatedGuardian);
						if(updatedGuardian.getEmailAddress() != null && updatedGuardian.getEmailAddress().length() > 0) {
							guardianIndividual.setEmailAddress(updatedGuardian.getEmailAddress());
						}
					} else if( (updatedGuardian.getKey() != null) &&
							   (updatedGuardian.getEmailAddress() == null || updatedGuardian.getEmailAddress().length() == 0) &&
							   (updatedGuardian.getFirstName() == null || updatedGuardian.getFirstName().length() == 0) &&
							   (updatedGuardian.getLastName() == null || updatedGuardian.getLastName().length() == 0) &&
							   (updatedGuardian.getPhoneNumber() == null || updatedGuardian.getPhoneNumber().length() == 0)       ) {
						log.debug("Guardian being deleted, key = " + updatedGuardian.getKey());
						/////////////////////////////////
						// Guardian Being Cleared/Deleted
						/////////////////////////////////
						Guardian originalGuardian = memberBeingModified.getGuardian(updatedGuardian.getKey());
						if(originalGuardian == null) {
							// all keys sent by the client must be valid
							jsonReturn.put("apiStatus", ApiStatusCode.INVALID_GUARDIAN_KEY_PARAMETER);
							return new JsonRepresentation(jsonReturn);
						}
						
						modificationMessages.add("Guardian " + updatedGuardian.getFullName() + " deleted for member " + memberBeingModified.getFullName());
						memberBeingModified.deleteGuardian(updatedGuardian.getKey());
					} else {
						//////////////////////////
						// Guardian Being Modified
						//////////////////////////
						Guardian originalGuardian = memberBeingModified.getGuardian(updatedGuardian.getKey());
						if(originalGuardian == null) {
							// all keys sent by the client must be valid
							jsonReturn.put("apiStatus", ApiStatusCode.INVALID_GUARDIAN_KEY_PARAMETER);
							return new JsonRepresentation(jsonReturn);
						}
						
						///////////////////////
						// UPDATE EMAIL ADDRESS
						///////////////////////
						String oldEmailAddress = (originalGuardian.getEmailAddress() == null || originalGuardian.getEmailAddress().length() == 0) ? Utility.NOT_SPECIFIED : originalGuardian.getEmailAddress();
						String newEmailAddress = (updatedGuardian.getEmailAddress() == null || updatedGuardian.getEmailAddress().length() == 0) ? Utility.NOT_SPECIFIED : updatedGuardian.getEmailAddress();
						
						// only work to do if the email address has changed
						if(!oldEmailAddress.equalsIgnoreCase(newEmailAddress)) {
							// ::BUSINESS_RULE:: (known: user is either creator, NA coordinator or owning primary member)
							// Don't allow the change if:
							//---------------------------
							//    - the NA email address is associated with a user
							//    - the email address is not unique for this member
							// Allow the change if:
							//---------------------
							//    - old email address was not specified so creator, NA coordinator or owning primary member can change
							//    - user is the team coordinator ('isCoordinator' true for creator and NA coordinator)
							Boolean isGuardianAssociatedWithUser = memberBeingModified.getUserIdByEmailAddress(oldEmailAddress) == null ? false : true;
							if(isGuardianAssociatedWithUser && memberBeingModified.isEmailAddressNetworkAuthenticated(oldEmailAddress)) {
								jsonReturn.put("apiStatus", ApiStatusCode.GUARDIAN_EMAIL_ADDRESS_CAN_NOT_BE_UPDATED);
								return new JsonRepresentation(jsonReturn);
							} else if( oldEmailAddress.equalsIgnoreCase(Utility.NOT_SPECIFIED) || isCoordinator) {
								// verify the email address is unique for this member
								if(!memberBeingModified.isNewGuardianEmailAddressAcceptable(updatedGuardian.getKey(), updatedGuardian.getEmailAddress())) {
									jsonReturn.put("apiStatus", ApiStatusCode.GUARDIAN_EMAIL_ADDRESS_NOT_UNIQUE);
									return new JsonRepresentation(jsonReturn);
								}
								
								// if old email address was specified and was network authenticated, then un-network authenticated
								if(!oldEmailAddress.equalsIgnoreCase(Utility.NOT_SPECIFIED) && memberBeingModified.isEmailAddressNetworkAuthenticated(oldEmailAddress)) {
									memberBeingModified.unNetworkAuthenticateEmailAddress(oldEmailAddress);
								}
								
								modificationMessages.add(Utility.getModMessage("Guardian Email Address", oldEmailAddress, newEmailAddress));
								
								// if the email address has not been cleared, then queue up the email for member notification later
								if(updatedGuardian.getEmailAddress() != null && updatedGuardian.getEmailAddress().length() > 0) {
									isEmailUpdated = true;
									guardianIndividual.setEmailAddress(updatedGuardian.getEmailAddress());
								}
							}
						}
						
						/////////////////////
						// UPDATE FIRST NAME
						/////////////////////
						String oldFirstName = (originalGuardian.getFirstName() == null || originalGuardian.getFirstName().length() == 0) ? Utility.NOT_SPECIFIED : originalGuardian.getFirstName();
						String newFirstName = (updatedGuardian.getFirstName() == null || updatedGuardian.getFirstName().length() == 0) ? Utility.NOT_SPECIFIED : updatedGuardian.getFirstName();
						// ensure the first name is really changing
						if(!oldFirstName.equalsIgnoreCase(newFirstName)) {
							modificationMessages.add(Utility.getModMessage("Guardian First Name", oldFirstName, newFirstName));
						}
						
						///////////////////
						// UPDATE LAST NAME
						///////////////////
						String oldLastName = (originalGuardian.getLastName() == null || originalGuardian.getLastName().length() == 0) ? Utility.NOT_SPECIFIED : originalGuardian.getLastName();
						String newLastName = (updatedGuardian.getLastName() == null || updatedGuardian.getLastName().length() == 0) ? Utility.NOT_SPECIFIED : updatedGuardian.getLastName();
						// ensure the last name is really changing
						if(!oldLastName.equalsIgnoreCase(newLastName)) {
							modificationMessages.add(Utility.getModMessage("Guardian Last Name", oldLastName, newLastName));
						}

						//////////////////////
						// UPDATE PHONE NUMBER
						//////////////////////
						String oldPhoneNumber = (originalGuardian.getPhoneNumber() == null || originalGuardian.getPhoneNumber().length() == 0) ? Utility.NOT_SPECIFIED : originalGuardian.getPhoneNumber();
						String newPhoneNumber = (updatedGuardian.getPhoneNumber() == null || updatedGuardian.getPhoneNumber().length() == 0) ? Utility.NOT_SPECIFIED : updatedGuardian.getPhoneNumber();
						// ensure the phone number is really changing
						if(!oldPhoneNumber.equalsIgnoreCase(newPhoneNumber)) {
							Boolean isGuardianAssociatedWithUser = memberBeingModified.getUserIdByPhoneNumber(oldPhoneNumber) == null ? false : true;
							//::BUSINESS_RULE:: cannot change SMS confirmed phone number if guardian is associated with a user
							if(isGuardianAssociatedWithUser && memberBeingModified.isPhoneNumberSmsConfirmed(oldPhoneNumber)) {
								jsonReturn.put("apiStatus", ApiStatusCode.GUARDIAN_PHONE_NUMBER_CAN_NOT_BE_UPDATED);
								return new JsonRepresentation(jsonReturn);
							}
							
							modificationMessages.add(Utility.getModMessage("Guardian Phone Number", Utility.formatPhoneNumber(oldPhoneNumber), Utility.formatPhoneNumber(newPhoneNumber)));
							guardianIndividual.setPhoneNumber(newPhoneNumber);
						}
						
						memberBeingModified.updateGuardian(updatedGuardian);
					} // end of else(Guardian Being Modified)
					
					// track members that may be associated with a User entity
					UserMemberInfo.addIfUniqueContactInfo(guardianIndividual, potentialAssociatedIndividuals);
				} // end of for(Guardian updatedGuardian : guardians)
			} // end of if(json.has("guardians"))
			
			// if new field is empty, field is cleared.
			if(json.has("participantRole")) {
				String participantRole = json.getString("participantRole").toLowerCase();
				String oldParticipantRole = memberBeingModified.getParticipantRole() == null ? "" : memberBeingModified.getParticipantRole().toLowerCase();
				
				//make sure a change is really being requested
				if(!participantRole.equalsIgnoreCase(oldParticipantRole)) {
					//::BUSINESS_RULE must be a coordinator to update participantRole
					if(isCoordinator) {
						//::BUSINESS_RULE  cannot change role if user is creator
						if(oldParticipantRole.equalsIgnoreCase(Member.CREATOR_PARTICIPANT) ) {
							apiStatus = ApiStatusCode.CREATOR_PARTICIPANT_ROLE_CANNOT_BE_CHANGED;
						} else if(Member.isValidParticipant(participantRole)) {
							memberBeingModified.setParticipantRole(participantRole);
							modificationMessages.add(Utility.getModMessage("Participant Role", oldParticipantRole, participantRole));
						} else {
							apiStatus = ApiStatusCode.INVALID_PARTICIPANT_ROLE_PARAMETER;
						}
					} else {
						apiStatus = ApiStatusCode.USER_NOT_A_COORDINATOR;
					}
				}
			}
			
			// if new field is empty, field is cleared.
			if(json.has("phoneNumber")) {
				String phoneNumber = json.getString("phoneNumber");
				phoneNumber = Utility.extractAllDigits(phoneNumber);
				phoneNumber = Utility.stripLeadingOneIfPresent(phoneNumber);
				// quick research shows phones numbers around the world must be between 5 and 15 digits
				// so if the phone number is present, then validate the length range
				if(phoneNumber.length() > 1 && (phoneNumber.length() < 5 || phoneNumber.length() > 15)) {
					apiStatus = ApiStatusCode.INVALID_PHONE_NUMBER_PARAMETER;
				} else {
					String oldPhoneNumber = memberBeingModified.getPhoneNumber() == null ? Utility.NOT_SPECIFIED : memberBeingModified.getPhoneNumber();
					String newPhoneNumber = phoneNumber.length() == 0 ? Utility.NOT_SPECIFIED : phoneNumber;
					if(!phoneNumber.equalsIgnoreCase(oldPhoneNumber)) {
						// ::BUSINESS_RULE:: nobody can change the SMS Confirmed Phone Number of a member already associated with a user
						if(memberBeingModified.getHasBeenSmsConfirmed() && memberBeingModified.hasAssociatedUser()) {
							jsonReturn.put("apiStatus", ApiStatusCode.PHONE_NUMBER_CAN_NOT_BE_UPDATED);
							return new JsonRepresentation(jsonReturn);
						}						
						
						// if phone number is being cleared, set the phone number field to null
						String newPhoneNumberValue = phoneNumber.length() == 0 ? null : phoneNumber;
						memberBeingModified.setPhoneNumber(newPhoneNumberValue);
						modificationMessages.add(Utility.getModMessage("Phone Number", Utility.formatPhoneNumber(oldPhoneNumber), Utility.formatPhoneNumber(newPhoneNumber)));
						primaryIndividual.setPhoneNumber(newPhoneNumberValue);
					}
				}
			}
			
			// if new field is empty, field is cleared, except for friend role.
			List<String> roles = new ArrayList<String>();
			int rjaSize = 0;
			if(json.has("roles")) {
				JSONArray roleJsonArray = json.getJSONArray("roles");
				rjaSize = roleJsonArray.length();
				for(int i=0; i<rjaSize; i++) {
					roles.add(roleJsonArray.getString(i));
				}
				List<String> oldRoles = memberBeingModified.getRoles();

				if(rjaSize == 0) {
					roles.add(Member.FAN_ROLE);
				}
				
				StringBuffer oldRoleNames = new StringBuffer();
				if(oldRoles == null || oldRoles.size() == 0) {
					oldRoleNames.append(Utility.NOT_SPECIFIED);
				} else {
					int oldRoleCnt = 0;
					for(String r : oldRoles) {
						if(oldRoleCnt > 0) oldRoleNames.append(", ");
						oldRoleNames.append(r);
						oldRoleCnt++;
					}
				}
				
				StringBuffer roleNames = new StringBuffer();
				int roleCnt = 0;
				for(String r : roles) {
					if(roleCnt > 0) roleNames.append(", ");
					roleNames.append(r);
					roleCnt++;
				}
				
				if(!oldRoleNames.toString().equalsIgnoreCase(roleNames.toString())) {
					log.debug("modMessage = " + "Roles changed from: " + oldRoleNames.toString() + " to: " + roleNames.toString());
					// TODO Modification message turned off for now - role feature is not used in client yet
					//modificationMessages.add("Roles changed from: " + oldRoleNames.toString() + " to: " + roleNames.toString());
					memberBeingModified.setRoles(roles);
				}
			}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			
			//::BUSINESSRULE:: first name or last name, email address or phone number must be specified
			if((memberBeingModified.getFirstName() == null || memberBeingModified.getFirstName().trim().length() == 0) &&
			   (memberBeingModified.getLastName() == null || memberBeingModified.getLastName().trim().length() == 0)   &&
			   (memberBeingModified.getEmailAddress() == null || memberBeingModified.getEmailAddress().trim().length() == 0) &&
			   (memberBeingModified.getPhoneNumber() == null || memberBeingModified.getPhoneNumber().length() == 0) ) {
				apiStatus = ApiStatusCode.FIRST_NAME_OR_LAST_NAME_OR_EMAIL_ADDRESS_OR_PHONE_NUMBER_REQUIRED;
			}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}

			// track members that may be associated with a User entity
			UserMemberInfo.addIfUniqueContactInfo(primaryIndividual, potentialAssociatedIndividuals);
			
			////////////////////////
			// FIND ASSOCIATED USERS
			////////////////////////
            List<User> associatedUsers = new ArrayList<User>();
			List<UserMemberInfo> notAssociatedIndividuals = new ArrayList<UserMemberInfo>();
			// care has been taken so potentialAssociatedIndividuals all have unique contact info.
			for(UserMemberInfo umi : potentialAssociatedIndividuals) {
				log.debug("checking for user association of ea = " + umi.getEmailAddress() + " pn = " + umi.getPhoneNumber());

				////////////////////////////////////////////////////////////////////
				// USER 'FIND' ALGORITHM
				//  1. Try to find matching user using email address
				//  2. If email match found, user find complete
				//  3. If no email match found, try to find user via phone number
				//  4. If phone number found, user find complete.
				//  5. If matching user not found, add to non-matching list.
				////////////////////////////////////////////////////////////////////
				
				String iea = umi.getEmailAddress();
				String ipn = umi.getPhoneNumber();
				Boolean wasUserFound = false;
				if(iea != null) {
					try {
						User aUser = (User)em.createNamedQuery("User.getByEmailAddressAndIsNetworkAuthenticated")
							.setParameter("emailAddress", iea.toLowerCase())
							.setParameter("isNetworkAuthenticated", true)
							.getSingleResult();
						associatedUsers.add(aUser);
						wasUserFound = true;
						log.debug("user association found for ea = " + iea);
			    	} catch (NoResultException e) {
			    		///////////////////////////////////////////////////////////////////////////////////////////////////
						// THIS IS NOT AN ERROR -- there may be no user entity associated with the specified email address.
			    		///////////////////////////////////////////////////////////////////////////////////////////////////
			    		log.debug("user association NOT found for ea = " + iea);
					} catch (NonUniqueResultException e) {
						log.exception("MemberResource:updateMember:NonUniqueResultException3", "", e);
					}
				}
				
	    		if(!wasUserFound && ipn != null) {
					try {
						User aUser = (User)em.createNamedQuery("User.getByPhoneNumberAndIsSmsConfirmed")
							.setParameter("phoneNumber", ipn.toLowerCase())
							.setParameter("isSmsConfirmed", true)
							.getSingleResult();
						associatedUsers.add(aUser);
						wasUserFound = true;
						log.debug("user association found for pn = " + ipn);
			    	} catch (NoResultException nre) {
			    		///////////////////////////////////////////////////////////////////////////////////////////////////
						// THIS IS NOT AN ERROR -- there may be no user entity associated with the specified phone number.
			    		///////////////////////////////////////////////////////////////////////////////////////////////////
			    		log.debug("user association NOT found for pn = " + ipn);
					} catch (NonUniqueResultException e) {
						log.exception("MemberResource:updateMember:NonUniqueResultException4", "two or more users with same phone number", e);
					}
	    		}
	    		
	    		if(!wasUserFound) {
	    			notAssociatedIndividuals.add(umi);
	    		}
			}
			
			List<String> emailConfirmNeededList = new ArrayList<String>();
			List<String> smsConfirmNeededList = new ArrayList<String>(); // returned in API so phone can send SMS to appropriate new members
			////////////////////////
			// BIND MEMBERS TO USERS
			////////////////////////
			// NOTE: because of same guardian can be in two memberships, single User can be associated with multiple Members
			for(User u : associatedUsers) {
				memberBeingModified.bindToUser(u, emailConfirmNeededList, smsConfirmNeededList);
			}
			
			// TODO replace with Access Preferences
			memberBeingModified.setDefaultAccessPreferences();
			
			// need to pre-access team in member, otherwise error when sending the message below
			memberBeingModified.getTeam();
			em.getTransaction().commit();
			
			//////////////////////////////////
		    // Update associated User entities
			//////////////////////////////////
			// - add user to this team
			// - persist the EA or PN potential set during the member Bind above
			for(User u : associatedUsers) {
		    	User.addTeamAndSetBoundValues(u, team);
			}
			
			//////////////////////////////////////////////////////////////////////
			// Finish building the emailConfirmNeededList and smsConfirmNeededList
			//////////////////////////////////////////////////////////////////////
			for(UserMemberInfo nai : notAssociatedIndividuals) {
				String ea = nai.getEmailAddress();
				String pn = nai.getPhoneNumber();
				if(ea != null && ea.length() > 0) {
					// ensure list has no duplicates
					if(!emailConfirmNeededList.contains(ea)) {emailConfirmNeededList.add(ea);}
				}
				if(pn != null && pn.length() > 0) {
					// ensure list has no duplicates
					if(!smsConfirmNeededList.contains(pn)) {smsConfirmNeededList.add(pn);}
				}
			}
			
			//////////////////////////////////////////////////////
			// Send appropriate message/email to new users/members
			//////////////////////////////////////////////////////
			if(associatedUsers.size() > 0) {
				PubHub.sendMemberWelcomeMessageToUsers(memberBeingModified, associatedUsers, team, currentUser);
			}

		    // send a welcome message to all individuals in the membership that are not yet users
			if(emailConfirmNeededList.size() > 0) {
			    PubHub.sendMemberWelcomeMessage(memberBeingModified, emailConfirmNeededList, currentUser);
			}
			
			// PRIORITY TODO guardian changes should be sent to the guardians in separate messages
			if(modificationMessages.size() > 0) {
				PubHub.sendMemberUpdatedMessage(memberBeingModified, modificationMessages, currentUser);
			}
			
			//::TODO  Need to deactivate any previous outstanding email confirmation messages for all modified email address
			// If this is not done, a member confirming an old email address may network authenticate the new one.
		} catch (IOException e) {
			log.exception("MemberResource:updateMember:IOException", "", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (JSONException e) {
			log.exception("MemberResource:updateMember:JSONException", "", e);
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
			log.exception("MemberResource:updateMember:JSONException2", "", e);
		}
		return new JsonRepresentation(jsonReturn);
    }

}