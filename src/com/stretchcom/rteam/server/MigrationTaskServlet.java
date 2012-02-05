package com.stretchcom.rteam.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.NamedQuery;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.KeyFactory;

public class MigrationTaskServlet extends HttpServlet {
	//private static final Logger log = Logger.getLogger(MigrationTaskServlet.class.getName());
	private static RskyboxClient log = new RskyboxClient();

	private static int MAX_TASK_RETRY_COUNT = 3;

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.debug("MigrationTaskServlet.doGet() entered - SHOULD NOT BE CALLED!!!!!!!!!!!!!!!!!");
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		log.debug("MigrationTaskServlet.doPost() entered");
		String response = "migration completed successfully";
		resp.setContentType("text/plain");

		try {
			// no parameters as of yet
			String migrationName = req.getParameter("migrationName");
			log.debug("migrationName parameter: " + migrationName);
			
			// need to get the retry count
			String taskRetryCountStr = req.getHeader("X-AppEngine-TaskRetryCount");
			// default the retry count to max because if it can't be extracted, we are packing up the books and going home
			int taskRetryCount = MAX_TASK_RETRY_COUNT;
			try {
				taskRetryCount = new Integer(taskRetryCountStr);
			} catch (Exception e1) {
				log.debug("should never happen, but no harm, no foul");
			}
			log.debug("taskRetryCount = " + taskRetryCount);

		    Properties props = new Properties();
		    Session session = Session.getDefaultInstance(props, null);
		    
		    // ensure valid parameters
		    if(migrationName == null || migrationName.length() == 0) {
		    	log.error("MigrationTaskServlet:doPost:parameters", "MigrationTaskServlet.doPost(): null or empty migrationName parameter");
		    	return;
		    }

	    	if(migrationName.equalsIgnoreCase("normalizeEmailAddressesTask")) {
	    		normalizeUserEmailAddresses();
	    		normalizeMemberEmailAddresses();
	    	} else if(migrationName.equalsIgnoreCase("guardianSmsEmailAddressesTask")) {
	    		adjustGuardianSmsEmailAddressListSizes();
	    	} else if(migrationName.equalsIgnoreCase("normalizePhoneNumbersTask")) {
	    		normalizePhoneNumbers();
	    	} else if(migrationName.equalsIgnoreCase("normalizeGuardianListsTask")) {
	    		normalizeGuardianLists();
	    	} else if(migrationName.equalsIgnoreCase("defaultMemberAccessPreferencesTask")) {
	    		defaultMemberAccessPreferences();
	    	} else if(migrationName.equalsIgnoreCase("setActivityIsReplyTask")) {
	    		setActivityIsReply();
	    	} else if(migrationName.equalsIgnoreCase("setTeamShortenedPageUrlTask")) {
	    		setTeamShortenedPageUrl();
	    	}
		    
			// Return status depends on how many times this been attempted. If max retry count reached, return HTTP 200 so
		    // retries attempt stop.
		    if(taskRetryCount >= MAX_TASK_RETRY_COUNT) {
		    	resp.setStatus(HttpServletResponse.SC_OK);
		    }
		    
			resp.getWriter().println(response);
		}
		catch (Exception ex) {
			response = "Should not happen. Email send: failure : " + ex.getMessage();
			log.debug(response);
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.getWriter().println(response);
		}
	}
	
    private void normalizeUserEmailAddresses() {
		EntityManager emUsers = EMF.get().createEntityManager();
		try {
			List<User> allUsers = (List<User>)emUsers.createNamedQuery("User.getAll").getResultList();
			log.debug("total number of users to be processed = " + allUsers.size());
			
			int userEmailUpdateCount = 0;
			for(User u : allUsers) {
				String emailAddress = u.getEmailAddress();
				if(emailAddress != null) {
					String lowerCaseEmailAddress = emailAddress.toLowerCase();
					if(!lowerCaseEmailAddress.equals(emailAddress)) {
						u.setEmailAddress(lowerCaseEmailAddress);
						userEmailUpdateCount++;
					}
				}
			}
			log.debug("# of users that were updated = " + userEmailUpdateCount);
    	} catch (Exception e) {
    		log.exception("MigrationTaskServlet:normalizeUserEmailAddresses:Exception", "", e);
    	} finally {
		    emUsers.close();
		}
    }
	
    private void normalizeMemberEmailAddresses() {
		EntityManager emMember = EMF.get().createEntityManager();
		try {
			List<Member> allMembers = (List<Member>)emMember.createNamedQuery("Member.getAll").getResultList();
			log.debug("total number of members to be processed = " + allMembers.size());
			
			int primaryEmailUpdateCount = 0;
			int networkAuthenticatedEmailUpdateCount = 0;
			int guardianEmailUpdateCount = 0;
			for(Member m : allMembers) {
				// convert the Primary Email Address to lower case
				String emailAddress = m.getEmailAddress();
				if(emailAddress != null) {
					String lowerCaseEmailAddress = emailAddress.toLowerCase();
					if(!lowerCaseEmailAddress.equals(emailAddress)) {
						m.setEmailAddress(lowerCaseEmailAddress);
						primaryEmailUpdateCount++;
					}
				}
				
				// convert all the Network Authenticated Email Addresses to lower case
				List<String> networkAuthenticatedEmailAddresses = m.getNetworkAuthenticatedEmailAddresses();
				if(networkAuthenticatedEmailAddresses != null) {
					List<String> newNetworkAuthenticatedEmailAddresses = new ArrayList<String>();
					Boolean convertedEmailAddressToLowerCase = false;
					for(String naEa : networkAuthenticatedEmailAddresses) {
						String lowerCaseNaEa = naEa.toLowerCase();
						if(!lowerCaseNaEa.equals(naEa)) {
							newNetworkAuthenticatedEmailAddresses.add(lowerCaseNaEa);
							networkAuthenticatedEmailUpdateCount++;
							convertedEmailAddressToLowerCase = true;
						} else {
							newNetworkAuthenticatedEmailAddresses.add(naEa);
						}
					}
					// only update the entire list if something was changed
					if(convertedEmailAddressToLowerCase) {
						m.setNetworkAuthenticatedEmailAddresses(newNetworkAuthenticatedEmailAddresses);
					}
				}
				
				// convert all the Guardian Email Addresses to lower case
				List<String> guardianEmailAddresses = m.getGuardianEmailAddresses();
				if(guardianEmailAddresses != null) {
					List<String> newGuardianEmailAddresses = new ArrayList<String>();
					Boolean convertedEmailAddressToLowerCase = false;
					for(String gEa : guardianEmailAddresses) {
						String lowerCaseGuardianEa = gEa.toLowerCase();
						if(!lowerCaseGuardianEa.equals(gEa)) {
							newGuardianEmailAddresses.add(lowerCaseGuardianEa);
							guardianEmailUpdateCount++;
							convertedEmailAddressToLowerCase = true;
						} else {
							newGuardianEmailAddresses.add(gEa);
						}
					}
					if(convertedEmailAddressToLowerCase) {
						m.setGuardianEmailAddresses(newGuardianEmailAddresses);
					}
				}
			}
			log.debug("# primary email addresses updated = " + primaryEmailUpdateCount + 
					 "  # network authenticated email addresses updated = " + networkAuthenticatedEmailUpdateCount + 
					 "  # guardian email addresses updated = " + guardianEmailUpdateCount);
    	} catch (Exception e) {
    		log.exception("MigrationTaskServlet:normalizeMemberEmailAddresses:Exception", "", e);
    	} finally {
		    emMember.close();
		}
    }
    
    
    private void adjustGuardianSmsEmailAddressListSizes() {
		EntityManager emMember = EMF.get().createEntityManager();
		try {
			List<Member> allMembers = (List<Member>)emMember.createNamedQuery("Member.getAll").getResultList();
			log.debug("total number of members to be processed = " + allMembers.size());
			
			int membersWithGuardians = 0;
			int membersWithGuardianSmsAddresses = 0;
			int membersWithoutGuardianSmsAddresses = 0;
			for(Member m : allMembers) {
				if(m.getGuardianKeys() != null && m.getGuardianKeys().size() > 0) {
					membersWithGuardians++;
					if(m.getGuardianSmsEmailAddresses().size() > 0) {
						membersWithGuardianSmsAddresses++;
					} else {
						membersWithoutGuardianSmsAddresses++;
						List<String> guardianSmsEmailAddresses = new ArrayList<String>();
						for(int i=0; i<m.getGuardianKeys().size(); i++) {
							guardianSmsEmailAddresses.add("");
						}
						m.setGuardianSmsEmailAddresses(guardianSmsEmailAddresses);
					}
				}
			} //end of for(allMembers)
			log.debug("# members with guardians = " + membersWithGuardians + 
					 "  # members with guardian SMS addresses = " + membersWithGuardianSmsAddresses + 
					 "  # members without guardians SMS addresses = " + membersWithoutGuardianSmsAddresses);
    	} catch (Exception e) {
    		log.exception("MigrationTaskServlet:adjustGuardianSmsEmailAddressListSizes:Exception", "", e);
    	} finally {
		    emMember.close();
		}
    }
    
    
    private void normalizePhoneNumbers() {
		EntityManager emMemberships = EMF.get().createEntityManager();
		try {
			List<Member> phoneNumberMemberships = (List<Member>)emMemberships.createNamedQuery("Member.getAll").getResultList();
			log.debug("total number of members to be processed = " + phoneNumberMemberships.size());
			
			int membersWithPhoneNumbersCount = 0;
			int guardianPhoneNumberUpdateCount = 0;
			int memberCount = 0;
			for(Member m : phoneNumberMemberships) {
				memberCount++;
//				String phoneNumber = m.getPhoneNumber();
//				if(phoneNumber != null && phoneNumber.length() > 0) {
//					log.debug("memberCount = " + memberCount + " phoneNumber = " + phoneNumber);
//					phoneNumber =  Utility.extractAllDigits(phoneNumber);
//					phoneNumber = Utility.stripLeadingOneIfPresent(phoneNumber);
//					m.setPhoneNumber(phoneNumber);
//					membersWithPhoneNumbersCount++;
//				}
				
				List<Guardian> guardians = m.getGuardians();
				for(Guardian g : guardians) {
					String guardianPhoneNumber = g.getPhoneNumber();
					if(guardianPhoneNumber != null && guardianPhoneNumber.length() > 0) {
						guardianPhoneNumber =  Utility.extractAllDigits(guardianPhoneNumber);
						guardianPhoneNumber = Utility.stripLeadingOneIfPresent(guardianPhoneNumber);
						g.setPhoneNumber(guardianPhoneNumber);
						m.updateGuardian(g);
						guardianPhoneNumberUpdateCount++;
					}
				}
			}
			log.debug("membersWithPhoneNumbersCount = " + membersWithPhoneNumbersCount + " guardianPhoneNumberUpdateCount = " + guardianPhoneNumberUpdateCount);
    	} catch (Exception e) {
    		log.exception("MigrationTaskServlet:normalizePhoneNumbers:Exception", "", e);
    	} finally {
		    emMemberships.close();
		}
    }

    private void normalizeGuardianLists() {
		EntityManager emMemberships = EMF.get().createEntityManager();
		try {
			List<Member> guardianMemberships = (List<Member>)emMemberships.createNamedQuery("Member.getAll").getResultList();
			log.debug("total number of members to be processed = " + guardianMemberships.size());
			
			int membersWithGuardiansCount = 0;
			for(Member m : guardianMemberships) {
				if(m.guardianMigration()) {
					membersWithGuardiansCount++;
				}
			}
			log.debug("# of memberships with Guardian updates = " + membersWithGuardiansCount);
    	} catch (Exception e) {
    		log.exception("MigrationTaskServlet:normalizeGuardianLists:Exception", "", e);
    	} finally {
		    emMemberships.close();
		}
    }

    private void defaultMemberAccessPreferences() {
		EntityManager emMemberships = EMF.get().createEntityManager();
		try {
			List<Member> memberships = (List<Member>)emMemberships.createNamedQuery("Member.getAll").getResultList();
			log.debug("total number of members to be processed = " + memberships.size());
			
			int memberCount = 0;
			for(Member m : memberships) {
				memberCount++;
				m.setDefaultAccessPreferences();
			}
			log.debug("total number of members processed = " + memberCount);
    	} catch (Exception e) {
    		log.exception("MigrationTaskServlet:defaultMemberAccessPreferences:Exception", "", e);
    	} finally {
		    emMemberships.close();
		}
    }
    
    private void setActivityIsReply() {
    	final int TIME_INTERVAL = 10; // days
		EntityManager emActivities = EMF.get().createEntityManager();
		try {
			Date upperDate = new Date();
			Date lowerDate = GMT.subtractDaysFromDate(upperDate, TIME_INTERVAL);
			Date earliestDate = GMT.subtractDaysFromDate(upperDate, 730); // go back two years
			
			int activityCount = 0;
			while(upperDate.after(earliestDate)) {
				List<Activity> activities = (List<Activity>)emActivities.createNamedQuery("Activity.getByUpperAndLowerCreatedDates")
						.setParameter("mostCurrentDate", upperDate)
						.setParameter("leastCurrentDate", lowerDate)
						.getResultList();
				log.debug("total number of activities between " + GMT.dateToString(lowerDate) + " and " +  GMT.dateToString(upperDate) + " = " + activities.size());
				
				for(Activity a : activities) {
					a.setIsReply(false);
					activityCount++;
				}
				// go back 10 days at a time
				upperDate = GMT.subtractDaysFromDate(upperDate, TIME_INTERVAL);
				lowerDate = GMT.subtractDaysFromDate(lowerDate, TIME_INTERVAL);
			}
			log.debug("total number of activities processed = " + activityCount);
    	} catch (Exception e) {
    		log.exception("MigrationTaskServlet:setActivityIsReplyException", "", e);
    	} finally {
		    emActivities.close();
		}
    }
    
