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
import javax.persistence.ManyToOne;
import javax.persistence.FetchType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.mortbay.log.Log;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

@Entity
@NamedQueries({
    @NamedQuery(
    		name="Member.getAll",
    		query="SELECT m FROM Member m"
    ),
    @NamedQuery(
    		name="Member.getByKeys",
    		query="SELECT m FROM Member m WHERE m.key = :keys"
    ),
    @NamedQuery(
    		name="Member.getByKey",
    		query="SELECT m FROM Member m WHERE m.key = :key"
    ),
    @NamedQuery(
    		name="Member.getByTeam",
    		query="SELECT m FROM Member m WHERE m.team = :team"
    ),
    @NamedQuery(
    		name="Member.getByEmailAddress",
    		query="SELECT m FROM Member m WHERE m.emailAddress = :emailAddress"
    ),
    @NamedQuery(
    		name="Member.getByNetworkAuthenticatedEmailAddress",
    		query="SELECT m FROM Member m WHERE m.networkAuthenticatedEmailAddresses = :emailAddress"
    ),
    @NamedQuery(
    		name="Member.getByNetworkAuthenticatedEmailAddressAndTeam",
    		query="SELECT m FROM Member m WHERE m.networkAuthenticatedEmailAddresses = :emailAddress AND m.team = :team"
    ),
    @NamedQuery(
    		name="Member.getByGuardianEmailAddress",
    		query="SELECT m FROM Member m WHERE m.guardianEmailAddresses = :guardianEmailAddress"
    ),
    @NamedQuery(
    		name="Member.getByNonNullGuardianEmailAddress",
    		query="SELECT m FROM Member m WHERE m.guardianEmailAddresses <> NULL"
    ),
    @NamedQuery(
    		name="Member.getByEmailAddressAndTeam",
    		query="SELECT m FROM Member m WHERE m.emailAddress = :emailAddress AND m.team = :team"
    ),
    @NamedQuery(
    		name="Member.getByEmailAddressAndParticipantRole",
    		query="SELECT m FROM Member m WHERE m.emailAddress = :emailAddress AND m.participantRole = :participantRole"
    ),
    @NamedQuery(
    		name="Member.getByPhoneNumber",
    		query="SELECT m FROM Member m WHERE m.phoneNumber = :phoneNumber"
    ),
    @NamedQuery(
    		name="Member.getBySmsConfirmedPhoneNumber",
    		query="SELECT m FROM Member m WHERE m.smsConfirmedPhoneNumbers = :phoneNumber"
    ),
    @NamedQuery(
    		name="Member.getByGuardianPhoneNumber",
    		query="SELECT m FROM Member m WHERE m.guardianPhoneNumbers = :guardianPhoneNumber"
    ),
})
/*
 * Getters never return null, but rather, empty Strings, empty Lists, etc.
 */
public class Member {
	//private static final Logger log = Logger.getLogger(Member.class.getName());
	private static RskyboxClient log = new RskyboxClient();
	
	//constants
	public static final String FRIEND_ROLE = "friend";
	public static final String COACH_ROLE = "coach";
	public static final String MANAGER_ROLE = "manager";
	public static final String PLAYER_ROLE = "player";
	public static final String PARENT_ROLE = "parent";
	public static final String SPONSOR_ROLE = "sponsor";
	public static final String FAMILY_ROLE = "family";
	public static final String TRAINER_ROLE = "trainer";
	public static final String ORGANIZER_ROLE = "organizer";
	public static final String FAN_ROLE = "fan";
	
	// what type of participant -- varies team-to-team
	public static final String CREATOR_PARTICIPANT = "creator";			// special type of coordinator (i.e. creator is a coordinator too)
	public static final String COORDINATOR_PARTICIPANT = "coordinator";	// coordinator is a special type of member (i.e. coordinator is a member too)
	public static final String MEMBER_PARTICIPANT = "member";
	public static final String FAN_PARTICIPANT = "fan";
	
	public static final String FEMALE_GENDER = "female";
	public static final String MALE_GENDER = "male";
	
	// quick research shows phones numbers around the world must be between 5 and 15 digits
	public static final Integer MINIMUM_PHONE_NUMBER_LENGTH = 5;
	public static final Integer MAXIMUM_PHONE_NUMBER_LENGTH = 15;

	private String firstName;
	private String lastName;
	private String emailAddress;
	private String jerseyNumber;  //uniqueness is NOT enforced
	private Text photoBase64;
	private Text thumbNail;
	private String participantRole;
	private String phoneNumber;
	private String userId;	// set when user becomes associated with member email address (i.e. both user and member email address are NA)
	private Boolean isEmailAddressActive;
	private String gender;
	private Integer age;
	private String streetAddress;
	private String city;
	private String state;
	private String zipcode;
	private Boolean isMarkedForDeletion;
	private Date markedForDeletionOn;
	private String markedForDeletionRequester;
	private Integer autoArchiveDayCount;  // copied from user entity - stored here for efficiency when sending messages.
	private Boolean hasBeenSmsConfirmed;
	private String smsEmailAddress;
	private Boolean hasRteamMessageAccessEnabled;
	private Boolean hasEmailMessageAccessEnabled;
	private Boolean hasSmsMessageAccessEnabled;
	private Boolean hasCcToSelfEnabled;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Key key;
	
    @ManyToOne(fetch = FetchType.LAZY)
    private Team team;
    
	@Basic
	private List<String> networkAuthenticatedEmailAddresses; // used to quickly query and find NA email address
    
	@Basic
	private List<String> smsConfirmedPhoneNumbers; // used to quickly query and find SMS confirmed phone numbers

	@Basic
	private List<String> guardianKeys;

	@Basic
	private List<String> guardianEmailAddresses;

	@Basic
	private List<String> guardianPhoneNumbers;
    
	@Basic
	private List<String> guardianUserIds; // set when user becomes associated with guardian email address
    
	@Basic
	private List<Boolean> guardianIsEmailAddressActives;
    
	@Basic
	private List<String> guardianFirstNames;  

	@Basic
	private List<String> guardianLastNames;  

	@Basic
	private List<Integer> guardianAutoArchiveDayCounts; 
    
	@Basic
	private List<String> guardianSmsEmailAddresses;  // set when guardian becomes SMS confirmed 
    
	@Basic
	private List<Boolean> guardianHasRteamMessageAccessEnabled;
    
	@Basic
	private List<Boolean> guardianHasEmailMessageAccessEnabled;
    
	@Basic
	private List<Boolean> guardianHasSmsMessageAccessEnabled;

	@Basic
	private List<String> roles;
	
	public List<Guardian> getGuardians() {
		List<Guardian> guardians = new ArrayList<Guardian>();
		
		if(this.guardianKeys == null || this.guardianKeys.size() == 0) {
			// return the empty list
			return guardians;
		}
		// all guardian arrays are same size, so it doesn't matter which one size is taken from
		int listSize = this.guardianKeys.size();
		for(int i=0; i<listSize; i++) {
			Guardian g = new Guardian();
			g.setKey(this.guardianKeys.get(i));
			
			///////////////////////////////////////////////////////////////////////
			// Convert "default" values stored in Big Table to "normal Java" values
			///////////////////////////////////////////////////////////////////////
			String ea = null;
			if(guardianEmailAddresses.size() > i) {
				ea = this.guardianEmailAddresses.get(i).equals("") ? null : this.guardianEmailAddresses.get(i);
			} else {
				log.error("Member:getGuardians:emailAddress", "guardianEmailAddresses array size corrupt for member name = " + this.getFullName());
			}
			g.setEmailAddress(ea);
			
			String pn = null;
			if(guardianPhoneNumbers.size() > i) {
				pn = this.guardianPhoneNumbers.get(i).equals("") ? null : this.guardianPhoneNumbers.get(i);
			} else {
				log.error("Member:getGuardians:phoneNumber", "guardianPhoneNumbers array size corrupt for member name = " + this.getFullName());
			}
			g.setPhoneNumber(pn);
			
			String ui = null;
			if(guardianUserIds.size() > i) {
				ui = this.guardianUserIds.get(i).equals("") ? null : this.guardianUserIds.get(i);
			} else {
				log.error("Member:getGuardians:userId", "guardianUserIds array size corrupt for member name = " + this.getFullName());
			}
			g.setUserId(ui);
			
			// no conversion needed since 'default' stored in big table is a boolean
			if(guardianIsEmailAddressActives.size() > i) {
				g.setIsEmailAddressActive(this.guardianIsEmailAddressActives.get(i));
			} else {
				log.error("Member:getGuardians:isEmailAddressActive", "guardianIsEmailAddressActives array size corrupt for member name = " + this.getFullName());
				g.setIsEmailAddressActive(false);
			}
			
			String fn = null;
			if(guardianFirstNames.size() > i) {
				fn = this.guardianFirstNames.get(i).equals("") ? null : this.guardianFirstNames.get(i);
			} else {
				log.error("Member:getGuardians:firstName", "guardianFirstNames array size corrupt for member name = " + this.getFullName());
			}
			g.setFirstName(fn);
			
			String ln = null;
			if(guardianLastNames.size() > i) {
				ln = this.guardianLastNames.get(i).equals("") ? null : this.guardianLastNames.get(i);
			} else {
				log.error("Member:getGuardians:lastName", "guardianLastNames array size corrupt for member name = " + this.getFullName());
			}
			g.setLastName(ln);
			
			Integer aadc = null;
			if(guardianAutoArchiveDayCounts.size() > i) {
				aadc = this.guardianAutoArchiveDayCounts.get(i).equals(-1) ? null : this.guardianAutoArchiveDayCounts.get(i);
			} else {
				log.error("Member:getGuardians:autoArchiveDayCounts", "guardianAutoArchiveDayCounts array size corrupt for member name = " + this.getFullName());
			}
			g.setAutoArchiveDayCount(aadc);
			
			String sea = null;
			if(guardianSmsEmailAddresses.size() > i) {
				sea = this.guardianSmsEmailAddresses.get(i).equals("") ? null : this.guardianSmsEmailAddresses.get(i);
			} else {
				log.error("Member:getGuardians:smsAddress", "guardianSmsEmailAddresses array size corrupt for member name = " + this.getFullName());
			}
			g.setSmsEmailAddress(sea);
			
			// no conversion needed since 'default' stored in big table is a boolean
			if(guardianHasRteamMessageAccessEnabled.size() > i) {
				g.setHasRteamMessageAccessEnabled(this.guardianHasRteamMessageAccessEnabled.get(i));
			} else {
				log.error("Member:getGuardians:rTeamMessageEnabled", "guardianHasRteamMessageAccessEnabled array size corrupt for member name = " + this.getFullName());
				g.setHasRteamMessageAccessEnabled(false);
			}
			
			// no conversion needed since 'default' stored in big table is a boolean
			if(guardianHasEmailMessageAccessEnabled.size() > i) {
				g.setHasEmailMessageAccessEnabled(this.guardianHasEmailMessageAccessEnabled.get(i));
			} else {
				log.error("Member:getGuardians:emailMessageEnabled", "guardianHasEmailMessageAccessEnabled array size corrupt for member name = " + this.getFullName());
				g.setHasEmailMessageAccessEnabled(false);
			}
			
			// no conversion needed since 'default' stored in big table is a boolean
			if(guardianHasSmsMessageAccessEnabled.size() > i) {
				g.setHasSmsMessageAccessEnabled(this.guardianHasSmsMessageAccessEnabled.get(i));
			} else {
				log.error("Member:getGuardians:smsMessageEnabled", "guardianHasSmsMessageAccessEnabled array size corrupt for member name = " + this.getFullName());
				g.setHasSmsMessageAccessEnabled(false);
			}
			
			// NA Email Address and SMS confirmed phone numbers are NOT held in guardian lists. Instead, these lists
			// apply to the entire membership.
			Boolean isEmailAddressNetworkAuthenticated = isEmailAddressNetworkAuthenticated(ea);
			g.setIsEmailAddressNetworkAuthenticated(isEmailAddressNetworkAuthenticated);
			Boolean isPhoneNumberSmsConfirmed = isPhoneNumberSmsConfirmed(pn);
			g.setHasBeenSmsConfirmed(isPhoneNumberSmsConfirmed);
			
			// a guardian is NA if either phone number or SMS have been confirmed
			Boolean isNetworkedAuthenticated = isEmailAddressNetworkAuthenticated || isPhoneNumberSmsConfirmed;
			g.setIsNetworkAuthenticated(isNetworkedAuthenticated);
			
			guardians.add(g);
		}
		return guardians;
	}
	
	// returns Guardian matching key if found. Otherwise, returns null.
	public Guardian getGuardian(String theKey) {
		if(this.guardianKeys == null || this.guardianKeys.size() == 0) {
			// return the empty list
			return null;
		}

		Integer guardianIndex = getGuardianIndexByKey(theKey);
		if(guardianIndex == null) {
			return null;
		}
		
		Guardian requestedGuardian = new Guardian();
		
		requestedGuardian.setKey(this.guardianKeys.get(guardianIndex));
		
		///////////////////////////////////////////////////////////////////////
		// Convert "default" values stored in Big Table to "normal Java" values
		///////////////////////////////////////////////////////////////////////
		String ea = this.guardianEmailAddresses.get(guardianIndex).equals("") ? null : this.guardianEmailAddresses.get(guardianIndex);
		requestedGuardian.setEmailAddress(ea);
		
		String pn = this.guardianPhoneNumbers.get(guardianIndex).equals("") ? null : this.guardianPhoneNumbers.get(guardianIndex);
		requestedGuardian.setPhoneNumber(pn);
		
		String ui = this.guardianUserIds.get(guardianIndex).equals("") ? null : this.guardianUserIds.get(guardianIndex);
		requestedGuardian.setUserId(ui);
		
		// no conversion needed since 'default' stored in big table is a boolean
		requestedGuardian.setIsEmailAddressActive(this.guardianIsEmailAddressActives.get(guardianIndex));
		
		String fn = this.guardianFirstNames.get(guardianIndex).equals("") ? null : this.guardianFirstNames.get(guardianIndex);
		requestedGuardian.setFirstName(fn);
		
		String ln = this.guardianLastNames.get(guardianIndex).equals("") ? null : this.guardianLastNames.get(guardianIndex);
		requestedGuardian.setLastName(ln);
		
		Integer aadc = this.guardianAutoArchiveDayCounts.get(guardianIndex).equals(-1) ? null : this.guardianAutoArchiveDayCounts.get(guardianIndex);
		requestedGuardian.setAutoArchiveDayCount(aadc);
		
		String sea = this.guardianSmsEmailAddresses.get(guardianIndex).equals("") ? null : this.guardianSmsEmailAddresses.get(guardianIndex);
		requestedGuardian.setSmsEmailAddress(sea);
		
		// no conversion needed since 'default' stored in big table is a boolean
		requestedGuardian.setHasRteamMessageAccessEnabled(this.guardianHasRteamMessageAccessEnabled.get(guardianIndex));
		
		// no conversion needed since 'default' stored in big table is a boolean
		requestedGuardian.setHasEmailMessageAccessEnabled(this.guardianHasEmailMessageAccessEnabled.get(guardianIndex));
		
		// no conversion needed since 'default' stored in big table is a boolean
		requestedGuardian.setHasSmsMessageAccessEnabled(this.guardianHasSmsMessageAccessEnabled.get(guardianIndex));
		
		Boolean isEmailAddressNetworkAuthenticated = isEmailAddressNetworkAuthenticated(ea);
		requestedGuardian.setIsEmailAddressNetworkAuthenticated(isEmailAddressNetworkAuthenticated);
		Boolean isPhoneNumberSmsConfirmed = isPhoneNumberSmsConfirmed(pn);
		requestedGuardian.setHasBeenSmsConfirmed(isPhoneNumberSmsConfirmed);
		
		// a guardian is NA if either phone number or SMS have been confirmed
		Boolean isNetworkedAuthenticated = isEmailAddressNetworkAuthenticated || isPhoneNumberSmsConfirmed;
		requestedGuardian.setIsNetworkAuthenticated(isNetworkedAuthenticated);

		return requestedGuardian;
	}
	
