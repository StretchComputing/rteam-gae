package com.stretchcom.rteam.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.restlet.gwt.Callback;
import org.restlet.gwt.Client;
import org.restlet.gwt.data.ChallengeResponse;
import org.restlet.gwt.data.ChallengeScheme;
import org.restlet.gwt.data.MediaType;
import org.restlet.gwt.data.Method;
import org.restlet.gwt.data.Preference;
import org.restlet.gwt.data.Protocol;
import org.restlet.gwt.data.Reference;
import org.restlet.gwt.data.Request;
import org.restlet.gwt.data.Response;
import org.restlet.gwt.resource.JsonRepresentation;
import org.restlet.gwt.resource.XmlRepresentation;
import org.restlet.gwt.resource.Representation;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class Rteam implements EntryPoint {

	private static final int REFRESH_INTERVAL = 5000;

	/**
	 * Entry point method.
	 */
	public void onModuleLoad() {
		final Client client = new Client(Protocol.HTTP);
		final UserAuthorization userAuth = new UserAuthorization(client);
		final AuthorizationError authorizationError = new AuthorizationError();
		final MemberDeleteConfirmation memDeleteConfirm = new MemberDeleteConfirmation();
		final MemberDeleteConfirmationError memDeleteConfirmError = new MemberDeleteConfirmationError();
		final MessageThreadConfirmation messageThreadConfirm = new MessageThreadConfirmation();
		final MessageThreadConfirmationError messageThreadConfirmError = new MessageThreadConfirmationError();
		final MessageThreadPollResponse messageThreadPollResponse = new MessageThreadPollResponse();
		final MessageThreadPollResponseError messageThreadPollResponseError = new MessageThreadPollResponseError();
		final TwitterAuthorizationConfirmation twitterAuthConfirm = new TwitterAuthorizationConfirmation();
		final TwitterAuthorizationConfirmationError twitterAuthConfirmError = new TwitterAuthorizationConfirmationError();
		
		// uaoutoken: used for user and member email confirmations
		String emailConfirmToken = com.google.gwt.user.client.Window.Location.getParameter("uaoutoken");
		
		// delconftoken: response from coordinator: accept or reject delete
		String deleteConfirmToken = com.google.gwt.user.client.Window.Location.getParameter("delconftoken");
		String decision = com.google.gwt.user.client.Window.Location.getParameter("decision");
		
		// confirmedtoken: used for confirmed message threads
		String confirmedToken = com.google.gwt.user.client.Window.Location.getParameter("confirmedtoken");
		
		// polltoken: response from poll
		String pollResponseToken = com.google.gwt.user.client.Window.Location.getParameter("polltoken");
		String pollResponse = com.google.gwt.user.client.Window.Location.getParameter("pollResponse");
		
		// twittertoken: twitter invoking callback
		String twitterToken = com.google.gwt.user.client.Window.Location.getParameter("twittertoken");
		String twitterOauthVerifier = com.google.gwt.user.client.Window.Location.getParameter("oauth_verifier");
		
		String baseUrlWithSlash = GWT.getHostPageBaseURL();

		if(emailConfirmToken != null) {
			String encodedToken = Reference.encode(emailConfirmToken);
			client.get(baseUrlWithSlash + "user?oneUseToken=" + encodedToken, new Callback() {
				
				@Override
				public void onEvent(Request request, Response response) {
					System.out.println("Response received to Get User Confirmation Info API call");
					JsonRepresentation rep = response.getEntityAsJson();
	                // Displays the properties and values.
	                JSONObject jsonObj = rep.getValue().isObject();
	                JSONString jsonString;
	                
	                String testOutput = null;
	                if(jsonObj == null) {
	                	testOutput = "Could not retrieve user info from server";
	                } else {
	                    String apiStatus = "";
	                    if(jsonObj.containsKey("apiStatus")) {
	                    	jsonString = jsonObj.get("apiStatus").isString();
	                    	apiStatus = jsonString.stringValue();
	                    }
	                    
	                    // TODO could get this to compile when referencing the ApiStatusCode statics - got GWT compile err - missing module?
	                    //if(apiStatus.equals(ApiStatusCode.SUCCESS)) {
	                    if(apiStatus.equals("100")) {
	                		// Associate the Main panel with the HTML host page.
	                		RootPanel.get("rteamMessage").add(userAuth);
	                		
	                        String firstName = "";
	                        if(jsonObj.containsKey("firstName")) {
	                        	jsonString = jsonObj.get("firstName").isString();
	                        	firstName = jsonString.stringValue();
	                        }
	                        
	                        String lastName = "";
	                        if(jsonObj.containsKey("lastName")) {
	                        	jsonString = jsonObj.get("lastName").isString();
	                        	lastName = jsonString.stringValue();
	                        }
	                        
	                        String emailAddress = "";
	                        if(jsonObj.containsKey("emailAddress")) {
	                        	jsonString = jsonObj.get("emailAddress").isString();
	                        	emailAddress = jsonString.stringValue();
	                        }
	                        
	                        String token = "";
	                        if(jsonObj.containsKey("token")) {
	                        	jsonString = jsonObj.get("token").isString();
	                        	token = jsonString.stringValue();
	                        }
	                        
	                        if(firstName.trim().length() == 0) {userAuth.setUserFirstName(emailAddress);}
	                        else                               {userAuth.setUserFirstName(firstName);}
	                        String tempFullName = firstName.trim() + " " + lastName.trim();
	                        if(tempFullName.length() == 1) {userAuth.setUserFullName(emailAddress);}
	                        else                           {userAuth.setUserFullName(tempFullName);}
	                        userAuth.setUserEmailAddress(emailAddress);
	                        userAuth.setUserToken(token);
	                    } else {
	                		// Associate the Main panel with the HTML host page.
	                		RootPanel.get("rteamMessage").add(authorizationError);
	                		
	            			StringBuffer sb = new StringBuffer();
	                		//if(apiStatus.equals(ApiStatusCode.EMAIL_CONFIRMATION_LINK_NO_LONGER_ACTIVE)) {
	                		if(apiStatus.equals("216")) {
	                			sb.append("The email confirmation link you clicked on is no longer active.");
	                		//} else if(apiStatus.equals(ApiStatusCode.EMAIL_ADDRESS_ALREADY_USED)) {
	                		} else if(apiStatus.equals("201")) {
	                            String emailAddress = "";
	                            if(jsonObj.containsKey("emailAddress")) {
	                            	jsonString = jsonObj.get("emailAddress").isString();
	                            	emailAddress = jsonString.stringValue();
	                            }
	                			
	                			sb.append("The email address ");
	                			sb.append(emailAddress);
	                			sb.append(" is already in active use by by another rTeam user. ");
	                			sb.append("Verify you entered the correct email address when you registered. ");
	                			sb.append("If you were attempting to create a second rTeam user account with the same email address, ");
	                			sb.append("we are sorry, but that is currently not supported.");
	                		} else {
	                			sb.append("An Email Address Confirmation Error has occurred.");
	                		}
	                		
	                		authorizationError.setErrorMessage(sb.toString());
	                    }
	                }
				}
			});
		} else if(deleteConfirmToken != null) {
			String encodedToken = Reference.encode(deleteConfirmToken);
			String encodedDecision = Reference.encode(decision);
			String url = baseUrlWithSlash + "member?oneUseToken=" + encodedToken + "&decision=" + encodedDecision;
			client.delete(url, new Callback() {
				
				@Override
				public void onEvent(Request request, Response response) {
					System.out.println("Response received to 'Delete Member Confirmation' API call");
					JsonRepresentation rep = response.getEntityAsJson();
	                // Displays the properties and values.
	                JSONObject jsonObj = rep.getValue().isObject();
	                JSONString jsonString;
	                
	                String testOutput = null;
	                if(jsonObj == null) {
	                	testOutput = "'Delete Member Confirmation' API failed";
	                } else {
	                    String apiStatus = "";
	                    if(jsonObj.containsKey("apiStatus")) {
	                    	jsonString = jsonObj.get("apiStatus").isString();
	                    	apiStatus = jsonString.stringValue();
	                    }
	                    
	                    // TODO could get this to compile when referencing the ApiStatusCode statics - got GWT compile err - missing module?
	                    //if(apiStatus.equals(ApiStatusCode.SUCCESS)) {
	                    if(apiStatus.equals("100")) {
	                		// Associate the Main panel with the HTML host page.
	                		RootPanel.get("rteamMessage").add(memDeleteConfirm);
	                		
	                        String firstName = "";
	                        if(jsonObj.containsKey("firstName")) {
	                        	jsonString = jsonObj.get("firstName").isString();
	                        	firstName = jsonString.stringValue();
	                        }
	                        
	                        String lastName = "";
	                        if(jsonObj.containsKey("lastName")) {
	                        	jsonString = jsonObj.get("lastName").isString();
	                        	lastName = jsonString.stringValue();
	                        }
	                        
	                        String emailAddress = "";
	                        if(jsonObj.containsKey("emailAddress")) {
	                        	jsonString = jsonObj.get("emailAddress").isString();
	                        	emailAddress = jsonString.stringValue();
	                        }
	                        
	                        memDeleteConfirm.setFullName(firstName + " " + lastName);
	                        memDeleteConfirm.setEmailAddress(emailAddress);
	                    } else {
	                		// Associate the Main panel with the HTML host page.
	                		RootPanel.get("rteamMessage").add(memDeleteConfirmError);
	                		
	            			StringBuffer sb = new StringBuffer();
	                		//if(apiStatus.equals(ApiStatusCode.DELETE_MEMBER_CONFIRMATION_LINK_NO_LONGER_ACTIVE)) {
	                		if(apiStatus.equals("217")) {
	                			sb.append("The member delete confirmation link you clicked on is no longer active.");
	                		//} else if(apiStatus.equals(ApiStatusCode.EMAIL_ADDRESS_ALREADY_USED)) {
	                		} else {
	                			sb.append("An Member Delete Confirmation Error has occurred.");
	                		}
	                		
	                		memDeleteConfirmError.setErrorMessage(sb.toString());
	                    }
	                }
				}
			});
		} else if(confirmedToken != null) {
			String encodedToken = Reference.encode(confirmedToken);
			String url = baseUrlWithSlash + "messageThread?oneUseToken=" + encodedToken;
			Representation emptyRep = null;
			client.put(url, emptyRep, new Callback() {
				
				@Override
				public void onEvent(Request request, Response response) {
					System.out.println("Response received to 'Delete Member Confirmation' API call");
					JsonRepresentation rep = response.getEntityAsJson();
	                // Displays the properties and values.
	                JSONObject jsonObj = rep.getValue().isObject();
	                JSONString jsonString;
	                
	                String testOutput = null;
	                if(jsonObj == null) {
	                	testOutput = "'Message Confirmation' API failed";
	                } else {
	                    String apiStatus = "";
	                    if(jsonObj.containsKey("apiStatus")) {
	                    	jsonString = jsonObj.get("apiStatus").isString();
	                    	apiStatus = jsonString.stringValue();
	                    }
	                    
	                    //if(apiStatus.equals(ApiStatusCode.SUCCESS)) {
	                    if(apiStatus.equals("100")) {
	                		// Associate the Main panel with the HTML host page.
	                		RootPanel.get("rteamMessage").add(messageThreadConfirm);
	                		
	                        String firstName = "";
	                        if(jsonObj.containsKey("firstName")) {
	                        	jsonString = jsonObj.get("firstName").isString();
	                        	firstName = jsonString.stringValue();
	                        }
	                        
	                        String lastName = "";
	                        if(jsonObj.containsKey("lastName")) {
	                        	jsonString = jsonObj.get("lastName").isString();
	                        	lastName = jsonString.stringValue();
	                        }
	                        
	                        messageThreadConfirm.setFullName(firstName + " " + lastName);
	                    } else {
	                		// Associate the Main panel with the HTML host page.
	                		RootPanel.get("rteamMessage").add(messageThreadConfirmError);
	                		
	            			StringBuffer sb = new StringBuffer();
	                		//if(apiStatus.equals(ApiStatusCode.DELETE_MEMBER_CONFIRMATION_LINK_NO_LONGER_ACTIVE)) {
	                		if(apiStatus.equals("800")) {
	                			sb.append("The message thread confirmation link you clicked on is no longer active.");
	                		//} else if(apiStatus.equals(ApiStatusCode.EMAIL_ADDRESS_ALREADY_USED)) {
	                		} else {
	                			sb.append("An Message Thread Confirmation Error has occurred.");
	                		}
	                		
	                		messageThreadConfirmError.setErrorMessage(sb.toString());
	                    }
	                }
				}
			});
		} else if(pollResponseToken != null) {
			String encodedToken = Reference.encode(pollResponseToken);
			String encodedPollResponse = Reference.encode(pollResponse);
			String url = baseUrlWithSlash + "messageThread?oneUseToken=" + encodedToken + "&pollResponse=" + encodedPollResponse;
			Representation emptyRep = null;
			client.put(url, emptyRep, new Callback() {
				
				@Override
				public void onEvent(Request request, Response response) {
					System.out.println("Response received to 'Message Thread (Poll) Response' API call");
					JsonRepresentation rep = response.getEntityAsJson();
	                // Displays the properties and values.
	                JSONObject jsonObj = rep.getValue().isObject();
	                JSONString jsonString;
	                
	                String testOutput = null;
	                if(jsonObj == null) {
	                	testOutput = "'Message Thread (Poll) Response' API failed";
	                } else {
	                    String apiStatus = "";
	                    if(jsonObj.containsKey("apiStatus")) {
	                    	jsonString = jsonObj.get("apiStatus").isString();
	                    	apiStatus = jsonString.stringValue();
	                    }
	                    
	                    // TODO could get this to compile when referencing the ApiStatusCode statics - got GWT compile err - missing module?
	                    //if(apiStatus.equals(ApiStatusCode.SUCCESS)) {
	                    if(apiStatus.equals("100")) {
	                		// Associate the Main panel with the HTML host page.
	                		RootPanel.get("rteamMessage").add(messageThreadPollResponse);
	                		
	                        String firstName = "";
	                        if(jsonObj.containsKey("firstName")) {
	                        	jsonString = jsonObj.get("firstName").isString();
	                        	firstName = jsonString.stringValue();
	                        }
	                        
	                        String lastName = "";
	                        if(jsonObj.containsKey("lastName")) {
	                        	jsonString = jsonObj.get("lastName").isString();
	                        	lastName = jsonString.stringValue();
	                        }
	                        
	                        StringBuffer dBuffer = new StringBuffer();
	                        List<Member> members = new ArrayList<Member>();
	                        dBuffer.append("*****");
	                        dBuffer.append(jsonObj.toString());
	                        dBuffer.append("*****");
	                        
	                        try {
		            			if(jsonObj.containsKey("members")) {
		            				JSONArray membersJsonArray = jsonObj.get("members").isArray();
		            					
		            				for(int i=0; i<membersJsonArray.size(); i++) {
		            					Member newMember = new Member();
		            					JSONObject memberJsonObject = membersJsonArray.get(i).isObject();
		            					
		            					if(memberJsonObject.containsKey("belongsToMember")) {
			            					JSONBoolean belongsToMember = memberJsonObject.get("belongsToMember").isBoolean();
			            					if(belongsToMember != null) newMember.setBelongsToMember(belongsToMember.booleanValue());
		            					}
		            					
		            					if(memberJsonObject.containsKey("memberId")) {
			            					JSONString memberId = memberJsonObject.get("memberId").isString();
			            					if(memberId != null) newMember.setMemberId(memberId.stringValue());
		            					}
		            					
		            					if(memberJsonObject.containsKey("memberName")) {
			            					JSONString memberFullName = memberJsonObject.get("memberName").isString();
			            					if(memberFullName != null) newMember.setMemberFullName(memberFullName.stringValue());
		            					}
		            					
		            					if(memberJsonObject.containsKey("reply")) {
			            					JSONString reply = memberJsonObject.get("reply").isString();
			            					if(reply != null) newMember.setReply(reply.stringValue());
		            					}
		            					
		            					if(memberJsonObject.containsKey("replyEmailAddress")) {
			            					JSONString replyEmailAddress = memberJsonObject.get("replyEmailAddress").isString();
			            					if(replyEmailAddress != null) newMember.setReplyEmailAddress(replyEmailAddress.stringValue());
		            					}
		            					
		            					if(memberJsonObject.containsKey("replyDate")) {
			            					JSONString replyDate = memberJsonObject.get("replyDate").isString();
			            					if(replyDate != null) newMember.setReplyDate(replyDate.stringValue());
		            					}
		            					
		            					members.add(newMember);
		            				}
		            			}
	                        } catch (Exception e) {
	                        	dBuffer.append(" ***** exception ***** ");
	                        	dBuffer.append(e.getMessage());
	                        }
	                        
	            			String numOfMembers = "" + members.size();
	            			messageThreadPollResponse.setNumOfMembers(numOfMembers);
	            			messageThreadPollResponse.setDebugMessage("debug OFF");
	            			//messageThreadPollResponse.setDebugMessage(dBuffer.toString());
	            			messageThreadPollResponse.setMembersTable(members);
	                    } else {
	                		// Associate the Main panel with the HTML host page.
	                		RootPanel.get("rteamMessage").add(messageThreadPollResponseError);
	                		
	            			StringBuffer sb = new StringBuffer();
	            			//TODO - remove magic number
	                		if(apiStatus.equals("800")) {
	                			sb.append("The Poll is no longer active.");
	                		} else {
	                			sb.append("An Message Thread (Poll) Response Error has occurred.");
	                		}
	                		
	                		messageThreadPollResponseError.setErrorMessage(sb.toString());
	                    }
	                }
				}
			});
		} else if(twitterToken != null) {
			String encodedToken = Reference.encode(twitterToken);
			String encodedOauthVerifier = Reference.encode(twitterOauthVerifier);
			String url = baseUrlWithSlash + "team/twitter/oauth?oneUseToken=" + encodedToken + "&oauthVerifier=" + encodedOauthVerifier;
			Representation emptyRep = null;
			client.put(url, emptyRep, new Callback() {
				
				@Override
				public void onEvent(Request request, Response response) {
					System.out.println("Response received to 'Get Twitter Confirmation' API call");
					JsonRepresentation rep = response.getEntityAsJson();
	                // Displays the properties and values.
	                JSONObject jsonObj = rep.getValue().isObject();
	                JSONString jsonString;
	                
	                String testOutput = null;
	                if(jsonObj == null) {
	                	testOutput = "'Get Twitter Confirmation' API failed";
	                } else {
	                    String apiStatus = "";
	                    if(jsonObj.containsKey("apiStatus")) {
	                    	jsonString = jsonObj.get("apiStatus").isString();
	                    	apiStatus = jsonString.stringValue();
	                    }
	                    
	                    //if(apiStatus.equals(ApiStatusCode.SUCCESS)) {
	                    if(apiStatus.equals("100")) {
	                		// Associate the Main panel with the HTML host page.
	                		RootPanel.get("rteamMessage").add(twitterAuthConfirm);
	                    } else {
	                		// Associate the Main panel with the HTML host page.
	                		RootPanel.get("rteamMessage").add(twitterAuthConfirmError);
	                		
	            			StringBuffer sb = new StringBuffer();
	            			if(apiStatus.equals("801")) {
	                			sb.append("Twitter callback token is no longer active");
	                		} else if(apiStatus.equals("220")) {
	                			sb.append("could not retrieve Access Token from Twitter");
	                		} else {
	                			sb.append("A Get Twitter Confirmation Error has occurred.");
	                		}
	                		
	            			twitterAuthConfirmError.setErrorMessage(sb.toString());
	                    }
	                }
				}
			});
		}
		// end of else

	}
	
    private static String getDisplayName(String theFirstName, String theLastName, String theEmailAddress) {
		String displayName = theEmailAddress;
		if(theFirstName != null && theFirstName.trim().length() > 0 && theLastName != null && theLastName.trim().length() > 0) {
			 displayName = theFirstName + " " + theLastName;
		} else if(theFirstName != null && theFirstName.trim().length() > 0) {
			displayName = theFirstName;
		} else if(theLastName != null && theLastName.trim().length() > 0) {
			displayName = theLastName;
		}
		return displayName;
    }
	

}