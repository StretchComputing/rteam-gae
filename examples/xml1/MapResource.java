package com.dg.demo.restlet.resource;

import com.dg.demo.model.MappedPersistence;
import com.dg.demo.model.impl.MappedCityPersistence;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

/**
 * Utility Resource to view the various strings
 * mapped in the Persistence.
 */
public class MapResource extends Resource {

    /**
     * The map requested by the client application
     */
    String mapName;

    /**
     * Creates a new MapResource object.
     *
     * @param context  -- The restlet Context instance.
     * @param request  -- The restlet REquest instance.
     * @param response -- The restlet Response instance.
     */
    public MapResource(Context context,
                       Request request,
                       Response response) {
        super(context, request, response);
        this.mapName = (String) request.getAttributes().get("key");

        // Here we add the representation variants exposed
        getVariants().add(new Variant(MediaType.TEXT_PLAIN));
    }

    /**
     * Do not allow post to this resource.
     *
     * @return -- always false for this resource.
     */
    @Override
    public boolean allowPost() {
        return false;
    }

    /**
     * Collect the values for a particular mapping and return
     * a formatted string.  By inputting "all" into the request,
     * it returns a list of the mappings available. When this
     * service returns, substituting "all" for one of the
     * returned values will return the values for that mapping key
     *
     * @param variant -- The variant describing the return mappings.
     *
     * @return -- a formatted string with the map values as a list.
     */
    @Override
    public Representation represent(Variant variant) {
        //UDCAPICache.getInstance();
        MappedPersistence persistence = new MappedCityPersistence();

        //LQSCache.getInstance();
        String message;
        //String mapName = (String) getRequest().getAttributes().get("key");

        if (mapName.equals("all")) {
            message = persistence.toString();
        } else if (persistence.valueSet().contains(mapName)) {
            message = persistence.getCommonValues(mapName);
        } else {
            message = "NO VALUES FOUND FOR: " + mapName;
        }
        return new StringRepresentation(message, MediaType.TEXT_PLAIN);
    }
}