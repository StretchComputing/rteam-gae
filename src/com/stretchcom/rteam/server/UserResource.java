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
import org.restlet.Request;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.Options;
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
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author joepwro
 */
public class UserResource extends ServerResource {
	//private static final Logger log = Logger.getLogger(UserResource.class.getName());
	private RskyboxClient log = new RskyboxClient(this);

    // The sequence of characters that identifies the resource.
    String emailAddress;
    String plainTextPassword;
    String oneUseToken;
    String includeMembers;
    String deleteNow;
    String passwordResetQuestion;
    String includePhoto;
  
    @Override  
    protected void doInit() throws ResourceException {  
        this.emailAddress = (String)getRequest().getAttributes().get("emailAddress");
        if(this.emailAddress != null) {
            this.emailAddress = Reference.decode(this.emailAddress);
            // email address always stored in lower case
            this.emailAddress = this.emailAddress.toLowerCase();
			log.debug("decoded emailAddress = " + this.emailAddress);
        }
        
        this.passwordResetQuestion = (String)getRequest().getAttributes().get("passwordResetQuestion");
        if(this.passwordResetQuestion != null) {
            this.passwordResetQuestion = Reference.decode(this.passwordResetQuestion);
			log.debug("decoded passwordResetQuestion = " + this.passwordResetQuestion);
        }

        // same method is used to handle both 'Get User Info' and 'Get User Token'
        // for 'Get User Token', the email address and password come in as query parameters
        Form form = getRequest().getResourceRef().getQueryAsForm();
		for (Parameter parameter : form) {
			log.debug("parameter " + parameter.getName() + " = " + parameter.getValue());
			if(parameter.getName().equals("emailAddress"))  {
				this.emailAddress = (String)parameter.getValue();
				this.emailAddress = Reference.decode(this.emailAddress);
				log.debug("UserResource:doInit() - decoded emailAddress = " + this.emailAddress);
			} 
			else if(parameter.getName().equals("password"))  {
				this.plainTextPassword = (String)parameter.getValue();
				this.plainTextPassword = Reference.decode(this.plainTextPassword);
				//log.debug("UserResource:doInit() - decoded password = " + this.plainTextPassword); // blocked for security reasons
			} 
			else if(parameter.getName().equals("oneUseToken"))  {
				this.oneUseToken = (String)parameter.getValue();
				this.oneUseToken = Reference.decode(this.oneUseToken);
				log.debug("UserResource:doInit() - decoded oneUseToken = " + this.oneUseToken);
			} 
			else if(parameter.getName().equals("includeMembers"))  {
				this.includeMembers = (String)parameter.getValue();
				this.includeMembers = Reference.decode(this.includeMembers);
				log.debug("UserResource:doInit() - decoded includeMembers = " + this.includeMembers);
			} 
			else if(parameter.getName().equals("deleteNow"))  {
				this.deleteNow = (String)parameter.getValue();
				this.deleteNow = Reference.decode(this.deleteNow);
				log.debug("UserResource:doInit() - decoded deleteNow = " + this.deleteNow);
			}
			else if(parameter.getName().equals("includePhoto"))  {
				this.includePhoto = (String)parameter.getValue();
				this.includePhoto = Reference.decode(this.includePhoto);
				log.debug("UserResource:doInit() - decoded includePhoto = " + this.includePhoto);
			}
		}
    }  