	// Creates guardian only if there currently are no guardians in the member entity. If there are
	// already guardians, failure (false) is returned. The guardian unique keys are created by this method.
	public Boolean createGuardians(List<Guardian> theNewGuardianList) {
		if(guardianKeys != null && guardianKeys.size() > 0) {
			return false;
		}
		
		if(theNewGuardianList == null || theNewGuardianList.size() == 0) {
			return false;
		}
		
		this.guardianKeys = new ArrayList<String>();
		this.guardianEmailAddresses = new ArrayList<String>();
		this.guardianPhoneNumbers = new ArrayList<String>();
		this.guardianUserIds = new ArrayList<String>();
		this.guardianIsEmailAddressActives = new ArrayList<Boolean>();
		this.guardianFirstNames = new ArrayList<String>();
		this.guardianLastNames = new ArrayList<String>();
		this.guardianAutoArchiveDayCounts = new ArrayList<Integer>();
		this.guardianSmsEmailAddresses = new ArrayList<String>();
		this.guardianHasRteamMessageAccessEnabled = new ArrayList<Boolean>();
		this.guardianHasEmailMessageAccessEnabled = new ArrayList<Boolean>();
		this.guardianHasSmsMessageAccessEnabled = new ArrayList<Boolean>();

		
		for(Guardian g : theNewGuardianList) {
			this.guardianKeys.add(TF.getPassword());
			
			////////////////////////////////////////////////////////////////
			// Convert "normal Java" values to "default" values in Big Table
			////////////////////////////////////////////////////////////////
			String ea = g.getEmailAddress() == null ? "" : g.getEmailAddress();
			this.guardianEmailAddresses.add(ea);
			
			String pn = g.getPhoneNumber() == null ? "" : g.getPhoneNumber();
			this.guardianPhoneNumbers.add(pn);
			
			String ui = g.getUserId() == null ? "" : g.getUserId();
			this.guardianUserIds.add(ui);
			
			Boolean eaa = true;
			this.guardianIsEmailAddressActives.add(eaa);
			
			String fn = g.getFirstName() == null ? "" : g.getFirstName();
			this.guardianFirstNames.add(fn);
			
			String ln = g.getLastName() == null ? "" : g.getLastName();
			this.guardianLastNames.add(ln);
			
			Integer aadc = g.getAutoArchiveDayCount() == null ? -1 : g.getAutoArchiveDayCount();
			this.guardianAutoArchiveDayCounts.add(aadc);
			
			String sea = g.getSmsEmailAddress() == null ? "" : g.getSmsEmailAddress();
			this.guardianSmsEmailAddresses.add(sea);
			
			Boolean ghrmae = false;
			this.guardianHasRteamMessageAccessEnabled.add(ghrmae);
			
			Boolean ghemae = false;
			this.guardianHasEmailMessageAccessEnabled.add(ghemae);
			
			Boolean ghsmae = false;
			this.guardianHasSmsMessageAccessEnabled.add(ghsmae);
		}
		
		return true;
	}
	
	// Updates the guardian by matching it to an existing guardian via the key.
	// Returns 'true' if call successful, 'false' otherwise.
	public Boolean updateGuardian(Guardian theNewGuardian) {
		if(theNewGuardian == null) {return false;}

		Integer guardianIndex = getGuardianIndexByKey(theNewGuardian.getKey());
		//log.debug("guardianIndex = " + guardianIndex);
		if(guardianIndex == null) {
			log.debug("updateGuardian() failed because it could not find guardian with matching key");
			return false;
		}
		
		///////////////////////////
		// Can never modify the key
		///////////////////////////
		
		////////////////////////////////////////////////////////////////
		// Convert "normal Java" values to "default" values in Big Table
		////////////////////////////////////////////////////////////////
		String ea = theNewGuardian.getEmailAddress() == null ? "" : theNewGuardian.getEmailAddress();
		//log.debug("guardianEmailAddresses size = " + guardianEmailAddresses.size());
		this.guardianEmailAddresses.set(guardianIndex, ea);
		
		String pn = theNewGuardian.getPhoneNumber() == null ? "" : theNewGuardian.getPhoneNumber();
		//log.debug("guardianPhoneNumbers size = " + guardianPhoneNumbers.size());
		this.guardianPhoneNumbers.set(guardianIndex, pn);
		
		String ui = theNewGuardian.getUserId() == null ? "" : theNewGuardian.getUserId();
		//log.debug("guardianUserIds size = " + guardianUserIds.size());
		this.guardianUserIds.set(guardianIndex, ui);
		
		Boolean eaa = theNewGuardian.getIsEmailAddressActive() == null ? true : theNewGuardian.getIsEmailAddressActive();
		//log.debug("guardianIsEmailAddressActives size = " + guardianIsEmailAddressActives.size());
		this.guardianIsEmailAddressActives.set(guardianIndex, eaa);
		
		String fn = theNewGuardian.getFirstName() == null ? "" : theNewGuardian.getFirstName();
		//log.debug("guardianFirstNames size = " + guardianFirstNames.size());
		this.guardianFirstNames.set(guardianIndex, fn);
		
		String ln = theNewGuardian.getLastName() == null ? "" : theNewGuardian.getLastName();
		//log.debug("guardianLastNames size = " + guardianLastNames.size());
		this.guardianLastNames.set(guardianIndex, ln);
		
		Integer aadc = theNewGuardian.getAutoArchiveDayCount() == null ? -1 : theNewGuardian.getAutoArchiveDayCount();
		//log.debug("guardianAutoArchiveDayCounts size = " + guardianAutoArchiveDayCounts.size());
		this.guardianAutoArchiveDayCounts.set(guardianIndex, aadc);
		
// never updated directly by client
//		String sea = theNewGuardian.getSmsEmailAddress() == null ? "" : theNewGuardian.getSmsEmailAddress();
//		this.guardianSmsEmailAddresses.set(guardianIndex, sea);
		
		Boolean ghrmae = theNewGuardian.getHasRteamMessageAccessEnabled() == null ? true : theNewGuardian.getHasRteamMessageAccessEnabled();
		//log.debug("getHasRteamMessageAccessEnabled size = " + getHasRteamMessageAccessEnabled());
		this.guardianHasRteamMessageAccessEnabled.set(guardianIndex, ghrmae);
		
		Boolean ghemae = theNewGuardian.getHasEmailMessageAccessEnabled() == null ? true : theNewGuardian.getHasEmailMessageAccessEnabled();
		//log.debug("guardianHasEmailMessageAccessEnabled size = " + guardianHasEmailMessageAccessEnabled());
		this.guardianHasEmailMessageAccessEnabled.set(guardianIndex, ghemae);
		
		Boolean ghsmae = theNewGuardian.getHasSmsMessageAccessEnabled() == null ? true : theNewGuardian.getHasSmsMessageAccessEnabled();
		//log.debug("guardianHasSmsMessageAccessEnabled size = " + guardianHasSmsMessageAccessEnabled());
		this.guardianHasSmsMessageAccessEnabled.set(guardianIndex, ghsmae);
		
		return true;
	}
	
	// Deletes the guardian by matching it to an existing guardian via the key.
	// Returns 'true' if call successful, 'false' otherwise.
	public Boolean deleteGuardian(String theKeyOfGuardianBeingDeleted) {
		if(theKeyOfGuardianBeingDeleted == null) {return false;}

		Integer guardianIndex = getGuardianIndexByKey(theKeyOfGuardianBeingDeleted);
		if(guardianIndex == null) {
			log.debug("deleteGuardian() failed because it could not find guardian with matching key");
			return false;
		}
		
		// remove email address from NA email address list if it is present
		String emailAddressBeingDeleted = this.guardianEmailAddresses.get(guardianIndex);
		if(emailAddressBeingDeleted != null) {
			emailAddressBeingDeleted = emailAddressBeingDeleted.toLowerCase();
			if(this.networkAuthenticatedEmailAddresses != null && this.networkAuthenticatedEmailAddresses.contains(emailAddressBeingDeleted)) {
				this.networkAuthenticatedEmailAddresses.remove(emailAddressBeingDeleted);
			}
		}
		
		// remove phone number from SMS confirmed phone number list if it is present
		String phoneNumberBeingDeleted = this.guardianPhoneNumbers.get(guardianIndex);
		if(phoneNumberBeingDeleted != null) {
			if(this.smsConfirmedPhoneNumbers != null && this.smsConfirmedPhoneNumbers.contains(phoneNumberBeingDeleted)) {
				this.smsConfirmedPhoneNumbers.remove(phoneNumberBeingDeleted);
			}
		}
		
		// NOTE: simple list.remove() does not work - try building a new list and using that
		this.guardianKeys = createNewListWithoutItem(guardianIndex, this.guardianKeys);
		this.guardianEmailAddresses = createNewListWithoutItem(guardianIndex, this.guardianEmailAddresses);
		this.guardianPhoneNumbers = createNewListWithoutItem(guardianIndex, this.guardianPhoneNumbers);
		this.guardianUserIds = createNewListWithoutItem(guardianIndex, this.guardianUserIds);
		this.guardianIsEmailAddressActives = createNewListWithoutItem(guardianIndex, this.guardianIsEmailAddressActives);
		this.guardianFirstNames = createNewListWithoutItem(guardianIndex, this.guardianFirstNames);
		this.guardianLastNames = createNewListWithoutItem(guardianIndex, this.guardianLastNames);
		this.guardianAutoArchiveDayCounts = createNewListWithoutItem(guardianIndex, this.guardianAutoArchiveDayCounts);
		this.guardianSmsEmailAddresses = createNewListWithoutItem(guardianIndex, this.guardianSmsEmailAddresses);
		this.guardianHasRteamMessageAccessEnabled = createNewListWithoutItem(guardianIndex, this.guardianHasRteamMessageAccessEnabled);
		this.guardianHasEmailMessageAccessEnabled = createNewListWithoutItem(guardianIndex, this.guardianHasEmailMessageAccessEnabled);
		this.guardianHasSmsMessageAccessEnabled = createNewListWithoutItem(guardianIndex, this.guardianHasSmsMessageAccessEnabled);
		
//		this.guardianKeys.remove(guardianIndex);
//		this.guardianEmailAddresses.remove(guardianIndex);
//		this.guardianPhoneNumbers.remove(guardianIndex);
//		this.guardianUserIds.remove(guardianIndex);
//		this.guardianIsEmailAddressActives.remove(guardianIndex);
//		this.guardianFirstNames.remove(guardianIndex);
//		this.guardianLastNames.remove(guardianIndex);
//		this.guardianAutoArchiveDayCounts.remove(guardianIndex);
		
		log.debug("deleteGuardian(): finished removing all fields from guardian");
		
		return true;
	}
	
	private List createNewListWithoutItem(Integer theIndexOfItemToRemove, List theOldList) {
		List newList = new ArrayList();
		
		for(int i=0; i<theOldList.size(); i++) {
			if(theIndexOfItemToRemove.equals(i)) {continue;}
			newList.add(theOldList.get(i));
		}
		
		return newList;
	}
	
	// Adds the guardian.
	// Returns 'true' if call successful, 'false' otherwise.
	public Boolean addGuardian(Guardian theGuardianBeingAdded) {
		if(theGuardianBeingAdded == null) {return false;}
		
		this.guardianKeys.add(TF.getPassword());
		
		////////////////////////////////////////////////////////////////
		// Convert "normal Java" values to "default" values in Big Table
		////////////////////////////////////////////////////////////////
		String ea = theGuardianBeingAdded.getEmailAddress() == null ? "" : theGuardianBeingAdded.getEmailAddress();
		this.guardianEmailAddresses.add(ea);
		
		String pn = theGuardianBeingAdded.getPhoneNumber() == null ? "" : theGuardianBeingAdded.getPhoneNumber();
		this.guardianPhoneNumbers.add(pn);
		
		String ui = theGuardianBeingAdded.getUserId() == null ? "" : theGuardianBeingAdded.getUserId();
		this.guardianUserIds.add(ui);
		
		Boolean eaa = theGuardianBeingAdded.getIsEmailAddressActive() == null ? true : theGuardianBeingAdded.getIsEmailAddressActive();
		this.guardianIsEmailAddressActives.add(eaa);
		
		String fn = theGuardianBeingAdded.getFirstName() == null ? "" : theGuardianBeingAdded.getFirstName();
		this.guardianFirstNames.add(fn);
		
		String ln = theGuardianBeingAdded.getLastName() == null ? "" : theGuardianBeingAdded.getLastName();
		this.guardianLastNames.add(ln);
		
		Integer aadc = theGuardianBeingAdded.getAutoArchiveDayCount() == null ? -1 : theGuardianBeingAdded.getAutoArchiveDayCount();
		this.guardianAutoArchiveDayCounts.add(aadc);
		
		String sea = theGuardianBeingAdded.getSmsEmailAddress() == null ? "" : theGuardianBeingAdded.getSmsEmailAddress();
		this.guardianSmsEmailAddresses.add(sea);
		
		Boolean hrmae = theGuardianBeingAdded.getHasRteamMessageAccessEnabled() == null ? true : theGuardianBeingAdded.getHasRteamMessageAccessEnabled();
		this.guardianHasRteamMessageAccessEnabled.add(hrmae);
		
		Boolean hemae = theGuardianBeingAdded.getHasEmailMessageAccessEnabled() == null ? true : theGuardianBeingAdded.getHasEmailMessageAccessEnabled();
		this.guardianHasEmailMessageAccessEnabled.add(hemae);
		
		Boolean hsmae = theGuardianBeingAdded.getHasSmsMessageAccessEnabled() == null ? true : theGuardianBeingAdded.getHasSmsMessageAccessEnabled();
		this.guardianHasSmsMessageAccessEnabled.add(hsmae);
		
		return true;
	}
	
