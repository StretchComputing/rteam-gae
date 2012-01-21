package com.stretchcom.rteam.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.OneToMany;
import javax.persistence.CascadeType;
import javax.persistence.Transient;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

@Entity
@NamedQueries({
    @NamedQuery(
    		name="User.getAll",
    		query="SELECT u FROM User u"
    ),
    @NamedQuery(
    		name="User.getByEmailAddress",
    		query="SELECT u FROM User u WHERE u.emailAddress = :emailAddress"
    ),
    @NamedQuery(
    		name="User.getByEmailAddressAndPasswordAndIsNetworkAuthenticated",
    		query="SELECT u FROM User u WHERE u.emailAddress = :emailAddress" + " AND " +
		          "u.password = :password" + " AND " +
		          "u.isNetworkAuthenticated = :isNetworkAuthenticated"
    ),
    @NamedQuery(
    		name="User.getByEmailAddressAndIsNetworkAuthenticated",
    		query="SELECT u FROM User u WHERE u.emailAddress = :emailAddress" + " AND " +
    		      "u.isNetworkAuthenticated = :isNetworkAuthenticated"
    ),
    @NamedQuery(
    		name="User.getByPhoneNumberAndIsSmsConfirmed",
    		query="SELECT u FROM User u WHERE u.phoneNumber = :phoneNumber" + " AND " +
    		      "u.isSmsConfirmed = :isSmsConfirmed"
    ),
    @NamedQuery(
    		name="User.getByToken",
    		query="SELECT u FROM User u WHERE u.token = :token"
    ),
    @NamedQuery(
    		name="User.getByKey",
    		query="SELECT u FROM User u WHERE u.key = :key"
    ),
    @NamedQuery(
    		name="User.getAllNonSuperUsers",
    		query="SELECT u FROM User u where u.isSuperUser = :isSuperUser"
    ),
})
public class User {
	//private static final Logger log = Logger.getLogger(User.class.getName());
	private static RskyboxClient log = new RskyboxClient();
	
	public static final Integer AUTO_ARCHIVE_DAY_COUNT_DEFAULT = 30;
	public static final Integer THUMB_NAIL_SHORT_SIDE  = 60;
	public static final Integer THUMB_NAIL_LONG_SIDE  = 80;
	
	public static final String ANDROID_DEVICE = "android";
	public static final String IOS_DEVICE = "ios";
	
	private String firstName;
	private String lastName;
	private String emailAddress;
	private String password;
	private String passwordResetQuestion;
	private String passwordResetAnswer;
	private String token;
	private String networkAuthenticationToken;
	private Boolean isNetworkAuthenticated = false;
	private Boolean isSuperUser = false;
	private String alertToken;
	private String phoneNumber;
	private String mobileCarrierCode;
	private String phoneNumberConfirmationCode;
	private String clientDevice;
	private String c2dmRegistrationId; // Android cloud to device messaging (C2DM)
	private Boolean isAggregator;	// aggregator is a user with special permission to access public APIs
	private Boolean inactivatedDueToDuplicateEmail;  // TODO create a CRON job to delete user entities where this is TRUE
	private String userIconOneId;
	private String userIconOneAlias;
	private String userIconOneImage;
	private String userIconTwoId;
	private String userIconTwoAlias;
	private String userIconTwoImage;
	private Boolean shouldSendMessageOption; // should send email/facebook message in addition to inbox message
	private Integer newMessageCount;
	private Integer autoArchiveDayCount;  // number of days after which the user's inbox and outbox messages are auto archived
	private Text photoBase64;
	private Text thumbNailBase64;
	private Double latitude; // local copy of team location data maintained in Location entity.
	private Double longitude; // local copy of team location data maintained in Location entity.
	private String location; // location user registered from
	private Boolean isSmsConfirmed = false;
	private String smsEmailAddress;
	private Boolean hasRteamMessageAccessEnabled = false;
	private Boolean hasEmailMessageAccessEnabled = false;
	private Boolean hasSmsMessageAccessEnabled = false;
	private Boolean hasCcToSelfEnabled = false;
	
	@Transient
	private String memberBoundEmailAddress;

	@Transient
	private String memberBoundPhoneNumber;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Key key;
    
    // User <=> Team relationship is managed as a 'soft schema'
	@Basic
    private List<Key> teams = new ArrayList<Key>();
    
    public List<Key> getTeams() {
		return teams;
	}

	public void setTeams(List<Key> teams) {
		this.teams = teams;
	}

	// User holds their most recent activity cacheID for each team -- used to determine if a user has New Activity
	// If Activity/Twitter is not used for a team, the cacheId will be null.
	@Basic
    private List<Long> teamNewestCacheIds = new ArrayList<Long>();
    
