package com.stretchcom.rteam.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

@Entity
@NamedQueries({
    @NamedQuery(
    		name="Team.getByKeys",
    		query="SELECT t FROM Team t WHERE t.key = :keys"
    ),
    @NamedQuery(
    		name="Team.getByKey",
    		query="SELECT t FROM Team t WHERE t.key = :key"
    ),
    @NamedQuery(
    		name="Team.getByOneUseTokenAndTokenStatus",
    		query="SELECT t FROM Team t WHERE t.oneUseToken = :oneUseToken AND t.oneUseTokenStatus = :oneUseTokenStatus"
    ),
})
public class Team {
	//private static final Logger log = Logger.getLogger(Team.class.getName());
	private static RskyboxClient log = new RskyboxClient();
	
	//constants
	public static final String FEMALE_GENDER = "female";
	public static final String MALE_GENDER = "male";
	public static final String COED_GENDER = "coed";

	public static final String NEW_TOKEN_STATUS = "new";
	public static final String USED_TOKEN_STATUS = "used";
	
	public static final Integer THUMB_NAIL_LONG_SIDE  = 82;  // size optimized for retina display
	public static final Integer THUMB_NAIL_SHORT_SIDE  = 62; // size optimized for retina display
	
	public static final String FIRST_TEAM_NAME = "Team 1";
	public static final String FIRST_TEAM_DESCRIPTION = "auto created first team";
	
	private String teamName;
	private String description;
	private String leagueName;
	private String sport;
	private String gender;
	private String siteUrl;  // external team site like eteamz
	private String pageUrl;  // rTeam team page. The base URL (i.e. http://rteamtest.appspot.com/teamPage/) is not stored
	private Key teamLocation; // key to the Location entity holding the team location. Use 'key' and not 'locationId' because never sent to client
	private Double latitude; // local copy of team location data maintained in Location entity.
	private Double longitude; // local copy of team location data maintained in Location entity.
	private String locationName; // local copy of team location data maintained in Location entity.
	private String city;  // ??? is this stored as Location attribute too, not sure right now.
	private String state; // ??? is this stored as Location attribute too, not sure right now.
	
	// Twitter Properties
	private String twitterAuthorizationUrl;
	private String twitterRequestToken;
	private String twitterRequestTokenSecret;
	private String twitterAccessToken;
	private String twitterAccessTokenSecret;
	private String twitterScreenName;
	private Long newestTwitterId;  // hold the twitter ID of the newest tweet that is cached by rTeam
	private Long newestCacheId;
	private Boolean isCacheStale = false;  // deprecated - not used anymore
	private Boolean useTwitter = false;
	private Date lastTwitterRefresh;  // used throttle how often Twitter is polled for 'direct Twitter Tweets'

	private String oneUseToken; // passed to twitter as query parameter of callback URL.
	private String oneUseTokenStatus;
	private Text photoBase64;
	private Text thumbNailBase64;

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Key key;
    
    @Basic
    private Set<Key> users = new HashSet<Key>();
    
    // Location <=> Team relationship is managed as a 'soft schema'.
    // Team holds a set of all team event locations
	@Basic
    private Set<Key> locations = new HashSet<Key>();
    
    public Set<Key> getLocations() {
		return locations;
	}

	public void setLocations(Set<Key> locations) {
		this.locations = locations;
	}

	@OneToMany(mappedBy = "team", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Member> members = new ArrayList<Member>();

    public List<Member> getMembers() {
		return members;
	}
    
    // returns the teammates of the calling user (i.e. all the members on the team less the caller)
    public List<Member> getTeamMates(User theCallingUser) {
    	List<Member> teamMates = new ArrayList<Member>();
    	for(Member tm : this.members) {
    		if(!(tm.getUserId() != null && tm.getUserId().equals(KeyFactory.keyToString(theCallingUser.getKey())))) {
    			teamMates.add(tm);
    		}
    	}
    	return teamMates;
    }

	public void setMembers(List<Member> members) {
		this.members = members;
	}

	public Set<Key> getUsers() {
		return users;
	}

	public void setUsers(Set<Key> users) {
		this.users = users;
	}

	public Team() {
    	
    }
    
    // used in client-side tests
    public Team(String teamName, String leagueName, String twitterUserName, String twitterPassword) {
    	this.teamName = teamName;
    	this.leagueName = leagueName;
    }

    public Key getKey() {
        return key;
    }

    public String getTeamName() {
		return teamName;
	}
    
	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}
	
	public String getLeagueName() {
		return leagueName;
	}
	
	public void setLeagueName(String leagueName) {
		this.leagueName = leagueName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSport() {
		return sport;
	}

	public void setSport(String sport) {
		this.sport = sport;
	}

	@Override
	// if two objects are equal according to the equals() method, they must have the same hashCode()
	// value (although the reverse is not generally true)
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass()) {
			//log.debug("***** Team.equals(): classes did NOT match ********");
			return false;
		}

