package com.stretchcom.rteam.server;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Reference;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

public class ClientTest {
	
	private static final String BASE_URL = "http://rteamtest.appspot.com/";
	private static final String USERS_RESOURCE_URI = "users";
	private static final String USER_RESOURCE_URI = "user";
	
	public static void main(String[] args) throws Exception {
		String emailAddress = "abc@xyz.com";
		verifyCreateUser(emailAddress);
		verifyGetUserInfo(emailAddress);
		verifyDeleteUser(emailAddress);
		verifyGetUserInfo(emailAddress);  // should fail of course
		//justEncode();
	}
	
	private static void verifyCreateUser(String theEmailAddress) {
		System.out.println("\n\nverifyCreateUser() starting .....\n");
		ClientResource usersRoot = new ClientResource(BASE_URL + USERS_RESOURCE_URI);
		JSONObject json = new JSONObject();
		try {
			json.put("firstName", "Joe");
			json.put("lastName", "Tester");
			json.put("emailAddress", theEmailAddress);
			json.put("role", "system tester");
			JsonRepresentation jsonRep = new JsonRepresentation(json);
			usersRoot.post(jsonRep).write(System.out);
			System.out.println("\n");
		} catch (ResourceException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("verifyCreateUser() complete\n");
		}
	}
	
	private static void verifyGetUserInfo(String theEmailAddress) {
		System.out.println("\n\nverifyGetUserInfo() starting .....\n");
		String encodedEmail = Reference.encode(theEmailAddress);
		String url = BASE_URL + USER_RESOURCE_URI + "/" + encodedEmail;
		System.out.println("url with encoding = " + url + "\n");
		ClientResource usersRoot = new ClientResource(url);
		try {
			usersRoot.get().write(System.out);
			System.out.println("\n");
		} catch (ResourceException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("verifyGetUserInfo() complete\n");
		}
	}
	
	private static void verifyDeleteUser(String theEmailAddress) {
		System.out.println("\n\n verifyDeleteUser() starting .....\n");
		String encodedEmail = Reference.encode(theEmailAddress);
		String url = BASE_URL + USER_RESOURCE_URI + "/" + encodedEmail;
		System.out.println("url with encoding = " + url + "\n");
		ClientResource usersRoot = new ClientResource(url);
		try {
			usersRoot.delete().write(System.out);
			System.out.println("\n");
		} catch (ResourceException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("verifyDeleteUser() complete\n");
		}
	}

	private static void justEncode() {
		String emailAddress = "joepwro@gmail.com";
		System.out.println("emailAddress = " + emailAddress + "\n");
		String encodedEmail = Reference.encode(emailAddress);
		System.out.println("encodedEmail = " + encodedEmail + "\n");
	}
}
