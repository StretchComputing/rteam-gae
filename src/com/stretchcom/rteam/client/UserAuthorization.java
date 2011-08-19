package com.stretchcom.rteam.client;

import org.restlet.gwt.Callback;
import org.restlet.gwt.Client;
import org.restlet.gwt.data.ChallengeResponse;
import org.restlet.gwt.data.ChallengeScheme;
import org.restlet.gwt.data.MediaType;
import org.restlet.gwt.data.Method;
import org.restlet.gwt.data.Request;
import org.restlet.gwt.data.Response;
import org.restlet.gwt.resource.JsonRepresentation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.TextBox;
import org.restlet.gwt.data.Protocol;
import com.google.gwt.http.client.*;

public class UserAuthorization extends Composite {
	interface UserAuthorizationUiBinder extends UiBinder<Widget, UserAuthorization> {}
	private static UserAuthorizationUiBinder uiBinder = GWT.create(UserAuthorizationUiBinder.class);
	
	private String userToken;
	private Client client;
	private String teamNameStored;
	private FlexTable memberTable;
	private String teamId;
	private String memberEmailAddressStored;
	private String memberPhoneNumberStored;
	private String memberFirstNameStored;
	private String memberLastNameStored;
	private final MultiWordSuggestOracle sportSuggestions = new MultiWordSuggestOracle();

	
	@UiField
	HTMLPanel htmlPanel;

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// User Fields
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@UiField
	SpanElement userFirstName;
	
	@UiField
	SpanElement userEmailAddress;

	@UiField
	SpanElement userFullName;
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// 'Create Team' Fields
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@UiField
    TextBox teamName;
	
	@UiField
	SpanElement newTeamName;
	
	@UiField(provided = true)
	SuggestBox sportSuggestBox;
	
	@UiField
	Button createTeamButton;
	
	@UiField
	DivElement createTeamDiv;
	
	@UiField
	DivElement createTeamErrorDiv;

	@UiField
	SpanElement createTeamErrorMessage;
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// 'Add Member' Fields
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@UiField
    TextBox memberEmailAddress;
	
	@UiField
    TextBox memberPhoneNumber;
	
	@UiField
    TextBox memberFirstName;
	
	@UiField
    TextBox memberLastName;
	
	@UiField
	Button addMemberButton;
	
	@UiField
	DivElement addMemberDiv;
	
	@UiField
	DivElement addMemberErrorDiv;

	@UiField
	SpanElement addMemberErrorMessage;
	
	@UiField
	DivElement addMemberSuccessDiv;

	@UiField
	SpanElement addMemberSuccessMessage;
	
