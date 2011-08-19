package com.stretchcom.rteam.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;
import twitter4j.http.RequestToken;

import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.repackaged.com.google.common.util.Base64;

public class TwitterClient {
	private static final Logger log = Logger.getLogger(TwitterClient.class.getName());
	
	private static final String API_KEY = "fAqwt1YyGDnhH54AjNRYQ";
	private static final String CONSUMER_KEY = "fAqwt1YyGDnhH54AjNRYQ";
	private static final String CONSUMER_SECRET = "BLLxOdGgZHWosQOMc1eS9ieC4yrrWHOj331C3eIkQ";
	
	private static final String REQUEST_TOKEN_URL = "https://api.twitter.com/oauth/request_token";
	private static final String ACCESS_TOKEN_URL = "https://api.twitter.com/oauth/access_token";
	private static final String AUTHORIZE_URL  = "https://api.twitter.com/oauth/authorize";
	
	private static final String SCREEN_NAME_PARAMETER  = "screen_name";
	
	private static final Integer PAGE_COUNT  = 100;
	private static final Integer MAX_STATUSES  = 1000;
	public static final Integer MAX_TWITTER_CHARACTER_COUNT  = 140;
	
	// Returns Twitter authorization URL if successful, null otherwise. Typical reason for failure: cannot access twitter.
	// theRequestTokenInfo out parameter
	// theRequestTokenInfo[0]: twitter authorization URL
	// theRequestTokenInfo[1]: twitter request token
	// theRequestTokenInfo[2]: twitter request token secret
	public static boolean getRequestToken(String theOneUseToken, List<String> theRequestTokenInfo) {
			boolean returnValue = true;
		    Twitter twitter;
			RequestToken requestToken;
			try {
				twitter = new TwitterFactory().getInstance();
				twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
				String callbackUrl = RteamApplication.BASE_URL_WITH_SLASH + RteamApplication.GWT_HOST_PAGE + "?twittertoken=" + theOneUseToken;
				log.info("callback URL sent to twitter = " + callbackUrl);
				requestToken = twitter.getOAuthRequestToken(callbackUrl);
				
				String authorizationUrl = requestToken.getAuthorizationURL();
				theRequestTokenInfo.add(authorizationUrl);
				String twitterRequestToken = requestToken.getToken();
				theRequestTokenInfo.add(twitterRequestToken);
				String twitterRequestTokenSecret = requestToken.getTokenSecret();
				theRequestTokenInfo.add(twitterRequestTokenSecret);
				log.info("twitter authorization URL = " + authorizationUrl);
				log.info("twitter request token = " + twitterRequestToken);
				log.info("twitter request token secret = " + twitterRequestTokenSecret);
			} catch (TwitterException e) {
				log.info("getRequestToken() twitter exception = " + e.getMessage());
				returnValue = false;
			} catch (Exception e) {
				log.info("getRequestToken() exception = " + e.getMessage());
				returnValue = false;
			}
			return returnValue;
		  }
	
	
	// Returns the access token (string, not twitter4j object) if successful, null otherwise.
	// theAccessTokenInfo out parameter
	// theAccessTokenInfo[0]: twitter access token
	// theAccessTokenInfo[1]: twitter access token secret
	// theAccessTokenInfo[2]: screen name
	public static boolean getAccessToken(String theTwitterRequestToken, String theTwitterRequestTokenSecret,
										 String theTwitterOauthVerifier, List<String> theAccessTokenInfo) {
		boolean returnValue = true;
		try {
			Twitter twitter = new TwitterFactory().getInstance();
			twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
			AccessToken accessToken = twitter.getOAuthAccessToken(theTwitterRequestToken, theTwitterRequestTokenSecret, theTwitterOauthVerifier);
			
			String twitterAccessToken = accessToken.getToken();
			theAccessTokenInfo.add(twitterAccessToken);
			log.info("twitter access token = " + twitterAccessToken);
			
			String twitterAccessTokenSecret = accessToken.getTokenSecret();
			theAccessTokenInfo.add(twitterAccessTokenSecret);
			log.info("twitter access token secret = " + twitterAccessTokenSecret);
			
			String screenNameParam = accessToken.getScreenName();
			theAccessTokenInfo.add(screenNameParam);
			log.info("screen name = " + screenNameParam);
			
			int userId = accessToken.getUserId();
			log.info("user id = " + userId);
		} catch (TwitterException e) {
			log.info("getAccessToken() twitter exception = " + e.getMessage());
			returnValue = false;
		} catch (Exception e) {
			log.info("getAccessToken() exception = " + e.getMessage());
			returnValue = false;
		}
		return returnValue;
	}
	
	@SuppressWarnings("deprecation")
	public static Status updateStatus(String theNewStatus, String theTwitterAccessToken, String theTwitterAccessTokenSecret) {
		Status returnValue = null;
		try {
			// TODO ensure Twitter 140 char count not exceeded
			Twitter twitter = new TwitterFactory().getInstance();
			twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
			twitter.setOAuthAccessToken(theTwitterAccessToken, theTwitterAccessTokenSecret);
			returnValue = twitter.updateStatus(theNewStatus);
			log.info("updated status sent to Twitter = " + returnValue.getText() + ". Status ID = " + returnValue.getId());
		} catch (TwitterException e) {
			log.info("updateStatus() twitter exception = " + e.getMessage());
		} catch (Exception e) {
			log.info("updateStatus() exception = " + e.getMessage());
		}
		return returnValue;
	}
	
	
	// The goal of this method is to retrieve all statuses newer than the specified since_id, though the call current will max out at
	// a certain number of total statuses. This 'max-ing' out is not exact since the granularity of checking is the page count.
	@SuppressWarnings("deprecation")
	public static List<Activity> getTeamActivities(Team theTeam, Long theSinceId) {
		List<Activity> activities = new ArrayList<Activity>();
		try {
			Twitter twitter = new TwitterFactory().getInstance();
			twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
			twitter.setOAuthAccessToken(theTeam.getTwitterAccessToken(), theTeam.getTwitterAccessTokenSecret());
			
			// call twitter, incrementing the page count each time, until nothing is returned or max status threshold exceeded.
			Paging paging = new Paging();
			paging.setCount(PAGE_COUNT);
			
			// found out via an exception that pageCount cannot be zero
			int pageCount = 1;
			paging.setPage(pageCount);
			
			// found out via exception that sinceId cannot be zero
			if(theSinceId > 0) {paging.setSinceId(theSinceId);}
			List<Status> allStatuses = new ArrayList<Status>();
			while(true) {
				List<Status> statuses = twitter.getHomeTimeline(paging);
				if(statuses.size() == 0) break;
				allStatuses.addAll(statuses);
				if(allStatuses.size() > MAX_STATUSES) {
					log.severe("TwitterClient::getTeamActivities() MAX_STATUSES exceeded");
				}
				pageCount++;
				paging.setPage(pageCount);
			}
			
			for(Status s : allStatuses) {
				Activity a = new Activity();
				a.setTwitterId(s.getId());
				a.setText(s.getText());
				a.setCreatedGmtDate(s.getCreatedAt());
				a.setTeamId(KeyFactory.keyToString(theTeam.getKey()));
				a.setTeamName(theTeam.getTeamName());
				activities.add(a);
			}
			
			log.info("TwitterClient::getTeamActivities() number of activties retrieved from Twitter = " + activities.size());
		} catch (TwitterException e) {
			log.info("getTeamActivities() twitter exception = " + e.getMessage());
		} catch (Exception e) {
			log.info("getTeamActivities() exception = " + e.getMessage());
			e.printStackTrace();
		}
		return activities;
	}
}
