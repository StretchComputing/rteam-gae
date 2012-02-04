package com.stretchcom.rteam.server;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.handinteractive.mobile.UAgentInfo;

public class UrlRewriteFilter implements Filter {
    //private static final Logger log = Logger.getLogger(UrlRewriteFilter.class.getName());
	private static RskyboxClient log = new RskyboxClient();
	
	private static final String SCOREBOARD_URL_PARTIAL = "/WEB-INF/scoreboard.html";

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
        ServletException {
    	
    	// Handle URL rewrite for two scenarios:
    	// 
    	// --------------------------
    	// URL pattern: rteam.com/123
    	// --------------------------
    	// Structure for the shortened team page URL. 123 represents the Team::pageUrl
    	// Convert to:  rteam.com/WEB-INF/scoreboard.html&<team_id>
    	//
    	// ----------------------------------------------------------
    	// URL pattern: REST call (numberous variations -- see below)
    	// ----------------------------------------------------------
    	// Convert to v1/<original_url>
    	// Was needed for backward compatibility when the "v1" version number was introduced to the API URLs

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String uri = httpRequest.getRequestURI();
            log.debug("Url Rewrite filter. Before URI: " + uri);
            if(!uri.toLowerCase().startsWith("/sms") && uri.length() <= 4) {
            	// strip the leading slash
            	uri = uri.substring(1);
            	String teamId = Team.getTeamId(uri);
            	if(teamId != null) {
                	uri = SCOREBOARD_URL_PARTIAL + "?teamId=" + teamId;
                    log.debug("after rewrite for team scoreboard -- URI: " + uri);
                    RequestDispatcher rd = httpRequest.getRequestDispatcher(uri);
                    rd.forward(request, response);
                    return;
            	}
            } else {
                // for any legacy REST API, we need to add in the v1 to the REST URL
                if(!uri.toLowerCase().contains("v1") &&
                  (uri.toLowerCase().startsWith("/user") ||
                   uri.toLowerCase().startsWith("/team") ||
                   uri.toLowerCase().startsWith("/member") ||
                   uri.toLowerCase().startsWith("/game") ||
                   uri.toLowerCase().startsWith("/practice") ||
                   uri.toLowerCase().startsWith("/attendee") ||
                   uri.toLowerCase().startsWith("/message")||
                   uri.toLowerCase().startsWith("/activit") ||
                   uri.toLowerCase().startsWith("/mobilecarriers") ||
                   uri.toLowerCase().startsWith("/sendemailtask") ||
                   uri.toLowerCase().startsWith("/sms") ||
                   uri.toLowerCase().startsWith("/cron"))
                   ) {
                    uri = "/v1" + uri;
                    log.debug("after rewrite for REST API versioning -- URI: " + uri);
                    RequestDispatcher rd = httpRequest.getRequestDispatcher(uri);
                    rd.forward(request, response);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig fc) throws ServletException {
    }

}
