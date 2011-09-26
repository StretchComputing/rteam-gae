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

    private static final Logger log = Logger.getLogger(UrlRewriteFilter.class.getName());

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
        ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String uri = httpRequest.getRequestURI();
            log.info("Url Rewrite filter. Before URI: " + uri);
            
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
                log.info("Url Rewrite filter. After URI: " + uri);
                RequestDispatcher rd = httpRequest.getRequestDispatcher(uri);
                rd.forward(request, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig fc) throws ServletException {
    }

}