	public UserAuthorization(Client theClient) {
		this.client = theClient;
		sportSuggestBox = new SuggestBox(sportSuggestions);
		initWidget(uiBinder.createAndBindUi(this));
	    initSportSuggestions();
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// User Methods
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void setUserFullName(String theUserFullName) { userFullName.setInnerText(theUserFullName); }
	
	public void setUserEmailAddress(String theUserEmailAddress) { userEmailAddress.setInnerText(theUserEmailAddress); }
	
	public void setUserFirstName(String theUserFirstName) { userFirstName.setInnerText(theUserFirstName); }
	
	public void setUserToken(String theUserToken) {this.userToken = theUserToken; }
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// 'Create Team' Methods
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public String getTeamName() {
		this.teamNameStored = teamName.getValue();
		newTeamName.setInnerText(this.teamNameStored);
		return this.teamNameStored;
	}
	
	public String getNewTeamName() {return teamNameStored; }
	
	public String getSport() {return sportSuggestBox.getValue(); }
	
	public void setTeamId(String theTeamId) {this.teamId = theTeamId; }

	@UiHandler("createTeamButton")
	void onCreateTeamClick(ClickEvent e) {
		//Window.alert("Token = " + this.userToken);
		hideCreateTeamErrorMessage();
		callCreateTeamApi();
	}
	
	private void showCreateTeamErrorMessage(String theErrorMessage) {
		createTeamErrorDiv.getStyle().setDisplay(Style.Display.BLOCK);
		createTeamErrorMessage.setInnerText(theErrorMessage);
	}
	
	private void hideCreateTeamErrorMessage() {
		createTeamErrorMessage.setInnerText("");
		createTeamErrorDiv.getStyle().setDisplay(Style.Display.NONE);
	}
	
	private void initSportSuggestions() {
		sportSuggestions.add("badminton");
		sportSuggestions.add("beach volleyball");
		sportSuggestions.add("baseball");
		sportSuggestions.add("basketball");
		sportSuggestions.add("bowling");
		sportSuggestions.add("cheerleading");
		sportSuggestions.add("cricket");
		sportSuggestions.add("cross country");
		sportSuggestions.add("curling");
		sportSuggestions.add("cycling");
		sportSuggestions.add("dance");
		sportSuggestions.add("diving");
		sportSuggestions.add("fencing");
		sportSuggestions.add("field hockey");
		sportSuggestions.add("figure skating");
		sportSuggestions.add("football");
		sportSuggestions.add("golf");
		sportSuggestions.add("gymnastics");
		sportSuggestions.add("handball");
		sportSuggestions.add("hockey");
		sportSuggestions.add("lacrosse");
		sportSuggestions.add("poms");
		sportSuggestions.add("rowing");
		sportSuggestions.add("rugby");
		sportSuggestions.add("shooting");
		sportSuggestions.add("soccer");
		sportSuggestions.add("softball");
		sportSuggestions.add("squash");
		sportSuggestions.add("swimming");
		sportSuggestions.add("table tennis");
		sportSuggestions.add("tennis");
		sportSuggestions.add("track and field");
		sportSuggestions.add("ultimate frisbee");
		sportSuggestions.add("volleyball");
		sportSuggestions.add("water polo");
		sportSuggestions.add("weightlifting");
		sportSuggestions.add("wrestling");
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// 'Add Member' Methods
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public String getMemberEmailAddress() {return memberEmailAddress.getValue(); }
	public void clearMemberEmailAddress() {memberEmailAddress.setValue(""); }
	
	public String getMemberPhoneNumber() {return memberPhoneNumber.getValue(); }
	public void clearMemberPhoneNumber() {memberPhoneNumber.setValue(""); }
	
	public String getMemberFirstName() {return memberFirstName.getValue(); }
	public void clearMemberFirstName() {memberFirstName.setValue(""); }
	
	public String getMemberLastName() {return memberLastName.getValue(); }
	public void clearMemberLastName() {memberLastName.setValue(""); }

	@UiHandler("addMemberButton")
	void onAddMemberClick(ClickEvent e) {
		hideAddMemberErrorMessage();
		hideAddMemberSuccessMessage();
		callAddMemberApi();
	}
	
	// hide team div and show member div
	private void showAddMemberDiv() {
		//createTeamDiv.removeFromParent();
		createTeamDiv.getStyle().setDisplay(Style.Display.NONE);
		addMemberDiv.getStyle().setDisplay(Style.Display.BLOCK);
	}
	
	// for now, all members are of type EMAIL
	private void addMemberToMemberTable() {
		if(memberTable == null) {addMemberTable();}
		Object[] row = {this.memberEmailAddressStored, this.memberPhoneNumberStored, this.memberFirstNameStored,
				        this.memberLastNameStored, "email"};
		addRow(row);
	}
	
	void addMemberTable()
    {
		memberTable = new FlexTable();
		addColumn("Team Member Contact Info", 0);
		addColumn("Phone Number", 1);
		addColumn("First Name", 2);
		addColumn("Last Name", 3);
		addColumn("Type of Messaging Used", 4);
		
		memberTable.setCellSpacing(1);
		memberTable.setCellPadding(10);
		memberTable.addStyleName("FlexTable");  
		
		htmlPanel.add(memberTable);
    }
	
	private void showAddMemberErrorMessage(String theErrorMessage) {
		//Window.alert("showAddMemberErrorMessage() entered, theErrorMessage = " + theErrorMessage);
		addMemberErrorDiv.getStyle().setDisplay(Style.Display.BLOCK);
		addMemberErrorMessage.setInnerText(theErrorMessage);
	}
	
	private void hideAddMemberErrorMessage() {
		addMemberErrorMessage.setInnerText("");
		addMemberErrorDiv.getStyle().setDisplay(Style.Display.NONE);
	}
	
	private void showAddMemberSuccessMessage(String theSuccessMessage) {
		addMemberSuccessDiv.getStyle().setDisplay(Style.Display.BLOCK);
		addMemberSuccessMessage.setInnerText(theSuccessMessage);
	}
	
	private void hideAddMemberSuccessMessage() {
		addMemberSuccessMessage.setInnerText("");
		addMemberSuccessDiv.getStyle().setDisplay(Style.Display.NONE);
	}
	
	private void addColumn(Object theColumnHeading, int theColIndex) {
		//Window.alert("ac1");
		Widget widget = createCellWidget(theColumnHeading);
		//Window.alert("ac2");
		widget.setWidth("100%");
		//Window.alert("ac3");
		widget.addStyleName("FlexTable-ColumnLabel");
		//Window.alert("ac4");
		memberTable.setWidget(0, theColIndex, widget);
		//Window.alert("ac5");
		memberTable.getCellFormatter().addStyleName(0, theColIndex, "FlexTable-ColumnLabelCell");
		//Window.alert("ac6");
	}
	
	private Widget createCellWidget(Object theCellObject) {
		Widget widget = null;
		//Window.alert("ccw1: theCellObject = " + theCellObject.toString());
		if(theCellObject instanceof Widget) {
			//Window.alert("ccw2");
			widget = (Widget) theCellObject;
		}
		else {
			//Window.alert("ccw3");
			widget = new Label(theCellObject.toString());
		}
		//Window.alert("ccw4");
		return widget;
	}

	private void addRow(Object[] theCellObjects) {
		int rowIndex = memberTable.getRowCount();
		//Window.alert("acr1: rowIndex = " + rowIndex);
		for(int cell = 0; cell < theCellObjects.length; cell++) {
			//Window.alert("acr2");
			Widget widget = createCellWidget(theCellObjects[cell]);
			//Window.alert("acr3");
			memberTable.setWidget(rowIndex, cell, widget);
			//Window.alert("acr4");
			memberTable.getCellFormatter().addStyleName(rowIndex, cell, "FlexTable-Cell");
		}
		//Window.alert("acr5");
		HTMLTable.RowFormatter rf = memberTable.getRowFormatter();
		if((rowIndex % 2) != 0) {rf.addStyleName(rowIndex, "FlexTable-OddRow");}
		else                    {rf.addStyleName(rowIndex, "FlexTable-EvenRow");}
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// API Methods
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private void callCreateTeamApi() {
		String url = GWT.getHostPageBaseURL() + "teams";
		RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));

		try {
			String challengeResponse = "login" + ":" + this.userToken;
			String encodedAuthorization = Base64.encode(challengeResponse);
			builder.setHeader("Authorization", "Basic " + encodedAuthorization);
			
			JSONObject json = new JSONObject();
			
			if(getTeamName().length() == 0) {
				showCreateTeamErrorMessage("Team Name is a required field");
				return;
			}
			JSONString jsonTeamName = new JSONString(getTeamName());
			json.put("teamName", jsonTeamName);
			
			// TODO allow user to input description
			JSONString jsonDescription = new JSONString("TODO: allow user to input this");
			json.put("description", jsonDescription);
			
			JSONString jsonSport = new JSONString(getSport());
			json.put("sport", jsonSport);
			//Window.alert("json.toString() = " + json.toString());
			
			com.google.gwt.http.client.Request request = builder.sendRequest(json.toString(), new RequestCallback() {
			    public void onError(com.google.gwt.http.client.Request request, Throwable exception) {
			    	// Couldn't connect to server (could be timeout, SOP violation, etc.)
			    	//Window.alert("HTTP Error = " + exception.getMessage());
			    	showCreateTeamErrorMessage("We apologize but the rTeam service is currently unavailable");
			    }
	
			    public void onResponseReceived(com.google.gwt.http.client.Request request, com.google.gwt.http.client.Response response) {
			      if (201 == response.getStatusCode()) {
			    	  // success
			    	  JSONValue jsonValue = JSONParser.parseLenient(response.getText());
			    	  JSONObject jsonObject = jsonValue.isObject();
			    	  JSONString jsonString;
			    	  if(jsonObject != null) {
			    		  jsonString = jsonObject.get("apiStatus").isString();
			    		  String apiStatus = jsonString.stringValue();
			    		  if(apiStatus.equals("100")) {
				    		  jsonString = jsonObject.get("teamId").isString();
				    		  setTeamId(jsonString.stringValue());
				    		  showAddMemberDiv();
			    		  } else {
			    			  showCreateTeamErrorMessage("Could not create specified team. Status = " + apiStatus);
			    		  }
			    	  }
			      } else {
			    	  // display error
			    	  showCreateTeamErrorMessage("Error: could not create specified team");
			      }
			    }
			  });
		} catch (RequestException e) {
			showCreateTeamErrorMessage("We apologize but the rTeam service is currently unavailable");
		}
	}
	
	private void callAddMemberApi() {
		String url = GWT.getHostPageBaseURL() + "team/" + this.teamId + "/members";
		RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));

