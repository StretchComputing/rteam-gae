package com.stretchcom.rteam.server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Logger;

import com.google.appengine.repackaged.com.google.common.util.Base64;

import sun.misc.BASE64Encoder;

public class Utility {
	private static final Logger log = Logger.getLogger(Utility.class.getName());
	public static final String NOT_SPECIFIED = "(not specified)";
	
	public static String removeAllWhiteSpace(String theInputString) {
		// remove the leading whitespace
		theInputString = theInputString.replaceAll("^\\s+", "");
		
		// remove the trailing whitespace
		theInputString = theInputString.replaceAll("\\s+$", "");
		
		// remove whitespace between words
		theInputString = theInputString.replaceAll("\\b\\s{2,}\\b", " ");
		
		return theInputString;
	}
	
	public static String extractAllDigits(String theInputString) {
		// remove all non-digits from the string
		theInputString = theInputString.replaceAll("\\D", "");
		return theInputString;
	}
	
	// returns true if all characters are digits
	public static Boolean isPhoneNumber(String thePotentialNumber) {
		if(thePotentialNumber == null) {return false;}
		int originalSize = thePotentialNumber.length();
		
		// remove all non-digits from the string
		thePotentialNumber = thePotentialNumber.replaceAll("\\D", "");
		int modifiedSize = thePotentialNumber.length();
		return originalSize == modifiedSize;
	}
	
	// only works for US numbers (i.e. 10 digits)
	// any non-digits are removed before formatting is done
	public static String formatPhoneNumber(String thePhoneNumber) {
		if(thePhoneNumber == null) {return thePhoneNumber;}
		
		// remove all non-digits from the string, so we can start from scratch
		thePhoneNumber = thePhoneNumber.replaceAll("\\D", "");

		if(thePhoneNumber.length() == 10) {
			StringBuffer sb = new StringBuffer();
			sb.append("(");
			sb.append(thePhoneNumber.substring(0,3));
			sb.append(") ");
			sb.append(thePhoneNumber.substring(3,6));
			sb.append("-");
			sb.append(thePhoneNumber.substring(6));
			return sb.toString();
		}
		return thePhoneNumber;
	}
	
	// If phoneNumber passed in is 11 digits and contains a leading "1", the phoneNumber without leading "1" is returned.
	// Otherwise, the phoneNumber passed is returned without change.
	public static String stripLeadingOneIfPresent(String thePhoneNumber) {
		if(thePhoneNumber == null) {return thePhoneNumber;}
		
		if(thePhoneNumber.length() == 11) {
			String leadingDigit = thePhoneNumber.substring(0, 1);
			if(leadingDigit.equals("1")) {
				thePhoneNumber = thePhoneNumber.substring(1, thePhoneNumber.length());
			}
		}
		
		return thePhoneNumber;
	}
    
    public static String getModMessage(String theFieldName, String theOldValue, String theNewValue) {
    	theOldValue = theOldValue.trim().length() == 0 ? NOT_SPECIFIED : theOldValue.trim();
    	theNewValue = theNewValue.trim().length() == 0 ? NOT_SPECIFIED : theNewValue.trim();
    	String modMessage = theFieldName + ": from '" + theOldValue + "' to '" + theNewValue + "'";
    	log.info("modMessage = " + modMessage);
    	return modMessage;
    }
    
    public static String capitalize(String theInputString) {
        if(theInputString == null) {
        	return null;
        }

        // Get first letter
        String firstLetter = theInputString.substring(0,1);
        
        // Get remainder of the string
        String remainder = "";
        if(theInputString.length() > 1) {
        	remainder   = theInputString.substring(1);
        }
        
        return firstLetter.toUpperCase() + remainder.toLowerCase();
    }
    
    
    // returns the event type as a string.  Possible values: [game, practice, event]
    public static String getEventType(Game theGame, Practice thePractice) {
    	String eventTypeStr = theGame != null ? Practice.GAME_EVENT_TYPE : Practice.PRACTICE_EVENT_TYPE;
    	if(thePractice != null) {
    		log.info("thePractice.getEventType() = " + thePractice.getEventType());
    	}
    	if(eventTypeStr.equalsIgnoreCase(Practice.PRACTICE_EVENT_TYPE) && thePractice.getEventType().equalsIgnoreCase(Practice.GENERIC_EVENT_TYPE)) {
    		eventTypeStr = "event";
    	}
    	return eventTypeStr;
    }
    
    // returns the event type as a string.  Possible values: [game, practice, event]
    // Assumes thePractices contains all the same type of event
    public static String getEventType(List<Game> theGames, List<Practice> thePractices) {
    	String eventTypeStr = theGames != null ? Practice.GAME_EVENT_TYPE : Practice.PRACTICE_EVENT_TYPE;
    	if(eventTypeStr.equalsIgnoreCase(Practice.PRACTICE_EVENT_TYPE) && thePractices.size() > 0 &&
    			thePractices.get(0).getEventType().equalsIgnoreCase(Practice.GENERIC_EVENT_TYPE)) {
    		eventTypeStr = "event";
    	}
    	return eventTypeStr;
    }
    
	public static String encrypt(String thePlainText) {
		String encryptedText = null;
		MessageDigest md = null;
		try {
			// use SHA encryption algorithm
			md = MessageDigest.getInstance("SHA");
			
			// convert input plain text into UTF-8 encoded bytes
			md.update(thePlainText.getBytes("UTF-8"));
			
			// extract the encrypted bytes
			byte raw[] = md.digest();
			
			// convert encrypted bytes to base64 encoded string so data can be stored in the database
			encryptedText = Base64.encode(raw);
		} catch (Exception e) {
			log.severe("Utility::encrypt() exception = " + e.getMessage());
		}
		return encryptedText;
	}
	
	// returns phone number if found, otherwise returns null
	public static String getPhoneNumberFromSmsEmailAddress(String theSmsEmailAddress) {
		if(theSmsEmailAddress == null) {return null;}
		
		int index = theSmsEmailAddress.indexOf("@");
		if(index >= 0) {
			String phoneNumber = theSmsEmailAddress.substring(0, index);
			return phoneNumber;
		}
		return null;
	}
	
	// returns carrier name if found, otherwise returns null
	public static String getCarrierNameFromSmsEmailAddress(String theSmsEmailAddress) {
		if(theSmsEmailAddress == null) {return null;}
		
		int index = theSmsEmailAddress.indexOf("@");
		if(index >= 0) {
			String carrierName = theSmsEmailAddress.substring(index+1);
			return carrierName;
		}
		return null;
	}
	
	public static Boolean doesEmailAddressStartWithPhoneNumber(String theEmailAddress) {
		if(theEmailAddress == null) {return false;}
		
		int index = theEmailAddress.indexOf("@");
		if(index >= 1) {
			String potentialPhoneNumber = theEmailAddress.substring(0, index);
			if(isPhoneNumber(potentialPhoneNumber)) {
				return true;
			}
		}
		
		return false;
	}
}
