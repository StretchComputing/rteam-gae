package com.stretchcom.rteam.server;


import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.google.appengine.api.labs.taskqueue.TaskOptions.Method;
  
/** 
 * Resource that manages a list of items. 
 *  
 */  
public class UsersResource extends ServerResource {  
	//private static final Logger log = Logger.getLogger(UsersResource.class.getName());
	private RskyboxClient log = new RskyboxClient(this);
  
    @Override  
    protected void doInit() throws ResourceException {  
    	log.info("API requested with URL: " + this.getReference().toString());
    }
    
    // Handles 'Create a new user' API
    @Post("json")
    public JsonRepresentation createUser(Representation entity) {
    	log.debug("createUser(@Post) entered ..... ");
        User user = new User();
        JSONObject jsonReturn = new JSONObject();
		EntityManager em = EMF.get().createEntityManager();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_CREATED);
        try {
			JsonRepresentation jsonRep = new JsonRepresentation(entity);
			log.debug("jsonRep = " + jsonRep.toString());
			JSONObject json = jsonRep.getJsonObject();
			
			Boolean alreadyMember = null;
			if(json.has("alreadyMember")) {
				if(json.getString("alreadyMember").equalsIgnoreCase("true")) {
					alreadyMember = true;
				} else {
					alreadyMember = false;
				}
			}
			
			if(json.has("firstName")) {
				user.setFirstName(json.getString("firstName"));
			}
			
			if(json.has("lastName")) {
				user.setLastName(json.getString("lastName"));
			}
			
			if(json.has("emailAddress")) {
				user.setEmailAddress(json.getString("emailAddress"));
			}
			
			if(json.has("password")) {
				String plainTextPassword = json.getString("password");
				String encryptedPassword = Utility.encrypt(plainTextPassword);
				log.debug("encryptedPassword = " + encryptedPassword);
				if(encryptedPassword == null) {
					this.setStatus(Status.SERVER_ERROR_INTERNAL);
				} else {
					user.setPassword(encryptedPassword);
				}
			}
			
			if(json.has("passwordResetQuestion")) {
				user.setPasswordResetQuestion(json.getString("passwordResetQuestion"));
			}
			
			if(json.has("passwordResetAnswer")) {
				user.setPasswordResetAnswer(json.getString("passwordResetAnswer"));
			}
			
			//::BUSINESSRULE:: phone number must be within a well defined size range
			if(json.has("phoneNumber")) {
				String phoneNumber = json.getString("phoneNumber");
				phoneNumber = Utility.extractAllDigits(phoneNumber);
				phoneNumber = Utility.stripLeadingOneIfPresent(phoneNumber);
				if(phoneNumber.length() < Member.MINIMUM_PHONE_NUMBER_LENGTH || phoneNumber.length() > Member.MAXIMUM_PHONE_NUMBER_LENGTH) {
					jsonReturn.put("apiStatus", ApiStatusCode.INVALID_PHONE_NUMBER_PARAMETER);
					return new JsonRepresentation(jsonReturn);
				}

				user.setPhoneNumber(phoneNumber);
			}
			
			if(json.has("mobileCarrierCode")) {
				String carrierEmailDomainName = null;
				String mobileCarrierCode = json.getString("mobileCarrierCode");
				carrierEmailDomainName = MobileCarrier.findEmailDomainName(mobileCarrierCode);
				if(carrierEmailDomainName == null) {
					jsonReturn.put("apiStatus", ApiStatusCode.INVALID_MOBILE_CARRIER_CODE_PARAMETER);
					return new JsonRepresentation(jsonReturn);
				}
				user.setMobileCarrierCode(mobileCarrierCode);
				if(user.getPhoneNumber() != null) {
					user.setSmsEmailAddress(user.getPhoneNumber() + carrierEmailDomainName);
					user.setPhoneNumberConfirmationCode(TF.getConfirmationCode());
				}
			}
			
			if(json.has("latitude")) {
				String latitudeStr = json.getString("latitude");
				try {
					Double latitude = new Double(latitudeStr);
					user.setLatitude(latitude);
				} catch (NumberFormatException e) {
					jsonReturn.put("apiStatus", ApiStatusCode.INVALID_LATITUDE_PARAMETER);
					return new JsonRepresentation(jsonReturn);
				}
			}
			
			if(json.has("longitude")) {
				String longitudeStr = json.getString("longitude");
				try {
					Double longitude = new Double(longitudeStr);
					user.setLongitude(longitude);
				} catch (NumberFormatException e) {
					jsonReturn.put("apiStatus", ApiStatusCode.INVALID_LONGITUDE_PARAMETER);
					return new JsonRepresentation(jsonReturn);
				}
			}
			
			if(json.has("location")) {
				user.setLocation(json.getString("location"));
			}
			
			// not passed in via this API, but need to default the autoArchiveDayCount
			user.setAutoArchiveDayCount(User.AUTO_ARCHIVE_DAY_COUNT_DEFAULT);
			
			// do input parameter verification
			// firstName and lastName required if not alreadyMember
			if(alreadyMember != null && !alreadyMember) {
				apiStatus = ApiStatusCode.INVALID_ALREADY_MEMBER_PARAMETER;
			}
			else if(alreadyMember != null && !alreadyMember && (user.getFirstName() == null || user.getFirstName().length() == 0 ||
					user.getLastName() == null || user.getLastName().length() == 0) ) {
				apiStatus = ApiStatusCode.FIRST_AND_LAST_NAMES_REQUIRED;
			} else if(user.getEmailAddress() == null || user.getEmailAddress().length() == 0 ||
					user.getPassword() == null || user.getPassword().length() == 0) {
				apiStatus = ApiStatusCode.EMAIL_ADDRESS_AND_PASSWORD_REQUIRED;
			} else {
				User matchingUser = User.getUserWithEmailAddress(user.getEmailAddress());
				if(matchingUser != null) {
					apiStatus = ApiStatusCode.EMAIL_ADDRESS_ALREADY_USED;
				}
			}
			
//			log.debug("status = " + this.getStatus().toString());
//			log.debug("Log message apiStatus = " + apiStatus);
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_CREATED)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}
			
			// default options and other behind the scenes variables
			user.setShouldSendMessageOption(false);
			user.setNewMessageCount(0);

			em.getTransaction().begin();
			String token = TF.get();
			String naToken = TF.get();
			user.setToken(token);
			user.setNetworkAuthenticationToken(naToken);
			em.persist(user);
		    em.getTransaction().commit();
		    
		    // Send the welcome email
		    PubHub.sendUserWelcomeMessage(user);
		    
		    // Send the 'phone number confirmation' SMS if appropriate
		    if(user.getSmsEmailAddress() != null && user.getSmsEmailAddress().length() > 0) {
		    	PubHub.sendPhoneNumberConfirmation(user.getSmsEmailAddress(), user.getPhoneNumberConfirmationCode());
		    }
		    
			// if no prior memberships, create the first team for user, and then add that team to the user
		    Team firstTeam = null;
		    List<Member> confirmedMemberships = Member.getConfirmedMemberships(user.getEmailAddress(), user.getPhoneNumber());
		    if(confirmedMemberships.size() == 0) {
			    log.debug("new user has no confirmed memberships, about to create first team");
				firstTeam = Team.createFirst(user);
				user.addTeam(firstTeam, null);
		    }
		    
			String baseUri = this.getRequest().getHostRef().getIdentifier();
			this.getResponse().setLocationRef(baseUri + "/" + user.getEmailAddress());
			
			jsonReturn.put("token", token);
			if(firstTeam != null) jsonReturn.put("teamId", KeyFactory.keyToString(firstTeam.getKey()));
			log.debug("jsonReturn = " + jsonReturn.toString());
		} catch (IOException e) {
			log.exception("UsersResource:createUser:IOException", "", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (JSONException e) {
			log.exception("UsersResource:createUser:JSONException1", "", e);
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
			log.exception("UsersResource:createUser:JSONException2", "", e);
		}
		return new JsonRepresentation(jsonReturn);
    }
    
    // Handles 'Delete All Users' API
    @Delete
    public JsonRepresentation removeAllUsers() {
    	log.debug("UsersResource:remove() entered");
    	EntityManager em = EMF.get().createEntityManager();
        JSONObject jsonReturn = new JSONObject();
        
        String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
        try {
    		User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
    			log.error("UsersResource:removeAllUsers:userNotRetrieved", "user could not be retrieved from Request attributes!!");
    		} else if(!currentUser.getIsSuperUser()) {
    			apiStatus = ApiStatusCode.NOT_A_SUPER_USER;
    		}
    		else {
        		List<User> users = (List<User>)em.createNamedQuery("User.getAllNonSuperUsers")
        			.setParameter("isSuperUser", false)
        			.getResultList();
        		log.debug("UsersResource.remove(): removing " + users.size() + " Users");
        		
        		// tried to remove all users in a single transaction, but got the following error:
        		// "can't operate on multiple entity groups in a single transaction"
        		// alternate solution. Create a separate transaction for each user deleted
        		//::TODO do batch delete instead
        		for(User u : users) {
        	    	em.getTransaction().begin();
            		User aUser = (User)em.createNamedQuery("User.getByKey")
	        			.setParameter("key", u.getKey())
	        			.getSingleResult();
        			em.remove(aUser);
        			em.getTransaction().commit();
        		}
    		}
        } catch (NoResultException e) {
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
			log.exception("UsersResource:removeAllUsers:NoResultException", "no result exception", e);
		} finally {
		    if (em.getTransaction().isActive()) {
		        em.getTransaction().rollback();
		    }
		    em.close();
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("UsersResource:removeAllUsers:JSONException", "no result exception", e);
		}
		return new JsonRepresentation(jsonReturn);
    }
    
    // Handles 'Migration' API
    @Put 
    public JsonRepresentation handleMigration(Representation entity) {
    	JSONObject jsonReturn = new JSONObject();
    	log.debug("updateUsers(@Put) entered ..... private migration");
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		
        try {
			JsonRepresentation jsonRep = new JsonRepresentation(entity);
			JSONObject json = jsonRep.getJsonObject();
			log.debug("received json object = " + json.toString());
			
			if(json.has("secret")) {
				String secret = json.getString("secret");
				if(secret.equals("ae53b1f9")) {
					if(json.has("migrationName")) {
						String migrationName = json.getString("migrationName");
						
						// Initial data migrations were not done as GAE Tasks, but as the size of the data grows, the
						// migrations often require the longer 10 min execution time allowed to tasks. Migration that
						// are tasks are identified by a migration name that ends with "Task"
						if(migrationName.endsWith("Task")) {
							createMigrationTask(migrationName);
						} else {
							// These migration are NOT run as tasks - limited to 30 second execution time by GAE
							if(migrationName.equalsIgnoreCase("createUserCacheIds")) {
								createUserCacheIdsMigration();
							} else if(migrationName.equalsIgnoreCase("encryptPasswords")) {
								String parameterOne = json.has("parameterOne") ? json.getString("parameterOne") : null;
								encryptPasswordsMigration(parameterOne);
							} else if(migrationName.equalsIgnoreCase("bindMembersToUser")) {
								String parameterOne = json.has("parameterOne") ? json.getString("parameterOne") : null;
								bindMembersToUserMigration(parameterOne);
							} else if(migrationName.equalsIgnoreCase("addAutoArchiveDayCountToUsers")) {
								addAutoArchiveDayCountToUsersMigration();
							} else if(migrationName.equalsIgnoreCase("addAutoArchiveDayCountToNaMembers")) {
								addAutoArchiveDayCountToNaMembersMigration();
							} else if(migrationName.equalsIgnoreCase("addActiveThruGmtDateToMessageThreads")) {
								addActiveThruGmtDateToMessageThreadsMigration();
							} else if(migrationName.equalsIgnoreCase("addActiveThruGmtDateToRecipients")) {
								addActiveThruGmtDateToRecipientsMigration();
							} else {
								log.error("UsersResource:handleMigration:migrationNameMatch", "could NOT match migrationName");
							}
						}
					}
					else {
						log.error("UsersResource:handleMigration:noMigrationName", "migration called with no 'migrationName' parameter");
					}
				}
			}
        } catch (IOException e) {
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
			log.exception("UsersResource:handleMigration:IOException", "error extracting JSON object from Post", e);
		} catch (JSONException e) {
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
			log.exception("UsersResource:handleMigration:JSONException1", "", e);
		} finally {
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("UsersResource:handleMigration:JSONException2", "", e);
		}
		return new JsonRepresentation(jsonReturn);
    	
    }
    
    private void createMigrationTask(String theMigrationName) {
		// URL "/migrationTask" is routed to MigrationTaskServlet in web.xml
		// not calling name() to name the task, so the GAE will assign a unique name that has not been used in 7 days (see book)
		// method defaults to POST, but decided to be explicit here
		// PRIORITY TODO need to somehow secure this URL. Book uses web.xml <security-constraint> but not sure why it restricts the
		//               URL to task queues (I see how it restricts it to admins)
		TaskOptions taskOptions = TaskOptions.Builder.url("/migrationTask")
				.method(Method.POST)
				.param("migrationName", theMigrationName);
		Queue queue = QueueFactory.getQueue("migration"); // "migration" queue is defined in WEB-INF/queue.xml
		queue.add(taskOptions);
    }
    
    private void createUserCacheIdsMigration() {
		EntityManager em = EMF.get().createEntityManager();
		
		try {
			List<User> users = (List<User>)em.createNamedQuery("User.getAll").getResultList();
			log.debug("UsersResource.updateUsers(): migrating " + users.size() + " Users by adding cacheIds when needed.");
		
			// try this without a transaction
			for(User u : users) {
				List<Long> teamNewestCacheIds = new ArrayList<Long>();
				List<Key> teams = u.getTeams();
				if(teams.size() == 0) {continue;}
				for(int i=0; i<teams.size(); i++) {
					teamNewestCacheIds.add(0L);
				}
				u.setTeamNewestCacheIds(teamNewestCacheIds);
				log.debug("user " + u.getFullName() + " successfully migrated. Number of cacheIds = " + teams.size());
			}
		} catch(Exception e) {
			log.exception("UsersResource:createUserCacheIdsMigration:Exception", "", e);
		} finally {
		    em.close();
		}
    }
    
    private void encryptPasswordsMigration(String theParameterOne) {
		EntityManager em = EMF.get().createEntityManager();
		
		try {
			List<User> users = (List<User>)em.createNamedQuery("User.getAll").getResultList();
			log.debug("UsersResource.updateUsers(): migrating " + users.size() + " Users by encrypting their password.");
		
			// try this without a transaction
			for(User u : users) {
				// test with rteamtest1 first
				if(theParameterOne != null && !u.getEmailAddress().equalsIgnoreCase(theParameterOne)) {
					continue;
				}
				String plainTextPassword = u.getPassword();
				String encryptedPassword = Utility.encrypt(plainTextPassword);
				u.setPassword(encryptedPassword);
				log.debug("user " + u.getFullName() + " password successfully encrypted.");
			}
		} catch(Exception e) {
			log.exception("UsersResource:encryptPasswordsMigration:Exception", "", e);
		} finally {
		    em.close();
		}
    }
    
    
    private void bindMembersToUserMigration(String theParameterOne) {
		if(theParameterOne == null) {
			log.debug("no email address passed in, migration terminated immediately");
		}
		
    	EntityManager emUser = EMF.get().createEntityManager();
    	User user = null;
		try {
			user = (User)emUser.createNamedQuery("User.getByEmailAddress")
				.setParameter("emailAddress", theParameterOne.toLowerCase())
				.getSingleResult();
			log.debug("user found = " + user.getFullName());
		} catch (NoResultException e) {
			log.debug("user not found using supplied email address");
		} catch (NonUniqueResultException e) {
			log.exception("UsersResource:bindMembersToUserMigration:NonUniqueResultException", "two users have the same email address", e);
		} finally {
			emUser.close();
		}
		
		if(user == null) {
			return;
		}

		EntityManager emMemberships = EMF.get().createEntityManager();
		try {
			List<Member> creatorMemberships = (List<Member>)emMemberships.createNamedQuery("Member.getByEmailAddressAndParticipantRole")
				.setParameter("emailAddress", theParameterOne.toLowerCase())
				.setParameter("participantRole", Member.CREATOR_PARTICIPANT)
				.getResultList();
			log.debug("# of creator memberships found = " + creatorMemberships.size());
			
			for(Member m : creatorMemberships) {
    	    	emMemberships.getTransaction().begin();
        		Member singleMember = (Member)emMemberships.createNamedQuery("Member.getByKey")
        			.setParameter("key", m.getKey())
        			.getSingleResult();
        		
				// Setting the user ID in member indicates that is is 'synched up' with the user. The member can
				// hold user IDs for each of its email addresses, that why we set the user ID using the email address.
        		singleMember.setUserIdByEmailAddress(KeyFactory.keyToString(user.getKey()), theParameterOne);
        		log.debug("membership for team = " + singleMember.getTeam().getTeamName() + " bound to user " + user.getFullName());
        		singleMember.setAutoArchiveDayCountByEmailAddress(user.getAutoArchiveDayCount(), user.getEmailAddress());
        		
    			emMemberships.getTransaction().commit();
			}
    	} catch (Exception e) {
			log.exception("UsersResource:bindMembersToUserMigration:Exception", "two users have the same email address", e);
    	} finally {
		    if (emMemberships.getTransaction().isActive()) {
		    	emMemberships.getTransaction().rollback();
		    }
		    emMemberships.close();
		}
    }
    
    
    private void addAutoArchiveDayCountToUsersMigration() {
		EntityManager em = EMF.get().createEntityManager();
		
		try {
			List<User> users = (List<User>)em.createNamedQuery("User.getAll").getResultList();
		
			// try this without a transaction
			for(User u : users) {
				u.setAutoArchiveDayCount(30);
			}
			log.debug("Migrated " + users.size() + " Users by adding AutoArchiveDayCount.");
		} catch(Exception e) {
			log.exception("UsersResource:addAutoArchiveDayCountToUsersMigration:Exception", "", e);
		} finally {
		    em.close();
		}
    }
     
    private void addAutoArchiveDayCountToNaMembersMigration() {
		EntityManager em = EMF.get().createEntityManager();
		
		try {
			List<Member> members = (List<Member>)em.createNamedQuery("Member.getAll").getResultList();
		
			int totalIndividualCountsUpdated = 0;
			// try this without a transaction
			for(Member m : members) {
				List<String> emailAddresses = m.getNetworkAuthenticatedActiveEmailAddresses();
				for(String ea : emailAddresses) {
					m.setAutoArchiveDayCountByEmailAddress(30, ea);
					totalIndividualCountsUpdated++;
				}
			}
			log.debug("Migrated " + members.size() + " Members by adding AutoArchiveDayCount. Total totalIndividualCountsUpdated = " + totalIndividualCountsUpdated);
		} catch(Exception e) {
			log.exception("UsersResource:addAutoArchiveDayCountToNaMembersMigration:Exception", "", e);
		} finally {
		    em.close();
		}
    }
    
    private void addActiveThruGmtDateToMessageThreadsMigration() {
		EntityManager em = EMF.get().createEntityManager();
		
		try {
    		List<MessageThread> messageThreads = (List<MessageThread>)em.createNamedQuery("MessageThread.getByStatus")
				.setParameter("status", MessageThread.ACTIVE_STATUS)
				.getResultList();
		
			// Active date will be 30 days from created date -- but guaranteed to be at least 24 hours into the future
    		for(MessageThread mt : messageThreads) {
	    		Date activeThruGmtDate = mt.getCreatedGmtDate();
	    		activeThruGmtDate = GMT.addDaysToDate(activeThruGmtDate, 30);
	    		activeThruGmtDate = GMT.setToFutureDate(activeThruGmtDate);
	    		mt.setActiveThruGmtDate(activeThruGmtDate);
			}
			log.debug("ActiveThruGmtDate set for messageThread count = " + messageThreads.size());
		} catch(Exception e) {
			log.exception("UsersResource:addActiveThruGmtDateToMessageThreadsMigration:Exception", "", e);
		} finally {
		    em.close();
		}
    }
    
    private void addActiveThruGmtDateToRecipientsMigration() {
		EntityManager em = EMF.get().createEntityManager();
		
		try {
    		List<Recipient> recipients = (List<Recipient>)em.createNamedQuery("Recipient.getByNotStatus")
				.setParameter("status", Recipient.ARCHIVED_STATUS)
				.getResultList();
		
			// Active date will be 30 days from received date -- but guaranteed to be at least 24 hours into the future
			for(Recipient r : recipients) {
	    		Date activeThruGmtDate = r.getReceivedGmtDate();
	    		activeThruGmtDate = GMT.addDaysToDate(activeThruGmtDate, 30);
	    		activeThruGmtDate = GMT.setToFutureDate(activeThruGmtDate);
	    		r.setActiveThruGmtDate(activeThruGmtDate);
			}
			log.debug("ActiveThruGmtDate adjusted: recipients count = " + recipients.size());
		} catch(Exception e) {
			log.exception("UsersResource:addActiveThruGmtDateToRecipientsMigration:Exception", "", e);
		} finally {
		    em.close();
		}
    }
}  
