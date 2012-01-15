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
import org.restlet.Request;
import org.apache.commons.codec.binary.Base64;

public class RskyboxClient {
	private static final Logger log = Logger.getLogger(RskyboxClient.class.getName());
	
	// HTTP Methods
	private static final String HTTP_PUT = "PUT";
	private static final String HTTP_POST = "POST";
	
	// rSkybox credentials
	private static final String RSKYBOX_APPLICATION_ID = "ahRzfnJza3lib3gtc3RyZXRjaGNvbXITCxILQXBwbGljYXRpb24Y9agCDA";
	private static final String RSKYBOX_APPLICATION_TOKEN = "f59gi8rd80kl3sm94j4hpj33eg";
	private static final String RSKYBOX_BASIC_AUTH_USER_NAME = "rskyboxUser";
	
	private static final String RSKYBOX_SERVICE_PROVIDER = "GAE Server";
	private static final String RSKYBOX_USER_NAME = "rTeam Common Code";
	
	// Log Levels
	public static final String DEBUG = "debug";
	public static final String INFO = "info";
	public static final String ERROR = "error";
	public static final String EXCEPTION = "exception";
	
	// rSkybox URL https://rskybox-stretchcom.appspot.com/rest/v1/applications/<applicationId>/clientLogs
	private static final String RSKYBOX_BASE_URL_WITH_SLASH = "https://rskybox-stretchcom.appspot.com/rest/v1/applications/";
	
	public static void log(String theName, String theMessage, String theStackBackTrace, Request theRequest, Boolean theIncludeLocalLog) {
		// default log level is ERROR
		log(theName, ERROR, theMessage, theStackBackTrace, theRequest, theIncludeLocalLog);
	}
	
	public static void log(String theName, String theLevel, String theMessage, String theStackBackTrace, Request theRequest, Boolean theIncludeLocalLog) {
		if(theIncludeLocalLog != null && theIncludeLocalLog) {
			if(theLevel.equalsIgnoreCase(DEBUG) || theLevel.equalsIgnoreCase(INFO)) {log.info(theMessage);}
			if(theLevel.equalsIgnoreCase(ERROR) || theLevel.equalsIgnoreCase(EXCEPTION)) {log.severe(theMessage);}
		}
		
		if(!isLogLevelValid(theLevel)) {log.severe("bad log level = " + theLevel);}
		
		//////////////////////////
		// Create the JSON Payload
		//////////////////////////
		JSONObject jsonPayload = null;
		try {
			jsonPayload = new JSONObject();
			jsonPayload.put("logLevel", theLevel);
			jsonPayload.put("logName", theName);
			jsonPayload.put("message", theMessage);
			jsonPayload.put("stackBackTrace", theStackBackTrace);
			
			// TODO rSkybox should rename instanceUrl to something more generic - here we use the current user's login ID if there is a current user
			String endUser = RSKYBOX_USER_NAME;
			if(theRequest != null) {
				User currentUser = Utility.getCurrentUser(theRequest);
				if(currentUser != null) {
					endUser = currentUser.getEmailAddress();
				}
			}
			jsonPayload.put("userName", endUser);
			jsonPayload.put("instanceUrl", RSKYBOX_SERVICE_PROVIDER);
			//log.info("jsonPayload = " + jsonPayload.toString());
			
			String response = null;
			String urlStr = RSKYBOX_BASE_URL_WITH_SLASH + RSKYBOX_APPLICATION_ID + "/clientLogs";
			URL url = null;
			try {
				url = new URL(urlStr);
				response = send(url, jsonPayload.toString());
				if(response.length() > 0) {
					JSONObject jsonReturn = new JSONObject(response);
					if(jsonReturn.has("apiStatus")) {
						String apiStatus = jsonReturn.getString("apiStatus");
						//log.info("apiStatus of rSkybox API call = " + apiStatus);
					}
				}
			} catch (MalformedURLException e) {
				log.severe("MalformedURLException exception: " + e.getMessage());
			}
		} catch (JSONException e1) {
			log.severe("JSONException exception: " + e1.getMessage());
			return;
		}
	}
	
	// theUrl: complete url
	// thePayload: the JSON payload to send, if any.  Can be null.
	static private String send(URL theUrl, String theJsonPayload) {
		//log.info("RskyboxClient::send theUrl = " + theUrl.toString());

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
			connection.setRequestProperty("Content-Type", "application/json");
			
			///////////////////////
			// Basic Authentication
			///////////////////////
			StringBuilder buf = new StringBuilder(RSKYBOX_BASIC_AUTH_USER_NAME);
			buf.append(':');
			buf.append(RSKYBOX_APPLICATION_TOKEN);
			byte[] bytes = null;
			try {
				bytes = buf.toString().getBytes("ISO-8859-1");
			} catch (java.io.UnsupportedEncodingException uee) {
				log.severe("base64 encoding failed: " + uee.getMessage());
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
			writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
			writer.write(theJsonPayload);
			writer.flush();

			////////////////////
			// Get HTTP response
			////////////////////
			int responseCode = connection.getResponseCode();
			//log.info("responseCode = " + responseCode);
			
			if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
				// read-back the response
				reader = new InputStreamReader(connection.getInputStream());
				BufferedReader in = new BufferedReader(reader);
				StringBuffer responseBuffer = new StringBuffer();
				//log.info("reading the response ...");
				while (true) {
					String inputLine = in.readLine();
					if(inputLine == null) {break;}
					//log.info("response inputLine = " + inputLine);
					responseBuffer.append(inputLine);
				}
				in.close();
				response = responseBuffer.toString();
			} else {
				// Server returned HTTP error code.{
				log.severe("RskyboxClient::send server returned HTTP error code: " + responseCode);
			}
		} catch (UnsupportedEncodingException ex) {
			log.severe("RskyboxClient::send UnsupportedEncodingException: " + ex);
		} catch (MalformedURLException ex) {
			log.severe("RskyboxClient::send MalformedURLException: " + ex);
		} catch (IOException ex) {
			log.severe("RskyboxClient::send IOException: " + ex);
		} finally {
			try {
				if (writer != null) {writer.close();}
			} catch (Exception ex) {
				log.severe("RskyboxClient::send Exception closing writer: " + ex);
			}
			try {
				if (reader != null) {reader.close();}
			} catch (Exception ex) {
				log.severe("RskyboxClient::send Exception closing reader: " + ex);
			}
			if (connection != null) {connection.disconnect();}
		}

		return response;
	}
	
	private static Boolean isLogLevelValid(String theLogLevel) {
		if(theLogLevel.equalsIgnoreCase(DEBUG) ||
		   theLogLevel.equalsIgnoreCase(INFO) ||
		   theLogLevel.equalsIgnoreCase(ERROR) ||
		   theLogLevel.equalsIgnoreCase(EXCEPTION)) {
			return true;
		}
		return false;
	}
}