	//::TODO  remove hack below allowing Options method - needed for restlet gwt bug
    // Handles 'Get User Token' API
    // Handles 'Get User Confirmation Info' API (GWT)
    // Handles 'Get User Info' API
    // Handles 'Get User Password Reset Question' API
    @Get("json")
    @Options("json")
    public JsonRepresentation getUserInfo(Variant variant) {
        log.debug("UserResource:toJson() entered");
        JSONObject jsonReturn = new JSONObject();
		EntityManager em = EMF.get().createEntityManager();

		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		try {
			if(this.passwordResetQuestion != null && this.passwordResetQuestion.equalsIgnoreCase("passwordResetQuestion")) {
    			// -------------------------------------------------------
				// This is the 'Get User Password Reset Question' API call
    			// -------------------------------------------------------
    			// this API is not authorized via token
    			log.debug("This is the 'Get User Password Reset Question' API call");
    			
    			User user = null;
    			try {
					user = (User) em.createNamedQuery("User.getByEmailAddress")
						.setParameter("emailAddress", this.emailAddress)
						.getSingleResult();
					
					String resetQuestion = user.getPasswordResetQuestion() == null ? "" : user.getPasswordResetQuestion();
					jsonReturn.put("passwordResetQuestion", resetQuestion);
				} catch (NoResultException e) {
					apiStatus = ApiStatusCode.USER_NOT_FOUND;
				} catch (NonUniqueResultException e) {
	    			log.exception("UserResource:getUserInfo:NonUniqueResultException1",
	    					                "two or more users have same email address", e);
					this.setStatus(Status.SERVER_ERROR_INTERNAL);
				} 
			} else if(this.plainTextPassword != null) {
    			// -------------------------------------
				// This is the 'Get User Token' API call
    			// -------------------------------------
    			// this API is not authorized via token
    			log.debug("user token has been requested");
    			
    			User user = null;
    			try {
    				String encryptedPassword = Utility.encrypt(this.plainTextPassword);
					user = (User) em.createNamedQuery("User.getByEmailAddressAndPasswordAndIsNetworkAuthenticated")
						.setParameter("emailAddress", this.emailAddress)
						.setParameter("password", encryptedPassword)
						.setParameter("isNetworkAuthenticated", true)
						.getSingleResult();
				} catch (NoResultException e) {
					// email didn't match any user - apiStatus and setStatus handled below
				} catch (NonUniqueResultException e) {
	    			log.exception("UserResource:getUserInfo:NonUniqueResultException2",
			                "two or more network authenticated users have same email address and password", e);
				} 
				
				if(user != null) {
					jsonReturn.put("token", user.getToken());
					if(user.getUserIconOneId() != null) jsonReturn.put("userIconOneId", user.getUserIconOneId());
					if(user.getUserIconOneAlias() != null) jsonReturn.put("userIconOneAlias", user.getUserIconOneAlias());
					if(user.getUserIconOneImage() != null) jsonReturn.put("userIconOneImage", user.getUserIconOneImage());
					if(user.getUserIconTwoId() != null) jsonReturn.put("userIconTwoId", user.getUserIconTwoId());
					if(user.getUserIconTwoAlias() != null) jsonReturn.put("userIconTwoAlias", user.getUserIconTwoAlias());
					if(user.getUserIconTwoImage() != null) jsonReturn.put("userIconTwoImage", user.getUserIconTwoImage());
				} else {
					apiStatus = ApiStatusCode.INVALID_USER_CREDENTIALS;
				}
    		} else if(this.oneUseToken != null) {
    			// -------------------------------------------------------
				// This is the 'Get User Confirmation Info' API call (GWT)
    			// -------------------------------------------------------
    			//
    			// currently, this is handling both user and member NA due to structure of Rteam.java
    			// TODO handle member NA elsewhere
    			//
    			
    			log.debug("This is the 'Get User Confirmation Info' API call");
    			
    			//::SIDE_EFFECT:: API is get confirm info, but here we are updating the user/member entities
    			//::EVENT::
    			UserMemberInfo userMemberInfo = MessageThreadResource.handleUserMemberConfirmEmailResponse(this.oneUseToken);
    			
    			if(userMemberInfo.getApiStatus().equals(ApiStatusCode.SUCCESS)) {
                	jsonReturn.put("firstName", userMemberInfo.getFirstName());
        			jsonReturn.put("lastName", userMemberInfo.getLastName());
        			jsonReturn.put("emailAddress", userMemberInfo.getEmailAddress());
        			jsonReturn.put("token", userMemberInfo.getOneUseToken());
        			
        			// new team member message sent to members, not new users
        			if(userMemberInfo.getTeam() != null && userMemberInfo.getParticipantRole() != null) {
        				PubHub.sendNewTeamMemberMessage(userMemberInfo);
        			}
    			} else if(userMemberInfo.getApiStatus().equals(ApiStatusCode.SERVER_ERROR)) {
    				this.setStatus(Status.SERVER_ERROR_INTERNAL);
    			} else if(userMemberInfo.getApiStatus().equals(ApiStatusCode.EMAIL_ADDRESS_ALREADY_USED)) {
    				jsonReturn.put("emailAddress", userMemberInfo.getEmailAddress());
    			}
    			apiStatus = userMemberInfo.getApiStatus();
    		} else {
    			// -------------------------------------
    			// This is the 'Get User Info' API call
    			// -------------------------------------
    			log.debug("This is the 'Get User Info' API call");
    			
        		User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
        		if(currentUser == null) {
	    			log.error("UserResource:getUserInfo:currentUser", "current user could not be retrieved from Request attributes");
    				this.setStatus(Status.SERVER_ERROR_INTERNAL);
    				return new JsonRepresentation(jsonReturn);
        		}
        	
				jsonReturn.put("firstName", currentUser.getFirstName());
				jsonReturn.put("lastName", currentUser.getLastName());
				jsonReturn.put("emailAddress", this.emailAddress);
				jsonReturn.put("isNetworkAuthenticated", currentUser.getIsNetworkAuthenticated().toString());
				jsonReturn.put("isSuperUser", currentUser.getIsSuperUser());
				// only return phone number if this optional field has been specified
				if(currentUser.getPhoneNumber() !=null) jsonReturn.put("phoneNumber", currentUser.getPhoneNumber());
				String passwordQuestion = currentUser.getPasswordResetQuestion();
				if(passwordQuestion != null && passwordQuestion.length() > 0) {
					jsonReturn.put("passwordResetQuestion", passwordQuestion);
				}
				Integer autoArchiveDayCount = currentUser.getAutoArchiveDayCount();
				if(autoArchiveDayCount != null) {
					jsonReturn.put("autoArchiveDayCount", autoArchiveDayCount);
				}
				
				if(this.includePhoto != null && this.includePhoto.equalsIgnoreCase("true")) {
		    		String photoBase64 = currentUser.getPhotoBase64();
		    		if(photoBase64 != null) {
		        		jsonReturn.put("photo", currentUser.getPhotoBase64());
		    		}
				}
				jsonReturn.put("latitude", currentUser.getLatitude());
				jsonReturn.put("longitude", currentUser.getLongitude());
    		}
		} catch (JSONException e) {
			log.exception("UserResource:getUserInfo:JSONException", "error building JSON object", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (NoResultException e) {
			apiStatus = ApiStatusCode.USER_NOT_FOUND;
		} catch (NonUniqueResultException e) {
			log.exception("UserResource:getUserInfo:NonUniqueResultException3", "error building JSON object", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} finally {
			em.close();
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("UserResource:getUserInfo:JSONException2", "error creating JSON return object", e);
		}
        return new JsonRepresentation(jsonReturn);
    }
    
    // Handles 'Delete User' API
    // This deactivates the user and if requested, associated memberships.
    @Delete
    public JsonRepresentation remove() {
    	log.debug("UserResource:remove() entered");
    	EntityManager em = EMF.get().createEntityManager();
    	JSONObject jsonReturn = new JSONObject();
    	
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
    	em.getTransaction().begin();
        try {
    		User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
    		if(currentUser == null) {
    			log.error("UserResource:remove:currentUser", "user could not be retrieved from Request attributes!!");
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				return new JsonRepresentation(jsonReturn);
    		}

    		// must get user within this transaction
    		User user = (User)em.createNamedQuery("User.getByKey")
				.setParameter("key", currentUser.getKey())
				.getSingleResult();

    		// either delete the user or deactivate
    		Boolean isDeactivateRequest = false;
    		if(this.deleteNow != null && this.deleteNow.equalsIgnoreCase("true")) {
    			// delete the user now - for TESTING only
    			em.remove(user);
    		} else {
    			// deactivate the user
        		// nulling out the token deactivates the user
        		user.setToken(null);
        		isDeactivateRequest = true;
    		}
     		em.getTransaction().commit();
     		
    		// Deactivate appropriate Memberships
    		// can only deactivate memberships if user is network authenticated and the memberships are network authenticated
     		if(isDeactivateRequest && user.getIsNetworkAuthenticated() && this.includeMembers != null && this.includeMembers.equalsIgnoreCase("true")) {
     			EntityManager em2 = EMF.get().createEntityManager();
     			try {
         			em.getTransaction().begin();
        			List<Member> naMembers = (List<Member>)em.createNamedQuery("Member.getByNetworkAuthenticatedEmailAddress")
    					.setParameter("emailAddress", user.getEmailAddress())
    					.getResultList();
         			
         			if(naMembers != null) {
         				for(Member nam : naMembers) {
         					nam.deactivateEmailAddress(user.getEmailAddress());
         				}
         			} else {
         				// null is only returned if there was an server error
         				this.setStatus(Status.SERVER_ERROR_INTERNAL);
         			}
         			em2.getTransaction().commit();
     			} catch(Exception e) {
     				log.exception("UserResource:remove:Exception", "query(s) to get members from user email failed", e);
     				this.setStatus(Status.SERVER_ERROR_INTERNAL);
     			} finally {
     			    if (em2.getTransaction().isActive()) {
     			        em2.getTransaction().rollback();
     			    }
     			    em2.close();
     			}
     		}
        } catch (NoResultException e) {
        	// server error because user was already found earlier within this request (i.e. currentUser)
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (NonUniqueResultException e) {
			log.exception("UserResource:remove:NonUniqueResultException", "two or more users have same the same key", e);
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
			log.exception("UserResource:remove:JSONException", "error creating JSON return object", e);
			e.printStackTrace();
		}
        return new JsonRepresentation(jsonReturn);
    }
    
    // Handles 'Update User' API
    // Handles 'User Password Reset' API
    @Put 
    public JsonRepresentation updateUser(Representation entity) {
    	JSONObject jsonReturn = new JSONObject();
    	log.debug("updateUser(@Put) entered ..... ");
		EntityManager em = EMF.get().createEntityManager();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);

		String randomPassword = null;
		Boolean isEmailUpdated = false;
		em.getTransaction().begin();
        try {
			JsonRepresentation jsonRep = new JsonRepresentation(entity);
			JSONObject json = jsonRep.toJsonObject();
			log.debug("received json object = " + json.toString());
			
			if(json.has("isPasswordReset")) {
				////////////////////////////
				// 'User Password Reset' API
				////////////////////////////
				
	    		try {
					//::TODO user matching email address must be Network Authenticated
	    			User user = (User)em.createNamedQuery("User.getByEmailAddress")
						.setParameter("emailAddress", this.emailAddress)
						.getSingleResult();
	    			
					String passwordResetAnswer = "";
	    			if(json.has("passwordResetAnswer")) {
						passwordResetAnswer = json.getString("passwordResetAnswer");
					}
	    			
	    			// there are a few ways to "match" the password reset question
	    			if( (user.getPasswordResetAnswer() == null && passwordResetAnswer.length() == 0) ||
	    			    (user.getPasswordResetAnswer() != null && user.getPasswordResetAnswer().equalsIgnoreCase(passwordResetAnswer))) {
						randomPassword = TF.getPassword();
						String encryptedRandomPassword = Utility.encrypt(randomPassword);
						user.setPassword(encryptedRandomPassword);
						em.getTransaction().commit();
						Emailer.sendPasswordResetEmail(user, randomPassword);
						log.debug("this is a password reset request for user with email address = " + this.emailAddress);
	    			} else {
						apiStatus = ApiStatusCode.PASSWORD_RESET_FAILED;
	    			}
				} catch (NoResultException e) {
					// email address not found - this is a bad request
					apiStatus = ApiStatusCode.USER_NOT_FOUND;
				} catch (NonUniqueResultException e) {
					log.exception("UserResource:updateUser:NonUniqueResultException", "two or more users have the same email address", e);
					this.setStatus(Status.SERVER_ERROR_INTERNAL);
				}
			} else {
				////////////////////////////
				// 'Update User' API
				////////////////////////////
				
	    		User currentUser = (User)this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
	    		if(currentUser == null) {
					this.setStatus(Status.SERVER_ERROR_INTERNAL);
     				log.error("UserResource:updateUser:currentUser", "user could not be retrieved from Request attributes!!");
	    			return new JsonRepresentation(jsonReturn);
	    		}
	    		
	    		// currentUser above is not "managed", so retrieve a managed user entity object
	    		User user = (User)em.createNamedQuery("User.getByEmailAddress")
					.setParameter("emailAddress", currentUser.getEmailAddress())
					.getSingleResult();

				// if new field is empty, original value is not updated.
				if(json.has("firstName")) {
					String firstName = json.getString("firstName");
					if(firstName.length() > 0) user.setFirstName(firstName);
				}
				
				// if new field is empty, original value is not updated.
				if(json.has("lastName")) {
					String lastName = json.getString("lastName");
					if(lastName.length() > 0) user.setLastName(lastName);
				}
				
				// if new field is empty, original value is not updated.
				// the email is not considered updated unless the new email address is different than the original
				if(json.has("emailAddress")) {
					String emailAddress = json.getString("emailAddress");
					String originalEmailAddress = user.getEmailAddress();
					if(emailAddress.length() > 0 && originalEmailAddress != null && !originalEmailAddress.equalsIgnoreCase(emailAddress)) {
						user.setEmailAddress(emailAddress);
						user.setIsNetworkAuthenticated(false);
						isEmailUpdated = true;
					}
				}
				
				// if new field is empty, field is cleared.
				// TODO TEST ONLY, disable somehow during production
				if(json.has("isNetworkAuthenticated")) {
					String isNetworkAuthenticatedStr = json.getString("isNetworkAuthenticated");
					if(isNetworkAuthenticatedStr.equalsIgnoreCase("true")) {
						user.setIsNetworkAuthenticated(true);
					} else if(isNetworkAuthenticatedStr.equalsIgnoreCase("false")) {
						user.setIsNetworkAuthenticated(true);
					} else {
						apiStatus = ApiStatusCode.INVALID_IS_NETWORKAUTHENTICATED_PARAMETER;
					}
					
				}
				
				// if new field is empty, original value is not updated.
				if(json.has("password")) {
					String plainTextPassword = json.getString("password");
					if(plainTextPassword.length() > 0) {
						String encryptedPassword = Utility.encrypt(plainTextPassword);
						user.setPassword(encryptedPassword);
						}
				}

				// if new field is empty, original value is not updated.
				if(json.has("alertToken")) {
					String alertToken = json.getString("alertToken");
					if(alertToken.length() > 0) user.setAlertToken(alertToken);
				}

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
					}
				}
				
				Boolean sendConfirmation = null;
				if(json.has("sendConfirmation")) {
					sendConfirmation = json.getBoolean("sendConfirmation");
					log.debug("json sendConfirmation = " + sendConfirmation);
				}
				
				// sendConfirmation request overrides any confirmation code that may have been passed in.
				Boolean justSmsConfirmed = false;
				if(sendConfirmation != null && sendConfirmation) {
					// since we will be sending out a new confirmation SMS, create a new confirmation code
					user.setPhoneNumberConfirmationCode(TF.getConfirmationCode());
				} else {
					if(json.has("confirmationCode")) {
						String confirmationCode = json.getString("confirmationCode");
						// let's keep this simple on the user -- confirmation code is NOT case sensitive
						if(confirmationCode.equalsIgnoreCase(user.getPhoneNumberConfirmationCode())) {
							log.debug("user now SMS confirmed");
							user.setIsSmsConfirmed(true);
							justSmsConfirmed = true;
						} else {
							log.debug("invalid confirmation code = " + confirmationCode);
							jsonReturn.put("apiStatus", ApiStatusCode.INVALID_CONFIRMATION_CODE_PARAMETER);
							return new JsonRepresentation(jsonReturn);
						}
					} 
				}

				// if new field is empty, original value is cleared.
				if(json.has("passwordResetQuestion")) {
					String passwordResetQuestion = json.getString("passwordResetQuestion");
					user.setPasswordResetQuestion(passwordResetQuestion);
				}

				// if new field is empty, original value is cleared.
				if(json.has("passwordResetAnswer")) {
					String passwordResetAnswer = json.getString("passwordResetAnswer");
					user.setPasswordResetAnswer(passwordResetAnswer);
				}

				// if new field is empty, original value is cleared.
				if(json.has("userIconOneId")) {
					String userIconOneId = json.getString("userIconOneId");
					user.setUserIconOneId(userIconOneId);
				}

				// if new field is empty, original value is cleared.
				if(json.has("userIconOneAlias")) {
					String userIconOneAlias = json.getString("userIconOneAlias");
					user.setUserIconOneAlias(userIconOneAlias);
				}

				// if new field is empty, original value is cleared.
				if(json.has("userIconOneImage")) {
					String userIconOneImage = json.getString("userIconOneImage");
					user.setUserIconOneImage(userIconOneImage);
				}

				// if new field is empty, original value is cleared.
				if(json.has("userIconTwoId")) {
					String userIconTwoId = json.getString("userIconTwoId");
					user.setUserIconTwoId(userIconTwoId);
				}

				// if new field is empty, original value is cleared.
				if(json.has("userIconTwoAlias")) {
					String userIconTwoAlias = json.getString("userIconTwoAlias");
					user.setUserIconTwoAlias(userIconTwoAlias);
				}

				// if new field is empty, original value is cleared.
				if(json.has("userIconTwoImage")) {
					String userIconTwoImage = json.getString("userIconTwoImage");
					user.setUserIconTwoImage(userIconTwoImage);
				}

				// if new field is empty, original value is not updated.
				Integer autoArchiveDayCountAdjustment = 0;
				if(json.has("autoArchiveDayCount")) {
					Integer autoArchiveDayCount;
					try {
						autoArchiveDayCount = json.getInt("autoArchiveDayCount");
						if(!user.getAutoArchiveDayCount().equals(autoArchiveDayCount)) {
							// the count has changed, existing messaging entities for this user must be updated to reflect new dates
							autoArchiveDayCountAdjustment = autoArchiveDayCount - user.getAutoArchiveDayCount();
							log.debug("autoArchiveDayCountAdjustment = " + autoArchiveDayCountAdjustment);
						}
						user.setAutoArchiveDayCount(autoArchiveDayCount);
					} catch (JSONException e) {
						log.debug("autoArchiveDayCount value is not an integer");
						jsonReturn.put("apiStatus", ApiStatusCode.INVALID_AUTO_ARCHIVE_DAY_COUNT_PARAMETER);
						return new JsonRepresentation(jsonReturn);
					}
				}
				
				Boolean isPortrait = null;
				if(json.has("isPortrait")) {
					isPortrait = json.getBoolean("isPortrait");
					log.debug("json isPortrait = " + isPortrait);
				}

				Boolean firstPhotoAdded = false;
				String photoBase64 = null;
				String thumbNailBase64 = null;
				if(json.has("photo")) {
					if(isPortrait == null) {
						jsonReturn.put("apiStatus", ApiStatusCode.IS_PORTRAIT_AND_PHOTO_MUST_BE_SPECIFIED_TOGETHER);
						return new JsonRepresentation(jsonReturn);
					}
					
					photoBase64 = json.getString("photo");
					String oldPhoto = user.getPhotoBase64();
					if(oldPhoto == null) {
						firstPhotoAdded = true;
					}
					try {
						// decode the base64 encoding to create the thumb nail
						byte[] rawPhoto = Base64.decodeBase64(photoBase64);
						ImagesService imagesService = ImagesServiceFactory.getImagesService();
						Image oldImage = ImagesServiceFactory.makeImage(rawPhoto);
						
						int tnWidth = isPortrait == true ? User.THUMB_NAIL_SHORT_SIDE : User.THUMB_NAIL_LONG_SIDE;
						int tnHeight = isPortrait == true ? User.THUMB_NAIL_LONG_SIDE : User.THUMB_NAIL_SHORT_SIDE;
						Transform resize = ImagesServiceFactory.makeResize(tnWidth, tnHeight);
						Image newImage = imagesService.applyTransform(resize, oldImage);
						thumbNailBase64 = Base64.encodeBase64String(newImage.getImageData());
						
						user.setThumbNailBase64(thumbNailBase64);
						user.setPhotoBase64(photoBase64);
						log.debug("user photo and thumb nail skeleton were successfully persisted");
					} catch(Exception e) {
						jsonReturn.put("apiStatus", ApiStatusCode.INVALID_PHOTO_PARAMETER);
						return new JsonRepresentation(jsonReturn);
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
				
				if(json.has("clientDevice")) {
					user.setClientDevice(json.getString("clientDevice"));
				}
				
				if(json.has("c2dmRegistrationId")) {
					user.setC2dmRegistrationId(json.getString("c2dmRegistrationId"));
				}

				em.getTransaction().commit();
				
				if(randomPassword != null) {
					Emailer.sendPasswordResetEmail(user, randomPassword);
					log.debug("password reset email being sent");
				}
				
				if(isEmailUpdated) {
					PubHub.sendUserEmailUpdateMessage(user);
					log.debug("email confirmation being sent");
				}
				
				// If the archive day count changed, the following work must be done:
				// - must update the ActiveThruGmtDate in all active messageThreads (inbox) for this user
				// - must update the ActiveThruGmtDate in all active recipients (outbox) for this user
				// - all the user's memberships must be updated with new count since memberships hold a copy of the count for efficient sends
				if(!autoArchiveDayCountAdjustment.equals(0)) {
					MessageThread.upateUserActiveThruGmtDate(user, autoArchiveDayCountAdjustment);
					Recipient.upateUserActiveThruGmtDate(user, autoArchiveDayCountAdjustment);
					Member.updateMemberShipsWithNewAutoArchiveDayCount(user);
				}
				
				// ::BUSINESS_RULE:: The photo and thumb nail will be propagated to this user's memberships that have no
				//                   photo associated with them.
				if(firstPhotoAdded) {
					Member.updateMemberShipsWithNewPhoto(user, photoBase64, thumbNailBase64);
				}
				
			    // Send the 'phone number confirmation' SMS if appropriate
			    if( sendConfirmation != null && sendConfirmation &&
			    	user.getSmsEmailAddress() != null && user.getSmsEmailAddress().length() > 0) {
			    	PubHub.sendPhoneNumberConfirmation(user.getSmsEmailAddress(), user.getPhoneNumberConfirmationCode());
			    } else if(justSmsConfirmed) {
			    	User.synchUpWithSmsConfirmedMemberships(user);
			    }
			}
		} catch (IOException e) {
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
			log.exception("UserResource:udateUser:IOException", "error extracting JSON object from Post", e);
		} catch (JSONException e) {
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
			log.exception("UserResource:udateUser:JSONException", "error converting json representation into a JSON object", e);
		} catch (NoResultException e) {
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
			log.exception("UserResource:udateUser:NoResultException", "user not found", e);
			e.printStackTrace();
		} finally {
		    if (em.getTransaction().isActive()) {
		        em.getTransaction().rollback();
		    }
		    em.close();
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("UserResource:udateUser:JSONException2", "error converting json representation into a JSON object", e);
		}
		return new JsonRepresentation(jsonReturn);
    }
}