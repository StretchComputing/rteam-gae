package com.stretchcom.rteam.server;

import java.io.IOException;
import java.util.ArrayList;
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
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/**
 * Resource that manages a list of items.
 * 
 */
public class TeamsResource extends ServerResource {
	//private static final Logger log = Logger.getLogger(TeamsResource.class.getName());
	private RskyboxClient log = new RskyboxClient(this);

    @Override  
    protected void doInit() throws ResourceException {  
    	log.info("API requested with URL: " + this.getReference().toString());
    }

    // Handles 'Create a new team' API
	@Post
	public JsonRepresentation createTeam(Representation entity) {
		JSONObject jsonReturn = new JSONObject();
		log.debug("createTeam(@Post) entered ..... ");
		EntityManager em = EMF.get().createEntityManager();

		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_CREATED);
		
		Team team = null;
		User currentUser = null;
		em.getTransaction().begin();
		try {
			currentUser = (User) this.getRequest().getAttributes().get(RteamApplication.CURRENT_USER);
			if (currentUser == null) {
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				log.error("TeamsResource:createTeam:currentUser", "user could not be retrieved from Request attributes!!");
				return new JsonRepresentation(jsonReturn);
			} else {
				log.debug("currentUser = " + currentUser.getFullName());
			}

			team = new Team();
			JsonRepresentation jsonRep = new JsonRepresentation(entity);
			JSONObject json = jsonRep.getJsonObject();

			if (json.has("teamName")) {
				team.setTeamName(json.getString("teamName"));
			}

			if (json.has("description")) {
				team.setDescription(json.getString("description"));
			}

			if (json.has("leagueName")) {
				String leagueName = json.getString("leagueName");
				if (leagueName != null && leagueName.length() > 0)
					team.setLeagueName(leagueName);
			}

			// defaults to 'false' if not specified
			boolean useTwitter = false;
			if(json.has("useTwitter")) {
				useTwitter = json.getBoolean("useTwitter");
				log.debug("json useTwitter = " + useTwitter);
			}
			team.setUseTwitter(useTwitter);

			if (json.has("gender")) {
				String gender = json.getString("gender");
				if (gender != null && gender.length() > 0)
					team.setGender(gender);
			}

			if (json.has("teamSiteUrl")) {
				String siteUrl = json.getString("teamSiteUrl");
				if (siteUrl != null && siteUrl.length() > 0)
					team.setSiteUrl(siteUrl);
			}

			if (json.has("sport")) {
				String sport = json.getString("sport");
				if (sport != null && sport.length() > 0)
					team.setSport(sport);
			}

			if (json.has("city")) {
				String city = json.getString("city");
				if (city != null && city.length() > 0)
					team.setCity(city);
			}

			if (json.has("state")) {
				String state = json.getString("state");
				if (state != null && state.length() > 0)
					team.setState(state);
			}

			String latitudeStr = null;
			if (json.has("latitude")) {
				latitudeStr = json.getString("latitude");
			}

			String longitudeStr = null;
			if (json.has("longitude")) {
				longitudeStr = json.getString("longitude");
			}

			// TODO don't think team needs to store users - not sure what the
			// intention here was originally
			Set<Key> users = team.getUsers();
			users.add(currentUser.getKey());

			Double latitude = null;
			Double longitude = null;

			// TeamName and Description are required
			if (team.getTeamName() == null || team.getTeamName().length() == 0
					|| team.getDescription() == null
					|| team.getDescription().length() == 0) {
				apiStatus = ApiStatusCode.TEAM_NAME_AND_DESCRIPTION_REQUIRED;
			}
			// must be a valid state name/abbreviation, if present
			else if (team.getState() != null && !StateMap.get().isValid(team.getState())) {
				apiStatus = ApiStatusCode.INVALID_STATE_PARAMETER;
			}
			// gender must be a valid option, if present
			else if (team.getGender() != null && !team.isGenderValid(team.getGender())) {
				apiStatus = ApiStatusCode.INVALID_GENDER_PARAMETER;
			}
			// longitude and latitude: if either is specified, both must be specified
			else {
				if ((latitudeStr == null && longitudeStr != null) || (latitudeStr != null && longitudeStr == null)) {
					apiStatus = ApiStatusCode.LATITIUDE_AND_LONGITUDE_MUST_BE_SPECIFIED_TOGETHER;
				} else {
					if (latitudeStr != null) {
						try {
							latitude = new Double(latitudeStr);
							team.setLatitude(latitude);
						} catch (NumberFormatException e) {
							apiStatus = ApiStatusCode.INVALID_LATITUDE_PARAMETER;
						}
					}

					if (apiStatus.equals(ApiStatusCode.SUCCESS) && longitudeStr != null) {
						try {
							longitude = new Double(longitudeStr);
							team.setLongitude(longitude);
						} catch (NumberFormatException e) {
							apiStatus = ApiStatusCode.INVALID_LONGITUDE_PARAMETER;
						}
					}
				}
			}
			
			//::BUSINESS_RULE:: must be NA to set up a Twitter account
			if(apiStatus.equals(ApiStatusCode.SUCCESS) && team.getUseTwitter()) {
				if(!currentUser.getIsNetworkAuthenticated()) {
					apiStatus = ApiStatusCode.USER_NOT_NETWORK_AUTHENTICATED;
					log.debug("user must be network authenticated to connect to a Twitter account");
				}
			}

			// ::TODO if latitude and longitude set, use reverse geocode to override and set the city and state
			
			if(!apiStatus.equals(ApiStatusCode.SUCCESS) || !this.getStatus().equals(Status.SUCCESS_CREATED)) {
				jsonReturn.put("apiStatus", apiStatus);
				return new JsonRepresentation(jsonReturn);
			}

			// save the base team page URL
			// TODO make sure URL has only legal characters
			String baseTeamPageUrl = createBaseTeamName(team);
			// only the base team name is persisted - that is all that is
			// needed to match via query later
			team.setPageUrl(baseTeamPageUrl);

			String teamPageUrl = RteamApplication.BASE_URL_WITH_SLASH + "teamPage/" + baseTeamPageUrl;

			// ::BusinessRule:: everyone must be a member of the team, so add the user as the 'creator' member.
			// automatically add user as first member of the newly created team. Member roles, etc can be manually updated later.
			// ::MEMBER::USER::
			Member member = new Member();
			member.setEmailAddress(currentUser.getEmailAddress());
			member.setPhoneNumber(currentUser.getPhoneNumber());  // may be NULL
			member.setFirstName(currentUser.getFirstName());
			member.setLastName(currentUser.getLastName());
			member.setAutoArchiveDayCount(currentUser.getAutoArchiveDayCount());
			
			// Verified that the following lines cause a NPE error if currentUser photo and thumb nail not set
			if(currentUser.getPhotoBase64() != null) member.setPhotoBase64(currentUser.getPhotoBase64());
			if(currentUser.getThumbNailBase64() != null) member.setThumbNailBase64(currentUser.getThumbNailBase64());

			// if NA or confirmed, set userId and copy over NA and confirmation -- as appropriate
			if(currentUser.getIsNetworkAuthenticated() || currentUser.getIsSmsConfirmed()) {
				member.setUserId(KeyFactory.keyToString(currentUser.getKey()));

				// user can be both NA and SMS confirmed
				if(currentUser.getIsNetworkAuthenticated()) {
					member.networkAuthenticateEmailAddress(currentUser.getEmailAddress());
				}
				if(currentUser.getIsSmsConfirmed()) {
					// confirming confirms the individual and sets the SMS address.
					member.smsConfirmPhoneNumber(currentUser.getSmsEmailAddress());
				}
			}

			member.setParticipantRole(Member.CREATOR_PARTICIPANT);
			List<String> roles = new ArrayList<String>();
			roles.add(Member.ORGANIZER_ROLE);
			member.setRoles(roles);
			List<Member> members = team.getMembers();

			// TODO replace with Access Preferences
			member.setDefaultAccessPreferences();

			members.add(member);
			
			// if team using twitter, get necessary request tokens, etc.
			Boolean twitterAuthorizationInitialized = false;
			if(team.getUseTwitter()) {
				twitterAuthorizationInitialized = true;
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
			
			// useTwitter is not persisted as 'true' until Twitter authorization is complete
			team.setUseTwitter(false);
			em.persist(team);
			em.getTransaction().commit();
			String keyWebStr = KeyFactory.keyToString(team.getKey());
			log.debug("team " + team.getTeamName() + " with key " + keyWebStr + " created successfully");

			// TODO URL should be filtered to have only legal characters
			String baseUri = this.getRequest().getHostRef().getIdentifier();
			this.getResponse().setLocationRef(baseUri + "/" + team.getTeamName());

			jsonReturn.put("teamId", keyWebStr);
			jsonReturn.put("teamPageUrl", teamPageUrl);
			if(twitterAuthorizationInitialized) {
				jsonReturn.put("twitterAuthorizationUrl", team.getTwitterAuthorizationUrl());
			}
		} catch (IOException e) {
			log.exception("TeamsResource:createTeam:IOException", "", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (JSONException e) {
			log.exception("TeamsResource:createTeam:JSONException1", "", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} finally {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
			em.close();
		}

		// update User in a separate transaction - User and Team are in the same
		// Entity Group?
		if (currentUser != null && this.getStatus().equals(Status.SUCCESS_CREATED)) {
			log.debug("adding Team to user team list");
			EntityManager em2 = EMF.get().createEntityManager();
			em2.getTransaction().begin();
			try {
				currentUser = (User) em2.createNamedQuery("User.getByKey")
						.setParameter("key", currentUser.getKey())
						.getSingleResult();
				List<Key> teams = currentUser.getTeams();
				log.debug("number of teams for user " + currentUser.getLastName() + " = " + teams.size());
				log.debug("about to add team to user: team key = " + team.getKey());
				currentUser.addTeam(team);
				em2.getTransaction().commit();
			} catch (NoResultException e) {
				log.exception("TeamsResource:createTeam:NoResultException", "user not found", e);
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (NonUniqueResultException e) {
				log.exception("TeamsResource:createTeam:NonUniqueResultException", "should never happen - two or more users have same key", e);
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
			} finally {
				if (em2.getTransaction().isActive()) {
					em2.getTransaction().rollback();
				}
				em2.close();
			}
		}

		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("TeamsResource:createTeam:JSONException2", "", e);
		}
		return new JsonRepresentation(jsonReturn);
	}

	// Handles 'Get list of teams' API
	@Get("json")
	public JsonRepresentation getTeamList(Variant variant) {
		log.debug("TeamResource:toJson() entered");
		JSONObject jsonReturn = new JSONObject();
		EntityManager em = EMF.get().createEntityManager();

		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		try {
			User currentUser = (User) this.getRequest().getAttributes().get(
					RteamApplication.CURRENT_USER);
			if (currentUser == null) {
				log.error("TeamsResource:getTeamList:currentUser", "user could not be retrieved from Request attributes!!");
				this.setStatus(Status.SERVER_ERROR_INTERNAL);
				return new JsonRepresentation(jsonReturn);
			}

			User user = (User) em.createNamedQuery("User.getByKey")
					.setParameter("key", currentUser.getKey())
					.getSingleResult();
			List<Key> teamKeys = user.getTeams();
			log.debug("number of user teams = " + teamKeys.size());

			// cannot use a NamedQuery for a batch get of keys
			List<Team> teams = new ArrayList<Team>();
			if (teamKeys.size() > 0) {
				teams = (List<Team>) em.createQuery(
						"select from " + Team.class.getName() + " where key = :keys").setParameter("keys",
						teamKeys).getResultList();
			}

			// Need to know participantRole for each team which is stored in the Member entity. So create
			// a membership list and use that list to determine the participantRoles.
			List<Member> members = (List<Member>) em.createNamedQuery("Member.getByNetworkAuthenticatedEmailAddress")
					.setParameter("emailAddress", currentUser.getEmailAddress())
					.getResultList();

			// NOTE: teams created while the user is not yet NA will create memberships that are not NA. So, at this point
			//       in time, those memberships may still not be NA. So the number of teams held in the user entity may
			//       exceed the number of memberships.  Those missing memberships should all have participant role of 'creator'.

			JSONArray jsonArray = new JSONArray();
			for (Team t : teams) {
				JSONObject jsonTeamObj = new JSONObject();
				jsonTeamObj.put("teamId", KeyFactory.keyToString(t.getKey()));
				jsonTeamObj.put("teamName", t.getTeamName());
				String siteUrl = t.getSiteUrl() != null ? t.getSiteUrl() : "";
				jsonTeamObj.put("teamSiteUrl", siteUrl);
				String sport = t.getSport() != null ? t.getSport() : "";
				jsonTeamObj.put("sport", sport);

				// find the member entity that matches this team so the participantRole can be set. If not matching
				// matching membership is found, participant role will be creator. (see NOTE several lines above).
				jsonTeamObj.put("participantRole", Member.CREATOR_PARTICIPANT);
				for (Member m : members) {
					if (m.getTeam().equals(t)) {
						log.debug("participantRole is being set = " + m.getParticipantRole());
						jsonTeamObj.put("participantRole", m.getParticipantRole());
						break;
					}
				}
				jsonTeamObj.put("useTwitter", t.getUseTwitter());

				jsonArray.put(jsonTeamObj);
			}
			jsonReturn.put("teams", jsonArray);
		} catch (NoResultException e) {
			log.exception("TeamsResource:getTeamList:NoResultException", "user not found", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (NonUniqueResultException e) {
			log.exception("TeamsResource:getTeamList:NonUniqueResultException", "two or more users have same key", e);
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		} catch (JSONException e) {
			log.exception("TeamsResource:getTeamList:JSONException1", "", e);
		}

		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.exception("TeamsResource:getTeamList:JSONException1", "", e);
		}
		return new JsonRepresentation(jsonReturn);
	}

	private Boolean isTeamNameUnique(String theTeamName, List<Team> theTeams) {
		for (Team t : theTeams) {
			if (t.getTeamName().equalsIgnoreCase(theTeamName)) {
				return false;
			}
		}
		return true;
	}

	private String createBaseTeamName(Team theTeam) {
		String baseTeamName = "";
		String teamCity = theTeam.getCity();
		// ::TODO pick another default city
		if (teamCity == null || teamCity.length() == 0) {
			teamCity = "zion";
		}

		// remove all whitespace
		teamCity = Utility.removeAllWhiteSpace(teamCity);

		String teamDescription = theTeam.getDescription();
		if (teamDescription == null || teamDescription.length() == 0) {
			teamDescription = "team";
		}

		// remove all whitespace
		teamDescription = Utility.removeAllWhiteSpace(teamDescription);

		return teamCity + "/" + teamDescription;
	}

}
