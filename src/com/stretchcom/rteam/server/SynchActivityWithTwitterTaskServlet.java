package com.stretchcom.rteam.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class SynchActivityWithTwitterTaskServlet extends HttpServlet {
	//private static final Logger log = Logger.getLogger(SynchActivityWithTwitterTaskServlet.class.getName());
	private static RskyboxClient log = new RskyboxClient();
	
	
	private static int MAX_TASK_RETRY_COUNT = 3;
	private static int MAX_TWITTER_TWEETS_ON_SYNCH = 100;
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.debug("SynchActivityWithTwitterTaskServlet.doGet() entered - SHOULD NOT BE CALLED!!!!!!!!!!!!!!!!!");
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		log.debug("SynchActivityWithTwitterTaskServlet.doPost() entered");
		String response = "activities synched with Twitter successfully";
		resp.setContentType("text/plain");

		try {
			String teamId = req.getParameter("teamId");
			log.debug("teamId parameter: "	+ teamId);
			
			// need to get the retry count
			String taskRetryCountStr = req.getHeader("X-AppEngine-TaskRetryCount");
			// default the retry count to max because if it can't be extracted, we are packing up the books and going home
			int taskRetryCount = MAX_TASK_RETRY_COUNT;
			try {
				taskRetryCount = new Integer(taskRetryCountStr);
			} catch (Exception e1) {
				log.debug("should never happen, but no harm, no foul -- default above kicks in");
			}
			log.debug("taskRetryCount = " + taskRetryCount);

		    Properties props = new Properties();
		    Session session = Session.getDefaultInstance(props, null);
		    
		    // ensure valid parameters
		    if(teamId == null || teamId.length() == 0) {
		    	log.error("SynchActivityWithTwitterTaskServlet:doPost:parameters", "SynchActivityWithTwitterTaskServlet: null or empty parameter");
		    	return;
		    }
		    
		    Long newestCacheId = 0L;
		    EntityManager emTeam = EMF.get().createEntityManager();
		    Team team = null;
		    try {
    			Key teamKey = KeyFactory.stringToKey(teamId);
        		team = (Team)emTeam.createNamedQuery("Team.getByKey")
        			.setParameter("key", teamKey)
        			.getSingleResult();
        		newestCacheId = team.getNewestCacheId();
		    } catch (NoResultException e) {
	        	log.debug("SynchActivityWithTwitterTaskServlet: no result exception, team not found");
			} finally {
				emTeam.close();
			}
			
			if(newestCacheId.equals(0L)) {
				log.debug("SynchActivityWithTwitterTaskServlet: no activities for this team so no work to do");
				return;
			}

	    	// Synch up to the last 100 Activities with Twitter
		    EntityManager em = EMF.get().createEntityManager();
	        try {
				Long upperCacheId = newestCacheId + 1; // query returns activities less than upperCacheId
				Long lowerCacheId = upperCacheId - MAX_TWITTER_TWEETS_ON_SYNCH; // 
				if(lowerCacheId < 0L) {lowerCacheId = 0L;}
				
				List<Activity> teamRequestedActivities = (List<Activity>)em.createNamedQuery("Activity.getByTeamIdAndUpperAndLowerCacheIds")
					.setParameter("teamId", teamId)
					.setParameter("upperCacheId", upperCacheId)
					.setParameter("lowerCacheId", lowerCacheId)
					.getResultList();
				
				if(teamRequestedActivities.size() == 0) {
					log.error("SynchActivityWithTwitterTaskServlet:doPost:parameters", "SynchActivityWithTwitterTaskServlet: no activities found, but there should have been");
					return;
				}
				
				// because cacheId has been set, this will get sorted by created date - reverse chronological order
				Collections.sort(teamRequestedActivities);

				// Tweet activities oldest to newest
				int activityIndex = teamRequestedActivities.size() - 1;
				twitter4j.Status twitterStatus = null;
				while(true) {
					Activity a = teamRequestedActivities.get(activityIndex);
					// truncate if necessary
					String statusUpdate = a.getText();
					if(statusUpdate.length() > TwitterClient.MAX_TWITTER_CHARACTER_COUNT) {
						statusUpdate = statusUpdate.substring(0, TwitterClient.MAX_TWITTER_CHARACTER_COUNT - 2) + "..";
					}

					twitterStatus = TwitterClient.updateStatus(statusUpdate, team.getTwitterAccessToken(), team.getTwitterAccessTokenSecret());
					
					// if Twitter update failed just log and continue on
					if(twitterStatus == null) {
						log.error("SynchActivityWithTwitterTaskServlet:doPost:Twitter", "Twitter update failed, but continuing on ...");
					} else {
						a.setTwitterId(twitterStatus.getId());
					}
					
					activityIndex--;
					if(activityIndex < 0) {break;}
				}
	    		
	    		resp.setStatus(HttpServletResponse.SC_OK);
	        } catch (Exception e) {
				log.exception("SynchActivityWithTwitterTaskServlet:doPost:Exception", "SynchActivityWithTwitterTaskServlet: EntityManager exception = " + e.getMessage(), e);
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} finally {
			    em.close();
			}
		    
			// Return status depends on how many times this been attempted. If max retry count reached, return HTTP 200 so
		    // retry attempts stop.
		    if(taskRetryCount >= MAX_TASK_RETRY_COUNT) {
		    	resp.setStatus(HttpServletResponse.SC_OK);
		    }
		    
			resp.getWriter().println(response);
		}
		catch (Exception ex) {
			response = "Should not happen. SynchActivityWithTwitterTaskServlet: failure : " + ex.getMessage();
			log.debug(response);
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.getWriter().println(response);
		}
	}
}
