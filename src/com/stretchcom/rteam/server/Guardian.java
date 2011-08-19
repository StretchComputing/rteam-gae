package com.stretchcom.rteam.server;

import java.util.logging.Logger;

public class Guardian {
	private static final Logger log = Logger.getLogger(Guardian.class.getName());
	
	private String key;
	private String emailAddress;
	private String phoneNumber;
	private String userId;
	private Boolean isEmailAddressActive;
	private String firstName;
	private String lastName;
	private Integer autoArchiveDayCount;
	private Boolean isNetworkAuthenticated;
	private Boolean hasBeenSmsConfirmed;
	private String smsEmailAddress;
	private Boolean isEmailAddressNetworkAuthenticated;
	private Boolean hasRteamMessageAccessEnabled;
	private Boolean hasEmailMessageAccessEnabled;
	private Boolean hasSmsMessageAccessEnabled;

	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}

	public String getEmailAddress() {
		return emailAddress;
	}
	public void setEmailAddress(String theEmailAddress) {
		// always store email in lower case to make queries and comparisons case insensitive
		if(theEmailAddress != null) {theEmailAddress = theEmailAddress.toLowerCase();}
		this.emailAddress = theEmailAddress;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public Boolean getIsEmailAddressActive() {
		return isEmailAddressActive;
	}
	public void setIsEmailAddressActive(Boolean isEmailAddressActive) {
		this.isEmailAddressActive = isEmailAddressActive;
	}

	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Integer getAutoArchiveDayCount() {
		return autoArchiveDayCount;
	}
	public void setAutoArchiveDayCount(Integer autoArchiveDayCount) {
		this.autoArchiveDayCount = autoArchiveDayCount;
	}

	public Boolean getIsNetworkAuthenticated() {
		return isNetworkAuthenticated;
	}
	public void setIsNetworkAuthenticated(Boolean isNetworkAuthenticated) {
		this.isNetworkAuthenticated = isNetworkAuthenticated;
	}
	
	public Boolean getHasBeenSmsConfirmed() {
		return hasBeenSmsConfirmed;
	}
	public void setHasBeenSmsConfirmed(Boolean hasBeenSmsConfirmed) {
		this.hasBeenSmsConfirmed = hasBeenSmsConfirmed;
	}
	
	public String getSmsEmailAddress() {
		return smsEmailAddress;
	}
	public void setSmsEmailAddress(String smsEmailAddress) {
		this.smsEmailAddress = smsEmailAddress;
	}
	
	public Boolean getIsEmailAddressNetworkAuthenticated() {
		return isEmailAddressNetworkAuthenticated;
	}
	
	public void setIsEmailAddressNetworkAuthenticated(
			Boolean isEmailAddressNetworkAuthenticated) {
		this.isEmailAddressNetworkAuthenticated = isEmailAddressNetworkAuthenticated;
	}
	public Boolean getHasRteamMessageAccessEnabled() {
		return hasRteamMessageAccessEnabled;
	}

	public void setHasRteamMessageAccessEnabled(Boolean hasRteamMessageAccessEnabled) {
		this.hasRteamMessageAccessEnabled = hasRteamMessageAccessEnabled;
	}

	public Boolean getHasEmailMessageAccessEnabled() {
		return hasEmailMessageAccessEnabled;
	}

	public void setHasEmailMessageAccessEnabled(Boolean hasEmailMessageAccessEnabled) {
		this.hasEmailMessageAccessEnabled = hasEmailMessageAccessEnabled;
	}

	public Boolean getHasSmsMessageAccessEnabled() {
		return hasSmsMessageAccessEnabled;
	}

	public void setHasSmsMessageAccessEnabled(Boolean hasSmsMessageAccessEnabled) {
		this.hasSmsMessageAccessEnabled = hasSmsMessageAccessEnabled;
	}
	
	public String getFullName() {
		return getDisplayName(this.firstName, this.lastName, this.emailAddress, this.phoneNumber, false);
	}
	
    public static String getDisplayName(String theFirstName, String theLastName, String theEmailAddress, String thePhoneNumber, Boolean theAbbreviate) {
		String displayName = theEmailAddress;
		if(theFirstName != null && theFirstName.trim().length() > 0 && theLastName != null && theLastName.trim().length() > 0) {
			if(theAbbreviate) {
				// the only thing different for abbreviation is only the last initial is used if both first and last names present
				displayName = theFirstName + " " + theLastName.charAt(0);
			} else {
				displayName = theFirstName + " " + theLastName;
			}
		} else if(theFirstName != null && theFirstName.trim().length() > 0) {
			displayName = theFirstName;
		} else if(theLastName != null && theLastName.trim().length() > 0) {
			displayName = theLastName;
		} else if(thePhoneNumber != null && thePhoneNumber.trim().length() > 0) {
			displayName = Utility.formatPhoneNumber(thePhoneNumber);
		}
		return displayName;
    }
	
}
