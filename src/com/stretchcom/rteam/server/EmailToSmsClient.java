package com.stretchcom.rteam.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.apache.commons.codec.binary.Base64;


public class EmailToSmsClient {
	//private static final Logger log = Logger.getLogger(EmailToSmsClient.class.getName());
	private static RskyboxClient log = new RskyboxClient();
	
	// HTTP Methods
	private static final String HTTP_PUT = "PUT";
	private static final String HTTP_POST = "POST";
	private static final String HTTP_GET = "GET";
	
	// Email-to-SMS URL
	private static final String EMAIL_TO_SMS_BASE_URL = "https://50.57.64.254:8443/rTeamSms/";
	//private static final String EMAIL_TO_SMS_BASE_URL = "http://50.57.64.254:8080/rTeamSms/";
	private static final String EMAIL_RESOURCE_URI = "email";
	private static final String IS_ALIVE_RESOURCE_URI = "vitals";
	private static final String PUSH_RESOURCE_URI = "pushNotifications";
	
	private static final String BASIC_AUTH_USER_NAME = "rTeamLogin";
	private static final String BASIC_AUTH_PASSWORD = "test123";
	private static final String RTEAM_APPLICATION = "rTeam";
	private static final String DEVELOPMENT_PUSH_TYPE = "Development";
	private static final String PRODUCTION_PUSH_TYPE = "Production";
	private static final String IOS_CLIENT = "iOS";
	private static final String ANDROID_CLIENT = "Android";
	
	private static final int TEN_SECONDS_IN_MILLIS = 10000;
	
	
	// can return NULL
	static public String sendMail(String theSubject, String theBody, String theToEmailAddress, String theFromEmailAddress) {
		log.debug("EmailToSmsClient::sendMail entered");
		
		//////////////////////////
		// Create the JSON Payload
		//////////////////////////
		JSONObject jsonPayload = null;
		try {
			jsonPayload = new JSONObject();
			jsonPayload.put("subject", theSubject);
			jsonPayload.put("body", theBody);
			jsonPayload.put("toEmailAddress", theToEmailAddress);
			jsonPayload.put("fromEmailAddress", theFromEmailAddress);
		} catch (JSONException e1) {
			log.exception("EmailToSmsClient:sendMail:JSONException", "error building JSON object", e1);
			return null;
		}
		
		String response = null;
		String urlStr = EMAIL_TO_SMS_BASE_URL + EMAIL_RESOURCE_URI;
		URL url = null;
		try {
			url = new URL(urlStr);
			response = send(url, HTTP_POST, jsonPayload.toString(), BASIC_AUTH_USER_NAME, BASIC_AUTH_PASSWORD);
		} catch (MalformedURLException e) {
			log.exception("EmailToSmsClient:sendMail:MalformedURLException", "", e);
		}
		
		return response;
	}
	
	//static public String {
	static public String push(String theAlert, Integer theBadge, String theDeviceToken, Boolean theIsDeveloper)  {
		log.debug("EmailToSmsClient::push entered");
		
		//////////////////////////
		// Create the JSON Payload
		//////////////////////////
		JSONObject jsonPayload = null;
		try {
			String pushType = PRODUCTION_PUSH_TYPE;
			if(theIsDeveloper) pushType = DEVELOPMENT_PUSH_TYPE;
			
			JSONArray devicesJsonArray = new JSONArray();
			jsonPayload = new JSONObject();
			
			JSONObject jsonDevice = new JSONObject();
			jsonDevice.put("Client", IOS_CLIENT);
			jsonDevice.put("DeviceToken", theDeviceToken);
			jsonDevice.put("PushType", pushType);
			
			devicesJsonArray.put(jsonDevice);
			jsonPayload.put("Devices", devicesJsonArray);
			jsonPayload.put("Message", theAlert);
			jsonPayload.put("Badge", theBadge);
			jsonPayload.put("Application", RTEAM_APPLICATION);
			
		} catch (JSONException e1) {
			log.exception("EmailToSmsClient:push:JSONException", "error building JSON object", e1);
			return null;
		}
		
		String response = null;
		String urlStr = EMAIL_TO_SMS_BASE_URL + PUSH_RESOURCE_URI;
		URL url = null;
		try {
			url = new URL(urlStr);
			response = send(url, HTTP_POST, jsonPayload.toString(), BASIC_AUTH_USER_NAME, BASIC_AUTH_PASSWORD);
		} catch (MalformedURLException e) {
			log.exception("EmailToSmsClient:push:MalformedURLException", "", e);
		}
		
		return response;
	}

	
	
