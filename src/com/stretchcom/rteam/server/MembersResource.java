package com.stretchcom.rteam.server;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

import com.google.appengine.api.datastore.KeyFactory;
  
/** 
 * Resource that manages a members of a team. 
 *  
 */  
public class MembersResource extends ServerResource {  
	//private static final Logger log = Logger.getLogger(MembersResource.class.getName());
	private RskyboxClient log = new RskyboxClient(this);
  
    // The sequence of characters that identifies the resource.
    String teamId;
    String includeFans;
    String multiple;
    
    @Override  
    protected void doInit() throws ResourceException {  
        // Get the "teamId" attribute value taken from the URI template /team/{teamId} 
        this.teamId = (String)getRequest().getAttributes().get("teamId"); 
        log.debug("MembersResource:doInit() - teamName = " + this.teamId);
        if(this.teamId != null) {
            this.teamId = Reference.decode(this.teamId);
            log.debug("UserResource:doInit() - decoded teamName = " + this.teamId);
        }
        
        this.multiple = (String)getRequest().getAttributes().get("multiple"); 
        log.debug("GamesResource:doInit() - multiple = " + this.multiple);
        if(this.multiple != null) {
            this.multiple = Reference.decode(this.multiple);
            log.debug("GamesResource:doInit() - decoded multiple = " + this.multiple);
        }
        
		Form form = getRequest().getResourceRef().getQueryAsForm();
		for (Parameter parameter : form) {
			log.debug("parameter " + parameter.getName() + " = " + parameter.getValue());
			if(parameter.getName().equals("includeFans"))  {
				this.includeFans = (String)parameter.getValue();
				this.includeFans = Reference.decode(this.includeFans);
				log.debug("MembersResource:doInit() - decoded includeFans = " + this.includeFans);
			} 
		}
    }  

