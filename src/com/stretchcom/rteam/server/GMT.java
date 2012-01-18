package com.stretchcom.rteam.server;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

public final class GMT {
	//private static final Logger log = Logger.getLogger(GMT.class.getName());
	private static RskyboxClient log = new RskyboxClient();
	
	private GMT() {
	}

	public static Date convertToGmtDate(String theDate, TimeZone theTimeZone) {
		return convertToGmtDate(theDate, true, theTimeZone);
	}

	public static Date convertToGmtDate(String theDate, Boolean theHasTime, TimeZone theTimeZone) {
		Date date = null;
		try {
			if(theHasTime) {
				date = stringToDate(theDate, theTimeZone);
			} else {
				date = stringWithoutTimeToDate(theDate, theTimeZone);
			}
		} catch(Exception e) {
			return null;
		}
		return date;
	}

	public static String convertToLocalDate(Date theDate, TimeZone theTimeZone) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		df.setTimeZone(theTimeZone);
		log.debug("convertToLocalDate(): timezone = " + theTimeZone.getDisplayName() + " local date = " + df.format(theDate));
		return df.format(theDate);
	}

	public static String convertToSimpleLocalDate(Date theDate, TimeZone theTimeZone) {
		// example of format:  Wed, Jul 4, 12:08 PM
		DateFormat df = new SimpleDateFormat("EEE, MMM d, hh:mm a");
		df.setTimeZone(theTimeZone);
		log.debug("convertToLocalDate(): timezone = " + theTimeZone.getDisplayName() + " local date = " + df.format(theDate));
		return df.format(theDate);
	}

	// only supports the format: YYYY-MM-DD hh:mm
	// parses date using specified time zone -- don't want to use the default which depends on server configuration
	public static Date stringToDate(String theDateStr, TimeZone theTimeZone) throws ParseException {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		df.setTimeZone(theTimeZone);
		return df.parse(theDateStr);
	}
	
	// only supports the format: YYYY-MM-DD
	// parses date using specified time zone -- don't want to use the default which depends on server configuration
	public static Date stringWithoutTimeToDate(String theDateStr, TimeZone theTimeZone) throws ParseException {
		log.debug("stringWithoutTimeToDate(): date input = " + theDateStr);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		df.setTimeZone(theTimeZone);
		return df.parse(theDateStr);
	}

	// only supports the format: YYYY-MM-DD hh:mm 
	public static String dateToString(Date theDate) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		return df.format(theDate);
	}
	
	// returns null if the TimeZoneStr passed in is not recognized time zone name
	// only full time zone names are supported - not time zone abbreviations
	public static TimeZone getTimeZone(String theTimeZoneNameStr) {
		if(theTimeZoneNameStr == null) {return null;}
		
		String[] timeZoneNames = TimeZone.getAvailableIDs();
		boolean isValid = false;
		for(String s: timeZoneNames) {
			//::TODO should this comparison ignore case?
			if(s.equals(theTimeZoneNameStr)) {
				isValid = true;
				break;
			}
		}
		if(isValid) {
			return TimeZone.getTimeZone(theTimeZoneNameStr);
		} else {
			return null;
		}
	}
	
	// returns
	// -------
	// List Item index 0 - the GMT Date corresponding to 12:00 am today of the given timezone
	// List Item index 1 - the GMT Date corresponding to 11:59 pm today of the given timezone
	public static List<Date> getTodayBeginAndEndDates(TimeZone theTimeZone) {
		List<Date> todayDates = new ArrayList<Date>();
		
		// calendar return current time by default
		Calendar startOfToday = Calendar.getInstance(theTimeZone);
		
		// set clock back to stroke of midnight
		startOfToday.set(Calendar.HOUR_OF_DAY, 0);
		startOfToday.set(Calendar.MINUTE, 0);
		startOfToday.set(Calendar.SECOND, 0);
		todayDates.add(startOfToday.getTime());
		// TODO remove logging code
		log.debug("start of Today = " + convertToLocalDate(startOfToday.getTime(), theTimeZone));
		
		
		// calendar return current time by default
		Calendar endOfToday = Calendar.getInstance(theTimeZone);
		
		// set clock 23:59:59 (last minute of the day)
		endOfToday.set(Calendar.HOUR_OF_DAY, 23);
		endOfToday.set(Calendar.MINUTE, 59);
		endOfToday.set(Calendar.SECOND, 59);
		todayDates.add(endOfToday.getTime());
		// TODO remove logging code
		log.debug("end of Today = " + convertToLocalDate(endOfToday.getTime(), theTimeZone));
		
		return todayDates;
	}
	
	
	// returns
	// -------
	// List Item index 0 - the GMT Date corresponding to 12:00 am tomorrow of the given timezone
	// List Item index 1 - the GMT Date corresponding to 11:59 pm tomorrow of the given timezone
	public static List<Date> getTomorrowBeginAndEndDates(TimeZone theTimeZone) {
		List<Date> tomorrowDates = new ArrayList<Date>();
		
		// calendar return current time by default
		Calendar startOfTomorrow = Calendar.getInstance(theTimeZone);
		startOfTomorrow.add(Calendar.DATE, 1);
		
		// reset clock back to stroke of midnight
		startOfTomorrow.set(Calendar.HOUR_OF_DAY, 0);
		startOfTomorrow.set(Calendar.MINUTE, 0);
		startOfTomorrow.set(Calendar.SECOND, 0);
		tomorrowDates.add(startOfTomorrow.getTime());
		// TODO remove logging code
		log.debug("start of Tomorrow = " + convertToLocalDate(startOfTomorrow.getTime(), theTimeZone));
		
		// calendar return current time by default
		Calendar endOfTomorrow = Calendar.getInstance(theTimeZone);
		endOfTomorrow.add(Calendar.DATE, 1);
		
		// reset clock back to stroke of midnight
		endOfTomorrow.set(Calendar.HOUR_OF_DAY, 23);
		endOfTomorrow.set(Calendar.MINUTE, 59);
		endOfTomorrow.set(Calendar.SECOND, 59);
		tomorrowDates.add(endOfTomorrow.getTime());
		// TODO remove logging code
		log.debug("end of Tomorrow = " + convertToLocalDate(endOfTomorrow.getTime(), theTimeZone));
		
		return tomorrowDates;
	}
	
	// Time zone isn't needed because date is converted to Calendar and back again and whatever the default time zone is should work.
	public static Date subtractDaysFromDate(Date theDate, int theNumberOfDays) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(theDate);
		cal.add(Calendar.DATE,-theNumberOfDays);
		return cal.getTime();
	}
	
	// Time zone isn't needed because date is converted to Calendar and back again and whatever the default time zone is should work.
	public static Date addDaysToDate(Date theDate, int theNumberOfDays) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(theDate);
		cal.add(Calendar.DATE,theNumberOfDays);
		return cal.getTime();
	}
	
	// Converts the date passed in to a future date that is at least 1 hours into the future. If the date passed in is already in 
	// the future, then that date is simply returned.
	// Time zone isn't needed because we are just adding 24 hours to the current date.
	public static Date setToFutureDate(Date theDate) {
		Calendar futureCal = Calendar.getInstance();
		futureCal.setTime(new Date());
		futureCal.add(Calendar.HOUR,1);
		// future date is 1 hour from now
		Date futureDate = futureCal.getTime();
		
		if(theDate.before(futureDate)) {
			return futureDate;
		}
		return theDate;
	}

	// Time zone isn't needed because date is converted to Calendar and back again and whatever the default time zone is should work.
	// TOOD oops, now I think it does matter what the timezone is.  Fix this.
	public static Date setTimeToEndOfTheDay(Date theDate) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(theDate);
		
		// set clock 23:59:59 (last minute of the day)
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		return cal.getTime();
	}

	// Time zone isn't needed because date is converted to Calendar and back again and whatever the default time zone is should work.
	// TOOD oops, now I think it does matter what the timezone is.  Fix this.
	public static Date setTimeToTheBeginningOfTheDay(Date theDate) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(theDate);
		
		// set clock 23:59:59 (last minute of the day)
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		return cal.getTime();
	}

}