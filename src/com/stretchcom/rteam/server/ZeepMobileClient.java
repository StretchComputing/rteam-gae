package com.stretchcom.rteam.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.text.DateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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
import org.apache.commons.codec.binary.Base64;

public class ZeepMobileClient {
	// For your user to use the JOIN command, they simply text "JOIN rTeam" to 88147

	
	//private static final Logger log = Logger.getLogger(ZeepMobileClient.class.getName());
	private static RskyboxClient log = new RskyboxClient();
	
	// HTTP Methods
	private static final String HTTP_PUT = "PUT";
	private static final String HTTP_POST = "POST";

	private static final String API_KEY = "4150569a-c214-4b08-a4ae-6c117d645911";
	private static final String SECRET_KEY = "84706af3e2d05a4f6ed6960b61ec2b8e9ef51625";
	private static final String POST_URL = "https://api.zeepmobile.com/messaging/2008-07-14/send_message";
	
	private static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
	
	public static String postSms(String theUserId, String theMessage) {
		
		// TODO truncate message if it is greater than MAX size
		
		String response = null;
		try {
			URL url = new URL(POST_URL);
			response = send(url, theUserId, theMessage);
		} catch (MalformedURLException e) {
			log.exception("ZeepMobileClient:postSms:MalformedExeption", "", e);
		}
		
		return response;
	}
	
	// theUrl: complete url
	// theMessage: the SMS message to send.
	static private String send(URL theUrl, String theUserId, String theMessage) {
		log.debug("ZeepMobileClient::send theUrl = " + theUrl.toString());
		log.debug("ZeepMobileClient::send theUserId = " + theUserId);
		log.debug("ZeepMobileClient::send thePayload = " + theMessage);

		String response = "";
		HttpURLConnection connection = null;
		OutputStreamWriter writer = null;
		InputStreamReader reader = null;
		try {
			/////////////////////
			// Prepare connection
			/////////////////////
			connection = (HttpURLConnection)theUrl.openConnection();
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setUseCaches(false);
			connection.setAllowUserInteraction(false);
			connection.setRequestMethod(HTTP_POST);
			
			// TODO not sure Date and Host need to be explicitly added by the code????
			connection.setRequestProperty("Date", httpDate());
			connection.setRequestProperty("Host", "zeepmobile.com");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			//connection.setRequestProperty("Content-Type", "application/x-www-form-urlenGistd");
			connection.setRequestProperty("Authorization", getAuthentication(theUserId, theMessage));
			
			// user_id=[user_id]&body=[message body]
			String encodedMessage = URLEncoder.encode(theMessage, "UTF-8");
			String encodedUserId = URLEncoder.encode(theUserId, "UTF-8");
			String content = "user_id=" + encodedUserId + "&body=" + encodedMessage;
			log.debug("content = " + content);
			connection.setRequestProperty("Content-Length", ""+content.length());

			////////////////////
			// Send HTTP Request
			////////////////////
			connection.connect();
			
			writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
			writer.write(content);
			writer.flush();

			////////////////////
			// Get HTTP response
			////////////////////
			int responseCode = connection.getResponseCode();
			log.debug("responseCode = " + responseCode);
			
			if (responseCode == HttpURLConnection.HTTP_OK) {
				// read-back the response
				reader = new InputStreamReader(connection.getInputStream());
				BufferedReader in = new BufferedReader(reader);
				StringBuffer responseBuffer = new StringBuffer();
				while (true) {
					String inputLine = in.readLine();
					if(inputLine == null) {break;}
					responseBuffer.append(inputLine);
				}
				in.close();
				response = responseBuffer.toString();
			} else // Server returned HTTP error code.
			{
				log.error("ZeepMobileClient:postSms:errorCode", "send server returned error code: " + responseCode);
			}

		} catch (UnsupportedEncodingException ex) {
			log.exception("ZeepMobileClient:postSms:UnsupportedEncodingException", "", ex);
		} catch (MalformedURLException ex) {
			log.exception("ZeepMobileClient:postSms:MalformedURLException", "", ex);
		} catch (IOException ex) {
			log.exception("ZeepMobileClient:postSms:IOException", "", ex);
		} finally {
			try {
				if (writer != null) {writer.close();}
			} catch (Exception ex) {
				log.exception("ZeepMobileClient:postSms:Exception1", "", ex);
			}
			try {
				if (reader != null) {reader.close();}
			} catch (Exception ex) {
				log.exception("ZeepMobileClient:postSms:Exception2", "", ex);
			}
			if (connection != null) {connection.disconnect();}
		}

		return response;
	}
	
	
	private static String getAuthentication(String theUserId, String theMessage) {
		String authentication = null;
		try {
		      // (eg. Sat, 12 Jul 2008 09:04:28 GMT) http_date = Time.now.httpdate
		      String encodedMessage = URLEncoder.encode(theMessage, "UTF-8");
		      String encodedUserId = URLEncoder.encode(theUserId, "UTF-8");
		      String parameters = "user_id=" + encodedUserId + "&body=" + encodedMessage;
		      String canonicalString = API_KEY + httpDate() + parameters;
		      System.out.println(canonicalString);
		      
		      Mac mac = Mac.getInstance("HmacSHA1");
		      byte[] keyBytes = SECRET_KEY.getBytes("UTF8");
		      SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");
		      mac.init(signingKey);
		      
		      byte[] signBytes = mac.doFinal(canonicalString.getBytes("UTF8"));
		      String b64Mac = Base64.encodeBase64String(signBytes);
		      authentication = "Zeep " + API_KEY + ":" + b64Mac;
		      
		    }
		    catch (NoSuchAlgorithmException e) {
		      log.exception("ZeepMobileClient:getAuthentication:NoSuchAlgorithmException", "", e);
		    }
		    catch (UnsupportedEncodingException e) {
			      log.exception("ZeepMobileClient:getAuthentication:UnsupportedEncodingException", "", e);
		    }
		    catch (InvalidKeyException e) {
			      log.exception("ZeepMobileClient:getAuthentication:InvalidKeyException", "", e);
		    }
		    
		    log.debug("authentication = " + authentication);
		    return authentication;
	}
	
	private static String httpDate() {
	      SimpleDateFormat httpDateFormat = new SimpleDateFormat();
	      httpDateFormat.applyPattern(PATTERN_RFC1123);
	      httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	      return httpDateFormat.format(new Date());
    }	
	
	// return today's date in format: 2008-07-14
	private static String getTodayDate() {
		Date date = Calendar.getInstance().getTime();
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		return formatter.format(date);
	}
	
	
}
