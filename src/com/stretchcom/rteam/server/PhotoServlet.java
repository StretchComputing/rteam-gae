package com.stretchcom.rteam.server;

import java.io.IOException;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

@SuppressWarnings("serial")
public class PhotoServlet extends HttpServlet {
    //private static final Logger log = Logger.getLogger(PhotoServlet.class.getName());
	private static RskyboxClient log = new RskyboxClient();
    
    private static final String PHOTO_EXT = ".jpg";

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.debug("PhotoServlet.doPost() entered - SHOULD NOT BE CALLED!!!!!!!!!!!!!!!!!");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.debug("PhotoServlet.doGet() entered");
        ServletOutputStream out = null;
        resp.setContentType("image/jpeg");

        try {
    		
        	//////////////////////
        	// Authorization Rules
        	//////////////////////
        	// TODO
         	// any authorization rules accessing images or are the all public?

            String activityId = getPathId(req);
        	byte[] photo = getPhoto(activityId);
            if (photo == null) {return;}
            resp.setContentLength(photo.length);
            out = resp.getOutputStream();
            out.write(photo);
        } catch (Exception e) {
            log.debug("PhotoServlet exception = " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }
    
	// returns activity ID or null if error
	private String getPathId(HttpServletRequest theReq) {
		// extract the crash detect ID from the URL (for http://hostname.com/mywebapp/servlet/MyServlet/a/b;c=123?d=789, returns /a/b;c=123
		String pathInfo = theReq.getPathInfo();
		log.debug("Image Servlet URL pathInfo = " + pathInfo);
		if(pathInfo == null || pathInfo.length() == 0) {return null;}
		
		// if all is going well, pathInfo should have the following format:  /<activityId>
		String activityId = null;
		if(pathInfo.startsWith("/")) {
			activityId = pathInfo.substring(1);
			log.debug("activity ID = " + activityId);
		}
		return activityId;
	}

    // returns base64 decoded photo associated with the specified activity if successful; null otherwise.
    private byte[] getPhoto(String theActivityId) {
        byte[] rawPhoto = null;

        // using the activity ID, retrieve the appropriate photo
        EntityManager em = EMF.get().createEntityManager();
        try {
            Key activityKey;
			try {
				activityKey = KeyFactory.stringToKey(theActivityId);
			} catch (Exception e) {
	            log.exception("PhotoServlet:getPhoto:Exception", "two or more Activity have same key", e);
				return null;
			}
			
            Activity activity = null;
            activity = (Activity) em.createNamedQuery("Activity.getByKey").setParameter("key", activityKey)
                .getSingleResult();

            String photoBase64 = activity.getPhotoBase64();
            if(photoBase64 != null) {
                rawPhoto = Base64.decodeBase64(photoBase64);
            } else {
            	log.error("PhotoServlet:getPhoto:photo", "no photo for specified activity");
            }
        } catch (NoResultException e) {
            // activity ID passed in is not valid
            log.debug("Activity not found");
        } catch (NonUniqueResultException e) {
            log.exception("PhotoServlet:getPhoto:NonUniqueResultException", "two or more Activity have same key", e);
        }

        return rawPhoto;
    }
}
