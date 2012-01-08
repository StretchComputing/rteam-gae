package com.stretchcom.rteam.server;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class UserMemberInfo {
	private static final Logger log = Logger.getLogger(UserMemberInfo.class.getName());
	
	private String apiStatus;
	private String firstName;
	private String lastName;
	private String fullName;
	private String emailAddress;
	private String phoneNumber;
	private String oneUseToken;
	private String oneUseSmsToken;
	private String memberId;
	private String userId;
	private Team team;
	private String participantRole;
	private Boolean isGuardian;
	private String primaryDisplayName; // only set if isGuardian is true
	private Boolean hasBeenSmsConfirmed;
	private String smsEmailAddress;
	private Integer autoArchiveDayCount;
	private Member member;  // held on temporarily so UMI list can be built in one loop and processed in another
	private Boolean hasRteamMessageAccessEnabled;
	private Boolean hasEmailMessageAccessEnabled;
	private Boolean hasSmsMessageAccessEnabled;
	private Boolean hasCcToSelfEnabled;

	public UserMemberInfo() {
	}

	public UserMemberInfo(String theApiStatus) {
		this.apiStatus = theApiStatus;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getApiStatus() {
		return apiStatus;
	}

	public void setApiStatus(String apiStatus) {
		this.apiStatus = apiStatus;
	}
	
	public String getOneUseToken() {
		return oneUseToken;
	}

	public void setOneUseToken(String oneUseToken) {
		this.oneUseToken = oneUseToken;
	}

	public String getOneUseSmsToken() {
		return oneUseSmsToken;
	}

	public void setOneUseSmsToken(String oneUseSmsToken) {
		this.oneUseSmsToken = oneUseSmsToken;
	}
	
	@Override
	// if two objects are equal according to the equals() method, they must have the same hashCode()
	// value (although the reverse is not generally true)
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((emailAddress == null) ? 0 : emailAddress.hashCode());
		return result;
	}

	@Override
	// determined by email address and phone number
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass()) {
			return false;
		}
		
		// email takes precedence on the compare and is used if either object being compared has an non-null email address
		UserMemberInfo other = (UserMemberInfo) obj;
		if (emailAddress == null) {
			if (other.emailAddress != null) return false;
			
			// if we get this far, the email address in both objects are null - so compare phone numbers
			if(phoneNumber != null && !phoneNumber.equals(other.phoneNumber)) {
				return false;
			}
		} else if (!emailAddress.equals(other.emailAddress)) {
			return false;
		}
		return true;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getMemberId() {
		return memberId;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}

	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}

	public String getParticipantRole() {
		return participantRole;
	}

	public void setParticipantRole(String participantRole) {
		this.participantRole = participantRole;
	}

	public Boolean getIsGuardian() {
		if(this.isGuardian == null) {
			return false;
		}
		return isGuardian;
	}

	public void setIsGuardian(Boolean isGuardian) {
		this.isGuardian = isGuardian;
	}

	public String getPrimaryDisplayName() {
		return primaryDisplayName;
	}

	public void setPrimaryDisplayName(String primaryDisplayName) {
		this.primaryDisplayName = primaryDisplayName;
	}
	
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
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

	public Integer getAutoArchiveDayCount() {
		return autoArchiveDayCount;
	}

	public void setAutoArchiveDayCount(Integer autoArchiveDayCount) {
		this.autoArchiveDayCount = autoArchiveDayCount;
	}

	public Member getMember() {
		return member;
	}

	public void setMember(Member member) {
		this.member = member;
	}

	public Boolean getHasRteamMessageAccessEnabled() {
		if(hasRteamMessageAccessEnabled == null) {return false;}
		return hasRteamMessageAccessEnabled;
	}

	public void setHasRteamMessageAccessEnabled(Boolean hasRteamMessageAccessEnabled) {
		this.hasRteamMessageAccessEnabled = hasRteamMessageAccessEnabled;
	}

	public Boolean getHasEmailMessageAccessEnabled() {
		if(hasEmailMessageAccessEnabled == null) {return false;}
		return hasEmailMessageAccessEnabled;
	}

	public void setHasEmailMessageAccessEnabled(Boolean hasEmailMessageAccessEnabled) {
		this.hasEmailMessageAccessEnabled = hasEmailMessageAccessEnabled;
	}

	public Boolean getHasSmsMessageAccessEnabled() {
		if(hasSmsMessageAccessEnabled == null) {return false;}
		return hasSmsMessageAccessEnabled;
	}

	public void setHasSmsMessageAccessEnabled(Boolean hasSmsMessageAccessEnabled) {
		this.hasSmsMessageAccessEnabled = hasSmsMessageAccessEnabled;
	}
	
	public Boolean getHasCcToSelfEnabled() {
		if(hasCcToSelfEnabled == null) {return false;}
		return hasCcToSelfEnabled;
	}

	public void setHasCcToSelfEnabled(Boolean hasCcToSelfEnabled) {
		this.hasCcToSelfEnabled = hasCcToSelfEnabled;
	}
	
	// List passed in is filtered to eliminate duplicates. The following fields are guaranteed to be unique across 
	// all entries in the list returned:
	//    * emailAddress
	//    * userId
	//    * phoneNumber
	// If two or more entries are determined to be duplicates, then these entries are merged, not deleted. An
	// example of why merging is important is as follows: entry one contains an emailAddress and userId and 
	// entry two contains a userId and phoneNumber.  The userId in the two entries is the same. A single entry
	// containing an emailAddress, userId and phoneNumber is returned in the list. 
	// Error message generated if merging entities contain non-empty fields that are not the same.
	public static List<UserMemberInfo> filterDuplicates(List<UserMemberInfo> theUserMemberInfoList) {
		List<UserMemberInfo> filteredList = new ArrayList<UserMemberInfo>();
		
		for(UserMemberInfo in : theUserMemberInfoList) {
			// check if the input entity matches any entity in the output list
			UserMemberInfo outMatch = null;
			for(UserMemberInfo out : filteredList) {
				if( (in.getEmailAddress() != null && out.getEmailAddress() != null && in.getEmailAddress().equalsIgnoreCase(out.getEmailAddress())) || 
				    (in.getUserId() != null && out.getUserId() != null && in.getUserId().equalsIgnoreCase(out.getUserId())) ||
				    (in.getPhoneNumber() != null && out.getPhoneNumber() != null && in.getPhoneNumber().equalsIgnoreCase(out.getPhoneNumber()))    )
				{
					outMatch = out;
					break;
				}
			}
			
			if(outMatch != null) {
				////////////////////////////////////////////////////////////////////////////////
				// merge the inputEntity into the outputEntity that's already in the output list
				////////////////////////////////////////////////////////////////////////////////
				if(in.getEmailAddress() == null && outMatch.getEmailAddress() != null) {
					// ok, nothing to do, output EA already set correctly
				} else if(in.getEmailAddress() != null && outMatch.getEmailAddress() == null) {
					outMatch.setEmailAddress(in.getEmailAddress());
				} else if(!in.getEmailAddress().equalsIgnoreCase(outMatch.getEmailAddress())) {
					// error because both EAs are not null and they don't equal
					log.severe("input EA = " + in.getEmailAddress() + " not equal to output EA = " + outMatch.getEmailAddress());
				}
			
				if(in.getUserId() == null && outMatch.getUserId() != null) {
					// ok, nothing to do, output userId already set correctly
				} else if(in.getUserId() != null && outMatch.getUserId() == null) {
					outMatch.setUserId(in.getUserId());
				} else if(!in.getUserId().equalsIgnoreCase(outMatch.getUserId())) {
					// error because both userIds are not null and they don't equal
					log.severe("input userId = " + in.getUserId() + " not equal to output userId = " + outMatch.getUserId());
				}

				if(in.getPhoneNumber() == null && outMatch.getPhoneNumber() != null) {
					// ok, nothing to do, output PN already set correctly
				} else if(in.getPhoneNumber() != null && outMatch.getPhoneNumber() == null) {
					outMatch.setPhoneNumber(in.getPhoneNumber());
				} else if(!in.getPhoneNumber().equalsIgnoreCase(outMatch.getPhoneNumber())) {
					// error because both PNs are not null and they don't equal
					log.severe("input PN = " + in.getPhoneNumber() + " not equal to output PN = " + outMatch.getPhoneNumber());
				}

			} else {
				// just add the inputEntity to the end of the output list
				filteredList.add(in);
			}
		}
		
		return filteredList;
	}
	
	// Add theIndividual to the list if it has unique emailAddress and phoneNumber
	public static void addIfUniqueContactInfo(UserMemberInfo theIndividual, List<UserMemberInfo> theUniqueIndividuals) {
		if(theIndividual == null) {return;}

		String ea = theIndividual.getEmailAddress();
		String pn = theIndividual.getPhoneNumber();
		
		// if individual passed in has no contact info, just return
		if( (ea == null || ea.length() == 0) && (pn == null || pn.length() == 0) ) {return;}
		
		// Verify emailAddress is not already in list
		if(ea != null && ea.length() > 0) {
			for(UserMemberInfo umi : theUniqueIndividuals) {
				if(umi.getEmailAddress() != null && umi.getEmailAddress().equalsIgnoreCase(ea)) {
					// list already contains an individual with this emailAddress
					return;
				}
			}
		}
		
		// Verify phoneNumber is not already in list
		if(pn != null && pn.length() > 0) {
			for(UserMemberInfo umi : theUniqueIndividuals) {
				if(umi.getPhoneNumber() != null && umi.getPhoneNumber().equalsIgnoreCase(pn)) {
					// list already contains an individual with this phoneNumber
					return;
				}
			}
		}
		
		theUniqueIndividuals.add(theIndividual);
	}
	
	public Boolean isAnyMessageAccessEnabled() {
		if(this.hasRteamMessageAccessEnabled || this.hasSmsMessageAccessEnabled || this.hasSmsMessageAccessEnabled) {
			return true;
		}
		return false;
	}
}