    // Handles 'Create a new member' API 
	// Handles 'Create multiple new member' API
    @Post  
    public JsonRepresentation createMember(Representation entity) {
    	log.debug("createMember(@Post) entered ..... ");
		EntityManager em = EMF.get().createEntityManager();
		JSONObject jsonReturn = new JSONObject();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_CREATED);
        try {
	    	User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
			if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				log.error("MembersResource:createMember:currentUser", "user could not be retrieved from Request attributes!!");
			}
			// teamId is required
			else if(this.teamId == null || this.teamId.length() == 0) {
				apiStatus = ApiStatusCode.TEAM_ID_REQUIRED;
				log.debug("invalid team ID");
			} 
			else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
				log.debug(apiStatus);
        	}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_CREATED)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			
    		JsonRepresentation jsonRep = new JsonRepresentation(entity);
			JSONObject json = jsonRep.toJsonObject();
			log.debug("received json object = " + json.toString());

            //////////////////////////////////////////////////////////////////////////////////////////
			// See if there are already Users associated with the individuals in this new membership.
            // Individuals in the membership that are already users can integrate this new membership
            // immediately -- a (email or SMS) confirmation is not needed.
            //////////////////////////////////////////////////////////////////////////////////////////
            List<UserMemberInfo> potentialAssociatedIndividuals = new ArrayList<UserMemberInfo>();
            List<Member> newMembers = new ArrayList<Member>();
            
    		Team team = (Team)em.createNamedQuery("Team.getByKey")
				.setParameter("key", KeyFactory.stringToKey(this.teamId))
				.getSingleResult();
			log.debug("team retrieved = " + team.getTeamName());
			
			//::BUSINESSRULE:: user must be the team creator or network authenticated to create a new member.
			Boolean isCreator = team.isCreator(currentUser.getEmailAddress());
			if(!isCreator && !currentUser.getIsNetworkAuthenticated()) {
				apiStatus = ApiStatusCode.USER_NOT_CREATOR_NOR_NETWORK_AUTHENTICATED;
				log.debug("user is not the team creator nor network authenticated");
			} 
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_CREATED)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}

   			Boolean isCoordinator = false;
   			if(isCreator) {
   				// creator is by definition a coordinator too
   				isCoordinator = true;
   			} else {
				List<Member> memberships = Member.getMemberShipsWithEmailAddress(currentUser.getEmailAddress(), team);
				for(Member m : memberships) {
    	    		if(m.isCoordinator()) {
    	    			isCoordinator = true;
    	    			break;
    	    		}
				}
   			}
   			
			if(this.multiple == null) {
            	////////////////////////////////////
                // Handles 'Create a new member' API 
            	////////////////////////////////////
				log.debug("creating a single member");
    			apiStatus = handleJsonForCreateNewMemberApi(json, potentialAssociatedIndividuals, team, isCoordinator, newMembers);
            } else {
                ///////////////////////////////////////////
            	// Handles 'Create multiple new member' API
                ///////////////////////////////////////////
    			apiStatus = handleJsonForCreateMultipleNewMembersApi(json, potentialAssociatedIndividuals, team, isCoordinator, newMembers);
            }
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_CREATED)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			
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
						log.exception("MembersResource:createMember:NonUniqueResultException", "two or more users with same email address", e);
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
						log.exception("MembersResource:createMember:NonUniqueResultException2", "two or more users with same phone number", e);
					}
	    		}
	    		
	    		if(!wasUserFound) {
	    			notAssociatedIndividuals.add(umi);
	    		}
			}
			
			List<Member> existingMembers = team.getMembers();
			List<String> emailConfirmNeededList = new ArrayList<String>();
			List<String> smsConfirmNeededList = new ArrayList<String>(); // returned in API so phone can send SMS to appropriate new members
			
			em.getTransaction().begin();
			////////////////////////
			// BIND MEMBERS TO USERS
			////////////////////////
			// NOTE: because of same guardian can be in two memberships, single User can be associated with multiple Members
			for(Member member : newMembers) {
				for(User u : associatedUsers) {
					member.bindToUser(u, emailConfirmNeededList, smsConfirmNeededList);
				}
				
				// add the member to the team
				existingMembers.add(member);
			}
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
			for(Member nm : newMembers) {
				List<User> associatedUsersForThisMember = null;
				List<String> emailConfirmNeededListForThisMember = null;
				if(this.multiple == null) {
	            	////////////////////////////////////
	                // Handles 'Create a new member' API 
	            	////////////////////////////////////
					// there is only a single member held in the newMember list, so just let the code below run
					associatedUsersForThisMember = associatedUsers;
					emailConfirmNeededListForThisMember = emailConfirmNeededList;
	             } else {
	                ///////////////////////////////////////////
	            	// Handles 'Create multiple new member' API
	                ///////////////////////////////////////////
	            	 associatedUsersForThisMember = new ArrayList<User>();
	            	 for(User au : associatedUsers) {
	            		 if(nm.containsEmailAddress(au.getEmailAddress())) {
	            			 associatedUsersForThisMember.add(au);
	            		 }
	            	 }
	            	 
	            	 emailConfirmNeededListForThisMember = new ArrayList<String>();
	            	 for(String ecnl : emailConfirmNeededList) {
	            		 if(nm.containsEmailAddress(ecnl)) {
	            			 emailConfirmNeededListForThisMember.add(ecnl);
	            		 }
	            	 }
	            }
			
				if(associatedUsersForThisMember.size() > 0) {
					PubHub.sendMemberWelcomeMessageToUsers(nm, associatedUsersForThisMember, team, currentUser);
				}
				
			    // send a welcome message to all individuals in the membership that are not yet users
				if(emailConfirmNeededListForThisMember.size() > 0) {
				    PubHub.sendMemberWelcomeMessage(nm, emailConfirmNeededListForThisMember, currentUser);
				}
			}
			
        	JSONArray jsonSmsRecipientArray = new JSONArray();
			for(String spn : smsConfirmNeededList) {
				JSONObject jsonRecipientObj = new JSONObject();
				jsonRecipientObj.put("phoneNumber", spn);
				jsonSmsRecipientArray.put(jsonRecipientObj);
			}
			jsonReturn.put("smsRecipients", jsonSmsRecipientArray);
		} catch (IOException e) {
			log.exception("MembersResource:createMember:IOException", "", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (JSONException e) {
			log.exception("MembersResource:createMember:JSONException1", "", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (NoResultException e) {
			log.exception("MembersResource:createMember:NoResultException", "", e);
			apiStatus = ApiStatusCode.TEAM_NOT_FOUND;
		} catch (NonUniqueResultException e) {
			log.exception("MembersResource:createMember:NonUniqueResultException3", "two or more teams have same team name", e);
		} finally {
		    if (em.getTransaction().isActive()) {
		        em.getTransaction().rollback();
		    }
		    em.close();
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("MembersResource:createMember:JSONException2", "", e);
		}
		return new JsonRepresentation(jsonReturn);
    }
    
    private String handleJsonForCreateNewMemberApi(JSONObject theJson, List<UserMemberInfo> thePotentialAssociatedIndividuals,
    											Team theTeam, Boolean theIsCoordinator, List<Member> theNewMembers)  throws JSONException {
        Member member = new Member();
        UserMemberInfo primaryIndividual = new UserMemberInfo();
		
		// ::DEPENDENCY:: Must process this before "guardians" because guardians verifies that guardian email addresses 
        //                don't equal the primary email address.
        if(theJson.has("emailAddress")) {
			String emailAddress = theJson.getString("emailAddress");
			if(emailAddress != null && emailAddress.length() > 0)  {
				
				if(!theTeam.isNewPrimaryEmailAddressAcceptable(emailAddress)) {
					return ApiStatusCode.MEMBER_EMAIL_ADDRESS_NOT_UNIQUE;
				}

				member.setEmailAddress(emailAddress);
				primaryIndividual.setEmailAddress(emailAddress);
			}
		}
		
		// ::BUSINESS_RULE:: Guardian email address don't have to be unique per team (parent can have multiple kids on same team), but
		//                   must be unique within the membership. That is, each guardian email must be unique and cannot be the
		//                   same as the primary email address.
		// ::BUSINESS_RULE:: Guardian phone numbers don't have to be unique per team (parent can have multiple kids on same team), but
		//                   must be unique within the membership. That is, each guardian phone number must be unique and cannot be the
		//                   same as the primary phone number.
        if(theJson.has("guardians")) {
        	JSONArray guardianJsonArray = theJson.getJSONArray("guardians");
			int arraySize = guardianJsonArray.length();
			log.debug("guardian json array length = " + arraySize);
			List<Guardian> guardians = new ArrayList<Guardian>();
			List<String> guardianEmailAddresses = new ArrayList<String>();
			List<String> guardianPhoneNumbers = new ArrayList<String>();
			
			for(int i=0; i<arraySize; i++) {
				JSONObject guardianJsonObj = guardianJsonArray.getJSONObject(i);
				Guardian guardian = new Guardian();
				UserMemberInfo guardianIndividual = new UserMemberInfo();
				guardianIndividual.setIsGuardian(true);
				
				String guardianEmailAddress = null;
				if(guardianJsonObj.has("emailAddress")) {
					guardianEmailAddress = guardianJsonObj.getString("emailAddress");
					if(member.getEmailAddress() !=null && guardianEmailAddress.equalsIgnoreCase(member.getEmailAddress())) {
						return ApiStatusCode.GUARDIAN_EMAIL_ADDRESS_NOT_UNIQUE;
					} else {
						for(String gea : guardianEmailAddresses) {
							if(gea != null && gea.equalsIgnoreCase(guardianEmailAddress)) {
								return ApiStatusCode.GUARDIAN_EMAIL_ADDRESS_NOT_UNIQUE;
							}
						}
					}
					guardianEmailAddresses.add(guardianEmailAddress);
					guardianIndividual.setEmailAddress(guardianEmailAddress);
				}
				guardian.setEmailAddress(guardianEmailAddress);
				
				String guardianPhoneNumber = null;
				if(guardianJsonObj.has("phoneNumber")) {
					guardianPhoneNumber = guardianJsonObj.getString("phoneNumber");
					guardianPhoneNumber = Utility.extractAllDigits(guardianPhoneNumber);
					guardianPhoneNumber = Utility.stripLeadingOneIfPresent(guardianPhoneNumber);
					if(guardianPhoneNumber.length() < Member.MINIMUM_PHONE_NUMBER_LENGTH || guardianPhoneNumber.length() > Member.MAXIMUM_PHONE_NUMBER_LENGTH) {
						return ApiStatusCode.INVALID_GUARDIAN_PHONE_NUMBER_PARAMETER;
					}
					
					if(guardianPhoneNumber.equalsIgnoreCase(member.getPhoneNumber())) {
						return ApiStatusCode.GUARDIAN_PHONE_NUMBER_NOT_UNIQUE;
					} else {
						for(String gpn : guardianPhoneNumbers) {
							if(gpn.equalsIgnoreCase(guardianPhoneNumber)) {
								return ApiStatusCode.GUARDIAN_PHONE_NUMBER_NOT_UNIQUE;
							}
						}
					}
					guardianPhoneNumbers.add(guardianPhoneNumber);
					guardianIndividual.setPhoneNumber(guardianPhoneNumber);
				}
				guardian.setPhoneNumber(guardianPhoneNumber);
				
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
			
				//::BUSINESSRULE:: first name or last name, email address or phone number must be specified, if not, silently ignore.
				if(!((guardianEmailAddress == null || guardianEmailAddress.length() == 0) && 
					 (guardianPhoneNumber == null || guardianPhoneNumber.length() == 0) &&
				     (guardianFirstName == null || guardianFirstName.length() == 0) && 
				     (guardianLastName == null || guardianLastName.length() == 0)              )) {
					guardians.add(guardian);
					// track members that may be associated with a User entity
					UserMemberInfo.addIfUniqueContactInfo(guardianIndividual, thePotentialAssociatedIndividuals);
				}
			}
			
			if(guardians.size() > 0) {
				if(!member.createGuardians(guardians)) {
					log.debug("createGuardians() failed");
					return ApiStatusCode.SERVER_ERROR;
				}
			}
		}
		
		if(theJson.has("firstName")) {
			member.setFirstName(theJson.getString("firstName"));
		}
		
		if(theJson.has("lastName")) {
			member.setLastName(theJson.getString("lastName"));
		}
		
		if(theJson.has("jerseyNumber")) {
			String jerseyNumber = theJson.getString("jerseyNumber");
			if(jerseyNumber != null && jerseyNumber.length() > 0)  member.setJerseyNumber(jerseyNumber);
		}
		
		// get and save participantRole -- default if necessary
		String participantRole = null;
		if(theJson.has("participantRole")) {
			participantRole = theJson.getString("participantRole");
			
			if(participantRole == null) {
				// if not specified, default to 'member' participant
				participantRole = Member.MEMBER_PARTICIPANT;
			} else {
				participantRole = participantRole.toLowerCase();
				if(!Member.isValidParticipant(participantRole)) {
					return ApiStatusCode.INVALID_PARTICIPANT_ROLE_PARAMETER;
				}
			}
		} else {
			// if not specified, default to 'member' participant
			participantRole = Member.MEMBER_PARTICIPANT;
		}
		member.setParticipantRole(participantRole);
		
		//::BUSINESSRULE:: user must be a coordinator to add a 'coordinator' or 'member'. 
		if(!theIsCoordinator && member.isMemberParticipant()) {
			return ApiStatusCode.USER_NOT_A_COORDINATOR;
		}
		
		//::BUSINESSRULE:: phone number must be within a well defined size range
		if(theJson.has("phoneNumber")) {
			String phoneNumber = theJson.getString("phoneNumber");
			phoneNumber = Utility.extractAllDigits(phoneNumber);
			phoneNumber = Utility.stripLeadingOneIfPresent(phoneNumber);
			if(phoneNumber.length() < Member.MINIMUM_PHONE_NUMBER_LENGTH || phoneNumber.length() > Member.MAXIMUM_PHONE_NUMBER_LENGTH) {
				return ApiStatusCode.INVALID_PHONE_NUMBER_PARAMETER;
			}
			
			if(!theTeam.isNewPrimaryPhoneNumberAcceptable(phoneNumber)) {
				return ApiStatusCode.MEMBER_PHONE_NUMBER_NOT_UNIQUE;
			}

			member.setPhoneNumber(phoneNumber);
			primaryIndividual.setPhoneNumber(phoneNumber);
		}
		
		if(theJson.has("gender")) {
			String gender = theJson.getString("gender");
			if(!member.isGenderValid(gender)) {
				return ApiStatusCode.INVALID_GENDER_PARAMETER;
			} else {
				
			}
		}
		
		if(theJson.has("age")) {
			String ageStr = theJson.getString("age");
			try {
				Integer age = new Integer(ageStr);
				member.setAge(age);
			} catch (NumberFormatException e) {
				return ApiStatusCode.INVALID_AGE_PARAMETER;
			}
		}
		
		if(theJson.has("streetAddress")) {
			String streetAddress = theJson.getString("streetAddress");
			if(streetAddress != null && streetAddress.length() > 0)  member.setStreetAddress(streetAddress);
		}
		
		if(theJson.has("state")) {
			String state = theJson.getString("state");
			if(StateMap.get().isValid(state)) {
				member.setState(state);
			} else {
				return ApiStatusCode.INVALID_STATE_PARAMETER;
			}
		}
		
		// TODO validate zipcode
		if(theJson.has("zipcode")) {
			String zipcode = theJson.getString("zipcode");
			if(zipcode != null && zipcode.length() > 0)  member.setZipcode(zipcode);
		}

		List<String> roles = new ArrayList<String>();
		int rjaSize = 0;
		if(theJson.has("roles")) {
			JSONArray roleJsonArray = theJson.getJSONArray("roles");
			rjaSize = roleJsonArray.length();
			for(int i=0; i<rjaSize; i++) {
				roles.add(roleJsonArray.getString(i));
			}
		}
		if(rjaSize == 0) {
			roles.add(Member.FAN_ROLE);
		}
		member.setRoles(roles);
		
		//::BUSINESSRULE:: first name or last name, email address or phone number must be specified
		if((member.getFirstName() == null || member.getFirstName().trim().length() == 0) &&
		   (member.getLastName() == null || member.getLastName().trim().length() == 0)   &&
		   (member.getEmailAddress() == null || member.getEmailAddress().trim().length() == 0) &&
		   (member.getPhoneNumber() == null || member.getPhoneNumber().length() == 0) ) {
			return ApiStatusCode.FIRST_NAME_OR_LAST_NAME_OR_EMAIL_ADDRESS_OR_PHONE_NUMBER_REQUIRED;
		}
		
		// TODO replace with Access Preferences
		member.setDefaultAccessPreferences();
		
		// track members that may be associated with a User entity
		UserMemberInfo.addIfUniqueContactInfo(primaryIndividual, thePotentialAssociatedIndividuals);
		
		theNewMembers.add(member);
		return ApiStatusCode.SUCCESS;
    }

    private String handleJsonForCreateMultipleNewMembersApi(JSONObject theJson, List<UserMemberInfo> thePotentialAssociatedIndividuals,
    		Team theTeam, Boolean theIsCoordinator, List<Member> theNewMembers)  throws JSONException {
    	
    	if(theJson.has("members")) {
			JSONArray membersJsonArray = theJson.getJSONArray("members");
			int arraySize = membersJsonArray.length();
			log.debug("members json array length = " + arraySize);
			
			List<Member> existingMembers = theTeam.getMembers();
			List<Member> newAndExistingMembers = new ArrayList<Member>();
			newAndExistingMembers.addAll(existingMembers);
			for(int i=0; i<arraySize; i++) {
				JSONObject memberJsonObj = membersJsonArray.getJSONObject(i);
				Member member = new Member();
				UserMemberInfo primaryIndividual = new UserMemberInfo();
				
				// Must process this before "guardians" because guardians verifies that guardian email addresses don't equal
		        // the primary email address.
				if(memberJsonObj.has("emailAddress")) {
					String emailAddress = memberJsonObj.getString("emailAddress");
					if(emailAddress != null && emailAddress.length() > 0)  {
						//::BUSINESS_RULE:: primary email address of member participants must be unique per team
						for(Member m : newAndExistingMembers) {
							if(m.getEmailAddress() !=null && m.getEmailAddress().equalsIgnoreCase(emailAddress)) {
								return ApiStatusCode.MEMBER_EMAIL_ADDRESS_NOT_UNIQUE;
							}
						}
						
						member.setEmailAddress(emailAddress);
						primaryIndividual.setEmailAddress(emailAddress);
					}
				}
				
				// ::BUSINESS_RULE:: Guardian email address don't have to be unique per team (parent can have multiple kids on same team), but
				//                   must be unique within the membership. That is, each guardian email must be unique and cannot be the
				//                   same as the primary email address.
				// ::BUSINESS_RULE:: Guardian phone numbers don't have to be unique per team (parent can have multiple kids on same team), but
				//                   must be unique within the membership. That is, each guardian phone number must be unique and cannot be the
				//                   same as the primary phone number.
				if(memberJsonObj.has("guardians")) {
					JSONArray guardianJsonArray = memberJsonObj.getJSONArray("guardians");
					int guardianArraySize = guardianJsonArray.length();
					log.debug("guardian json array length = " + guardianArraySize);
					List<Guardian> guardians = new ArrayList<Guardian>();
					List<String> guardianEmailAddresses = new ArrayList<String>();
					List<String> guardianPhoneNumbers = new ArrayList<String>();
					
					for(int j=0; j<guardianArraySize; j++) {
						JSONObject guardianJsonObj = guardianJsonArray.getJSONObject(j);
						Guardian guardian = new Guardian();
						UserMemberInfo guardianIndividual = new UserMemberInfo();
						
						String guardianEmailAddress = null;
						if(guardianJsonObj.has("emailAddress")) {
							guardianEmailAddress = guardianJsonObj.getString("emailAddress");
							if(member.getEmailAddress() != null && guardianEmailAddress.equalsIgnoreCase(member.getEmailAddress())) {
								return ApiStatusCode.GUARDIAN_EMAIL_ADDRESS_NOT_UNIQUE;
							} else {
								for(String gea : guardianEmailAddresses) {
									if(gea != null && gea.equalsIgnoreCase(guardianEmailAddress)) {
										return ApiStatusCode.GUARDIAN_EMAIL_ADDRESS_NOT_UNIQUE;
									}
								}
							}
							
							guardianEmailAddresses.add(guardianEmailAddress);
							guardianIndividual.setEmailAddress(guardianEmailAddress);
						}
						guardian.setEmailAddress(guardianEmailAddress);
						
						String guardianPhoneNumber = null;
						if(guardianJsonObj.has("phoneNumber")) {
							guardianPhoneNumber = guardianJsonObj.getString("phoneNumber");
							guardianPhoneNumber = Utility.extractAllDigits(guardianPhoneNumber);
							guardianPhoneNumber = Utility.stripLeadingOneIfPresent(guardianPhoneNumber);
							if(guardianPhoneNumber.length() < Member.MINIMUM_PHONE_NUMBER_LENGTH || guardianPhoneNumber.length() > Member.MAXIMUM_PHONE_NUMBER_LENGTH) {
								return ApiStatusCode.INVALID_GUARDIAN_PHONE_NUMBER_PARAMETER;
							}
							
							if(guardianPhoneNumber.equalsIgnoreCase(member.getPhoneNumber())) {
								return ApiStatusCode.GUARDIAN_PHONE_NUMBER_NOT_UNIQUE;
							} else {
								for(String gpn : guardianPhoneNumbers) {
									if(gpn.equalsIgnoreCase(guardianPhoneNumber)) {
										return ApiStatusCode.GUARDIAN_PHONE_NUMBER_NOT_UNIQUE;
									}
								}
							}
							guardianPhoneNumbers.add(guardianPhoneNumber);
							guardianIndividual.setPhoneNumber(guardianPhoneNumber);
						}
						guardian.setPhoneNumber(guardianPhoneNumber);
						
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
					
						//::BUSINESSRULE:: guardian first name, last name, email address or phone number must be specified.
						//                 If not, silently ignore.
						if(!((guardianEmailAddress == null || guardianEmailAddress.length() == 0) && 
							 (guardianPhoneNumber == null || guardianPhoneNumber.length() == 0) &&
							 (guardianFirstName == null || guardianFirstName.length() == 0) && 
							 (guardianLastName == null || guardianLastName.length() == 0)              )) {
								guardians.add(guardian);
								// track members that may be associated with a User entity
								UserMemberInfo.addIfUniqueContactInfo(guardianIndividual, thePotentialAssociatedIndividuals);
								
						}
					}
					
					if(!member.createGuardians(guardians)) {
						log.debug("createGuardians() failed");
						return ApiStatusCode.SERVER_ERROR;
					}
				}
				
				if(memberJsonObj.has("firstName")) {
					member.setFirstName(memberJsonObj.getString("firstName"));
				}
				
				if(memberJsonObj.has("lastName")) {
					member.setLastName(memberJsonObj.getString("lastName"));
				}

				if(memberJsonObj.has("phoneNumber")) {
					String phoneNumber = memberJsonObj.getString("phoneNumber");
					phoneNumber = Utility.extractAllDigits(phoneNumber);
					phoneNumber = Utility.stripLeadingOneIfPresent(phoneNumber);
					if(phoneNumber.length() < Member.MINIMUM_PHONE_NUMBER_LENGTH || phoneNumber.length() > Member.MAXIMUM_PHONE_NUMBER_LENGTH) {
						return ApiStatusCode.INVALID_PHONE_NUMBER_PARAMETER;
					}
					member.setPhoneNumber(phoneNumber);
					primaryIndividual.setPhoneNumber(phoneNumber);
					
					//::BUSINESS_RULE:: if no email address for new member, then phone number provided must be unique per team
					if(member.getEmailAddress() == null || member.getEmailAddress().trim().length() == 0) {
						for(Member m : newAndExistingMembers) {
							if(m.getPhoneNumber() != null && m.getPhoneNumber().equalsIgnoreCase(member.getPhoneNumber())) {
								return ApiStatusCode.MEMBER_PHONE_NUMBER_NOT_UNIQUE;
							}
						}
					}
				}
				
				// get and save participantRole -- default if necessary
				String participantRole = null;
				if(memberJsonObj.has("participantRole")) {
					participantRole = memberJsonObj.getString("participantRole");
					
					if(participantRole == null) {
						// if not specified, default to 'member' participant
						participantRole = Member.MEMBER_PARTICIPANT;
					} else {
						participantRole = participantRole.toLowerCase();
						if(!Member.isValidParticipant(participantRole)) {
							return ApiStatusCode.INVALID_PARTICIPANT_ROLE_PARAMETER;
						}
					}
				} else {
					// if not specified, default to 'member' participant
					participantRole = Member.MEMBER_PARTICIPANT;
				}
				member.setParticipantRole(participantRole);
				
				//::BUSINESSRULE:: user must be a coordinator to add a 'coordinator' or 'member'. 
				if(!theIsCoordinator && member.isMemberParticipant()) {
					return ApiStatusCode.USER_NOT_A_COORDINATOR;
				}
				
				//::BUSINESSRULE:: first name or last name, email address or phone number must be specified
				if((member.getFirstName() == null || member.getFirstName().trim().length() == 0) &&
				   (member.getLastName() == null || member.getLastName().trim().length() == 0)   &&
				   (member.getEmailAddress() == null || member.getEmailAddress().trim().length() == 0) &&
				   (member.getPhoneNumber() == null || member.getPhoneNumber().length() == 0) ) {
					return ApiStatusCode.FIRST_NAME_OR_LAST_NAME_OR_EMAIL_ADDRESS_OR_PHONE_NUMBER_REQUIRED;
				}

				// TODO replace with Access Preferences
				member.setDefaultAccessPreferences();

				// track members that may be associated with a User entity
				UserMemberInfo.addIfUniqueContactInfo(primaryIndividual, thePotentialAssociatedIndividuals);

				theNewMembers.add(member);
				newAndExistingMembers.add(member);
			}
		}
    	
		return ApiStatusCode.SUCCESS;
    }
    
    // Handles 'Get list of team members' API  
    @Get("json")
    public JsonRepresentation getMemberList(Variant variant) {
        log.debug("MembersResource:getMemberList() entered");
        JSONObject jsonReturn = new JSONObject();
		EntityManager em = EMF.get().createEntityManager();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		try {
	    	User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
			if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				log.error("MembersResource:getMemberList:currentUser", "user could not be retrieved from Request attributes!!");
			}
			// teamId is required
			else if(this.teamId == null || this.teamId.length() == 0) {
				apiStatus = ApiStatusCode.TEAM_ID_REQUIRED;
				log.debug("invalid team ID");
			} 
			else if(!currentUser.isUserMemberOfTeam(this.teamId)) {
				apiStatus = ApiStatusCode.USER_NOT_MEMBER_OF_SPECIFIED_TEAM;
				log.debug(apiStatus);
        	}
			// validate includeFans parameter
			else if( this.includeFans != null && !this.includeFans.equalsIgnoreCase("true") &&
					!this.includeFans.equalsIgnoreCase("false")) {
				apiStatus = ApiStatusCode.INVALID_INCLUDE_FANS_PARAMETER;
				log.debug(apiStatus);
			}
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_OK)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			
			Team team = (Team)em.createNamedQuery("Team.getByKey")
				.setParameter("key", KeyFactory.stringToKey(this.teamId))
				.getSingleResult();
			
			List<Member> members = null;
			if(this.includeFans != null && this.includeFans.equalsIgnoreCase("true"))  {
				members = team.getMembers();
			} else {
				// TODO more efficient to get non-fan members from the database, though multiple queries required?
				members = new ArrayList<Member>();
				// filter out the fans
				for(Member m : team.getMembers()) {
					if(!m.isFan()) {
						members.add(m);
					}
				}
			}
    		
			JSONArray jsonArray = new JSONArray();
			for(Member m : members) {
				JSONObject jsonMemberObj = new JSONObject();
				jsonMemberObj.put("memberId", KeyFactory.keyToString(m.getKey()));
				jsonMemberObj.put("memberName", m.getFullName());
				jsonMemberObj.put("participantRole", m.getParticipantRole());
				jsonMemberObj.put("emailAddress", m.getEmailAddress());
				jsonMemberObj.put("isNetworkAuthenticated", m.getIsNetworkAuthenticated());
				jsonMemberObj.put("isSmsConfirmed", m.isPhoneNumberSmsConfirmed(m.getPhoneNumber()));
				jsonMemberObj.put("isEmailConfirmed", m.getIsEmailAddressNetworkAuthenticated());
				log.debug("User " + m.getFullName() + " isNetworkAuthenticated = " + m.getIsNetworkAuthenticated() + " isSmsConfirmed = " + m.isPhoneNumberSmsConfirmed(m.getPhoneNumber()) + "isEmailConfirmed = " + m.getIsEmailAddressNetworkAuthenticated());
				Boolean isUser = m.getUserId() == null ? false : true;
				jsonMemberObj.put("isUser", isUser);
				Boolean isCurrentUser = false;
				if(m.getUserId() != null && m.getUserId().equals(KeyFactory.keyToString(currentUser.getKey()))) {
					isCurrentUser = true;
				}
				jsonMemberObj.put("isCurrentUser", isCurrentUser);
				jsonMemberObj.put("phoneNumber", m.getPhoneNumber());
				
            	JSONArray jsonGuardianArray = new JSONArray();
				List<Guardian> guardians = m.getGuardians();
    			for(Guardian g : guardians) {
    				JSONObject jsonGuardianObj = new JSONObject();
    				jsonGuardianObj.put("emailAddress", g.getEmailAddress());
    				jsonGuardianObj.put("firstName", g.getFirstName());
    				jsonGuardianObj.put("lastName", g.getLastName());
    				jsonGuardianObj.put("phoneNumber", g.getPhoneNumber());
    				jsonGuardianObj.put("isNetworkAuthenticated", g.getIsNetworkAuthenticated());
    				jsonGuardianObj.put("isSmsConfirmed", g.getHasBeenSmsConfirmed());
    				jsonGuardianObj.put("isEmailConfirmed", g.getIsEmailAddressNetworkAuthenticated());
    				Boolean isGuardianUser = g.getUserId() != null && g.getUserId().length() > 0 ? true : false;
    				jsonGuardianObj.put("isUser", isGuardianUser);
    				jsonGuardianArray.put(jsonGuardianObj);
    			}
    			jsonMemberObj.put("guardians", jsonGuardianArray);
				
				jsonArray.put(jsonMemberObj);
			}
			jsonReturn.put("members", jsonArray);
		} catch (JSONException e) {
			log.exception("MembersResource:getMemberList:JSONException1", "", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (NoResultException e) {
			log.exception("MembersResource:getMemberList:NoResultException", "", e);
			apiStatus = ApiStatusCode.TEAM_NOT_FOUND;;
		} catch (NonUniqueResultException e) {
			log.exception("MembersResource:getMemberList:NonUniqueResultException", "", e);
		} 
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("MembersResource:getMemberList:JSONException2", "", e);
		}
		return new JsonRepresentation(jsonReturn);
    }
    
    
}  
