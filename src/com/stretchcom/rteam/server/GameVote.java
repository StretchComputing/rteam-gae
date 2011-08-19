package com.stretchcom.rteam.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
    		name="GameVote.getByKey",
    		query="SELECT g FROM GameVote g WHERE g.key = :key"
    ),
    @NamedQuery(
    		name="GameVote.getByUserIdAndGameIdAndVoteType",
    		query="SELECT g FROM GameVote g WHERE " +
    			  "g.userId = :userId" + " AND " +
    		      "g.gameId = :gameId" + " AND " +
    		      "g.voteType = :voteType"
    ),
    @NamedQuery(
    		name="GameVote.getByUserIdAndGameIdAndMemberIdAndVoteType",
    		query="SELECT g FROM GameVote g WHERE " +
    			  "g.userId = :userId" + " AND " +
    		      "g.gameId = :gameId" + " AND " +
    		      "g.memberId = :memberId" + " AND " +
    		      "g.voteType = :voteType"
    ),
    @NamedQuery(
    		name="GameVote.getByGameIdAndMemberIdAndVoteTypeAndIsTally",
    		query="SELECT g FROM GameVote g WHERE " +
    		      "g.gameId = :gameId" + " AND " +
    		      "g.memberId = :memberId" + " AND " +
    		      "g.voteType = :voteType" + " AND " +
    			  "g.isTally = :isTally" 
    ),
    @NamedQuery(
    		name="GameVote.getByGameIdAndVoteTypeAndIsTally",
    		query="SELECT g FROM GameVote g WHERE " +
    		      "g.gameId = :gameId" + " AND " +
    		      "g.voteType = :voteType" + " AND " +
    			  "g.isTally = :isTally" 
    ),
})
public class GameVote {
	private static final Logger log = Logger.getLogger(GameVote.class.getName());
	
	//constants
	public static final String MVP_VOTE_TYPE = "mvp";
	public static final String OVERALL_GAME_VOTE_TYPE = "overall";
	
	public static final String YES_TALLY = "yes";
	public static final String NO_TALLY = "no";
	
    private String userId;  // must be a user to vote
	private String teamId;
	private String gameId;
	private String memberId;
	private Date createdGmtDate;
	private Date lastUpdatedGmtDate;
	private String voteType;
	private String isTally;
	private Integer voteCount;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Key key;

	public Key getKey() {
        return key;
    }

    public String getTeamId() {
		return teamId;
	}

	public void setTeamId(String teamId) {
		this.teamId = teamId;
	}

	public Date getCreatedGmtDate() {
		return createdGmtDate;
	}

	public void setCreatedGmtDate(Date createdGmtDate) {
		this.createdGmtDate = createdGmtDate;
	}
	
	public Date getLastUpdatedGmtDate() {
		return lastUpdatedGmtDate;
	}