// NOTE: following code did not work right. Not sure why, but with the way teams are retrieved, any teams with the same name were NOT updated!!!!!!!!!!!!!!!
//    private void setTeamShortenedPageUrl() {
//    	//////////////////////////////////////////////////////////////////////////
//    	// Divide the teams into 63 chunks based on starting char of the team name
//    	//////////////////////////////////////////////////////////////////////////
//    	String keyChars = "!0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz}"; 
//		EntityManager emTeams = EMF.get().createEntityManager();
//		try {
//			int teamCount = 0;
//			int lowerIndex = 0;
//			int upperIndex = 1;
//			while(upperIndex < keyChars.length()) {
//				List<Team> teams = (List<Team>)emTeams.createNamedQuery("Team.getByTeamNameRange")
//						.setParameter("startInclusive", keyChars.charAt(lowerIndex))
//						.setParameter("endExclusive", keyChars.charAt(upperIndex))
//						.getResultList();
//				log.debug("total number of teams between " + keyChars.charAt(lowerIndex) + " and " +  keyChars.charAt(upperIndex) + " = " + teams.size());
//				
//				for(Team t : teams) {
//					//////////////////////////////////////////////////////////////////////////////
//				    // NOTE: unlike the other migrations, this would NOT work without transactions
//					//////////////////////////////////////////////////////////////////////////////
//					emTeams.getTransaction().begin();
//					//log.debug("processing team = " + t.getTeamName());
//					t.setPageUrl(UrlShort.reserveUniqueId());
//					//t.setPageUrl("");
//					emTeams.getTransaction().commit();
//					teamCount++;
//				}
//				
//				// increment the indexes
//				lowerIndex++;
//				upperIndex++;
//			}
//			log.debug("total number of teams processed = " + teamCount);
//    	} catch (Exception e) {
//    		log.exception("MigrationTaskServlet:setTeamShortenedPageUrl", "", e);
//    	} finally {
//		    if (emTeams.getTransaction().isActive()) {
//		    	emTeams.getTransaction().rollback();
//		    }
//		    emTeams.close();
//		}
//    }
    
    private void setTeamShortenedPageUrl() {
    	// must reset the UrlShort entity too because the migration did not work the first time I ran it
		EntityManager emUrlShort = EMF.get().createEntityManager();
		try {
			List<UrlShort> urlShorts = (List<UrlShort>)emUrlShort.createNamedQuery("UrlShort.getAll").getResultList();
			log.debug("total number of UrlShorts retrieved " + urlShorts.size());
			
			for(UrlShort us : urlShorts) {
				us.setUniqueId("0");
			}
			log.debug("total number of urlShorts processed = " + urlShorts.size());
    	} catch (Exception e) {
    		log.exception("MigrationTaskServlet:setTeamShortenedPageUrl", "", e);
    	} finally {
    		emUrlShort.close();
		}
    	
		EntityManager emTeams = EMF.get().createEntityManager();
		try {
			int teamCount = 0;
			List<Team> teams = (List<Team>)emTeams.createNamedQuery("Team.getAll").getResultList();
			log.debug("total number of teams retrieved " + teams.size());
			
			for(Team t : teams) {
				t.setPageUrl(UrlShort.reserveUniqueId());
				teamCount++;
			}
			log.debug("total number of teams processed = " + teamCount);
    	} catch (Exception e) {
    		log.exception("MigrationTaskServlet:setTeamShortenedPageUrl", "", e);
    	} finally {
		    if (emTeams.getTransaction().isActive()) {
		    	emTeams.getTransaction().rollback();
		    }
		    emTeams.close();
		}
    }
}