	// can return NULL
	static public String isAlive() {
		log.debug("EmailToSmsClient::isAlive entered");
		
		//////////////////////////
		// Create the JSON Payload
		//////////////////////////
		String response = null;
		String urlStr = EMAIL_TO_SMS_BASE_URL + IS_ALIVE_RESOURCE_URI;
		URL url = null;
		try {
			url = new URL(urlStr);
			response = send(url, HTTP_GET, null, BASIC_AUTH_USER_NAME, BASIC_AUTH_PASSWORD);
		} catch (MalformedURLException e) {
			log.exception("EmailToSmsClient:sendMail:MalformedURLException", "", e);
		}
		
		return response;
	}

	
	// theUrl: complete url
	// thePayload: the JSON payload to send, if any.  Can be null.
	// theHttpMethod: one of GET POST HEAD OPTIONS PUT DELETE TRACE
	static private String send(URL theUrl, String theHttpMethod, String theJsonPayload, 
			                   String theBasicAuthUserName, String theBasicAuthPassword) {
		log.debug("EmailToSmsClient::send theUrl = " + theUrl.toString());
		log.debug("EmailToSmsClient::send theJsonPayload = " + theJsonPayload);
		log.debug("EmailToSmsClient::send theHttpMethod = " + theHttpMethod);

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
			connection.setRequestMethod(theHttpMethod);
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setConnectTimeout(TEN_SECONDS_IN_MILLIS);
			connection.setRequestProperty("accept", "*/*");
			connection.setRequestProperty("accept-encoding", "*/*");
			
			
			///////////////////////
			// Basic Authentication
			///////////////////////
			StringBuilder buf = new StringBuilder(theBasicAuthUserName);
			buf.append(':');
			buf.append(theBasicAuthPassword);
			byte[] bytes = null;
			try {
				bytes = buf.toString().getBytes("ISO-8859-1");
			} catch (java.io.UnsupportedEncodingException uee) {
				log.exception("EmailToSmsClient:send:UnsupportedEncodingException", "", uee);
			}

			String header = "Basic " + Base64.encodeBase64String(bytes);
			connection.setRequestProperty("Authorization", header);

			////////////////////
			// Send HTTP Request
			////////////////////
			connection.connect();
			
			if(theJsonPayload == null) {
				theJsonPayload = "{}";
			}
			if(!theHttpMethod.equalsIgnoreCase(HTTP_GET)) {
				writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
				writer.write(theJsonPayload);
				writer.flush();
			}

			////////////////////
			// Get HTTP response
			////////////////////
			int responseCode = connection.getResponseCode();
			log.debug("responseCode = " + responseCode);
			
			if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
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
				log.error("EmailToSmsClient:send:errorCode", "server returned error code: " + responseCode);
			}

		} catch (UnsupportedEncodingException ex) {
			log.exception("EmailToSmsClient:send:UnsupportedEncodingException2", "", ex);
		} catch (MalformedURLException ex) {
			log.exception("EmailToSmsClient:send:MalformedURLException", "", ex);
		} catch (IOException ex) {
			log.exception("EmailToSmsClient:send:IOException", "", ex);
		} finally {
			try {
				if (writer != null) {writer.close();}
			} catch (Exception ex) {
				log.exception("EmailToSmsClient:send:Exception1", "", ex);
			}
			try {
				if (reader != null) {reader.close();}
			} catch (Exception ex) {
				log.exception("EmailToSmsClient:send:Exception2", "", ex);
			}
			if (connection != null) {connection.disconnect();}
		}

		return response;
	}
}
