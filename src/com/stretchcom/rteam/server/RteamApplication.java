package com.stretchcom.rteam.server;  
  
import java.io.File;

import org.restlet.Application;  
import org.restlet.Restlet;  
import org.restlet.data.LocalReference;
import org.restlet.data.Protocol;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;  
  
// This class is "registered" as the application Class in web.xml
// Application class is a subclass of Restlet, so is a type of Restlet
public class RteamApplication extends Application {  
	public static final String CURRENT_USER = "rTeam.currentUser";
	//::TODO make URL HTTPS?
	public static final String BASE_URL_WITH_SLASH = "http://rteamtest.appspot.com/";
	//public static final String BASE_URL_WITH_SLASH = "http://localhost:8888/";
	public static final String RTEAM_USER_ID = "rteamuserid"; // User ID in Message Threads created for automated email notifications
	public static final String GWT_HOST_PAGE = "Rteam.html";
	public static final String EMAIL_START_TOKEN_MARKER = ":rTeamId:";
	public static final String EMAIL_END_TOKEN_MARKER = "::";
  
    /** 
     * Creates a root Restlet that will receive all incoming calls. 
     */  
    @Override 
    // called by the Restlet framework when the applications starts.
    // The application must override this method. It must create and return the inbound root restlet
    public synchronized Restlet createInboundRoot() {  
        Router router = new Router(getContext());  
        
        // TODO what's this for
        getConnectorService().getClientProtocols().add(Protocol.FILE);

        // Serve the files generated by the GWT compilation step.
        File warDir = new File("");
        if (!"war".equals(warDir.getName())) {
            warDir = new File(warDir, "war/");
        }

        // for serving up the GWT Host HTML file - copied from GWT serialization example
        Directory dir = new Directory(getContext(), LocalReference.createFileReference(warDir));
        router.attachDefault(dir);
  
        // Define routes
        router.attach("/user", UserResource.class);
        router.attach("/user/{passwordResetQuestion}", UserResource.class);
        router.attach("/users", UsersResource.class);
        router.attach("/users/deleteAll", UsersResource.class);
        router.attach("/teams", TeamsResource.class);
        router.attach("/team/{teamId}", TeamResource.class);
        router.attach("/team/twitter/oauth", TeamResource.class);
        router.attach("/team/{teamId}/members", MembersResource.class);
        router.attach("/team/{teamId}/members/{multiple}", MembersResource.class);
        router.attach("/team/{teamId}/member/{memberId}", MemberResource.class);
        router.attach("/member", MemberResource.class);
        router.attach("/team/{teamId}/games", GamesResource.class);
        router.attach("/team/{teamId}/games/recurring/{multiple}", GamesResource.class);
        router.attach("/team/{teamId}/games/{timeZone}", GamesResource.class);
        router.attach("/games/{timeZone}", GamesResource.class);
        router.attach("/team/{teamId}/game/{gameId}", GameResource.class);
        router.attach("/team/{teamId}/game/{gameId}/{timeZone}", GameResource.class);
        router.attach("/team/{teamId}/game/{gameId}/vote/{voteType}", GameResource.class);
        router.attach("/team/{teamId}/game/{gameId}/vote/{voteType}/tallies", GameResource.class);
        router.attach("/team/{teamId}/practices", PracticesResource.class);
        router.attach("/team/{teamId}/practices/recurring/{multiple}", PracticesResource.class);
        router.attach("/team/{teamId}/practices/{timeZone}", PracticesResource.class);
        router.attach("/practices/{timeZone}", PracticesResource.class);
        router.attach("/team/{teamId}/practice/{practiceId}", PracticeResource.class);
        router.attach("/team/{teamId}/practice/{practiceId}/{timeZone}", PracticeResource.class);
        router.attach("/attendees", AttendeesResource.class);
        router.attach("/team/{teamId}/messageThreads", MessageThreadsResource.class);
        router.attach("/team/{teamId}/messageThreads/{timeZone}", MessageThreadsResource.class);
        router.attach("/team/{teamId}/messageThread/{messageThreadId}", MessageThreadResource.class);
        router.attach("/team/{teamId}/messageThread/{messageThreadId}/{timeZone}", MessageThreadResource.class);
        router.attach("/messageThread", MessageThreadResource.class);
        router.attach("/messageThreads", MessageThreadsResource.class);
        router.attach("/messageThreads/{timeZone}", MessageThreadsResource.class);
        router.attach("/messageThreads/count/{newCount}", MessageThreadsResource.class);
        router.attach("/team/{teamId}/activities", ActivitiesResource.class);
        router.attach("/team/{teamId}/activities/{timeZone}", ActivitiesResource.class);
        router.attach("/activities/status/{userVote}", ActivitiesResource.class);
        router.attach("/activities/{timeZone}", ActivitiesResource.class);
        router.attach("/team/{teamId}/activity/{activityId}", ActivityResource.class);
        router.attach("/team/{teamId}/activity/{activityId}/{media}", ActivityResource.class);
        router.attach("/mobileCarriers", MobileCarriersResource.class);
        router.attach("/sendEmailTask", EmailerResource.class); // don't think this is used other than in testing
        router.attach("/users/migration", UsersResource.class);
        router.attach("/sms", SmsResource.class); // handle Zeep Mobile callbacks
        router.attach("/cron/{job}", CronResource.class);
        
        // authentication requests, then route
        AuthorizationFilter authorizationFilter = new AuthorizationFilter(getContext());
        authorizationFilter.setNext(router);
        return authorizationFilter;
    }  
  
}  