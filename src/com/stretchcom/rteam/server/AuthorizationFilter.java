package com.stretchcom.rteam.server;

import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.routing.Filter;

public class AuthorizationFilter extends Filter{
	private static final Logger log = Logger.getLogger(AuthorizationFilter.class.getName());
	
	public AuthorizationFilter(Context context) {
		super(context);
	}
	
	protected int beforeHandle(Request request, Response response) {
		if(validAuthentication(request)) {
			return Filter.CONTINUE;
		} else {
			// return HTTP 401 status - unauthorized
			response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
			return Filter.STOP;
		}
	}
	
	private boolean validAuthentication(Request request) {
		EntityManager em = EMF.get().createEntityManager();
		
		Map<String, Object> map = request.getAttributes();
		Object obj = map.get("org.restlet.http.headers");
		log.info("obj = " + obj.toString());
		
		String url = request.getResourceRef().getIdentifier();
		Method method = request.getMethod();
		String urlLower = url.toLowerCase();
		//String rawEntity = request.getEntityAsText();
		//::TODO change logging level below
		log.info("validAuthentication(): urlLower = " + urlLower);
		log.info("validAuthentication(): method = " + method.getName());
		//log.info("rawEntity = " + rawEntity);
		if(urlLower.endsWith("users") && method.equals(Method.POST)) {
			// API 'Create a New User' does not use token authorization
			log.info("validAuthentication(): API 'Create a New User' does not use token authorization");
			return true;
		} else if(urlLower.endsWith("mobilecarriers") && method.equals(Method.GET)) {
			// API 'Get Mobile Carriers List' does not use token authorization
			log.info("validAuthentication(): API 'Get Mobile Carriers List' does not use token authorization");
			return true;
		} else if( urlLower.contains("sendemailtask") && method.equals(Method.POST)) {
			// email task does not use token authorization
			log.info("validAuthentication(): email task does not use token authorization");
			return true;
		} else if( urlLower.contains("member?") && method.equals(Method.GET)) {
			// API 'Get Membership Status' does not use token authorization
			log.info("validAuthentication(): API 'Get Membership Status' does not use token authorization");
			return true;
		} else if( urlLower.contains("member?") && urlLower.contains("oneUseToken") && method.equals(Method.DELETE)) {
			// API 'Delete Member Confirmation' does not use token authorization
			log.info("validAuthentication(): API 'Delete Member Confirmation' does not use token authorization");
			return true;
		} else if( urlLower.contains("messagethread?") && urlLower.contains("oneusetoken") && method.equals(Method.PUT)) {
			// API 'MessageThread Confirmation' does not use token authorization
			log.info("validAuthentication(): API 'MessageThread Confirmation' does not use token authorization");
			return true;
		} else if(urlLower.contains("user?") && (urlLower.contains("emailaddress") || urlLower.contains("oneusetoken")) && 
				  (method.equals(Method.GET) || method.equals(Method.OPTIONS))) {
			//::TODO  remove hack above allowing Options method - needed for restlet gwt bug 
			// API 'Get User Token' does not use token authorization
			// API 'Get User Confirmation Info' does not use HTTP based token authorization (a token is passed as a query parameter)
			// NOTE: 'Get User Info' does not match this condition because it does not contain query parameters -- no "?" after user
			log.info("validAuthentication(): API 'Get User Token' does not use token authorization");
			return true;
		}  else if(urlLower.contains("user/passwordresetquestion") && (method.equals(Method.GET) || method.equals(Method.OPTIONS))) {
			//::TODO  remove hack above allowing Options method - needed for restlet gwt bug
			// API 'Get User Password Question' does not use token authorization
			log.info("validAuthentication(): API 'Get User Password Question' does not use token authorization");
			return true;
		} else if(urlLower.contains("user?") && method.equals(Method.PUT)) {
			// API 'User Password Reset' does not use token authorization
			// NOTE: 'Update User' does not match this condition because it does not contain query parameters -- no "?" after user
			log.info("validAuthentication(): API 'User Password Reset' does not use token authorization");
			return true;
		} else if(urlLower.contains("oauth?") && urlLower.contains("oneusetoken") && method.equals(Method.PUT)) {
			// API 'Get Twitter Confirmation' does not use token authorization
			log.info("validAuthentication(): API 'Get Twitter Confirmation' does not use token authorization");
			return true;
		} else if(urlLower.contains("?uaoutoken")) {
			//////////////////////////////////////////////////////////////
			// NOTE::this check need to proceed the generic GWT one below
			//////////////////////////////////////////////////////////////
			
			// extract token
			//::TODO could the '=' match too early in the URL because of encoding?
//			int beginIndex = urlLower.indexOf("=");
//			String oneUseToken = urlLower.substring(beginIndex+1);
//			MessageThreadResource.handleUserMemberConfirmEmailResponse(oneUseToken);
//			log.info("validAuthentication(): User/Member Authentication One Use (UAOU) token = " + oneUseToken);
			return true;
		} else if( (urlLower.contains("rteam") && (urlLower.contains("html") || urlLower.contains("css")) ) ||
				   urlLower.contains("images")) {
			log.info("validAuthentication(): GWT Page found -- passing authentication without token lookup");
			return true;
		} else if (urlLower.contains("favicon")) {
			return true;
		} else if(urlLower.contains("/users/migration") && method.equals(Method.PUT)) {
			log.info("validAuthentication(): API 'Users Migration' does not use token authorization");
			return true;
		}  else if( urlLower.contains("sms") && method.equals(Method.POST)) {
			// sms task does not use token authorization
			log.info("validAuthentication(): sms task does not use token authorization");
			return true;
		} else if( urlLower.contains("/cron") && method.equals(Method.GET)) {
			log.info("validAuthentication(): CRON jobs do not use token authorization");
			return true;
		} 
		
		if(request.getChallengeResponse() == null) {
			log.severe("unrecognized request: either URL is bad or HTTP Command is wrong");
			return false;
		}
		
		String login = request.getChallengeResponse().getIdentifier();
		String token = new String(request.getChallengeResponse().getSecret());
		log.info("login = " + login);
		log.info("token = " + token);
		try {
			log.info("1");
			User user = (User)em.createNamedQuery("User.getByToken")
				.setParameter("token", token)
				.getSingleResult();
			log.info("2");
			// store "current" user in request attributes so it can be used by Restlets downstream
			request.getAttributes().put(RteamApplication.CURRENT_USER, user);
			log.finer("validAuthentication(): token found, request has been authenticated");
		} catch (NoResultException e) {
			log.finer("validAuthentication(): token not found, request failed authentication");
			return false;
		} catch (NonUniqueResultException e) {
			log.severe("should never happen - two or more users have same token");
			e.printStackTrace();
		} catch (Exception e) {
			log.info("exception = " + e.getMessage() );
		}
		log.info("returning from validAuthentication()");
		return true;
	}
}