	private Integer getGuardianIndexByKey(String theKey) {
		if(theKey == null || this.guardianKeys == null) {return null;}
		
		int listSize = this.guardianKeys.size();
		Integer guardianIndex = null;
		for(int i=0; i<listSize; i++) {
			if(this.guardianKeys.get(i).equals(theKey)) {
				guardianIndex = i;
				return guardianIndex;
			}
		}
		return null;
	}
	
	// guaranteed not to return null
	public List<String> getGuardianKeys() {
		return this.guardianKeys == null ? new ArrayList<String>() : this.guardianKeys;
	}

	private void setGuardianKeys(List<String> guardianKeys) {
		this.guardianKeys = guardianKeys;
	}
	
	// TODO make private - only made public for normailizeEmailAddresses Migration
	// guaranteed not to return null
	public List<String> getGuardianEmailAddresses() {
		return this.guardianEmailAddresses == null ? new ArrayList<String>() : this.guardianEmailAddresses;
	}

	// TODO make private - only made public for normailizeEmailAddresses Migration
	public void setGuardianEmailAddresses(List<String> guardianEmailAddresses) {
		this.guardianEmailAddresses = guardianEmailAddresses;
	}

	private List<String> getGuardianPhoneNumbers() {
		return this.guardianPhoneNumbers == null ? new ArrayList<String>() : this.guardianPhoneNumbers;
	}

	private void setGuardianPhoneNumbers(List<String> guardianPhoneNumbers) {
		this.guardianPhoneNumbers = guardianPhoneNumbers;
	}

	private List<String> getGuardianUserIds() {
		return this.guardianUserIds == null ? new ArrayList<String>() : this.guardianUserIds;
	}

	private void setGuardianUserIds(List<String> guardianUserIds) {
		this.guardianUserIds = guardianUserIds;
	}

	private Boolean getIsEmailAddressActive() {
		return this.isEmailAddressActive;
	}

	private void setIsEmailAddressActive(Boolean isEmailAddressActive) {
		this.isEmailAddressActive = isEmailAddressActive;
	}
	
	private List<Boolean> getGuardianIsEmailAddressActives() {
		return guardianIsEmailAddressActives;
	}

	private void setGuardianIsEmailAddressActives(List<Boolean> guardianIsEmailAddressActives) {
		this.guardianIsEmailAddressActives = guardianIsEmailAddressActives;
	}

	public String getJerseyNumber() {
		return this.jerseyNumber == null ? "" : this.jerseyNumber;
	}

	public void setJerseyNumber(String jerseyNumber) {
		this.jerseyNumber = jerseyNumber;
	}

	// guaranteed not to return null
	public String getEmailAddress() {
		return this.emailAddress == null ? "" : this.emailAddress;
	}

	public void setEmailAddress(String theEmailAddress) {
		// always store email in lower case to make queries and comparisons case insensitive
		if(theEmailAddress != null) {theEmailAddress = theEmailAddress.toLowerCase();}
		this.emailAddress = theEmailAddress;
	}