	public void setLastUpdatedGmtDate(Date lastUpdatedGmtDate) {
		this.lastUpdatedGmtDate = lastUpdatedGmtDate;
	}
	
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}
	
	public String getMemberId() {
		return memberId;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}

	public String getVoteType() {
		return voteType;
	}

	public void setVoteType(String voteType) {
		this.voteType = voteType;
	}

	public String getIsTally() {
		return isTally;
	}

	public void setIsTally(String isTally) {
		this.isTally = isTally;
	}

	public Integer getVoteCount() {
		return voteCount;
	}

	public void setVoteCount(Integer voteCount) {
		this.voteCount = voteCount;
	}
	
	
	/////////////////
	// STATIC METHODS
	/////////////////
	public static void castUserVote(User theUser, String theMemberId, Game theGame, String theVoteType, Team theTeam) {
		EntityManager em = EMF.get().createEntityManager();
		GameVote gameVote = null;
		String userId = KeyFactory.keyToString(theUser.getKey());
		String gameId = KeyFactory.keyToString(theGame.getKey());
		String teamId = KeyFactory.keyToString(theTeam.getKey());
		
		try {
			// see if the user has already cast the vote for this game and this is a update
			gameVote = (GameVote)em.createNamedQuery("GameVote.getByUserIdAndGameIdAndVoteType")
				.setParameter("userId", userId)
				.setParameter("gameId", gameId)
				.setParameter("voteType", theVoteType)
				.getSingleResult();
    		log.info("user has already cast a " + theVoteType + " vote for this game");
    		
    		// a previous vote was cast -- verify the user is really changing their vote
    		String originalMemberId = gameVote.getMemberId();
    		if(!originalMemberId.equals(theMemberId)) {
    			gameVote.setMemberId(theMemberId);
    			
				// decrement the tally from the original member who was voted for the first time
    			decrementTally(originalMemberId, gameId, theVoteType);
    			log.info("decremented vote count for memberId = " + originalMemberId);
    			
    			// increment the tally for the member just voted for
    			incrementTally(theMemberId, gameId, theVoteType, teamId);
    			log.info("incremented vote count for memberId = " + theMemberId);
    		}
		} catch (NoResultException e) {
			//////////////////////////////////////////////////////////////////
			// Not a error just that this user has not yet voted for this game
			//////////////////////////////////////////////////////////////////
			
			// create the record for this new vote
			gameVote = new GameVote();
			gameVote.setUserId(userId);
			gameVote.setGameId(gameId);
			gameVote.setMemberId(theMemberId);
			gameVote.setLastUpdatedGmtDate(new Date());
			gameVote.setVoteType(theVoteType);
			gameVote.setIsTally(NO_TALLY);
			gameVote.setTeamId(teamId);
			em.persist(gameVote);
			log.info("first time user voted for this game so created the user's vote record");
			
			// increment the tally for the member just voted for
			incrementTally(theMemberId, gameId, theVoteType, teamId);
			log.info("incremented vote count for memberId = " + theMemberId);
			
		} catch (NonUniqueResultException e) {
			log.severe("should never happen - two or more gameVotes for same userId/gameId/memberId/voteType");
		} finally {
			em.close();
		}
	}
	
	// expects the member Tally exists - if it doesn't, an error is logged
	public static void decrementTally(String theMemberId, String theGameId, String theVoteType) {
		EntityManager em = EMF.get().createEntityManager();
		try {
			GameVote memberTally = (GameVote)em.createNamedQuery("GameVote.getByGameIdAndMemberIdAndVoteTypeAndIsTally")
				.setParameter("gameId", theGameId)
				.setParameter("memberId", theMemberId)
				.setParameter("voteType", theVoteType)
				.setParameter("isTally", YES_TALLY)
				.getSingleResult();
			int count = memberTally.getVoteCount();
			count = count - 1;
			memberTally.setVoteCount(count);
			log.info("decremented count for memberId = " + theMemberId + " new count = " + count);
		} catch (NoResultException e) {
			log.severe("member tally should have already existed");
		} catch (NonUniqueResultException e) {
			log.severe("should never happen - two or more games have same key");
		} finally {
			em.close();
		}
	}
	
	// If the member Tally does not exist, then create a new one
	public static void incrementTally(String theMemberId, String theGameId, String theVoteType, String theTeamId) {
		EntityManager em = EMF.get().createEntityManager();
		try {
			GameVote memberTally = (GameVote)em.createNamedQuery("GameVote.getByGameIdAndMemberIdAndVoteTypeAndIsTally")
				.setParameter("gameId", theGameId)
				.setParameter("memberId", theMemberId)
				.setParameter("voteType", theVoteType)
				.setParameter("isTally", YES_TALLY)
				.getSingleResult();
			int count = memberTally.getVoteCount();
			count = count + 1;
			memberTally.setVoteCount(count);
			log.info("incremented count for memberId = " + theMemberId + " new count = " + count);
		} catch (NoResultException e) {
			// this is not an error - member tally does not exist yet so create it
			GameVote gameVote = new GameVote();
			gameVote.setGameId(theGameId);
			gameVote.setMemberId(theMemberId);
			gameVote.setLastUpdatedGmtDate(new Date());
			gameVote.setVoteType(theVoteType);
			gameVote.setIsTally(YES_TALLY);
			gameVote.setVoteCount(1);
			gameVote.setTeamId(theTeamId);
			em.persist(gameVote);
			log.info("created a new Tally for memberId = " + theMemberId);
		} catch (NonUniqueResultException e) {
			log.severe("should never happen - two or more games have same key");
		} finally {
			em.close();
		}
	}
	
	public static Boolean isVoteTypeValid(String theVoteType) {
		if(theVoteType.equalsIgnoreCase(MVP_VOTE_TYPE)) {
			return true;
		}
		return false;
	}
	
	// returns null if no members received any votes or if there was an error determining the MVP
	public static String getMvp(Game theGame) {
		EntityManager em = EMF.get().createEntityManager();
		String gameId = KeyFactory.keyToString(theGame.getKey());
		String mvpMemberId = null;
		
		try {
			List<GameVote> memberTallies = (List<GameVote>)em.createNamedQuery("GameVote.getByGameIdAndVoteTypeAndIsTally")
				.setParameter("gameId", gameId)
				.setParameter("voteType", MVP_VOTE_TYPE)
				.setParameter("isTally", YES_TALLY)
				.getResultList();
			log.info("getMvp(): number of memberTallies = " + memberTallies.size());
			
			// find member with the most votes
			// TODO how do we handle ties?
			Integer mostVotes = 0;
			for(GameVote mt : memberTallies) {
				if(mt.getVoteCount().compareTo(mostVotes) > 0) {
					mvpMemberId = mt.getMemberId();
					mostVotes = mt.getVoteCount();
				}
			}
		} catch (Exception e) {
			log.severe("getMvp() exception = " + e.getMessage());
		} finally {
			em.close();
		}
		
		return mvpMemberId;
	}
	
}