    public List<Long> getTeamNewestCacheIds() {
		// PRIORITY TODO remove patch
		//::PATCH:: for Beta, newestCacheIds are being added after teams already added, so make sure lists are the same size
		//resizeNewestCacheIds();
		
		return teamNewestCacheIds;
	}

	public void setTeamNewestCacheIds(List<Long> teamNewestCacheIds) {
		this.teamNewestCacheIds = teamNewestCacheIds;
	}

	public User() {
    	
    }
    
    // used in client-side tests
    public User(String firstName, String lastName, String emailAddress, String role, String token) {
    	this.firstName = firstName;
    	this.lastName = lastName;
    	this.emailAddress = emailAddress;
    	this.token = token;
    }

    public Key getKey() {
        return key;
    }

    public String getFirstName() {
		return firstName;
	}
    
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	
	public String getLastName() {
		return lastName;
	}
	
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	public String getFullName() {
		// return unabbreviated name
		return getDisplayName(false);
	}

	public String getEmailAddress() {
		return emailAddress;
	}
	
	public void setEmailAddress(String theEmailAddress) {
		// always store email in lower case to make queries and comparisons case insensitive
		if(theEmailAddress != null) {theEmailAddress = theEmailAddress.toLowerCase();}
		this.emailAddress = theEmailAddress;
	}
	
    // by default, returns abbreviated last name
	public String getDisplayName() {
    	return getDisplayName(this.getFirstName(), this.getLastName(), true);
    }
	
	public String getDisplayName(Boolean theAbbreviate) {
    	return getDisplayName(this.getFirstName(), this.getLastName(), theAbbreviate);
    }
   
    public static String getDisplayName(String theFirstName, String theLastName, Boolean theAbbreviate) {
		String displayName = "";
		if(theFirstName != null && theFirstName.trim().length() > 0 && theLastName != null && theLastName.trim().length() > 0) {
			if(theAbbreviate) {
				// the only thing different for abbreviation is only the last initial is used if both first and last names present
				displayName = theFirstName + " " + theLastName.charAt(0);
			} else {
				displayName = theFirstName + " " + theLastName;
			}
		} else if(theFirstName != null && theFirstName.trim().length() > 0) {
			displayName = theFirstName;
		} else if(theLastName != null && theLastName.trim().length() > 0) {
			displayName = theLastName;
		}
		return displayName;
    }