		try {
			JSONObject json = new JSONObject();
			String memEA = getMemberEmailAddress();
			if(memEA == null || memEA.length() == 0) {
				showAddMemberErrorMessage("Email Address is a required field");
				return;
			}
			//Window.alert("memEA = " + memEA);

			String challengeResponse = "login" + ":" + this.userToken;
			String encodedAuthorization = Base64.encode(challengeResponse);
			builder.setHeader("Authorization", "Basic " + encodedAuthorization);
			
			JSONString jsonMemberEmailAddress = new JSONString(getMemberEmailAddress());
			json.put("emailAddress", jsonMemberEmailAddress);
			this.memberEmailAddressStored = jsonMemberEmailAddress.stringValue();
			
			// clear stored phone number as we move from member to member
			this.memberPhoneNumberStored = "";
			String memPhoneNum = getMemberPhoneNumber();
			if(memPhoneNum != null && memPhoneNum.length() > 0) {
				JSONString jsonPhoneNumber = new JSONString(memPhoneNum);
				json.put("phoneNumber", jsonPhoneNumber);
				this.memberPhoneNumberStored = jsonPhoneNumber.stringValue();
			}
			
			// clear stored first name as we move from member to member
			this.memberFirstNameStored = "";
			String memFirstName = getMemberFirstName();
			if(memFirstName != null && memFirstName.length() > 0) {
				JSONString jsonFirstName = new JSONString(memFirstName);
				json.put("firstName", jsonFirstName);
				this.memberFirstNameStored = jsonFirstName.stringValue();
			}
			
			// clear stored first name as we move from member to member
			this.memberLastNameStored = "";
			String memLastName = getMemberLastName();
			if(memLastName != null && memLastName.length() > 0) {
				JSONString jsonLastName = new JSONString(memLastName);
				json.put("lastName", jsonLastName);
				this.memberLastNameStored = jsonLastName.stringValue();
			}
			
			com.google.gwt.http.client.Request request = builder.sendRequest(json.toString(), new RequestCallback() {
			    public void onError(com.google.gwt.http.client.Request request, Throwable exception) {
			    	// Couldn't connect to server (could be timeout, SOP violation, etc.)
			    	showAddMemberErrorMessage("We apologize but the rTeam service is currently unavailable");
			    }
	
			    public void onResponseReceived(com.google.gwt.http.client.Request request, com.google.gwt.http.client.Response response) {
			    	  //Window.alert("Status code of Add Member API = " + response.getStatusCode());
				      if (201 == response.getStatusCode()) {
				    	  // HTTP status success
				    	  JSONValue jsonValue = JSONParser.parseLenient(response.getText());
				    	  JSONObject jsonObject = jsonValue.isObject();
				    	  JSONString jsonString;
				    	  if(jsonObject != null) {
				    		  jsonString = jsonObject.get("apiStatus").isString();
				    		  String apiStatus = jsonString.stringValue();
				    		  //Window.alert("apiStatus of Add Member API = " + apiStatus);
				    		  if(apiStatus.equals("100")) {
					    		  addMemberToMemberTable();
					    		  clearMemberEmailAddress();
					    		  clearMemberPhoneNumber();
					    		  clearMemberFirstName();
					    		  clearMemberLastName();
					    		  showAddMemberSuccessMessage("Member added successfuly");
				    		  } else if(apiStatus.equals("209")) {
				    			  showAddMemberErrorMessage("Specified email address already used by another team member");
				    		  } else {
				    			  showAddMemberErrorMessage("Could not add member. Status = " + apiStatus);
				    		  }
				    	  }
				      } else {
				    	  // HTTP status error
				    	  showAddMemberErrorMessage("Error: could not add member");
				      }
				    }
				  });
		} catch (RequestException e) {
			showAddMemberErrorMessage("We apologize but the rTeam service is currently unavailable");
		}
	}

}
