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
import org.apache.commons.codec.binary.Base64;

public class UrbanAirshipClient {
	private static final Logger log = Logger.getLogger(UrbanAirshipClient.class.getName());
	
	// HTTP Methods
	private static final String HTTP_PUT = "PUT";
	private static final String HTTP_POST = "POST";
	
	// UrbanAirship development application  -- rteamtest
	private static final String URBAN_AIRSHIP_DEV_APPLICATION_KEY = "DIhK_rduSnu8z5mDWmzasw";
	private static final String URBAN_AIRSHIP_DEV_APPLICATION_SECRET = "suKZxgVWRzyEGUrvV3q6Yg";
	private static final String URBAN_AIRSHIP_DEV_APPLICATION_MASTER_SECRET = "Xs0cQtcVTay7cqc3aHskcQ";
	
	// UrbanAirship production application  -- rteamprod
	private static final String URBAN_AIRSHIP_APPLICATION_KEY = "6T4u4R1pT1KUsWmkcwJhKw";
	private static final String URBAN_AIRSHIP_APPLICATION_SECRET = "H4QryrMjTd625or7yo8alQ";
	private static final String URBAN_AIRSHIP_APPLICATION_MASTER_SECRET = "U7ZFMe-BSYO_iUppQoBqdg";
	
	// UrbanAirship APIs
	private static final String URBAN_AIRSHIP_BASE_URL = "https://go.urbanairship.com/api/";
	private static final String URBAN_AIRSHIP_REGISTRATION = "device_tokens";
	private static final String URBAN_AIRSHIP_PUSH = "push";

	// Returns response from API. Can be null.
	static public String register(String theDeviceToken, Boolean theIsDeveloper) {
		log.info("UrbanAirshipClient::register entered, theDeviceToken = " + theDeviceToken);
		
		if(theDeviceToken == null) {
			return null;
		}
		
		String applicationKey = URBAN_AIRSHIP_APPLICATION_KEY;
		String applicationSecret = URBAN_AIRSHIP_APPLICATION_SECRET;
		if(theIsDeveloper) {
			applicationKey = URBAN_AIRSHIP_DEV_APPLICATION_KEY;
			applicationSecret = URBAN_AIRSHIP_DEV_APPLICATION_SECRET;
		}
		
		String response = null;
		String urlStr = URBAN_AIRSHIP_BASE_URL + URBAN_AIRSHIP_REGISTRATION + "/" + theDeviceToken;
		URL url = null;
		try {
			url = new URL(urlStr);
			response = send(url, HTTP_PUT, null, applicationKey, applicationSecret);
		} catch (MalformedURLException e) {
			log.severe("MalformedURLException exception: " + e.getMessage());
		}
		
		return response;
	}
	
	static public String push(String theAlert, Integer theBadge, String theDeviceToken, Boolean theIsDeveloper) {
		log.info("UrbanAirshipClient::push entered, theAlert = " + theAlert);
		
		String applicationKey = URBAN_AIRSHIP_APPLICATION_KEY;
		String applicationMasterSecret = URBAN_AIRSHIP_APPLICATION_MASTER_SECRET;
		if(theIsDeveloper) {
			applicationKey = URBAN_AIRSHIP_DEV_APPLICATION_KEY;
			applicationMasterSecret = URBAN_AIRSHIP_DEV_APPLICATION_MASTER_SECRET;
		}
		
		//////////////////////////
		// Create the JSON Payload
		//////////////////////////
		JSONObject jsonPayload = null;
		try {
			jsonPayload = new JSONObject();
			
			JSONArray jsonDeviceTokens = new JSONArray();
			jsonDeviceTokens.put(theDeviceToken);
			jsonPayload.put("device_tokens", jsonDeviceTokens);
			
			JSONObject jsonAps = new JSONObject();
			if(theBadge != null) jsonAps.put("badge", theBadge);
			if(theAlert != null) {
				jsonAps.put("alert", theAlert);
				jsonAps.put("sound", "");
			}
			jsonPayload.put("aps", jsonAps);
		} catch (JSONException e1) {
			log.severe("JSONException exception: " + e1.getMessage());
			return null;
		}
		log.info("jsonPayload = " + jsonPayload.toString());
		
		String response = null;
		String urlStr = URBAN_AIRSHIP_BASE_URL + URBAN_AIRSHIP_PUSH + "/";
		URL url = null;
		try {
			url = new URL(urlStr);
			response = send(url, HTTP_POST, jsonPayload.toString(), applicationKey, applicationMasterSecret);
		} catch (MalformedURLException e) {
			log.severe("MalformedURLException exception: " + e.getMessage());
		}
		
		return response;
	}
	
	// theUrl: complete url
	// thePayload: the JSON payload to send, if any.  Can be null.
	// theHttpMethod: one of GET POST HEAD OPTIONS PUT DELETE TRACE
	static private String send(URL theUrl, String theHttpMethod, String theJsonPayload,
			       String theBasicAuthUserName, String theBasicAuthPassword) {
		// TODO add parameter verification
		log.info("UrbanAirshipClient::send theUrl = " + theUrl.toString());
		log.info("UrbanAirshipClient::send theJsonPayload = " + theJsonPayload);

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
			log.info("responseCode = " + responseCode);
			
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
				log.severe("UrbanAirshipClient::push server returned error code: " + responseCode);
			}

		} catch (UnsupportedEncodingException ex) {
			log.severe("UrbanAirshipClient::push UnsupportedEncodingException: " + ex);
		} catch (MalformedURLException ex) {
			log.severe("UrbanAirshipClient::push MalformedURLException: " + ex);
		} catch (IOException ex) {
			log.severe("UrbanAirshipClient::push IOException: " + ex);
		} finally {
			try {
				if (writer != null) {writer.close();}
			} catch (Exception ex) {
				log.severe("UrbanAirshipClient::push Exception closing writer: " + ex);
			}
			try {
				if (reader != null) {reader.close();}
			} catch (Exception ex) {
				log.severe("UrbanAirshipClient::push Exception closing reader: " + ex);
			}
			if (connection != null) {connection.disconnect();}
		}

		return response;
	}
}