    public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}

    public Boolean getIsNetworkAuthenticated() {
		return isNetworkAuthenticated;
	}

	public void setIsNetworkAuthenticated(Boolean isNetworkAuthenticated) {
		this.isNetworkAuthenticated = isNetworkAuthenticated;
	}
	
	public String getNetworkAuthenticationToken() {
		return networkAuthenticationToken;
	}

	public void setNetworkAuthenticationToken(String networkAuthenticationToken) {
		this.networkAuthenticationToken = networkAuthenticationToken;
	}

	public Boolean getIsSuperUser() {
		return isSuperUser;
	}

	public void setIsSuperUser(Boolean isSuperUser) {
		this.isSuperUser = isSuperUser;
	}

	public String getAlertToken() {
		return alertToken;
	}

	public void setAlertToken(String alertToken) {
		this.alertToken = alertToken;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getC2dmRegistrationId() {
		return c2dmRegistrationId;
	}

	public void setC2dmRegistrationId(String c2dmRegistrationId) {
		this.c2dmRegistrationId = c2dmRegistrationId;
	}

	public Boolean getIsAggregator() {
		return isAggregator;
	}

	public void setIsAggregator(Boolean isAggregator) {
		this.isAggregator = isAggregator;
	}

    public boolean isUserMemberOfTeam(String theTeamId) {
    	if(this.teams == null) {
    		return false;
    	}
		Key teamKey = KeyFactory.stringToKey(theTeamId);
		for(Key k : this.teams) {
			if(k.equals(teamKey)) {
				return true;
			}
		}
		return false;
    }
    
	public String getPasswordResetAnswer() {
		return passwordResetAnswer;
	}

	public void setPasswordResetAnswer(String passwordResetAnswer) {
		this.passwordResetAnswer = passwordResetAnswer;
	}

	public String getPasswordResetQuestion() {
		return passwordResetQuestion;
	}

	public void setPasswordResetQuestion(String passwordResetQuestion) {
		this.passwordResetQuestion = passwordResetQuestion;
	}

	public Boolean getInactivatedDueToDuplicateEmail() {
		return inactivatedDueToDuplicateEmail;
	}

	public void setInactivatedDueToDuplicateEmail(
			Boolean inactivatedDueToDuplicateEmail) {
		this.inactivatedDueToDuplicateEmail = inactivatedDueToDuplicateEmail;
	}

	// ------------------------------------------------------------------------------------------------
	// TEAMS MUST ONLY BE ADDED TO A USER WITH THIS METHOD OR teamNewestCacheIds WILL GET OUT OF SYNCH
	// ------------------------------------------------------------------------------------------------
	// Method ensures that the Lists 'teams' and 'teamNewestCacheId' are kept in lock step. Every team has
	// has an associated newestCacheId even if it is null
	public void addTeam(Team theTeam) {
		if(this.teams == null) {
			this.teams = new ArrayList<Key>();
		}
		if(this.teamNewestCacheIds == null) {
			this.teamNewestCacheIds = new ArrayList<Long>();
		}
		
		// PRIORITY TODO remove patch
		//::PATCH:: for Beta, newestCacheIds are being added after teams already added, so make sure lists are the same size
		//resizeNewestCacheIds();
		
		// only add the team if it is not already in the list
		Key teamKey = theTeam.getKey();
		if(!this.teams.contains(teamKey)) {
			this.teams.add(teamKey);
			// add the team's newestCacheId which will be 0L if team not using Activity/Twitter
			this.teamNewestCacheIds.add(theTeam.getNewestCacheId());
		}
		
		// PRIORITY TODO: remove this debug code
		// verify the key was added to the end of the list
		log.debug("addTeam(): size of Team key list = " + this.teams.size());
		int cnt = 0;
		for(Key tk : this.teams) {
			if(tk.equals(teamKey)) {
				log.debug("addTeam(): index of new team key in list = " + cnt);
				break;
			}
			cnt++;
		}
	}
	
	// ------------------------------------------------------------------------------------------------
	// TEAMS MUST ONLY BE DELETED FROM A USER WITH THIS METHOD OR teamNewestCacheIds WILL GET OUT OF SYNCH
	// ------------------------------------------------------------------------------------------------
	public void removeTeam(Key theTeamKey) {
		if(this.teams == null || !this.teams.contains(theTeamKey)) {
			return;
		}
		
		int index = 0;
		for(Key tk : this.teams) {
			if(tk.equals(theTeamKey)) {
				break;
			}
			index++;
		}
		
		// TODO Remove this robustness enhancer
		Boolean entityIsOutOfSynch = false;
		if(this.teams.size() != this.teamNewestCacheIds.size()) {
			entityIsOutOfSynch = true;
		}
		
		// teams list and teamNewestCahceIds list MUST be kept in lock step
		this.teams.remove(index);
		
		// TODO Remove this robustness enhancer
		if(entityIsOutOfSynch) {
			log.error("User:removeTeam:teamCount", "number of user teams not equal to number of newest Cache Ids. Cache IDs all being reset to 0L.");
			// rebuild the cache ID list setting them all back to zero 
			this.teamNewestCacheIds.clear();
			for(int i=0; i<this.teams.size(); i++) {
				this.teamNewestCacheIds.add(0L);
			}
		} else {
			this.teamNewestCacheIds.remove(index);
		}
		
		return;
	}
	
	//::PATCH::
	private void resizeNewestCacheIds() {
		if(this.teamNewestCacheIds.size() < this.teams.size()) {
			int sizeDifference = this.teams.size() - this.teamNewestCacheIds.size();
			for(int i=0; i<sizeDifference; i++) {
				this.teamNewestCacheIds.add(0L);
			}
		}
	}
	
	public void resetNewestCacheId(String theTeamId) {
		if(this.teams.size() != this.teamNewestCacheIds.size()) {
			log.error("User:resetNewestCacheId:adjustTeamSize", "team size = " + this.teams.size() + " newestCacheId size = " + this.teamNewestCacheIds.size());
			return;
		}
		
		Key specifiedKey = null;
		try {
			specifiedKey = KeyFactory.stringToKey(theTeamId);
		} catch (Exception e) {
			log.exception("User:resetNewestCacheId:Exception", "", e);
			return;
		}
		
		int index = 0;
		for(Key tk : this.teams) {
			if(tk.equals(specifiedKey)) {
				break;
			}
			index++;
		}
		this.teamNewestCacheIds.set(index, 0L);
	}
    
    public void decrementAndPersistNewMessageCount() {
		User user = null;
    	EntityManager em = EMF.get().createEntityManager();
		try {
			user = (User)em.createNamedQuery("User.getByKey")
				.setParameter("key", this.key)
				.getSingleResult();
			user.decrementNewMessageCount();
		} catch (NoResultException e) {
			log.exception("User:decrementAndPersistNewMessageCount:NoResultException", "user not found using key", e);
		} catch (NonUniqueResultException e) {
			log.exception("User:decrementAndPersistNewMessageCount:NoResultException", "two users have the same key", e);
		} finally {
			em.close();
		}
		return;
    }
    
    public void setAndPersistNewMessageCount(Integer theNewMessageCount) {
    	this.newMessageCount = theNewMessageCount;
		User user = null;
    	EntityManager em = EMF.get().createEntityManager();
		try {
			user = (User)em.createNamedQuery("User.getByKey")
				.setParameter("key", this.key)
				.getSingleResult();
			user.setNewMessageCount(theNewMessageCount);
		} catch (NoResultException e) {
			log.exception("User:setAndPersistNewMessageCount:NoResultException", "user not found using key", e);
		} catch (NonUniqueResultException e) {
			log.exception("User:setAndPersistNewMessageCount:NoResultException", "two users have the same key", e);
		} finally {
			em.close();
		}
		return;
    }
    

	public String getUserIconTwoAlias() {
		return userIconTwoAlias;
	}

	public void setUserIconTwoAlias(String userIconTwoAlias) {
		this.userIconTwoAlias = userIconTwoAlias;
	}

	public String getUserIconTwoId() {
		return userIconTwoId;
	}

	public void setUserIconTwoId(String userIconTwoId) {
		this.userIconTwoId = userIconTwoId;
	}

	public String getUserIconTwoImage() {
		return userIconTwoImage;
	}

	public void setUserIconTwoImage(String userIconTwoImage) {
		this.userIconTwoImage = userIconTwoImage;
	}

	public String getUserIconOneAlias() {
		return userIconOneAlias;
	}

	public void setUserIconOneAlias(String userIconOneAlias) {
		this.userIconOneAlias = userIconOneAlias;
	}

	public String getUserIconOneId() {
		return userIconOneId;
	}

	public void setUserIconOneId(String userIconOneId) {
		this.userIconOneId = userIconOneId;
	}
	
	public String getUserIconOneImage() {
		return userIconOneImage;
	}

	public void setUserIconOneImage(String userIconOneImage) {
		this.userIconOneImage = userIconOneImage;
	}

	public Integer incrementNewMessageCount() {
		if(this.newMessageCount == null) {
			this.newMessageCount = new Integer(0);
		}
		this.newMessageCount++;
		return this.newMessageCount;
	}
	
	public Integer decrementNewMessageCount() {
		if(this.newMessageCount == null) {
			this.newMessageCount = new Integer(0);
			return this.newMessageCount;
		} else if(this.newMessageCount == 0) {
			return 0;
		}
		this.newMessageCount--;
		return this.newMessageCount;
	}
	
	public void setNewMessageCount(Integer theNewCount) {
		this.newMessageCount = theNewCount;
	}
	
	public Integer getNewMessageCount() {
		return this.newMessageCount;
	}

	public Boolean getShouldSendMessageOption() {
		return shouldSendMessageOption;
	}

	public void setShouldSendMessageOption(Boolean shouldSendMessageOption) {
		this.shouldSendMessageOption = shouldSendMessageOption;
	}

	public Integer getAutoArchiveDayCount() {
		return autoArchiveDayCount;
	}

	public void setAutoArchiveDayCount(Integer autoArchiveDayCount) {
		this.autoArchiveDayCount = autoArchiveDayCount;
	}
	
	public String getThumbNailBase64() {
		return this.thumbNailBase64 == null? null : this.thumbNailBase64.getValue();
	}

	public void setThumbNailBase64(byte[] thumbNailBase64) {
		this.thumbNailBase64 = new Text(new String(thumbNailBase64));
	}

	public void setThumbNailBase64(String thumbNailBase64) {
		this.thumbNailBase64 = new Text(thumbNailBase64);
	}

	public String getPhotoBase64() {
		return this.photoBase64 == null? null : this.photoBase64.getValue();
	}

	public void setPhotoBase64(String photo) {
		this.photoBase64 = new Text(photo);
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}


	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Boolean getIsSmsConfirmed() {
		if(this.isSmsConfirmed == null) {return false;}
		return this.isSmsConfirmed;
	}

	public void setIsSmsConfirmed(Boolean isSmsConfirmed) {
		this.isSmsConfirmed = isSmsConfirmed;
	}

	public String getMobileCarrierCode() {
		return mobileCarrierCode;
	}

	public void setMobileCarrierCode(String mobileCarrierCode) {
		this.mobileCarrierCode = mobileCarrierCode;
	}
	
	public String getPhoneNumberConfirmationCode() {
		return phoneNumberConfirmationCode;
	}

	public void setPhoneNumberConfirmationCode(String phoneNumberConfirmationCode) {
		this.phoneNumberConfirmationCode = phoneNumberConfirmationCode;
	}

	public String getClientDevice() {
		return clientDevice;
	}

	public void setClientDevice(String clientDevice) {
		this.clientDevice = clientDevice;
	}

	public String getSmsEmailAddress() {
		return smsEmailAddress;
	}

	public void setSmsEmailAddress(String smsEmailAddress) {
		this.smsEmailAddress = smsEmailAddress;
	}

	public Boolean getHasRteamMessageAccessEnabled() {
		return hasRteamMessageAccessEnabled;
	}

	public void setHasRteamMessageAccessEnabled(Boolean hasRteamMessageAccessEnabled) {
		this.hasRteamMessageAccessEnabled = hasRteamMessageAccessEnabled;
	}

	public Boolean getHasEmailMessageAccessEnabled() {
		return hasEmailMessageAccessEnabled;
	}

	public void setHasEmailMessageAccessEnabled(Boolean hasEmailMessageAccessEnabled) {
		this.hasEmailMessageAccessEnabled = hasEmailMessageAccessEnabled;
	}

	public Boolean getHasSmsMessageAccessEnabled() {
		return hasSmsMessageAccessEnabled;
	}

	public void setHasSmsMessageAccessEnabled(Boolean hasSmsMessageAccessEnabled) {
		this.hasSmsMessageAccessEnabled = hasSmsMessageAccessEnabled;
	}
	
	public Boolean getHasCcToSelfEnabled() {
		return hasCcToSelfEnabled;
	}

	public void setHasCcToSelfEnabled(Boolean hasCcToSelfEnabled) {
		this.hasCcToSelfEnabled = hasCcToSelfEnabled;
	}
	
	public String getMemberBoundEmailAddress() {
		return memberBoundEmailAddress;
	}

	public void setMemberBoundEmailAddress(String memberBoundEmailAddress) {
		this.memberBoundEmailAddress = memberBoundEmailAddress;
	}

	public String getMemberBoundPhoneNumber() {
		return memberBoundPhoneNumber;
	}

	public void setMemberBoundPhoneNumber(String memberBoundPhoneNumber) {
		this.memberBoundPhoneNumber = memberBoundPhoneNumber;
	}

	
	////////////////////
	// STATIC METHODS //
	////////////////////
    
    @SuppressWarnings("unchecked")
	public static User getUserWithEmailAddress(String theEmailAddress) {
    	if(theEmailAddress == null) {return null;}
    	
    	EntityManager em = EMF.get().createEntityManager();
    	
    	try {
        	//::BUSINESS_RULE:: authenticated user email addresses must be unique
    		List<User> users = (List<User>)em.createNamedQuery("User.getByEmailAddressAndIsNetworkAuthenticated")
				.setParameter("emailAddress", theEmailAddress.toLowerCase())
				.setParameter("isNetworkAuthenticated", true)
				.getResultList();
    	
	    	if(users != null && users.size() > 0) {
	    		return users.get(0);
	    	}
    	} catch (Exception e) {
    		log.exception("User:getUserWithEmailAddress:Exception", "Query User.getByEmailAddressAndIsNetworkAuthenticated failed", e);
    	} finally {
    		em.close();
    	}
    	return null;
    }
    
    public static User getUserFromUserKey(Key theUserKey) {
		User user = null;
    	EntityManager em = EMF.get().createEntityManager();
		try {
			user = (User)em.createNamedQuery("User.getByKey")
				.setParameter("key", theUserKey)
				.getSingleResult();
		} catch (NoResultException e) {
			log.debug("user not found using key");
		} catch (NonUniqueResultException e) {
			log.exception("User:getUserWithEmailAddress:Exception", "two users have the same key", e);
		} finally {
			em.close();
		}
		return user;
    }
    
    // ::MEMBER::USER::
    // User is now network authenticated.  Using the email address, 'synch up' all network authenticated
    // memberships -- if any -- by placing the user ID inside the member entities. Also, all active messages
    // should be assigned to this user so user can view messages via rTeam application.
    public static void synchUpWithAuthorizedMemberships(User theUser) {
    	// update the memberships of this user
    	EntityManager em = EMF.get().createEntityManager();
    	List<Member> memberships = null;
    	
		// build the team list -- which will be added to the user entity below
		List<Key> teams = new ArrayList<Key>();
		List<Long> teamNewestCacheIds = new ArrayList<Long>();
		
		/////////////////////////////////////////////////////////////////////////////////////
    	// associated memberships with user by setting the appropriate user ID in the member
		/////////////////////////////////////////////////////////////////////////////////////
		try {
			log.debug("user email address = " + theUser.getEmailAddress());
			memberships = (List<Member>)em.createNamedQuery("Member.getByNetworkAuthenticatedEmailAddress")
				.setParameter("emailAddress", theUser.getEmailAddress())
				.getResultList();
			log.debug("# of memberships found = " + memberships.size());
			
			// TODO - get "illegal argument" if I try to update multiple members in the same transaction
			for(Member m : memberships) {
    	    	em.getTransaction().begin();
        		Member singleMember = (Member)em.createNamedQuery("Member.getByKey")
        			.setParameter("key", m.getKey())
        			.getSingleResult();
        		
				// Setting the user ID in member indicates that is is 'synched up' with the user. The member can
				// hold user IDs for each of its email addresses, that why we set the user ID using the email address.
        		singleMember.setUserIdByEmailAddress(KeyFactory.keyToString(theUser.getKey()), theUser.getEmailAddress());
        		singleMember.setAutoArchiveDayCountByEmailAddress(theUser.getAutoArchiveDayCount(), theUser.getEmailAddress());
        		
				// a person can be a guardian for multiple members, but we want a list of teams without duplicates
				if(!teams.contains(singleMember.getTeam().getKey())) {
					teams.add(singleMember.getTeam().getKey());
					// add the team's newestCacheId which will be 0L if team not using Activity/Twitter
					teamNewestCacheIds.add(singleMember.getTeam().getNewestCacheId());
				}
    			em.getTransaction().commit();
			}
    	} catch (Exception e) {
    		log.exception("User:synchUpWithAuthorizedMemberships:Exception1", "", e);
    	} finally {
		    if (em.getTransaction().isActive()) {
		    	em.getTransaction().rollback();
		    }
		    em.close();
		}
		
		/////////////////////////////////////////////////////////////////////////////////////
    	// Network Authenticate any memberships this user has that were created as part of
    	// creating new teams when the user was not yet network authenticated.
		/////////////////////////////////////////////////////////////////////////////////////
    	List<Member> creatorMemberships = null;
    	EntityManager em4 = EMF.get().createEntityManager();
    	try {
			creatorMemberships = (List<Member>)em4.createNamedQuery("Member.getByEmailAddressAndParticipantRole")
				.setParameter("emailAddress", theUser.getEmailAddress())
				.setParameter("participantRole", Member.CREATOR_PARTICIPANT)
				.getResultList();
			log.debug("# of creator memberships found = " + creatorMemberships.size());
			
			// TODO - get "illegal argument" if I try to update multiple members in the same transaction
			for(Member m : creatorMemberships) {
    	    	em4.getTransaction().begin();
        		Member singleMember = (Member)em4.createNamedQuery("Member.getByKey")
        			.setParameter("key", m.getKey())
        			.getSingleResult();
        		
        		singleMember.networkAuthenticateEmailAddress(theUser.getEmailAddress());
        		
				// Setting the user ID in member indicates that is is 'synched up' with the user. The member can
				// hold user IDs for each of its email addresses, that why we set the user ID using the email address.
        		singleMember.setUserIdByEmailAddress(KeyFactory.keyToString(theUser.getKey()), theUser.getEmailAddress());
        		singleMember.setAutoArchiveDayCountByEmailAddress(theUser.getAutoArchiveDayCount(), theUser.getEmailAddress());
        		
    			em4.getTransaction().commit();
			}
    	} catch (Exception e) {
    		log.exception("User:synchUpWithAuthorizedMemberships:Exception2", "", e);
    	} finally {
		    if (em4.getTransaction().isActive()) {
		    	em4.getTransaction().rollback();
		    }
		    em4.close();
		}
    	
    	// update the recipient entities that are associated with network authenticated memberships of this user
    	// Need to do this in a separate transaction
    	//::TODO if there were a very large number of recipients for this user, could max job duration be exceeded?
    	String sniffedFirstName = "";
    	String sniffedLastName = "";
    	if(memberships != null && memberships.size() > 0) {
        	EntityManager em2 = EMF.get().createEntityManager();
        	try {
    			for(Member m : memberships) {
    				if(m.hasAnyNetworkAuthenticatedEmailAddresses()) {
    					List<Recipient> recipients = (List<Recipient>)em2.createNamedQuery("Recipient.getByMemberIdAndEmailAddress")
							.setParameter("memberId", KeyFactory.keyToString(m.getKey()))
							.setParameter("emailAddress", m.getEmailAddress())
							.getResultList();
    					log.debug("# of recipients found = " + recipients.size());
    					
    					// TODO - get "illegal argument" if I try to update multiple recipients in the same transaction
    					for(Recipient r : recipients) {
    	        	    	em2.getTransaction().begin();
    	            		Recipient singleRecipient = (Recipient)em2.createNamedQuery("Recipient.getByKey")
    		        			.setParameter("key", r.getKey())
    		        			.getSingleResult();
    	            		
    	            		singleRecipient.setUserId(KeyFactory.keyToString(theUser.getKey()));
    	        			em2.getTransaction().commit();
    					}
    					
    					// User optionally provides first and last name during registration. Below we may set it
    					// the user names from member info collected here.
    					if(sniffedFirstName.length() == 0) {
    						sniffedFirstName = m.getFirstName();
    					}
    					if(sniffedLastName.length() == 0) {
    						sniffedLastName = m.getLastName();
    					}
    				}
    			}
        	} catch (Exception e) {
        		log.exception("User:synchUpWithAuthorizedMemberships:Exception3", "", e);
        	} finally {
    		    if (em2.getTransaction().isActive()) {
    		    	em2.getTransaction().rollback();
    		    }
    		    em2.close();
    		}
    	}
    	
    	/////////////////////////////////////////////////////////////////////////////////////////
    	// update the user with the team list and extracted first and last names collected above
    	/////////////////////////////////////////////////////////////////////////////////////////
    	if(teams.size() > 0) {
        	EntityManager em3 = EMF.get().createEntityManager();
        	try {
        		em3.getTransaction().begin();
				User withinTransactionUser = (User)em3.createNamedQuery("User.getByKey")
					.setParameter("key", theUser.getKey())
					.getSingleResult();
				
				// user may already have existing teams from SMS memberships and from creating their own teams before becoming NA
				List<Key> allTeams = withinTransactionUser.getTeams() == null ? new ArrayList<Key>() : withinTransactionUser.getTeams();
				List<Long> allTeamNewestCacheIds = withinTransactionUser.getTeamNewestCacheIds() == null ? new ArrayList<Long>() : withinTransactionUser.getTeamNewestCacheIds();
				appendTeams(allTeams, teams, allTeamNewestCacheIds, teamNewestCacheIds);
				withinTransactionUser.setTeams(allTeams);
				withinTransactionUser.setTeamNewestCacheIds(allTeamNewestCacheIds);
				
				if(withinTransactionUser.getFirstName() == null || withinTransactionUser.getFirstName().length() == 0) {
					if(sniffedFirstName.length() > 0) {
						withinTransactionUser.setFirstName(sniffedFirstName);
					}
				}
				if(withinTransactionUser.getLastName() == null || withinTransactionUser.getLastName().length() == 0) {
					if(sniffedLastName.length() > 0) {
						withinTransactionUser.setLastName(sniffedLastName);
					}
				}
					
				em3.getTransaction().commit();
        	} catch (NoResultException e) {
        		log.exception("User:synchUpWithAuthorizedMemberships:NoResultException", "user not found", e);
    		} catch (NonUniqueResultException e) {
    			log.exception("User:synchUpWithAuthorizedMemberships:NonUniqueResultException", "two or more users have same key", e);
    		} finally {
    		    if (em3.getTransaction().isActive()) {
    		    	em3.getTransaction().rollback();
    		    }
    		    em3.close();
    		}
    	} // end of if
    }
    
    // ::MEMBER::USER::
    // User is now SMS confirmed.  Using the phone number, 'synch up' all SMS confirmed 
    // memberships -- if any -- by placing the user ID inside the member entities.
    public static void synchUpWithSmsConfirmedMemberships(User theUser) {
    	// update the memberships of this user
    	EntityManager em = EMF.get().createEntityManager();
    	List<Member> memberships = null;
    	
		// build the team list -- which will be added to the user entity below
		List<Key> teams = new ArrayList<Key>();
		List<Long> teamNewestCacheIds = new ArrayList<Long>();
		
		/////////////////////////////////////////////////////////////////////////////////////
    	// associated memberships with user by setting the appropriate user ID in the member
		/////////////////////////////////////////////////////////////////////////////////////
		try {
			log.debug("user phone number = " + theUser.getPhoneNumber());
			memberships = (List<Member>)em.createNamedQuery("Member.getBySmsConfirmedPhoneNumber")
				.setParameter("phoneNumber", theUser.getPhoneNumber())
				.getResultList();
			log.debug("# of memberships found = " + memberships.size());
			
			// TODO - get "illegal argument" if I try to update multiple members in the same transaction
			for(Member m : memberships) {
    	    	em.getTransaction().begin();
        		Member singleMember = (Member)em.createNamedQuery("Member.getByKey")
        			.setParameter("key", m.getKey())
        			.getSingleResult();
        		
				// Setting the user ID in member indicates that is is 'synched up' with the user. The member can
				// hold user IDs for each of its participants, that why we set the user ID using the phone number.
        		singleMember.setUserIdByPhoneNumber(KeyFactory.keyToString(theUser.getKey()), theUser.getPhoneNumber());
        		singleMember.setAutoArchiveDayCountByPhoneNumber(theUser.getAutoArchiveDayCount(), theUser.getPhoneNumber());
        		
				// a person can be a guardian for multiple members, but we want a list of teams without duplicates
				if(!teams.contains(singleMember.getTeam().getKey())) {
					teams.add(singleMember.getTeam().getKey());
					// add the team's newestCacheId which will be 0L if team not using Activity/Twitter
					teamNewestCacheIds.add(singleMember.getTeam().getNewestCacheId());
				}
    			em.getTransaction().commit();
			}
    	} catch (Exception e) {
    		log.exception("User:synchUpWithSmsConfirmedMemberships:Exception", "", e);
    	} finally {
		    if (em.getTransaction().isActive()) {
		    	em.getTransaction().rollback();
		    }
		    em.close();
		}
    	
    	/////////////////////////////////////
    	// update the user with the team list 
    	/////////////////////////////////////
    	if(teams.size() > 0) {
        	EntityManager em3 = EMF.get().createEntityManager();
        	try {
        		em3.getTransaction().begin();
				User withinTransactionUser = (User)em3.createNamedQuery("User.getByKey")
					.setParameter("key", theUser.getKey())
					.getSingleResult();
				
				// user may already have existing teams from SMS memberships and from creating their own teams before becoming NA
				List<Key> allTeams = withinTransactionUser.getTeams() == null ? new ArrayList<Key>() : withinTransactionUser.getTeams();
				List<Long> allTeamNewestCacheIds = withinTransactionUser.getTeamNewestCacheIds() == null ? new ArrayList<Long>() : withinTransactionUser.getTeamNewestCacheIds();
				appendTeams(allTeams, teams, allTeamNewestCacheIds, teamNewestCacheIds);
				withinTransactionUser.setTeams(allTeams);
				withinTransactionUser.setTeamNewestCacheIds(allTeamNewestCacheIds);
				em3.getTransaction().commit();
        	} catch (NoResultException e) {
            	log.exception("User:synchUpWithSmsConfirmedMemberships:NoResultException", "user not found", e);
     		} catch (NonUniqueResultException e) {
    			log.exception("User:synchUpWithSmsConfirmedMemberships:NoResultException", "two or more users have same key", e);
    		} finally {
    		    if (em3.getTransaction().isActive()) {
    		    	em3.getTransaction().rollback();
    		    }
    		    em3.close();
    		}
    	} // end of if
    }
    
    // theAllTeams: in/out parameter. Contains existing user teams as in parameter - contains all teams as out parameter 
    // theNewTeams: new user teams 
    // theAllTeamNewestCacheIds: in/out parameter. Contains existing user cacheIds as in parameter - contains all cacheIds as out parameter
    // theNewTeamNewestCacheIds: new user cacheIds
    private static void appendTeams(List<Key> theAllTeams, List<Key> theNewTeams,
    							    List<Long> theAllTeamNewestCacheIds, List<Long> theNewTeamNewestCacheIds) {
    	for(int i=0; i<theNewTeams.size(); i++) {
    		if(!theAllTeams.contains(theNewTeams.get(i))) {
    			theAllTeams.add(theNewTeams.get(i));
    			theAllTeamNewestCacheIds.add(theNewTeamNewestCacheIds.get(i));
    		}
    	}
    	return;
    }
    
    public static void addTeam(User theUser, Team theTeam) {
    	EntityManager em = EMF.get().createEntityManager();
    	try {
    		em.getTransaction().begin();
			User user = (User)em.createNamedQuery("User.getByKey")
				.setParameter("key", theUser.getKey())
				.getSingleResult();
			user.addTeam(theTeam);
			em.getTransaction().commit();
    	} catch (NoResultException e) {
        	log.exception("User:addTeam:NoResultException", "user not found", e);
		} catch (NonUniqueResultException e) {
			log.exception("User:addTeam:NonUniqueResultException", "two or more users have same key", e);
		} finally {
		    if (em.getTransaction().isActive()) {
		    	em.getTransaction().rollback();
		    }
		    em.close();
		}
    }
    
    public static void addTeamAndSetBoundValues(User theUser, Team theTeam) {
    	EntityManager em = EMF.get().createEntityManager();
    	try {
    		em.getTransaction().begin();
			User user = (User)em.createNamedQuery("User.getByKey")
				.setParameter("key", theUser.getKey())
				.getSingleResult();
			user.addTeam(theTeam);
			if(theUser.getMemberBoundEmailAddress() != null) {
				user.setEmailAddress(theUser.getMemberBoundEmailAddress());
				log.debug("user email address set to member bound value = " + theUser.getMemberBoundEmailAddress());
			}
			if(theUser.getMemberBoundPhoneNumber() != null) {
				user.setPhoneNumber(theUser.getMemberBoundPhoneNumber());
				log.debug("user phone number set to member bound value = " + theUser.getMemberBoundPhoneNumber());
			}
			em.getTransaction().commit();
    	} catch (NoResultException e) {
        	log.exception("User:addTeamAndSetBoundValues:NoResultException", "user not found", e);
		} catch (NonUniqueResultException e) {
			log.exception("User:addTeamAndSetBoundValues:NonUniqueResultException", "should never happen - two or more users have same key", e);
		} finally {
		    if (em.getTransaction().isActive()) {
		    	em.getTransaction().rollback();
		    }
		    em.close();
		}
    }

}
