package com.stretchcom.rteam.server;

import java.util.logging.Logger;

import org.restlet.gwt.data.Reference;

public class FacebookClient {
	//private static final Logger log = Logger.getLogger(FacebookClient.class.getName());
	private static RskyboxClient log = new RskyboxClient();
	
	private static final String APPPLICATION_ID = "178840355459758";
	private static final String APPPLICATION_SECRET = "4053ef0d83fa3fca87b4803b03fe066e";
	private static final String BASE_AUTHORIZATION_URL = "https://graph.facebook.com/oauth/authorize";
	private static final String BASE_ACCESS_TOKEN_URL = "https://graph.facebook.com/oauth/access_token";
	private static final String TOUCH_DISPLAY = "touch";
	
	// see permissions at: http://developers.facebook.com/docs/authentication/permissions
	private static final String SMS_PERMISSION = "sms";
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////
	// TODO
	// implement Deauthorize Callback as described: http://developers.facebook.com/docs/authentication/
	// If facebook deauthorized, may need to reactivate email. A member must have either facebook or
	// email enabled.
	///////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	// based on instructions at: http://developers.facebook.com/docs/authentication/
	public static String buildAuthorizationUrl(String theOneUseToken) {
		String authorizationUrl = "";
		
		// client_id is the facebook application ID
		String encodedClientId = Reference.encode(APPPLICATION_ID);
		
		// the rTeam callback URL is just a query parameter in this URL so it must be encoded
		String callbackUrl = RteamApplication.BASE_URL_WITH_SLASH + RteamApplication.GWT_HOST_PAGE + "?facebooktoken=" + theOneUseToken;
		String encodedCallbackUrl = Reference.encode(callbackUrl);
		
		// requesting 'extended permissions' to send messages (sms permission)
		authorizationUrl = BASE_AUTHORIZATION_URL + "?client_id=" + encodedClientId + "&redirect_uri=" + encodedCallbackUrl +
		                   "&scope=" + SMS_PERMISSION + "&display=" + TOUCH_DISPLAY;
		return authorizationUrl;
	}
	
	// based on instructions at: http://developers.facebook.com/docs/authentication/
	// theCode: returned by facebook in the argument 'code' when the rTeam callback URL is invoked
	public static String buildAccessTokenUrl(String theOneUseToken, String theCode) {
		String accessTokenUrl = "";
		
		// client_id is the facebook application ID
		String encodedClientId = Reference.encode(APPPLICATION_ID);
		
		// the rTeam callback URL is just a query parameter in this URL so it must be encoded
		String callbackUrl = RteamApplication.BASE_URL_WITH_SLASH + RteamApplication.GWT_HOST_PAGE + "?facebooktoken=" + theOneUseToken;
		String encodedCallbackUrl = Reference.encode(callbackUrl);
		
		// be safe and URL encode the 'code' too
		String encodedCode = Reference.encode(theCode);
		
		// Application secret is the client_secret
		String encodedClientSecret = Reference.encode(APPPLICATION_SECRET);
		
		accessTokenUrl = BASE_ACCESS_TOKEN_URL + "?client_id=" + encodedClientId + "&redirect_uri=" + encodedCallbackUrl +
		                   "&client_secret=" + encodedClientSecret + "&code=" + encodedCode;
		return accessTokenUrl;
	}

}