		Team other = (Team) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key)) {
			//log.debug("keys did not match: key = " + key.toString() + " other key = " + other.key.toString());
			return false;
		}
		return true;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}
	
	public String getSiteUrl() {
		return siteUrl;
	}

	public void setSiteUrl(String siteUrl) {
		this.siteUrl = siteUrl;
	}

	public String getPageUrl() {
		return pageUrl;
	}

	public void setPageUrl(String pageUrl) {
		this.pageUrl = pageUrl;
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
	
	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}
	
	public Boolean isGenderValid(String theGender) {
		if( theGender.equalsIgnoreCase(FEMALE_GENDER) ||
			theGender.equalsIgnoreCase(MALE_GENDER) ||
			theGender.equalsIgnoreCase(COED_GENDER)) {
			return true;
		}
		return false;
	}

	public String getLocationName() {
		return locationName;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	public Key getTeamLocation() {
		return teamLocation;
	}

	public void setTeamLocation(Key teamLocation) {
		this.teamLocation = teamLocation;
	}

	public String getTwitterAccessToken() {
		return twitterAccessToken;
	}

	public void setTwitterAccessToken(String twitterAccessToken) {
		this.twitterAccessToken = twitterAccessToken;
	}

	public String getTwitterAccessTokenSecret() {
		return twitterAccessTokenSecret;
	}

	public void setTwitterAccessTokenSecret(String twitterAccessTokenSecret) {
		this.twitterAccessTokenSecret = twitterAccessTokenSecret;
	}

	public String getTwitterRequestTokenSecret() {
		return twitterRequestTokenSecret;
	}

	public void setTwitterRequestTokenSecret(String twitterRequestTokenSecret) {
		this.twitterRequestTokenSecret = twitterRequestTokenSecret;
	}

	public String getTwitterRequestToken() {
		return twitterRequestToken;
	}

	public void setTwitterRequestToken(String twitterRequestToken) {
		this.twitterRequestToken = twitterRequestToken;
	}

	public String getTwitterAuthorizationUrl() {
		return twitterAuthorizationUrl;
	}

	public void setTwitterAuthorizationUrl(String twitterAuthorizationUrl) {
		this.twitterAuthorizationUrl = twitterAuthorizationUrl;
	}

	public Boolean getUseTwitter() {
		return useTwitter;
	}

	public void setUseTwitter(Boolean useTwitter) {
		this.useTwitter = useTwitter;
	}
	
	public String getTwitterScreenName() {
		return twitterScreenName;
	}

	public void setTwitterScreenName(String twitterScreenName) {
		this.twitterScreenName = twitterScreenName;
	}

	public String getOneUseTokenStatus() {
		return oneUseTokenStatus;
	}

	public void setOneUseTokenStatus(String oneUseTokenStatus) {
		this.oneUseTokenStatus = oneUseTokenStatus;
	}

	public String getOneUseToken() {
		return oneUseToken;
	}

	public void setOneUseToken(String oneUseToken) {
		this.oneUseToken = oneUseToken;
	}

	// guaranteed not to return null
	public Long getNewestTwitterId() {
		if(this.newestTwitterId == null) return 0L;
		else return this.newestTwitterId;
	}

	public void setNewestTwitterId(Long newestTwitterId) {
		this.newestTwitterId = newestTwitterId;
	}
	
	// guaranteed not to return null
	public Long getNewestCacheId() {
		if(this.newestCacheId == null) return 0L;
		else return this.newestCacheId;
	}

	public void setNewestCacheId(Long newestCacheId) {
		this.newestCacheId = newestCacheId;
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

	public Boolean getIsCacheStale() {
		return isCacheStale;
	}

	public void setIsCacheStale(Boolean isCacheStale) {
		this.isCacheStale = isCacheStale;
	}
	
	public Date getLastTwitterRefresh() {
		return lastTwitterRefresh;
	}

	public void setLastTwitterRefresh(Date lastTwitterRefresh) {
		this.lastTwitterRefresh = lastTwitterRefresh;
	}
    
    public void updateCacheStale(Boolean theIsCacheStale) {
		Team team = null;
    	EntityManager em = EMF.get().createEntityManager();
		try {
			team = (Team)em.createNamedQuery("Team.getByKey")
				.setParameter("key", this.key)
				.getSingleResult();
			team.setIsCacheStale(theIsCacheStale);
		} catch (NoResultException e) {
			log.exception("Team:updateCacheStale:NoResultException", "team not found using key (101)", e);
		} catch (NonUniqueResultException e) {
			log.exception("Team:updateCacheStale:NoResultException", "two teams have the same key (101", e);
		} finally {
			em.close();
		}
		return;
    }
    
    public void updateNewestCacheId(Long theCacheId) {
		Team team = null;
    	EntityManager em = EMF.get().createEntityManager();
		try {
			team = (Team)em.createNamedQuery("Team.getByKey")
				.setParameter("key", this.key)
				.getSingleResult();
			team.setNewestCacheId(theCacheId);
		} catch (NoResultException e) {
			log.exception("Team:updateNewestCacheId:NoResultException", "team not found using key", e);
		} catch (NonUniqueResultException e) {
			log.exception("Team:updateNewestCacheId:NonUniqueResultException", "two teams have the same key", e);
		} finally {
			em.close();
		}
		return;
    }
	
	// Returns TRUE if the specified email address belongs to the team creator. Otherwise, returns FALSE.
	public Boolean isCreator(String theMemberEmailAddress) {
		if(this.members == null || this.members.size() == 0) {
			// no members, return false
			return false;
		}
		
		for(Member m : members) {
			// only looking at the primary email address of a member because creator must be the primary individual in a membershipp
			if(m.isCreator()) {
				if(m.getEmailAddress().equalsIgnoreCase(theMemberEmailAddress)) {
					return true;
				}
				else {
					// creator email address didn't match.  Only one creator for a team, so we can return failure without looking further
					return false;
				}
			}
		}
		
		// if we get this far, then the original creator must have already left the team
		return false;
	}
	
	// ::BUSINESS_RULE::
	// Primary email addresses must be unique among all members (note that some members have no primary email address)
	public Boolean isNewPrimaryEmailAddressAcceptable(String theEmailAddress) {
		if(theEmailAddress == null) {return true;}
		
		for(Member m : this.members) {
			if(m.getEmailAddress() != null && m.getEmailAddress().equalsIgnoreCase(theEmailAddress)) {
				return false;
			}
		}
		return true;
	}
	
	// ::BUSINESS_RULE::
	// Primary phone number must be unique among all members (note that some members have no primary phone number)
	public Boolean isNewPrimaryPhoneNumberAcceptable(String thePhoneNumber) {
		if(thePhoneNumber == null) {return true;}
		
		for(Member m : this.members) {
			if(m.getPhoneNumber() != null && m.getPhoneNumber().equalsIgnoreCase(thePhoneNumber)) {
				return false;
			}
		}
		return true;
	}
	
	public void initPageUrl() {
		this.pageUrl = createBaseTeamUrl();
	}
	
	public String getCompletePageUrl() {
		StringBuffer sb = new StringBuffer();
		sb.append(RteamApplication.BASE_URL_WITH_SLASH);
		sb.append("teamPage/");
		sb.append(this.pageUrl);
		return sb.toString();
	}
	
	private String createBaseTeamUrl() {
		String teamCity = this.getCity();
		if (teamCity == null || teamCity.length() == 0) {teamCity = "zion";}
		teamCity = Utility.removeAllWhiteSpace(teamCity);

		String teamName = this.getTeamName();
		if (teamName == null || teamName.length() == 0) {teamName = "team";}
		teamName = Utility.removeAllWhiteSpace(teamName);

		return teamCity + teamName;
	}
	
	public static Team createFirst(User theUser) {
		Team firstTeam = null;
    	EntityManager emTeam = EMF.get().createEntityManager();
		try {
			firstTeam = new Team();
			firstTeam.setTeamName(Team.FIRST_TEAM_NAME);
			firstTeam.setDescription(Team.FIRST_TEAM_DESCRIPTION);
			firstTeam.initPageUrl();
			firstTeam.addCreator(theUser);
			emTeam.persist(firstTeam);
		} finally {
			emTeam.close();
		}
		return firstTeam;
	}
	
	public void addCreator(User theCreatingUser) {
		// ::BusinessRule:: everyone must be a member of the team, so add the user as the 'creator' member.
		// automatically add user as first member of the newly created team. Member roles, etc can be manually updated later.
		// ::MEMBER::USER::
		Member member = new Member();
		member.setEmailAddress(theCreatingUser.getEmailAddress());
		member.setPhoneNumber(theCreatingUser.getPhoneNumber());  // may be NULL
		member.setFirstName(theCreatingUser.getFirstName());
		member.setLastName(theCreatingUser.getLastName());
		member.setAutoArchiveDayCount(theCreatingUser.getAutoArchiveDayCount());
		
		// Verified that the following lines cause a NPE error if currentUser photo and thumb nail not set
		if(theCreatingUser.getPhotoBase64() != null) member.setPhotoBase64(theCreatingUser.getPhotoBase64());
		if(theCreatingUser.getThumbNailBase64() != null) member.setThumbNailBase64(theCreatingUser.getThumbNailBase64());

		// if NA or confirmed, set userId and copy over NA and confirmation -- as appropriate
		if(theCreatingUser.getIsNetworkAuthenticated() || theCreatingUser.getIsSmsConfirmed()) {
			member.setUserId(KeyFactory.keyToString(theCreatingUser.getKey()));

			// user can be both NA and SMS confirmed
			if(theCreatingUser.getIsNetworkAuthenticated()) {
				member.networkAuthenticateEmailAddress(theCreatingUser.getEmailAddress());
			}
			if(theCreatingUser.getIsSmsConfirmed()) {
				// confirming confirms the individual and sets the SMS address.
				member.smsConfirmPhoneNumber(theCreatingUser.getSmsEmailAddress());
			}
		}

		member.setParticipantRole(Member.CREATOR_PARTICIPANT);
		List<String> roles = new ArrayList<String>();
		roles.add(Member.ORGANIZER_ROLE);
		member.setRoles(roles);

		// TODO replace with user specified Access Preferences
		member.setDefaultAccessPreferences();

		this.getMembers().add(member);
	}
}
