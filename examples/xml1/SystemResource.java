package com.dg.demo.restlet.resource;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import java.util.Calendar;

/**
 * Simple String retrieval to determine whether the system is up
 * and running.  it adds a timestamp for varification that the
 * message isn't cached and allows for identification of
 * the time zone.
 */
public class SystemResource extends Resource {

    /**
     * Creates a new MapResource object.
     *
     * @param context  -- Restlet Context Instance.
     * @param request  -- Restlet Request Instance.
     * @param response -- Restlet Response Instance.
     */
    public SystemResource(Context context,
                          Request request,
                          Response response) {
        super(context, request, response);
        // Here we add the representation variants exposed
        getVariants().add(new Variant(MediaType.TEXT_PLAIN));
    }

    /**
     * Do not allow post to this resource.
     *
     * @return -- always false for this resource.
     */
    public boolean allowPost() {
        return false;
    }

    /**
     * default method for "get" from Client Application.
     *  Returns wheter or not the system is available,
     * and the current time.
     *
     * @param variant -- Available variants.
     *
     * @return -- A String representing whether or
     *            not the demo system is available, etc.
     */
    public Representation represent(Variant variant) {
        //LQSCache.getInstance();
        String message = "hello!, the Demo " +
                "Service is available." +
                " Time of request is:"
                + Calendar.getInstance()
                .getTime().toString();

        return new StringRepresentation(message,
                MediaType.TEXT_PLAIN);
    }
}