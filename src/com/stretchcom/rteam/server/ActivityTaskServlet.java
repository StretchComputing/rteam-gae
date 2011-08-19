package com.stretchcom.rteam.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.restlet.data.Status;

// DEPRECATED
// -----------
// Originally, Activity was designed only to work with Twitter, so if Twitter was disabled, all
// the activity had to be deleted just to keep things clean if Activity was re-associated with
// a new Twitter account later on.  Now that Activity can stand alone without Twitter and a
// "synch" from Activity to Twitter is done when Twitter is added, it is no longer necessary
// to delete activity when Twitter is deassociated.

public class ActivityTaskServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(ActivityTaskServlet.class.getName());
	private static int MAX_TASK_RETRY_COUNT = 3;

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.info("ActivityTaskServlet.doGet() entered - SHOULD NOT BE CALLED!!!!!!!!!!!!!!!!!");
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		log.info("EmailTaskServlet.doPost() entered");
		String response = "team activities deleted successfully";
		resp.setContentType("text/plain");

		try {
			String teamId = req.getParameter("teamId");
			log.info("teamId parameter: "	+ teamId);
			
			// need to get the retry count
			String taskRetryCountStr = req.getHeader("X-AppEngine-TaskRetryCount");
			// default the retry count to max because if it can't be extracted, we are packing up the books and going home
			int taskRetryCount = MAX_TASK_RETRY_COUNT;
			try {
				taskRetryCount = new Integer(taskRetryCountStr);
			} catch (Exception e1) {
				log.info("should never happen, but no harm, no foul -- default above kicks in");
			}
			log.info("taskRetryCount = " + taskRetryCount);

		    Properties props = new Properties();
		    Session session = Session.getDefaultInstance(props, null);
		    
		    // ensure valid parameters
		    if(teamId == null || teamId.length() == 0) {
		    	log.severe("ActivityTaskServlet: null or empty parameter");
		    	return;
		    }

	    	// Delete all activities associated with this team
		    EntityManager em = EMF.get().createEntityManager();
	        try {
        		List<Activity> activities = (List<Activity>)em.createNamedQuery("Activity.getByTeamId")
    				.setParameter("teamId", teamId)
    				.getResultList();
        		log.info("ActivityTaskServlet: # of team activities to be deleted = " + activities.size());
    		
	    		//::TODO do batch delete instead
	    		for(Activity a : activities) {
	        	    em.remove(a);
	    		}
	    		
	    		resp.setStatus(HttpServletResponse.SC_OK);
	        } catch (Exception e) {
				log.severe("ActivityTaskServlet: EntityManager exception = " + e.getMessage());
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
			response = "Should not happen. Remove Team Activity Task Servlet: failure : " + ex.getMessage();
			log.info(response);
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.getWriter().println(response);
		}
	}
}
