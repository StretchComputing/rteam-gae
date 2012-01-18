package com.stretchcom.rteam.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Logger;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.codec.binary.Base64;

public class C2dmClient {
	//private static final Logger log = Logger.getLogger(C2dmClient.class.getName());
	private static RskyboxClient log = new RskyboxClient();
	
	private static final String SEND_URL = "https://android.clients.google.com/c2dm/send";
	private static final String TOKEN_URL = "https://www.google.com/accounts/ClientLogin";
	
	private final static String AUTH = "authentication";
	private static final String UPDATE_CLIENT_AUTH = "Update-Client-Auth";
	public static final String PARAM_REGISTRATION_ID = "registration_id";
	public static final String PARAM_DELAY_WHILE_IDLE = "delay_while_idle";
	public static final String PARAM_COLLAPSE_KEY = "collapse_key";
	private static final String UTF8 = "UTF-8";

	
	// TODO must process the Update-Client-Auth header in response
/*
Anytime you perform a Post to the Google C2DM 
server you should read the header response back from the server.  It 
occasionally includes a new client auth token.  You simply need to 
parse it out of the header and use it the next time you perform a 
post.  I'm currently parsing anything between Update-Client-Auth: and 
the next CRLF.  Don't forget to trim the string for whitespaces due to 
the space between Update-Client-Auth: and the token!
 */
	
	public static int sendMessage(String auth_token, String registrationId,
			String message) throws IOException {

		StringBuilder postDataBuilder = new StringBuilder();
		postDataBuilder.append(PARAM_REGISTRATION_ID).append("=").append(registrationId);
		postDataBuilder.append("&").append(PARAM_COLLAPSE_KEY).append("=").append("0");
		postDataBuilder.append("&").append("data.payload").append("=").append(URLEncoder.encode(message, UTF8));

		byte[] postData = postDataBuilder.toString().getBytes(UTF8);

		// Hit the DM URL.
		URL url = new URL(SEND_URL);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setUseCaches(false);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
		conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
		conn.setRequestProperty("Authorization", "GoogleLogin auth=" + auth_token);

		OutputStream out = conn.getOutputStream();
		out.write(postData);
		out.close();

		int responseCode = conn.getResponseCode();
		return responseCode;
	}
	
	// method code from: http://www.vogella.de/articles/AndroidCloudToDeviceMessaging/article.html#c2dm_sendmessage
	public static String getToken(String email, String password)
			throws IOException {
		// Create the post data
		// Requires a field with the email and the password
		StringBuilder builder = new StringBuilder();
		builder.append("Email=").append(email);
		builder.append("&Passwd=").append(password);
		builder.append("&accountType=GOOGLE");
		builder.append("&source=rteamAndroid");
		builder.append("&service=ac2dm");

		// Setup the HTTP Post
		byte[] data = builder.toString().getBytes();
		URL url = new URL(TOKEN_URL);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setUseCaches(false);
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		con.setRequestProperty("Content-Length", Integer.toString(data.length));

		// Issue the HTTP POST request
		OutputStream output = con.getOutputStream();
		output.write(data);
		output.close();

		// Read the response
		BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String line = null;
		String auth_key = null;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("Auth=")) {
				auth_key = line.substring(5);
			}
		}

		// Finally get the authentication token
		// To something useful with it
		return auth_key;
	}

}