    public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}

	public Member() {
    	
    }
    
    // used in client-side tests
    public Member(String firstName, String lastName, String twitterUserName, String twitterPassword) {
    	this.firstName = firstName;
    	this.lastName = lastName;
    }

    public Key getKey() {
        return key;
    }

    public String getFirstName() {
		return firstName;
	}
    
	public void setFirstName(String teamName) {
		this.firstName = teamName;
	}
	
	public String getLastName() {
		return lastName;
	}
	
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	public String getFullName() {
		return getDisplayName(this.firstName, this.lastName, this.emailAddress, this.phoneNumber);
	}
	
	// returns true if PRIMARY member is NA (i.e. primary email address is NA or primary phone number is SMS confirmed)
	public Boolean getIsNetworkAuthenticated() {
		Boolean isEmailAddressNA = isEmailAddressNetworkAuthenticated(this.emailAddress);
		Boolean isPhoneNumberSC = this.isPhoneNumberSmsConfirmed(this.phoneNumber);
		return isEmailAddressNA || isPhoneNumberSC;
	}

    public List<String> getRoles() {
    	return this.roles == null ? new ArrayList<String>() : this.roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	public String getThumbNailBase64() {
		return thumbNail == null? null : thumbNail.getValue();
	}

	public void setThumbNailBase64(String thumbNail) {
		this.thumbNail = new Text(thumbNail);
	}

	public String getParticipantRole() {
		return participantRole;
	}

	public void setParticipantRole(String participantRole) {
		this.participantRole = participantRole;
	}

	// guaranteed not to return null
	public String getPhoneNumber() {
		return this.phoneNumber == null ? "" : this.phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public Boolean getHasBeenSmsConfirmed() {
		return this.hasBeenSmsConfirmed == null ? false : this.hasBeenSmsConfirmed;
	}

	public void setHasBeenSmsConfirmed(Boolean hasBeenSmsConfirmed) {
		this.hasBeenSmsConfirmed = hasBeenSmsConfirmed;
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
	
	public boolean isCoordinator() {
		if(  this.participantRole != null &&
			(this.participantRole.equalsIgnoreCase(CREATOR_PARTICIPANT) ||
		     this.participantRole.equalsIgnoreCase(COORDINATOR_PARTICIPANT))) {
			return true;
		}
		return false;
	}
	
	public boolean isCreator() {
		if(  this.participantRole != null &&
			(this.participantRole.equalsIgnoreCase(CREATOR_PARTICIPANT))) {
			return true;
		}
		return false;
	}
	
	// "Member" refers to participation. Members include: creator, coordinators and plain members (but not fans).
	public boolean isMemberParticipant() {
		if(  this.participantRole != null &&
			(this.participantRole.equalsIgnoreCase(CREATOR_PARTICIPANT) ||
		     this.participantRole.equalsIgnoreCase(COORDINATOR_PARTICIPANT) || 
		     this.participantRole.equalsIgnoreCase(MEMBER_PARTICIPANT))) {
			return true;
		}
		return false;
	}
	
	public boolean isFan() {
		if(this.participantRole != null && this.participantRole.equalsIgnoreCase(FAN_PARTICIPANT)) {
			return true;
		}
		return false;
	}
	
	// returns true if one or more of the people in this membership are users; false otherwise
	public boolean isAssociatedWithUser() {
		if(this.userId != null || (this.guardianUserIds != null && this.guardianUserIds.size() > 0) ) {
			return true;
		}
		return false;
	}
	
	// returns true if one or more of the people in this membership are associated with the specified user; false otherwise
	public boolean isAssociatedWithUser(String theUserId) {
		if( (this.userId != null && this.userId.equalsIgnoreCase(theUserId)) ||
		    (this.guardianUserIds != null && this.guardianUserIds.contains(theUserId)) ) {
			return true;
		}
		return false;
	}
	
	// Returns all individuals of this membership that can receive some type of message because their emailAddress 
	// has been network authenticated and/or their phone number has been SMS confirmed.
	public List<UserMemberInfo> getAuthorizedRecipients(Team theTeam) {
		List<UserMemberInfo> authorizedRecipients =  new ArrayList<UserMemberInfo>();
		
		/////////////////////////////////
		// process the primary membership
		/////////////////////////////////
		UserMemberInfo umiPrimary = new UserMemberInfo();
		// memberId, team and participant role are set in the UMI as a convenience, though they are the same for all participants in this membership
		umiPrimary.setMemberId(KeyFactory.keyToString(this.getKey()));
		umiPrimary.setTeam(theTeam);
		umiPrimary.setParticipantRole(this.participantRole);
		
		// only add in email address if NA
		if(this.isEmailAddressNetworkAuthenticated(this.emailAddress)) {umiPrimary.setEmailAddress(this.emailAddress);}
		// only add in phone number if confirmed
		if(this.isPhoneNumberSmsConfirmed(this.phoneNumber)) {
			umiPrimary.setPhoneNumber(this.phoneNumber);
			umiPrimary.setSmsEmailAddress(this.smsEmailAddress);
		}
		umiPrimary.setHasBeenSmsConfirmed(true); // not sure this is really needed anymore
		umiPrimary.setUserId(this.userId); // may be null. Only non-null for members associated with users.
		umiPrimary.setFirstName(this.firstName);
		umiPrimary.setLastName(this.lastName);
		umiPrimary.setFullName(getDisplayName(this.firstName, this.lastName, this.emailAddress, this.phoneNumber));
		umiPrimary.setAutoArchiveDayCount(this.autoArchiveDayCount);
		umiPrimary.setHasRteamMessageAccessEnabled(this.hasRteamMessageAccessEnabled == null ? false : this.hasRteamMessageAccessEnabled);
		umiPrimary.setHasEmailMessageAccessEnabled(this.hasEmailMessageAccessEnabled == null ? false : this.hasEmailMessageAccessEnabled);
		umiPrimary.setHasSmsMessageAccessEnabled(this.hasSmsMessageAccessEnabled == null ? false: this.hasSmsMessageAccessEnabled);
		
		// only add to participant list if NA or confirmed
		if(umiPrimary.getEmailAddress() != null || umiPrimary.getPhoneNumber() != null) {
			authorizedRecipients.add(umiPrimary);
			log.debug("primary membership added as NA/confirmed participant - name = " + this.getFullName());
		}
		
		///////////////////////////////////
		// process the guardian memberships
		///////////////////////////////////
		List<Guardian> guardians = this.getGuardians();
		for(Guardian g : guardians) {
			UserMemberInfo umiGuardian = new UserMemberInfo();
			// memberId, team and participantRole are set in the UMI as a convenience, though they are the same for all participants in this membership
			umiGuardian.setMemberId(KeyFactory.keyToString(this.getKey()));
			umiGuardian.setTeam(theTeam);
			umiGuardian.setParticipantRole(this.participantRole);

			// only add in email address if NA
			if(this.isEmailAddressNetworkAuthenticated(g.getEmailAddress())) {umiGuardian.setEmailAddress(g.getEmailAddress());}
			// only add in phone number if confirmed
			if(this.isPhoneNumberSmsConfirmed(g.getPhoneNumber())) {
				umiGuardian.setPhoneNumber(g.getPhoneNumber());
				umiGuardian.setSmsEmailAddress(g.getSmsEmailAddress());
			}
			umiGuardian.setHasBeenSmsConfirmed(true); // not sure this is really needed anymore
			umiGuardian.setUserId(g.getUserId()); // may be null
			umiGuardian.setFirstName(g.getFirstName());
			umiGuardian.setLastName(g.getLastName());
			umiGuardian.setFullName(getDisplayName(g.getFirstName(), g.getLastName(), g.getEmailAddress(), g.getPhoneNumber()));
			umiGuardian.setAutoArchiveDayCount(g.getAutoArchiveDayCount());
			umiGuardian.setHasRteamMessageAccessEnabled(g.getHasRteamMessageAccessEnabled());
			umiGuardian.setHasEmailMessageAccessEnabled(g.getHasEmailMessageAccessEnabled());
			umiGuardian.setHasSmsMessageAccessEnabled(g.getHasSmsMessageAccessEnabled());
			
			// only add to participant list if NA or confirmed
			if(umiGuardian.getEmailAddress() != null || umiGuardian.getPhoneNumber() != null) {
				authorizedRecipients.add(umiGuardian);
				log.debug("guardian membership added as NA/confirmed participant - name = " + g.getFullName());
			}
		}
		
		return authorizedRecipients;
	}
	
	// network authenticate the member email addresses that match the specified email address
	public void networkAuthenticateEmailAddress(String theEmailAddress) {
		if(theEmailAddress == null) {
			return;
		} else {
			theEmailAddress = theEmailAddress.toLowerCase();
		}
		
		if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(theEmailAddress)) {
			// after network authentication, email address is ALWAYS (re)activated
			this.isEmailAddressActive = true;
		} else if(this.guardianEmailAddresses != null) {
			int index = 0;
			for(String ea : this.guardianEmailAddresses) {
				if(ea.equalsIgnoreCase(theEmailAddress)) {
					// after network authentication, email address is ALWAYS (re)activated
					this.guardianIsEmailAddressActives.set(index, true);
					break;
				}
				index++;
			}
		}
		
		// if the NA EA list doesn't exist yet, create it.
		if(this.networkAuthenticatedEmailAddresses == null) {
			this.networkAuthenticatedEmailAddresses = new ArrayList<String>();
		}
		
		// both the regular email address and the guardian email address are added to NA email address list
		if(!this.networkAuthenticatedEmailAddresses.contains(theEmailAddress)) {
			this.networkAuthenticatedEmailAddresses.add(theEmailAddress);
		}
	}
	
	// un-network authenticate the member email addresses that match the specified email address
	public void unNetworkAuthenticateEmailAddress(String theEmailAddress) {
		if(theEmailAddress == null) {
			return;
		} else {
			theEmailAddress = theEmailAddress.toLowerCase();
		}
		
		if(this.networkAuthenticatedEmailAddresses == null) {
			// there are no network authenticated email addresses, so return
			return;
		}
		
		if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(theEmailAddress)) {
			// after un-network authentication, email address is ALWAYS deactivated
			this.isEmailAddressActive = false;
		} else if(this.guardianEmailAddresses != null) {
			int index = 0;
			for(String ea : this.guardianEmailAddresses) {
				if(ea.equalsIgnoreCase(theEmailAddress)) {
					// after un-network authentication, email address is ALWAYS deactivated
					this.guardianIsEmailAddressActives.set(index, false);
					break;
				}
				index++;
			}
		}
		
		// remove email address from NA email address list if it is present
		if(this.networkAuthenticatedEmailAddresses.contains(theEmailAddress)) {
			this.networkAuthenticatedEmailAddresses.remove(theEmailAddress);
		}
	}
	
	// returns true if the PRIMARY email address is NA; false otherwise
	public Boolean getIsEmailAddressNetworkAuthenticated() {
		return isEmailAddressNetworkAuthenticated(this.emailAddress);
	}
	
	public Boolean isEmailAddressNetworkAuthenticated(String theEmailAddress) {
		if(theEmailAddress == null) {
			return false;
		} else {
			theEmailAddress = theEmailAddress.toLowerCase();
		}
		
		if(this.networkAuthenticatedEmailAddresses != null && this.networkAuthenticatedEmailAddresses.contains(theEmailAddress)) {
			return true;
		}
		return false;
	}
	
	public Boolean hasAnyNetworkAuthenticatedEmailAddresses() {
		if(this.networkAuthenticatedEmailAddresses != null && this.networkAuthenticatedEmailAddresses.size() > 0) {
			return true;
		}
		return false;
	}
	
	public Boolean isPhoneNumberSmsConfirmed(String thePhoneNumber) {
		if(thePhoneNumber == null || this.smsConfirmedPhoneNumbers == null) {return false;}
		
		if(this.smsConfirmedPhoneNumbers.contains(thePhoneNumber)) {
			return true;
		}
		return false;
	}
	
	// SMS confirm the specified SMS Address/phone number
	// theSmsAddress: either a 10 digit phone number or an SMS email address
	// Only the phoneNumber, not the entire SMSAddress, is stored in the smsConfirmedPhoneNumbers list.
	public void smsConfirmPhoneNumber(String theSmsAddress) {
		if(theSmsAddress == null) {return;}
		
		String smsPhoneNumber = theSmsAddress;
		if(theSmsAddress.contains("@")) {
			smsPhoneNumber = Utility.getPhoneNumberFromSmsEmailAddress(theSmsAddress);
			
			// A phoneNumber is being SMS confirmed. So that phoneNumber must already belong
			// to an individual in this membership -- either the primary or one of the guardians.
			// As part of this confirmation, we store the SMSAddress passed in. Later, when 
			// sending SMS messages, if the SMSAddress is defined, it is used to send an ad-free
			// message.
			if(this.phoneNumber != null && this.phoneNumber.equalsIgnoreCase(smsPhoneNumber)) {
				this.smsEmailAddress = theSmsAddress;
			} else if(this.guardianPhoneNumbers != null && this.guardianSmsEmailAddresses != null) {
				int index = 0;
				for(String gpn : this.guardianPhoneNumbers) {
					if(gpn.equalsIgnoreCase(smsPhoneNumber)) {
						this.guardianSmsEmailAddresses.set(index, theSmsAddress);
						break;
					}
					index++;
				}
			}
		}
		
		// if the SMS Confirm list doesn't exist yet, create it.
		if(this.smsConfirmedPhoneNumbers == null) {
			this.smsConfirmedPhoneNumbers = new ArrayList<String>();
		}
		
		// both the primary phone number and the guardian phone numbers are added to SMS Confirm list
		if(!this.smsConfirmedPhoneNumbers.contains(smsPhoneNumber)) {
			this.smsConfirmedPhoneNumbers.add(smsPhoneNumber);
		}
	}
	
	public Boolean containsEmailAddress(String theEmailAddress) {
		if(theEmailAddress == null) {return false;}
		
		if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(theEmailAddress)) {
			return true;
		} else if(this.guardianEmailAddresses != null) {
			int index = 0;
			for(String gea : this.guardianEmailAddresses) {
				if(gea.equalsIgnoreCase(theEmailAddress)) {
					return true;
				}
				index++;
			}
		}
		return false;
	}
	
	public Boolean containsPhoneNumber(String thePhoneNumber) {
		if(thePhoneNumber == null) {return false;}

		if(this.phoneNumber != null && this.phoneNumber.equalsIgnoreCase(thePhoneNumber)) {
			return true;
		} else if(this.guardianPhoneNumbers != null) {
			int index = 0;
			for(String gpn : this.guardianPhoneNumbers) {
				if(gpn.equalsIgnoreCase(thePhoneNumber)) {
					return true;
				}
				index++;
			}
		}
		return false;
	}
	
	public String getPhoneNumberByEmailAddress(String theEmailAddress) {
		String retPhoneNumber = null;
		if(theEmailAddress == null) {
			retPhoneNumber = null;
		} else if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(theEmailAddress)) {
			retPhoneNumber = this.phoneNumber;
		} else if(this.guardianEmailAddresses != null && this.guardianPhoneNumbers != null) {
			int index = 0;
			for(String gea : this.guardianEmailAddresses) {
				if(gea.equalsIgnoreCase(theEmailAddress)) {
					retPhoneNumber = this.guardianPhoneNumbers.get(index);
					
					// convert phone number from Big Table value to Java Normal value
					if(retPhoneNumber.length() == 0) {
						retPhoneNumber = null;
					}
					break;
				}
				index++;
			}
		}
		return retPhoneNumber;
	}
	
	public void setPhoneNumberByEmailAddress(String thePhoneNumber, String theEmailAddress) {
		if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(theEmailAddress)) {
			this.phoneNumber = thePhoneNumber;
		} else if(this.guardianEmailAddresses != null && this.guardianPhoneNumbers != null) {
			int index = 0;
			for(String gea : this.guardianEmailAddresses) {
				if(gea.equalsIgnoreCase(theEmailAddress)) {
					// convert phoneNumber from Java normal value to Big Table value
					if(thePhoneNumber == null) {thePhoneNumber = "";}
					
					this.guardianPhoneNumbers.set(index, thePhoneNumber);
					break;
				}
				index++;
			}
		}
	}
	
	public String getEmailAddressByPhoneNumber(String thePhoneNumber) {
		String retEmailAddress = null;
		if(thePhoneNumber == null) {
			retEmailAddress = null;
		} else if(this.phoneNumber != null && this.phoneNumber.equalsIgnoreCase(thePhoneNumber)) {
			retEmailAddress = this.emailAddress;
		} else if(this.guardianEmailAddresses != null && this.guardianPhoneNumbers != null) {
			int index = 0;
			for(String gpn : this.guardianPhoneNumbers) {
				if(gpn.equalsIgnoreCase(thePhoneNumber)) {
					retEmailAddress = this.guardianEmailAddresses.get(index);
					
					// convert email address from Big Table value to Java Normal value
					if(retEmailAddress.length() == 0) {
						retEmailAddress = null;
					}
					break;
				}
				index++;
			}
		}
		return retEmailAddress;
	}
	
	public void setEmailAddressByPhoneNumber(String theEmailAddress, String thePhoneNumber) {
		if(this.phoneNumber != null && this.phoneNumber.equalsIgnoreCase(thePhoneNumber)) {
			this.emailAddress = theEmailAddress;
		} else if(this.guardianEmailAddresses != null && this.guardianPhoneNumbers != null) {
			int index = 0;
			for(String gpn : this.guardianPhoneNumbers) {
				if(gpn.equalsIgnoreCase(thePhoneNumber)) {
					// convert emailAddress from Java normal value to Big Table value
					if(theEmailAddress == null) {theEmailAddress = "";}
					
					this.guardianEmailAddresses.set(index, theEmailAddress);
					break;
				}
				index++;
			}
		}
	}
	
	public void setUserIdByEmailAddress(String theUserId, String theEmailAddress) {
		if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(theEmailAddress)) {
			this.userId = theUserId;
		} else if(this.guardianEmailAddresses != null && this.guardianUserIds != null) {
			int index = 0;
			for(String gea : this.guardianEmailAddresses) {
				if(gea.equalsIgnoreCase(theEmailAddress)) {
					// convert theUserId from Java normal value to Big Table value
					if(theUserId == null) {theUserId = "";}
					
					this.guardianUserIds.set(index, theUserId);
					break;
				}
				index++;
			}
		}
	}
	
	public void setUserIdByPhoneNumber(String theUserId, String thePhoneNumber) {
		if(this.phoneNumber != null && this.phoneNumber.equalsIgnoreCase(thePhoneNumber)) {
			this.userId = theUserId;
		} else if(this.guardianPhoneNumbers != null && this.guardianUserIds != null) {
			int index = 0;
			for(String gpn : this.guardianPhoneNumbers) {
				if(gpn.equalsIgnoreCase(thePhoneNumber)) {
					// convert theUserId from Java normal value to Big Table value
					if(theUserId == null) {theUserId = "";}
					
					this.guardianUserIds.set(index, theUserId);
					break;
				}
				index++;
			}
		}
	}
	
	public String getUserIdByEmailAddress(String theEmailAddress) {
		String retUserId = null;
		if(theEmailAddress == null) {
			retUserId = null;
		} else if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(theEmailAddress)) {
			retUserId = this.userId;
		} else if(this.guardianEmailAddresses != null && this.guardianUserIds != null) {
			int index = 0;
			for(String gea : this.guardianEmailAddresses) {
				if(gea.equalsIgnoreCase(theEmailAddress)) {
					retUserId = this.guardianUserIds.get(index);
					
					// convert User ID from Big Table value to Java Normal value
					if(retUserId.length() == 0) {
						retUserId = null;
					}
					break;
				}
				index++;
			}
		}
		return retUserId;
	}
	
	public String getUserIdByPhoneNumber(String thePhoneNumber) {
		String retUserId = null;
		if(thePhoneNumber == null) {
			retUserId = null;
		} else if(this.phoneNumber != null && this.phoneNumber.equalsIgnoreCase(thePhoneNumber)) {
			retUserId = this.userId;
		} else if(this.guardianPhoneNumbers != null && this.guardianUserIds != null) {
			int index = 0;
			for(String gpn : this.guardianPhoneNumbers) {
				if(gpn.equalsIgnoreCase(thePhoneNumber)) {
					retUserId = this.guardianUserIds.get(index);
					
					// convert User ID from Big Table value to Java Normal value
					if(retUserId.length() == 0) {
						retUserId = null;
					}
					break;
				}
				index++;
			}
		}
		return retUserId;
	}
	
	public void setAutoArchiveDayCountByPhoneNumber(Integer theAutoArchiveDayCount, String thePhoneNumber) {
		if(this.phoneNumber != null && this.phoneNumber.equalsIgnoreCase(thePhoneNumber)) {
			this.autoArchiveDayCount = theAutoArchiveDayCount;
		} else if(this.guardianPhoneNumbers != null) {
			int index = 0;
			for(String gpn : this.guardianPhoneNumbers) {
				if(gpn.equalsIgnoreCase(thePhoneNumber)) {
					// convert theAutoArchiveDayCount from Java normal value to Big Table value
					if(theAutoArchiveDayCount == null) {theAutoArchiveDayCount = -1;}
					
					this.guardianAutoArchiveDayCounts.set(index, theAutoArchiveDayCount);
					break;
				}
				index++;
			}
		}
	}
	
	public void setAutoArchiveDayCountByEmailAddress(Integer theAutoArchiveDayCount, String theEmailAddress) {
		if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(theEmailAddress)) {
			this.autoArchiveDayCount = theAutoArchiveDayCount;
		} else if(this.guardianEmailAddresses != null) {
			int index = 0;
			for(String gea : this.guardianEmailAddresses) {
				if(gea.equalsIgnoreCase(theEmailAddress)) {
					// convert theAutoArchiveDayCount from Java normal value to Big Table value
					if(theAutoArchiveDayCount == null) {theAutoArchiveDayCount = -1;}
					
					this.guardianAutoArchiveDayCounts.set(index, theAutoArchiveDayCount);
					break;
				}
				index++;
			}
		}
	}
	
	public void setHasEmailMessageAccessEnabledByEmailAddress(String theEmailAddress, Boolean theHasEmailMessageAccess) {
		if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(theEmailAddress)) {
			this.hasEmailMessageAccessEnabled = theHasEmailMessageAccess;
		} else if(this.guardianEmailAddresses != null && this.guardianHasEmailMessageAccessEnabled != null) {
			int index = 0;
			for(String gea : this.guardianEmailAddresses) {
				if(gea.equalsIgnoreCase(theEmailAddress)) {
					this.guardianHasEmailMessageAccessEnabled.set(index, theHasEmailMessageAccess);
					break;
				}
				index++;
			}
		}
	}
	
	public void setHasEmailMessageAccessEnabledByPhoneNumber(String thePhoneNumber, Boolean theHasSmsMessageAccessEnabled) {
		if(this.phoneNumber != null && this.phoneNumber.equalsIgnoreCase(thePhoneNumber)) {
			this.hasSmsMessageAccessEnabled = theHasSmsMessageAccessEnabled;
		} else if(this.guardianPhoneNumbers != null && this.guardianHasSmsMessageAccessEnabled != null) {
			int index = 0;
			for(String gpn : this.guardianPhoneNumbers) {
				if(gpn.equalsIgnoreCase(thePhoneNumber)) {
					this.guardianHasSmsMessageAccessEnabled.set(index, theHasSmsMessageAccessEnabled);
					break;
				}
				index++;
			}
		}
	}
	
	public Integer getAutoArchiveDayCountByPhoneNumber(String thePhoneNumber) {
		if(thePhoneNumber == null) {return null;}
		
		Integer retAutoArchiveDayCount = null;
		if(this.phoneNumber != null && this.phoneNumber.equalsIgnoreCase(thePhoneNumber)) {
			retAutoArchiveDayCount = this.autoArchiveDayCount;
		} else if(this.guardianPhoneNumbers != null) {
			int index = 0;
			for(String gpn : this.guardianPhoneNumbers) {
				if(gpn.equalsIgnoreCase(thePhoneNumber)) {
					retAutoArchiveDayCount = this.guardianAutoArchiveDayCounts.get(index);
					
					// for guardian AutoArchiveDayCount list, "-1" is equivalent to null
					if(retAutoArchiveDayCount.equals(-1)) {
						retAutoArchiveDayCount = null;
					}
					break;
				}
				index++;
			}
		}
		return retAutoArchiveDayCount;
	}
	
	public Integer getAutoArchiveDayCountByEmailAddress(String theEmailAddress) {
		if(theEmailAddress == null) {return null;}
		
		Integer retAutoArchiveDayCount = null;
		if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(theEmailAddress)) {
			retAutoArchiveDayCount = this.autoArchiveDayCount;
		} else if(this.guardianEmailAddresses != null) {
			int index = 0;
			for(String gea : this.guardianEmailAddresses) {
				if(gea.equalsIgnoreCase(theEmailAddress)) {
					retAutoArchiveDayCount = this.guardianAutoArchiveDayCounts.get(index);
					
					// for guardian AutoArchiveDayCount list, "-1" is equivalent to null
					if(retAutoArchiveDayCount.equals(-1)) {
						retAutoArchiveDayCount = null;
					}
					break;
				}
				index++;
			}
		}
		return retAutoArchiveDayCount;
	}
	
	// deactivate email addresses that match the specified email address
	public void deactivateEmailAddress(String theEmailAddress) {
		if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(theEmailAddress)) {
			this.isEmailAddressActive = false;
		}
		
		if(this.guardianEmailAddresses == null || this.networkAuthenticatedEmailAddresses == null ||
		   this.networkAuthenticatedEmailAddresses.size() == 0 || this.guardianIsEmailAddressActives == null) {
			// If guardian email address activation list hasn't even been created yet, there is nothing to do
			// because deactivation only makes sense only after network authentication (before that, no
			// emails can be sent anyway).
			return;
		}
		
		int index = 0;
		for(String ea : this.guardianEmailAddresses) {
			if(ea.equalsIgnoreCase(theEmailAddress)) {
				this.guardianIsEmailAddressActives.set(index, false);
			}
			index++;
		}
	}
	
	public Boolean isGuardianNetworkAuthenticated(String theEmailAddress) {
		if(theEmailAddress == null) {
			return false;
		} else {
			theEmailAddress = theEmailAddress.toLowerCase();
		}
		
		if(this.networkAuthenticatedEmailAddresses == null) {
			return false;
		}
		
		if(this.networkAuthenticatedEmailAddresses.contains(theEmailAddress)) {
			return true;
		}
		
		return false;
	}
	
	public Boolean isGuardian(String theEmailAddress) {
		if(theEmailAddress == null || theEmailAddress.length() == 0 || 
				this.guardianEmailAddresses == null || this.guardianEmailAddresses.size() == 0) {
			return false;
		}
		
		for(String ea : this.guardianEmailAddresses) {
			if(theEmailAddress.equalsIgnoreCase(ea)) {
				return true;
			}
		}
		
		return false;
	}
	
	// Returns the full name based on email address. Name is either 'primary' or one of the guardians.
	// Since guardian information optional, if there only a first name or last name specified, then that is returned.
	// If there is no guardian name specified, empty string is returned.
	public String getFullNameByEmailAddress(String theEmailAddress) {
		if(theEmailAddress == null) {
			// no work to do
			return null;
		}
		
		//log.debug("getFullNameByEmailAddress(): input param theEmailAddress = " + theEmailAddress);
		if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(theEmailAddress)) {
			//log.debug("getFullNameByEmailAddress(): return primary email address");
			return getDisplayName(this.firstName, this.lastName, this.emailAddress, this.phoneNumber);
		}
		
		if(this.guardianFirstNames == null && this.guardianLastNames == null) {
			return theEmailAddress;
		}
		
		int index = 0;
		String gFirstName = null;
		String gLastName = null;
		boolean wasNameFound = false;
		for(String ea : this.guardianEmailAddresses) {
			if(ea.equalsIgnoreCase(theEmailAddress)) {
				gFirstName = this.guardianFirstNames.get(index);
				gLastName = this.guardianLastNames.get(index);
				wasNameFound = true;
				break;
			}
			index++;
		}
		
		if(wasNameFound) {
			if(gFirstName.length() == 0 && gLastName.length() == 0) {
				return theEmailAddress;
			}
			// both the first and last names for a guardian are optional, but they are guaranteed to be non-null.
			StringBuffer sb = new StringBuffer("");
			sb.append(gFirstName); // first name can be empty, but no harm adding it
			if(gLastName.length() > 0) {
				sb.append(" ");
				sb.append(gLastName);
			}
			//log.debug("getFullNameByEmailAddress(): return email address = " + sb.toString());
			return sb.toString();
		} else {
			return "";
		}
	}
	
	public String getFirstNameByEmailAddress(String theEmailAddress) {
		if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(theEmailAddress)) {
			return this.firstName;
		}
		
		if(this.guardianFirstNames == null) {
			return "";
		}
		
		int index = 0;
		String gFirstName = null;
		boolean wasNameFound = false;
		for(String ea : this.guardianEmailAddresses) {
			if(ea.equalsIgnoreCase(theEmailAddress)) {
				gFirstName = this.guardianFirstNames.get(index);
				// convert firstName from Big Table value to Java Normal value
				if(gFirstName.length() == 0) {
					gFirstName = null;
				}
				wasNameFound = true;
				break;
			}
			index++;
		}
		
		if(wasNameFound) {
			return gFirstName;
		} else {
			return "";
		}
	}
	
	public void setFirstNameByEmailAddress(String theFirstName, String theEmailAddress) {
		if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(theEmailAddress)) {
			this.firstName = theFirstName;
		} else if(this.guardianEmailAddresses != null && this.guardianFirstNames != null) {
			int index = 0;
			for(String gea : this.guardianEmailAddresses) {
				if(gea.equalsIgnoreCase(theEmailAddress)) {
					// convert firstName from Java normal value to Big Table value
					if(theFirstName == null) {theFirstName = "";}
					
					this.guardianFirstNames.set(index, theFirstName);
					break;
				}
				index++;
			}
		}
	}
	
	public String getFirstNameByPhoneNumber(String thePhoneNumber) {
		if(thePhoneNumber == null) {return "";}
		
		if(this.phoneNumber != null && this.phoneNumber.equalsIgnoreCase(thePhoneNumber)) {
			return this.firstName;
		}
		
		// verify both guardian lists exists, though they should all be the same size or all null
		if(this.guardianPhoneNumbers == null || this.guardianFirstNames == null) {
			return "";
		}
		
		int index = 0;
		String gFirstName = null;
		boolean wasNameFound = false;
		for(String pn : this.guardianPhoneNumbers) {
			if(pn.equalsIgnoreCase(thePhoneNumber)) {
				gFirstName = this.guardianFirstNames.get(index);
				// convert firstName from Big Table value to Java Normal value
				if(gFirstName.length() == 0) {
					gFirstName = null;
				}
				wasNameFound = true;
				break;
			}
			index++;
		}
		
		if(wasNameFound) {
			return gFirstName;
		} else {
			return "";
		}
	}
	
	public void setFirstNameByPhoneNumber(String theFirstName, String thePhoneNumber) {
		if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(thePhoneNumber)) {
			this.firstName = theFirstName;
		} else if(this.guardianPhoneNumbers != null && this.guardianFirstNames != null) {
			int index = 0;
			for(String gpn : this.guardianPhoneNumbers) {
				if(gpn.equalsIgnoreCase(thePhoneNumber)) {
					// convert firstName from Java normal value to Big Table value
					if(theFirstName == null) {theFirstName = "";}
					
					this.guardianFirstNames.set(index, theFirstName);
					break;
				}
				index++;
			}
		}
	}
	
	public String getLastNameByEmailAddress(String theEmailAddress) {
		if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(theEmailAddress)) {
			return this.lastName;
		}
		
		if(this.guardianLastNames == null) {
			return "";
		}
		
		int index = 0;
		String gLastName = null;
		boolean wasNameFound = false;
		for(String ea : this.guardianEmailAddresses) {
			if(ea.equalsIgnoreCase(theEmailAddress)) {
				gLastName = this.guardianLastNames.get(index);
				// convert lastName from Big Table value to Java Normal value
				if(gLastName.length() == 0) {
					gLastName = null;
				}
				wasNameFound = true;
				break;
			}
			index++;
		}
		
		if(wasNameFound) {
			return gLastName;
		} else {
			return "";
		}
	}
	
	public void setLastNameByEmailAddress(String theLastName, String theEmailAddress) {
		if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(theEmailAddress)) {
			this.lastName = theLastName;
		} else if(this.guardianEmailAddresses != null && this.guardianLastNames != null) {
			int index = 0;
			for(String gea : this.guardianEmailAddresses) {
				if(gea.equalsIgnoreCase(theEmailAddress)) {
					// convert lastName from Java normal value to Big Table value
					if(theLastName == null) {theLastName = "";}
					
					this.guardianLastNames.set(index, theLastName);
					break;
				}
				index++;
			}
		}
	}
	
	public String getLastNameByPhoneNumber(String thePhoneNumber) {
		if(thePhoneNumber == null) {return "";}
		
		if(this.phoneNumber != null && this.phoneNumber.equalsIgnoreCase(thePhoneNumber)) {
			return this.lastName;
		}
		
		// verify both guardian lists exists, though they should all be the same size or all null
		if(this.guardianPhoneNumbers == null || this.guardianLastNames == null) {
			return "";
		}
		
		int index = 0;
		String gLastName = null;
		boolean wasNameFound = false;
		for(String pn : this.guardianPhoneNumbers) {
			if(pn.equalsIgnoreCase(thePhoneNumber)) {
				gLastName = this.guardianLastNames.get(index);
				// convert lastName from Big Table value to Java Normal value
				if(gLastName.length() == 0) {
					gLastName = null;
				}
				wasNameFound = true;
				break;
			}
			index++;
		}
		
		if(wasNameFound) {
			return gLastName;
		} else {
			return "";
		}
	}
	
	public void setLastNameByPhoneNumber(String theLastName, String thePhoneNumber) {
		if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(thePhoneNumber)) {
			this.lastName = theLastName;
		} else if(this.guardianPhoneNumbers != null && this.guardianLastNames != null) {
			int index = 0;
			for(String gpn : this.guardianPhoneNumbers) {
				if(gpn.equalsIgnoreCase(thePhoneNumber)) {
					// convert lastName from Java normal value to Big Table value
					if(theLastName == null) {theLastName = "";}
					
					this.guardianLastNames.set(index, theLastName);
					break;
				}
				index++;
			}
		}
	}
	
	// returns true if the person within this membership specified by the phone number is network authenticated; false otherwise
	public Boolean isNetworkAuthenticatedByPhoneNumber(String thePhoneNumber) {
		if(thePhoneNumber == null) {return false;}
		
		if(this.phoneNumber != null && this.phoneNumber.equalsIgnoreCase(thePhoneNumber)) {
			if(this.emailAddress != null && isEmailAddressNetworkAuthenticated(this.emailAddress)) {
				return true;
			}
			return false;
		}
		
		// verify both guardian lists exists, though they should all be the same size or all null
		if(this.guardianPhoneNumbers == null || this.guardianEmailAddresses == null) {
			return false;
		}
		
		int index = 0;
		for(String pn : this.guardianPhoneNumbers) {
			if(pn.equalsIgnoreCase(thePhoneNumber)) {
				if(isEmailAddressNetworkAuthenticated(this.guardianEmailAddresses.get(index))) {
					return true;
				}
				return false;
			}
			index++;
		}
		return false;
	}
	
	public String getSmsEmailAddressByPhoneNumber(String thePhoneNumber) {
		if(thePhoneNumber == null) {return null;}
		
		if(this.phoneNumber != null && this.phoneNumber.equalsIgnoreCase(thePhoneNumber)) {
			return this.smsEmailAddress;
		}
		
		// verify both guardian lists exists, though they should all be the same size or all null
		if(this.guardianPhoneNumbers == null || this.guardianSmsEmailAddresses == null) {
			return null;
		}
		
		int index = 0;
		String gSmsEmailAddress = null;
		boolean wasFound = false;
		for(String pn : this.guardianPhoneNumbers) {
			if(pn.equalsIgnoreCase(thePhoneNumber)) {
				gSmsEmailAddress = this.guardianSmsEmailAddresses.get(index);
				// convert SmsEmailAddress from Big Table value to Java Normal value
				if(gSmsEmailAddress.length() == 0) {
					gSmsEmailAddress = null;
				}
				wasFound = true;
				break;
			}
			index++;
		}
		
		if(wasFound) {
			return gSmsEmailAddress;
		} else {
			return null;
		}
	}
	
	// Returns true if the specified user participates in this membership; false otherwise.
	// CAREFUL how this method is used since it matches membership EAs and PNs that are NOT CONFIRMED.
	public Boolean isUserParticipant(User theUser) {
		if(theUser == null) {return false;}

		if(theUser.getEmailAddress() != null) {
			if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(theUser.getEmailAddress())) {
				return true;
			}
			
			if(this.guardianEmailAddresses != null) {
				for(String gea : this.guardianEmailAddresses) {
					if(gea.equalsIgnoreCase(theUser.getEmailAddress())) {
						return true;
					}
				}
			}
		}
		
		if(theUser.getPhoneNumber() != null) {
			if(this.phoneNumber != null && this.phoneNumber.equalsIgnoreCase(theUser.getPhoneNumber())) {
				return true;
			}
			
			if(this.guardianPhoneNumbers != null) {
				for(String gpn : this.guardianPhoneNumbers) {
					if(gpn.equalsIgnoreCase(theUser.getPhoneNumber())) {
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	public List<String> getNetworkAuthenticatedActiveEmailAddresses() {
		List<String> naEmailAddresses = new ArrayList<String>();
		if(this.emailAddress != null && isEmailAddressNetworkAuthenticated(this.emailAddress) &&
				this.isEmailAddressActive != null && this.isEmailAddressActive.booleanValue()) {
			naEmailAddresses.add(this.emailAddress);
		}
		
		if(this.guardianEmailAddresses != null && this.networkAuthenticatedEmailAddresses != null) {
			int index = 0;
			for(String gea : this.guardianEmailAddresses) {
				if( this.networkAuthenticatedEmailAddresses.contains(gea) &&
				    this.guardianIsEmailAddressActives.get(index)) {
					naEmailAddresses.add(gea);
				}
				index++;
			}
		}
		
		return naEmailAddresses;
	}
	
	public List<String> getEmailAddresses() {
		List<String> allEmailAddresses = new ArrayList<String>();
		if(this.emailAddress != null) {
			allEmailAddresses.add(this.emailAddress);
		}
		
		if(this.guardianEmailAddresses != null) {
			for(String ea : this.guardianEmailAddresses) {
				if(ea != null && ea.length() > 0) {
					allEmailAddresses.add(ea);
				}
			}
		}
		
		return allEmailAddresses;
	}
	
	public Boolean isGenderValid(String theGender) {
		if( theGender.equalsIgnoreCase(FEMALE_GENDER) ||
			theGender.equalsIgnoreCase(MALE_GENDER)) {
			return true;
		}
		return false;
	}
	
	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getZipcode() {
		return zipcode;
	}

	public void setZipcode(String zipcode) {
		this.zipcode = zipcode;
	}
    
	public List<String> getNetworkAuthenticatedEmailAddresses() {
		return networkAuthenticatedEmailAddresses;
	}

	public void setNetworkAuthenticatedEmailAddresses(
			List<String> networkAuthenticatedEmailAddresses) {
		this.networkAuthenticatedEmailAddresses = networkAuthenticatedEmailAddresses;
	}
    
	public List<String> getSmsConfirmedPhoneNumbers() {
		return smsConfirmedPhoneNumbers;
	}

	public void setSmsConfirmedPhoneNumbers(
			List<String> smsConfirmedPhoneNumbers) {
		this.smsConfirmedPhoneNumbers = smsConfirmedPhoneNumbers;
	}

	public Boolean getIsMarkedForDeletion() {
		return isMarkedForDeletion;
	}

	public void setIsMarkedForDeletion(Boolean isMarkedForDeletion) {
		this.isMarkedForDeletion = isMarkedForDeletion;
	}

	public Date getMarkedForDeletionOn() {
		return markedForDeletionOn;
	}

	public void setMarkedForDeletionOn(Date markedForDeletionOn) {
		this.markedForDeletionOn = markedForDeletionOn;
	}

	public String getMarkedForDeletionRequester() {
		return markedForDeletionRequester;
	}

	public void setMarkedForDeletionRequester(String markedForDeletionRequester) {
		this.markedForDeletionRequester = markedForDeletionRequester;
	}
    
	private List<String> getGuardianFirstNames() {
		return guardianFirstNames;
	}

	private void setGuardianFirstNames(List<String> guardianFirstNames) {
		this.guardianFirstNames = guardianFirstNames;
	}

	private List<String> getGuardianLastNames() {
		return guardianLastNames;
	}

	private void setGuardianLastNames(List<String> guardianLastNames) {
		this.guardianLastNames = guardianLastNames;
	}

	// TODO public for migration only
	public List<String> getGuardianSmsEmailAddresses() {
		return guardianSmsEmailAddresses;
	}

	public void setGuardianSmsEmailAddresses(List<String> guardianSmsEmailAddresses) {
		this.guardianSmsEmailAddresses = guardianSmsEmailAddresses;
	}

	private Integer getAutoArchiveDayCount() {
		return autoArchiveDayCount;
	}

	public void setAutoArchiveDayCount(Integer autoArchiveDayCount) {
		this.autoArchiveDayCount = autoArchiveDayCount;
	}

	public String getPhotoBase64() {
		return this.photoBase64 == null? null : this.photoBase64.getValue();
	}

	public void setPhotoBase64(String photo) {
		this.photoBase64 = new Text(photo);
	}

	////////////////////
	// STATIC METHODS //
	////////////////////
    
	public static boolean isValidParticipant(String theParticipantRole) {
		if( theParticipantRole.equalsIgnoreCase(CREATOR_PARTICIPANT) ||
			theParticipantRole.equalsIgnoreCase(COORDINATOR_PARTICIPANT) || 
			theParticipantRole.equalsIgnoreCase(MEMBER_PARTICIPANT) ||
			theParticipantRole.equalsIgnoreCase(FAN_PARTICIPANT)) {
				return true;
			}
			return false;
	}

	@SuppressWarnings("unchecked")
	// Returns the list of network authenticated members on the specified team that match the specified email address.
	// If no matches, list is empty.
	public static List<Member> getMemberShipsWithEmailAddress(String theEmailAddress, Team theTeam) {
		if(theEmailAddress == null) {return null;}
		
    	EntityManager em = EMF.get().createEntityManager();
    	
    	List<Member> memberships = null;
		try {
			memberships = (List<Member>)em.createNamedQuery("Member.getByNetworkAuthenticatedEmailAddressAndTeam")
				.setParameter("team", theTeam)
				.setParameter("emailAddress", theEmailAddress.toLowerCase())
				.getResultList();
			// Need to access the members before return or I get an "Object Detached" error
			for(Member m : memberships) {
				m.getAge();
				log.debug("Member::getMemberShipsWithEmailAddress() primary of membership found = " + m.getFullName());
			}
		} catch (Exception e) {
    		log.exception("Member:getMemberHsipsWithEmailAddress:Exception", "Query Member.getByNetworkAuthenticatedEmailAddressAndTeam failed", e);
    	} finally {
    		em.close();
    	}
    	
    	// don't want to return null, but rather an empty list
    	if(memberships == null) {
    		memberships = new ArrayList<Member>();
    	}
    	return memberships;
    }
    
	// ::MEMBER::USER::
	// Member is now NA or confirmed.  Synch up with NA/confirmed user if it exists.
	// If user synch successful, returns true, otherwise returns false.
	// theMemberEmailAddressPhoneNumber: Can be an email address or a phone number. If it is an email address,
	//                                   then the user is matched using NA email address. If it is a phone number,
	//                                   then the user is matched using an SMS confirmed phone number.
    public static boolean synchUpWithAuthorizedUser(Member theMember, String theMemberEmailAddressPhoneNumber) {
    	log.debug("synchUpWithAuthorizedUser entered: theMemberEmailAddressPhoneNumber = " + theMemberEmailAddressPhoneNumber);
    	EntityManager em = EMF.get().createEntityManager();
    	User associatedUser = null;
    	Team newTeamForUser = null;
    	boolean userSynchSuccessful = false;
    	try {
    		if(Utility.isPhoneNumber(theMemberEmailAddressPhoneNumber)) {
    			associatedUser = (User)em.createNamedQuery("User.getByPhoneNumberAndIsSmsConfirmed")
				.setParameter("phoneNumber", theMemberEmailAddressPhoneNumber)
				.setParameter("isSmsConfirmed", true)
				.getSingleResult();
    		} else {
    			associatedUser = (User)em.createNamedQuery("User.getByEmailAddressAndIsNetworkAuthenticated")
				.setParameter("emailAddress", theMemberEmailAddressPhoneNumber.toLowerCase())
				.setParameter("isNetworkAuthenticated", true)
				.getSingleResult();
    		}
			
			// User exists, so update the member object.  Need to re-get the member within this new transaction
			try {
				em.getTransaction().begin();
				Member member = (Member)em.createNamedQuery("Member.getByKey")
					.setParameter("key", theMember.getKey())
					.getSingleResult();
				
				// the reason for all this work -- set the user ID in the member to associate the two entities
				member.setUserId(KeyFactory.keyToString(associatedUser.getKey()));
				member.setAutoArchiveDayCount(associatedUser.getAutoArchiveDayCount());
				userSynchSuccessful = true;

				// save the new team the user will now be associated with -- later in the function
				newTeamForUser = member.getTeam();
				
				em.getTransaction().commit();
				log.debug("new member with email address/phone number " + theMemberEmailAddressPhoneNumber + " is now synched with user = " + associatedUser.getFullName());
			} catch (NoResultException e) {
	    		log.exception("Member:synchUpWithAuthorizedUser:NoResultException", "could not get member using member key that should be good", e);
			}  finally {
			    if (em.getTransaction().isActive()) {
			    	em.getTransaction().rollback();
			    }
			    em.close();
			}
    	} catch (NoResultException e) {
			// THIS IS NOT AN ERROR -- there may be no user entity associated with the member.
		} catch (NonUniqueResultException e) {
    		log.exception("Member:synchUpWithAuthorizedUser:NonUniqueResultException", "two or more users with same email address/phone number", e);
		}
		
    	if(newTeamForUser != null) {
        	EntityManager em3 = EMF.get().createEntityManager();
        	try {
        		em3.getTransaction().begin();
        		// re-get the user inside this transaction
				User withinTransactionUser = (User)em3.createNamedQuery("User.getByKey")
					.setParameter("key", associatedUser.getKey())
					.getSingleResult();
				withinTransactionUser.addTeam(newTeamForUser);
				em3.getTransaction().commit();
        	} catch (NoResultException e) {
        		log.exception("Member:synchUpWithAuthorizedUser:NoResultException2", "", e);
            	userSynchSuccessful = false;
    		} catch (NonUniqueResultException e) {
        		log.exception("Member:synchUpWithAuthorizedUser:NonUniqueResultException2", "", e);
    			userSynchSuccessful = false;
    		} finally {
    		    if (em3.getTransaction().isActive()) {
    		    	em3.getTransaction().rollback();
    		    }
    		    em3.close();
    		}
    	}

    	// Update the recipient entities associated with this membership. Recipient email address field holds both
    	// email addresses and phone numbers, so the code below works for both.
    	// Must do this in a separate transaction.
		List<Recipient> recipientsPendingDelivery = new ArrayList<Recipient>();
		EntityManager em2 = EMF.get().createEntityManager();
    	try {
			List<Recipient> recipients = (List<Recipient>)em2.createNamedQuery("Recipient.getByMemberIdAndEmailAddress")
				.setParameter("memberId", KeyFactory.keyToString(theMember.getKey()))
				.setParameter("emailAddress", theMemberEmailAddressPhoneNumber.toLowerCase())
				.getResultList();
			for(Recipient r : recipients) {
		    	em2.getTransaction().begin();
	    		Recipient singleRecipient = (Recipient)em2.createNamedQuery("Recipient.getByKey")
	    			.setParameter("key", r.getKey())
	    			.getSingleResult();
	    		
	    		if(singleRecipient.getStatus() != null && singleRecipient.getStatus().equalsIgnoreCase(Recipient.PENDING_NETWORK_AUTHENTICATION_STATUS)) {
	    			singleRecipient.setStatus(Recipient.SENT_STATUS);
					recipientsPendingDelivery.add(r);
					
					// need to access recipient messageThread here so it is available to send emails after the transaction commits
					r.getMessageThread();
				}
				if(associatedUser != null) {
					singleRecipient.setUserId(KeyFactory.keyToString(associatedUser.getKey()));
				}
				em2.getTransaction().commit();
			}
    	} catch (Exception e) {
    		log.exception("Member:synchUpWithAuthorizedUser:Exception", "", e);
    		userSynchSuccessful = false;
    	} finally {
		    if (em2.getTransaction().isActive()) {
		    	em2.getTransaction().rollback();
		    }
		    em2.close();
		}
    	
    	// send emails (and SMSs) now that the transaction is closed
    	for(Recipient r : recipientsPendingDelivery) {
    		log.debug("recipient pending delivery email address = " + r.getToEmailAddress());
        	Emailer.send(r.getToEmailAddress(), r.getSubject(), r.getMessageThread().getMessage(), Emailer.NO_REPLY);
    	}
    	
    	return userSynchSuccessful;
    }
    
	// Update the autoArchiveDayCount attribute in memberships of the specified user
    public static void updateMemberShipsWithNewAutoArchiveDayCount(User theUser) {
    	EntityManager emMember = EMF.get().createEntityManager();
    	
    	List<Member> memberships = null;
		try {
			memberships = (List<Member>)emMember.createNamedQuery("Member.getByNetworkAuthenticatedEmailAddress")
				.setParameter("emailAddress", theUser.getEmailAddress())
				.getResultList();
			
    		for(Member m : memberships) {
    			emMember.getTransaction().begin();
    			//log.debug("Setting new member autoArchiveDayCount = " + theUser.getAutoArchiveDayCount() + " for EA = " + theUser.getEmailAddress());
    			m.setAutoArchiveDayCountByEmailAddress(theUser.getAutoArchiveDayCount(), theUser.getEmailAddress());
    			emMember.getTransaction().commit();
    		}
    		log.debug("autoArchiveDayCount adjusted: memberships count = " + memberships.size());
		} catch (Exception e) {
    		log.exception("Member:updateMemberShipsWithNewAutoArchiveDayCount:Exception", "", e);
    	} finally {
    		emMember.close();
    	}
    }
    
    public String getPrimaryDisplayName() {
		return getDisplayName(this.firstName, this.lastName, this.emailAddress, this.getPhoneNumber());
    }
    
	// Update the autoArchiveDayCount attribute in memberships of the specified user
    public static void updateMemberShipsWithNewPhoto(User theUser, String thePhotoBase64, String theThumbNailBase64) {
    	EntityManager emMember = EMF.get().createEntityManager();
    	
    	List<Member> memberships = null;
		try {
			memberships = (List<Member>)emMember.createNamedQuery("Member.getByNetworkAuthenticatedEmailAddress")
				.setParameter("emailAddress", theUser.getEmailAddress())
				.getResultList();
			
    		for(Member m : memberships) {
    			emMember.getTransaction().begin();
    			if(m.getPhotoBase64() == null) m.setPhotoBase64(thePhotoBase64);
    			if(m.getThumbNailBase64() == null) m.setThumbNailBase64(theThumbNailBase64);
    			emMember.getTransaction().commit();
    		}
    		log.debug("number of " + theUser.getFullName() + " memberships photos updated = " + memberships.size());
		} catch (Exception e) {
    		log.exception("Member:updateMemberShipsWithNewPhoto:Exception", "", e);
    	} finally {
    		emMember.close();
    	}
    }
    
    // non-static method that calls the static method below
    // by default, abbreviated display names are returned.
    public String getDisplayName() {
    	return getDisplayName(this.getFirstName(), this.getLastName(), this.getEmailAddress(), this.getPhoneNumber(), true);
    }
    
    public Boolean hasAssociatedUser() {
    	if(this.userId != null && this.userId.length() > 0) {
    		return true;
    	}
    	return false;
    }
    
	// ::BUSINESS_RULE::
	// Guardian email address has to be unique for this member
    // If theKeyOfGuardianBeingModified is NULL, then this is really an add, so no need to skip any checks
	public Boolean isNewGuardianEmailAddressAcceptable(String theKeyOfGuardianBeingModified, String theEmailAddress) {
		if(theEmailAddress == null) {return true;}
		
		// a blank email address really means no email address at all so unique check is not applicable
		if(theEmailAddress.length() == 0) {return true;}
		
		// see if it matches primary email address
		if(this.emailAddress != null && this.emailAddress.equalsIgnoreCase(theEmailAddress)) {
			return false;
		}
		
		// Now check if it matches an existing guardian email address
		if(this.guardianEmailAddresses != null) {
			Integer indexOfGuardianBeingModified = null;
			if(theKeyOfGuardianBeingModified != null) {
				indexOfGuardianBeingModified = getGuardianIndexByKey(theKeyOfGuardianBeingModified);
				if(indexOfGuardianBeingModified == null) {
					log.debug("isNewGuardianEmailAddressAcceptable() failed because it could not find guardian with matching key");
					return false;
				}
			}

			for(int i=0; i<this.guardianEmailAddresses.size(); i++) {
				// skip the guardian being modified, if one was specified
				if(indexOfGuardianBeingModified != null && indexOfGuardianBeingModified.equals(i)) {continue;}
				
				String gea = this.guardianEmailAddresses.get(i);
				if(gea != null && gea.equalsIgnoreCase(theEmailAddress)) {
					return false;
				}
			}
				
		}
		return true;
	}
    
	// ::BUSINESS_RULE::
	// New primary email address cannot match existing guardian email addresses
	public Boolean isNewPrimaryEmailAddressAcceptable(String theEmailAddress) {
		if(theEmailAddress == null) {return true;}
		
		// Check if it matches an existing guardian email address
		if(this.guardianEmailAddresses != null) {
			for(String gea : this.guardianEmailAddresses) {
				if(gea != null && gea.equalsIgnoreCase(theEmailAddress)) {
					return false;
				}
			}
		}
		return true;
	}
    
    public static String getDisplayName(String theFirstName, String theLastName, String theEmailAddress, String thePhoneNumber) {
    	return getDisplayName(theFirstName, theLastName, theEmailAddress, thePhoneNumber, false);
    }
    
    public static String getDisplayName(String theFirstName, String theLastName, String theEmailAddress, String thePhoneNumber, Boolean theAbbreviate) {
		String displayName = theEmailAddress;
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
		} else if(thePhoneNumber != null && thePhoneNumber.trim().length() > 0) {
			displayName = Utility.formatPhoneNumber(thePhoneNumber);
		}
		return displayName;
    }
    
	public static Member getMember(String theMemberId) {
		EntityManager em = EMF.get().createEntityManager();
		Member member = null;
		
		Key memberKey = null;
		try {
			memberKey = KeyFactory.stringToKey(theMemberId);
		} catch (Exception e1) {
    		log.exception("Member:getMember:Exception", "", e1);
			return null;
		}
		
		try {
			member = (Member)em.createNamedQuery("Member.getByKey")
				.setParameter("key", memberKey)
				.getSingleResult();
		} catch (NoResultException e) {
    		log.exception("Member:getMember:Exception", "member could not be retrieved using member key", e);
		} catch (NonUniqueResultException e) {
    		log.exception("Member:getMember:NonUniqueResultException", "member could not be retrieved using member key", e);
		} finally {
			em.close();
		}
		
		return member;
	}
    
	public static Boolean isPhoneNumberSmsConfirmedInAnyMember(String thePhoneNumber) {
		EntityManager em = EMF.get().createEntityManager();
		List<Member> smsConfirmedMemberships = null;
		
		try {
			smsConfirmedMemberships = (List<Member>)em.createNamedQuery("Member.getBySmsConfirmedPhoneNumber")
				.setParameter("phoneNumber", thePhoneNumber)
				.getResultList();
		log.debug("number of memberships with phone number " + thePhoneNumber + " already confirmed via SMS = " + smsConfirmedMemberships.size());
		} catch (Exception e) {
    		log.exception("Member:isPhoneNumberSmsConfirmedInAnyMember:Exception", "", e);
		} finally {
			em.close();
		}
		
		if(smsConfirmedMemberships != null && smsConfirmedMemberships.size() > 0) {
			return true;
		}
		return false;
	}
	
	//::SMS::EVENT::
	// Supports SMS confirmation of primary member and guardians
	// theSmsAddress: either a 10 digit phone number or an SMS email address
	// ----------------------------------------------------------------------
	// NOTE: A SMS join request is NOT a response. Member receives SMS from coordinator phone instructing
	// ----  them to originate a SMS. Because of this, there is no Recipient entity so "matching" must
	//       be done against members with phone numbers
	public static String confirmNewMemberViaSms(String theSmsAddress) {
		EntityManager emMember = EMF.get().createEntityManager();
		List<String> teamNames = new ArrayList<String>();
		List<Member> allMemberships = new ArrayList<Member>();
		List<Member> newlyConfirmedMemberships = new ArrayList<Member>();
		
		String smsPhoneNumber = theSmsAddress;
		if(theSmsAddress.contains("@")) {
			smsPhoneNumber = Utility.getPhoneNumberFromSmsEmailAddress(theSmsAddress);
		}
		
		// It is possible -- though maybe not likely -- that this member has multiple outstanding invitations. This one
		// response confirms all outstanding membership requests.
		//log.debug("phone number to match = " + thePhoneNumber);
		int membershipsConvertedToNewSmsCount = 0;
		try {
			List<Member> memberships = (List<Member>)emMember.createNamedQuery("Member.getByPhoneNumber")
				.setParameter("phoneNumber", smsPhoneNumber)
				.getResultList();
			List<Member> guardianMemberships = (List<Member>)emMember.createNamedQuery("Member.getByGuardianPhoneNumber")
				.setParameter("guardianPhoneNumber", smsPhoneNumber)
				.getResultList();
			
			log.debug("number of matching phoneNumbers found = " + memberships.size());
			log.debug("number of matching guardianPhoneNumbers found = " + guardianMemberships.size());
			// create one big membership list for processing
			allMemberships.addAll(memberships);
			allMemberships.addAll(guardianMemberships);
			
			for(Member m : allMemberships) {
    			Boolean isSmsConfirmed = m.isPhoneNumberSmsConfirmed(smsPhoneNumber);
    			if(!isSmsConfirmed) {
        			m.smsConfirmPhoneNumber(theSmsAddress);
        			m.setHasEmailMessageAccessEnabledByPhoneNumber(smsPhoneNumber, true);
        			teamNames.add(m.getTeam().getTeamName());
        			newlyConfirmedMemberships.add(m);
    			} else {
    				// Already confirmed, but it may only be the Zeep Mobile confirmation. Calling smsConfirmPhoneNumber()
    				// will set the SMS email address if the address passed in is an email address. Even if the membership
    				// already contains the smsEmailAddress, this method is idempotent
    				// TODO long term, this call can be removed once all Zeep Mobile SMS are converted over
    				m.smsConfirmPhoneNumber(theSmsAddress);
    				membershipsConvertedToNewSmsCount++;
    			}
    		}
    		log.debug("number of memberships with phone number " + smsPhoneNumber + " confirmed via SMS = " + newlyConfirmedMemberships.size() + " converted to new SMS " + membershipsConvertedToNewSmsCount);
		} catch (Exception e) {
    		log.exception("Member:confirmNewMemberViaSms:Exception", "", e);
    		return UserInterfaceMessage.SERVER_ERROR;
    	} finally {
    		emMember.close();
    	}
    	
    	 if(teamNames.size() == 0) {
    		 if(membershipsConvertedToNewSmsCount > 0) {
    			 return UserInterfaceMessage.SMS_CONVERT_SUCCESS;	
    		 }
    		 
    		 // need to check if this phone number has already been SMS confirmed
    		EntityManager emSmsMember = EMF.get().createEntityManager();
    		try {
    			List<Member> smsConfirmedmemberships = (List<Member>)emSmsMember.createNamedQuery("Member.getBySmsConfirmedPhoneNumber")
    				.setParameter("phoneNumber", smsPhoneNumber)
    				.getResultList();
    			log.debug("number of memberships with phone number " + smsPhoneNumber + " already confirmed via SMS = " + smsConfirmedmemberships.size());
    		
    			if(smsConfirmedmemberships.size() > 0) {
    	    		return UserInterfaceMessage.ALREADY_MEMBER;
    			} else {
    	    		return UserInterfaceMessage.NO_MEMBERSHIPS_PENDING;
    			}
    		} catch (Exception e) {
    			// ::ROBUSTNESS:: do NOT return error message. Code can survive this error.
        		log.exception("Member:confirmNewMemberViaSms:Exception2", "", e);
        	} finally {
        		emSmsMember.close();
        	}
    	}
    	
    	// attempt to associated the newly confirmed memberships with an active user
    	for(Member m : newlyConfirmedMemberships) {
         	synchUpWithAuthorizedUser(m, smsPhoneNumber);
    	}
		
    	// build return message
    	///////////////////////
    	StringBuffer sb = new StringBuffer();
		if(teamNames.size() > 1) {
			sb.append(UserInterfaceMessage.WELCOME_TO_TEAMS);
			sb.append(": ");
		} else {
			sb.append(UserInterfaceMessage.WELCOME_TO_TEAMS);
			sb.append(" ");
		}
		int index = 0;
		for(String tn : teamNames) {
			if(index > 0) {
				sb.append(" ");
			}
			sb.append(tn);
			index++;
		}
		return sb.toString();
	}
	
	public Boolean guardianMigration() {
		// use the largest guardian list to determine list size
		int listSize = 0;
		if(this.guardianKeys != null && this.guardianKeys.size() > listSize) {listSize = this.guardianKeys.size();}
		if(this.guardianPhoneNumbers != null && this.guardianPhoneNumbers.size() > listSize) {listSize = this.guardianPhoneNumbers.size();}
		if(this.guardianUserIds != null && this.guardianUserIds.size() > listSize) {listSize = this.guardianUserIds.size();}
		if(this.guardianIsEmailAddressActives != null && this.guardianIsEmailAddressActives.size() > listSize) {listSize = this.guardianIsEmailAddressActives.size();}
		if(this.guardianFirstNames != null && this.guardianFirstNames.size() > listSize) {listSize = this.guardianFirstNames.size();}
		if(this.guardianLastNames != null && this.guardianLastNames.size() > listSize) {listSize = this.guardianLastNames.size();}
		if(this.guardianAutoArchiveDayCounts != null && this.guardianAutoArchiveDayCounts.size() > listSize) {listSize = this.guardianAutoArchiveDayCounts.size();}
		if(this.guardianEmailAddresses != null && this.guardianEmailAddresses.size() > listSize) {listSize = this.guardianEmailAddresses.size();}
		if(this.guardianSmsEmailAddresses != null && this.guardianSmsEmailAddresses.size() > listSize) {listSize = this.guardianSmsEmailAddresses.size();}
		if(this.guardianHasRteamMessageAccessEnabled != null && this.guardianHasRteamMessageAccessEnabled.size() > listSize) {listSize = this.guardianHasRteamMessageAccessEnabled.size();}
		if(this.guardianHasEmailMessageAccessEnabled != null && this.guardianHasEmailMessageAccessEnabled.size() > listSize) {listSize = this.guardianHasEmailMessageAccessEnabled.size();}
		if(this.guardianHasSmsMessageAccessEnabled != null && this.guardianHasSmsMessageAccessEnabled.size() > listSize) {listSize = this.guardianHasSmsMessageAccessEnabled.size();}

		Boolean wasListUpdated = false;
		if(listSize > 0) {
			if(this.guardianKeys == null) {
				this.guardianKeys = new ArrayList<String>();
			}
			if(this.guardianKeys.size() < listSize) {
				for(int i=this.guardianKeys.size(); i<listSize; i++) {
					this.guardianKeys.add(TF.getPassword());
					log.debug("adding guardianKey to member = " + this.getFullName());
					wasListUpdated = true;
				}
			}
			
			if(this.guardianPhoneNumbers == null) {
				this.guardianPhoneNumbers = new ArrayList<String>();
			}
			if(this.guardianPhoneNumbers.size() < listSize) {
				for(int i=this.guardianPhoneNumbers.size(); i<listSize; i++) {
					this.guardianPhoneNumbers.add("");
					log.debug("adding guardianPhoneNumber to member = " + this.getFullName());
					wasListUpdated = true;
				}
			}
			
			if(this.guardianUserIds == null) {
				this.guardianUserIds = new ArrayList<String>();
			}
			if(this.guardianUserIds.size() < listSize) {
				for(int i=this.guardianUserIds.size(); i<listSize; i++) {
					this.guardianUserIds.add("");
					log.debug("adding guardianUserId to member = " + this.getFullName());
					wasListUpdated = true;
				}
			}
			
			if(this.guardianIsEmailAddressActives == null) {
				this.guardianIsEmailAddressActives = new ArrayList<Boolean>();
			}
			if(this.guardianIsEmailAddressActives.size() < listSize) {
				for(int i=this.guardianIsEmailAddressActives.size(); i<listSize; i++) {
					this.guardianIsEmailAddressActives.add(true);
					log.debug("adding guardianIsEmailAddressActive to member = " + this.getFullName());
					wasListUpdated = true;
				}
			}
			
			if(this.guardianFirstNames == null) {
				this.guardianFirstNames = new ArrayList<String>();
			}
			if(this.guardianFirstNames.size() < listSize) {
				for(int i=this.guardianFirstNames.size(); i<listSize; i++) {
					this.guardianFirstNames.add("");
					log.debug("adding guardianFirstName to member = " + this.getFullName());
					wasListUpdated = true;
				}
			}
			
			if(this.guardianLastNames == null) {
				this.guardianLastNames = new ArrayList<String>();
			}
			if(this.guardianLastNames.size() < listSize) {
				for(int i=this.guardianLastNames.size(); i<listSize; i++) {
					this.guardianLastNames.add("");
					log.debug("adding guardianLastName to member = " + this.getFullName());
					wasListUpdated = true;
				}
			}
			
			if(this.guardianAutoArchiveDayCounts == null) {
				this.guardianAutoArchiveDayCounts = new ArrayList<Integer>();
			}
			if(this.guardianAutoArchiveDayCounts.size() < listSize) {
				for(int i=this.guardianAutoArchiveDayCounts.size(); i<listSize; i++) {
					this.guardianAutoArchiveDayCounts.add(-1);
					log.debug("adding guardianAutoArchiveDayCount to member = " + this.getFullName());
					wasListUpdated = true;
				}
			}
			
			if(this.guardianEmailAddresses == null) {
				this.guardianEmailAddresses = new ArrayList<String>();
				log.debug("adding guardianEmailAddresses to member = " + this.getFullName());
			}
			if(this.guardianEmailAddresses.size() < listSize) {
				for(int i=this.guardianEmailAddresses.size(); i<listSize; i++) {
					this.guardianEmailAddresses.add("");
					//log.debug("adding guardianEmailAddresses to member = " + this.getFullName());
					wasListUpdated = true;
				}
			}
			
			if(this.guardianSmsEmailAddresses == null) {
				this.guardianSmsEmailAddresses = new ArrayList<String>();
				log.debug("adding guardianSmsEmailAddresses to member = " + this.getFullName());
			}
			if(this.guardianSmsEmailAddresses.size() < listSize) {
				for(int i=this.guardianSmsEmailAddresses.size(); i<listSize; i++) {
					this.guardianEmailAddresses.add("");
					//log.debug("adding guardianSmsEmailAddresses to member = " + this.getFullName());
					wasListUpdated = true;
				}
			}
			
			if(this.guardianHasRteamMessageAccessEnabled == null) {
				this.guardianHasRteamMessageAccessEnabled = new ArrayList<Boolean>();
				//log.debug("adding guardianHasRteamMessageAccessEnabled to member = " + this.getFullName());
			}
			if(this.guardianHasRteamMessageAccessEnabled.size() < listSize) {
				for(int i=this.guardianHasRteamMessageAccessEnabled.size(); i<listSize; i++) {
					this.guardianHasRteamMessageAccessEnabled.add(true);
					//log.debug("adding guardianHasRteamMessageAccessEnabled to member = " + this.getFullName());
					wasListUpdated = true;
				}
			}
			
			if(this.guardianHasEmailMessageAccessEnabled == null) {
				this.guardianHasEmailMessageAccessEnabled = new ArrayList<Boolean>();
				//log.debug("adding guardianHasEmailMessageAccessEnabled to member = " + this.getFullName());
			}
			if(this.guardianHasEmailMessageAccessEnabled.size() < listSize) {
				for(int i=this.guardianHasEmailMessageAccessEnabled.size(); i<listSize; i++) {
					this.guardianHasEmailMessageAccessEnabled.add(true);
					//log.debug("adding guardianHasEmailMessageAccessEnabled to member = " + this.getFullName());
					wasListUpdated = true;
				}
			}
			
			if(this.guardianHasSmsMessageAccessEnabled == null) {
				this.guardianHasSmsMessageAccessEnabled = new ArrayList<Boolean>();
				//log.debug("adding guardianHasSmsMessageAccessEnabled to member = " + this.getFullName());
			}
			if(this.guardianHasSmsMessageAccessEnabled.size() < listSize) {
				for(int i=this.guardianHasSmsMessageAccessEnabled.size(); i<listSize; i++) {
					this.guardianHasSmsMessageAccessEnabled.add(true);
					//log.debug("adding guardianHasSmsMessageAccessEnabled to member = " + this.getFullName());
					wasListUpdated = true;
				}
			}
		}
		return wasListUpdated;
	}
	
	// TODO deprecate after Access Preferences added to User Interface
	public void setDefaultAccessPreferences() {
		// default is just how it has always been:
		// if there is a associated user, then only rTeam Access
		// if there is an NA email address, then only email access
		// if there is a confirmed phone number, then only SMS access
		
		///////////////////////////
		// first the primary member
		///////////////////////////
		// for now, rTeam messaging is always enabled.
		this.hasRteamMessageAccessEnabled = true;
		
		this.hasEmailMessageAccessEnabled = false;
		this.hasSmsMessageAccessEnabled = false;
		if(this.emailAddress != null && this.emailAddress.length() > 0 && this.isEmailAddressNetworkAuthenticated(this.emailAddress)) {
			this.hasEmailMessageAccessEnabled = true;
		} else if(this.phoneNumber != null && this.phoneNumber.length() > 0 && this.isPhoneNumberSmsConfirmed(this.phoneNumber)) {
			this.hasSmsMessageAccessEnabled = true;
		}
		
		////////////////////////////
		// now the guardians, if any
		////////////////////////////
		if(this.guardianKeys != null) {
			// all guardian arrays are same size, so it doesn't matter which one size is taken from
			int listSize = this.guardianKeys.size();
			for(int i=0; i<listSize; i++) {
				// for now, rTeam messaging is always enabled.
				this.guardianHasRteamMessageAccessEnabled.set(i, true);
				
				if(this.guardianEmailAddresses.get(i) != null && this.guardianEmailAddresses.get(i).length() > 0 && this.isEmailAddressNetworkAuthenticated(this.guardianEmailAddresses.get(i))) {
					this.guardianHasEmailMessageAccessEnabled.set(i, true);
					this.guardianHasSmsMessageAccessEnabled.set(i, false);
				} else if(this.guardianPhoneNumbers.get(i) != null && this.guardianPhoneNumbers.get(i).length() > 0 && this.isPhoneNumberSmsConfirmed(this.guardianPhoneNumbers.get(i))) {
					this.guardianHasSmsMessageAccessEnabled.set(i, true);
					this.guardianHasEmailMessageAccessEnabled.set(i, false);
				}
			}
		}
	}
	
	// ::MEMBER::USER::
	// If appropriate, binds one of the individuals in the membership to the specified user.
	// NOTE: It is not an error the user does not "match" any of the individuals.
	// theEmailConfirmList: out parameter that contains email addresses requiring a confirm email be sent
	// theSmsConfirmList: out parameter that contains SMS phone numbers requiring a confirm SMS be sent
	public void bindToUser(User thePotentialAssociatedUser, List<String> theEmailConfirmList, List<String> theSmsConfirmList) {
		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// USER::MEMBER BIND ALGORITHM
		//  1. Match user to individual in this membership. No match is possible
		//     a. try to match using emailAddress
		//     b. try to match using phoneNumber
		//  2. Bind the user to matching individual in the membership
		//     a. Binding is due to matching EAs
		//        - set member EA to NA
		//        - if user PN is SMS Confirmed, push user PN to member (even if this overwrites member PN)
		//        - if member PN is empty and user PN is not, push user PN to member
		//        - if user PN is empty and member PN is not, push member PN to user
		//     b. Binding is due to matching PNs
		//        - set member PN to SMS Confirmed
		//        - if user EA is NA, push user EA to member (even if this overwrites member EA)
		//        - if member EA is empty and user EA is not, push user EA to member
		//        - if user EA is empty and member EA is not, push member EA to user
		//     c. If user name(s) is set and member name(s) is not, push name(s) from user to member
		//     d. If user thumbnails are set and member thumbnails are not, push thumbnails from user to member
		//  3. Set the individual's userId to point to the associatedUser
		//  4. If membership individual's EA is set, but not NA after bind, add EA to theEmailConfirmList.
		//  5. If membership individual's PN is set, but not SMS confirmed after bind, add PN to theSmsConfirmList.
		///////////////////////////////////////////////////////////////////////////////////////////////////////
		
		String userEA = thePotentialAssociatedUser.getEmailAddress();
		String userPN = thePotentialAssociatedUser.getPhoneNumber();
		String userId = KeyFactory.keyToString(thePotentialAssociatedUser.getKey());
		Boolean isUserNA = thePotentialAssociatedUser.getIsNetworkAuthenticated();
		Boolean isUserSmsConfirmed = thePotentialAssociatedUser.getIsSmsConfirmed();
		Integer userAutoArchiveDayCount = thePotentialAssociatedUser.getAutoArchiveDayCount();
		String memberFirstName = null;
		String memberLastName = null;
		
		////////////////////////////////////////
		//     a. Binding is due to matching EAs
		////////////////////////////////////////
		if(isUserNA && this.containsEmailAddress(userEA)) {
			//        - set member EA to NA
			this.networkAuthenticateEmailAddress(userEA);
			
			//        - if user PN is SMS Confirmed, push user PN to member (even if this overwrites member PN)
			//        - if member PN is empty and user PN is not, push user PN to member
			if(isUserSmsConfirmed || this.getPhoneNumberByEmailAddress(userEA) == null) {
				this.setPhoneNumberByEmailAddress(userPN, userEA);
				if(isUserSmsConfirmed) {this.smsConfirmPhoneNumber(userPN);}
			}
			//        - if user PN is empty and member PN is not, push member PN to user
			else if(userPN == null && this.getPhoneNumberByEmailAddress(userEA) != null) {
				thePotentialAssociatedUser.setMemberBoundPhoneNumber(this.getPhoneNumberByEmailAddress(userEA));
			}
			
			//  5. If membership individual's PN is set, but not SMS confirmed after bind, add PN to theSmsConfirmList.
			if(!isUserSmsConfirmed && this.getPhoneNumberByEmailAddress(userEA) != null) {
				// the list should NOT contain duplicates
				if(!theSmsConfirmList.contains(userPN)) {
					theSmsConfirmList.add(userPN);
				}
			}
			
			//     c. If user name(s) is set and member name(s) is not, push name(s) from user to member
			memberFirstName = this.getFirstNameByEmailAddress(userEA);
			if(memberFirstName == null || memberFirstName.trim().length() == 0) {
				this.setFirstNameByEmailAddress(thePotentialAssociatedUser.getFirstName(), userEA);
			}
			memberLastName = this.getLastNameByEmailAddress(userEA);
			if(memberLastName == null || memberLastName.trim().length() == 0) {
				this.setLastNameByEmailAddress(thePotentialAssociatedUser.getLastName(), userEA);
			}
			
			//     d. If user thumbnails are set and member thumbnails are not, push thumbnails from user to member
			// for the primary individual in the memberships only, set the photo and thumb nails if they exist
			if(this.getEmailAddress() != null && this.getEmailAddress().equalsIgnoreCase(userEA)) {
				// just go ahead and copy user's photo and thumb nail - if they are null, no harm in the copy
				if(thePotentialAssociatedUser.getPhotoBase64() != null) this.setPhotoBase64(thePotentialAssociatedUser.getPhotoBase64());
				if(thePotentialAssociatedUser.getThumbNailBase64() != null) this.setThumbNailBase64(thePotentialAssociatedUser.getThumbNailBase64());
				log.debug("member " + this.getDisplayName() + " successfully associated user photos");
			}
			
			//  3. Set the individual's userId to point to the associatedUser
			this.setUserIdByEmailAddress(userId, userEA);
			this.setAutoArchiveDayCountByEmailAddress(userAutoArchiveDayCount, userEA);
		}
		////////////////////////////////////////
		//     b. Binding is due to matching PNs
		////////////////////////////////////////
		else if(isUserSmsConfirmed && this.containsPhoneNumber(userPN)) {
			//        - set member PN to SMS Confirmed
			this.smsConfirmPhoneNumber(userPN);
			
			//        - if user EA is NA, push user EA to member (even if this overwrites member EA)
			//        - if member EA is empty and user EA is not, push user EA to member
			if(isUserNA || this.getEmailAddressByPhoneNumber(userPN) == null) {
				this.setEmailAddressByPhoneNumber(userEA, userPN);
				if(isUserNA) {this.networkAuthenticateEmailAddress(userEA);}
			}
			//        - if user EA is empty and member EA is not, push member EA to user
			else if(userEA == null && this.getEmailAddressByPhoneNumber(userPN) != null) {
				thePotentialAssociatedUser.setMemberBoundEmailAddress(this.getEmailAddressByPhoneNumber(userPN));
			}
			
			//  4. If membership individual's EA is set, but not NA after bind, add EA to theEmailConfirmList.
			if(!isUserNA && this.getEmailAddressByPhoneNumber(userPN) != null) {
				// the list should NOT contain duplicates
				if(!theEmailConfirmList.contains(userEA)) {
					theEmailConfirmList.add(userEA);
				}
			}

			//     c. If user name(s) is set and member name(s) is not, push name(s) from user to member
			memberFirstName = this.getFirstNameByPhoneNumber(userPN);
			if(memberFirstName == null || memberFirstName.trim().length() == 0) {
				this.setFirstNameByPhoneNumber(thePotentialAssociatedUser.getFirstName(), userPN);
			}
			memberLastName = this.getLastNameByPhoneNumber(userPN);
			if(memberLastName == null || memberLastName.trim().length() == 0) {
				this.setLastNameByPhoneNumber(thePotentialAssociatedUser.getLastName(), userPN);
			}
			
			//     d. If user thumbnails are set and member thumbnails are not, push thumbnails from user to member
			// for the primary individual in the memberships only, set the photo and thumb nails if they exist
			if(this.getPhoneNumber() != null && this.getPhoneNumber().equalsIgnoreCase(userPN)) {
				// just go ahead and copy user's photo and thumb nail - if they are null, no harm in the copy
				if(thePotentialAssociatedUser.getPhotoBase64() != null) this.setPhotoBase64(thePotentialAssociatedUser.getPhotoBase64());
				if(thePotentialAssociatedUser.getThumbNailBase64() != null) this.setThumbNailBase64(thePotentialAssociatedUser.getThumbNailBase64());
				log.debug("member " + this.getDisplayName() + " successfully associated user photos");
			}
			
			//  3. Set the individual's userId to point to the associatedUser
			this.setUserIdByPhoneNumber(userId, userPN);
			this.setAutoArchiveDayCountByPhoneNumber(userAutoArchiveDayCount, userPN);
		} else {
			// 'no match' is expected.
		}
	}
    
	// Returns all confirmed memberships.
	// if theEmailAddress is not null, email address confirmed memberships are returned
	// if thePhoneNumber is not null, sms confirmed memberships are returned
	public static List<Member> getConfirmedMemberships(String theEmailAddress, String thePhoneNumber) {
		EntityManager em = EMF.get().createEntityManager();
		List<Member> smsConfirmedMemberships = null;
		List<Member> emailAddressConfirmedMemberships = null;
		List<Member> allConfirmedMemberships = new ArrayList<Member>();
		
		try {
			if(thePhoneNumber != null) {
				smsConfirmedMemberships = (List<Member>)em.createNamedQuery("Member.getBySmsConfirmedPhoneNumber")
						.setParameter("phoneNumber", thePhoneNumber)
						.getResultList();
				allConfirmedMemberships.addAll(smsConfirmedMemberships);
			}
			if(theEmailAddress != null) {
				emailAddressConfirmedMemberships = (List<Member>) em.createNamedQuery("Member.getByNetworkAuthenticatedEmailAddress")
						.setParameter("emailAddress", theEmailAddress)
			 			.getResultList();
				allConfirmedMemberships.addAll(emailAddressConfirmedMemberships);
			}
		log.debug("number of memberships with phone number " + thePhoneNumber + " and email address " + theEmailAddress + " = " + allConfirmedMemberships.size());
		} catch (Exception e) {
    		log.exception("Member:getConfirmedMemberships:Exception", "", e);
		} finally {
			em.close();
		}
		
		return allConfirmedMemberships;
	}
	
  
}
