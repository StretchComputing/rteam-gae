package com.dg.demo.restlet.resource;

import com.dg.demo.model.MappedPersistence;
import com.dg.demo.model.impl.MappedCityPersistence;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.DomRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;

public class CityXMLResource extends Resource {

    /**
     * @param context  -- Restlet Context Instance.
     * @param request  -- Restlet Request Instance.
     * @param response -- Restlet Response Instance.
     */
    public CityXMLResource(Context context,
                           Request request,
                           Response response) {
        super(context, request, response);
        getVariants().add(
                new Variant(MediaType.TEXT_PLAIN));
        getVariants().add(
                new Variant(MediaType.APPLICATION_XML));
        //handle(request,response);
    }

    /**
     * Flag to allow POST via web form.
     *
     * @return true if POST is allowed, false if not.
     */
    public boolean allowPost() {
        return true;
    }

    /**
     * Represent the requested object in the requested format.
     *
     * @param variant -- the requested variant or default.
     *
     * @return an XML representation or an error string.
     */
    public Representation represent(Variant variant) {
        DomRepresentation representation = null;
        try {
            representation = getFraudXMLRepresentation();
        } catch (IOException e) {
            e.printStackTrace();
            getResponse().setEntity("ERROR " + e.getMessage(),
                    MediaType.TEXT_PLAIN);
        }
        return representation;
    }

    /**
     * Handle a POST Http request.
     *
     * @param entity -- the declared entity
     */
    public void acceptRepresentation(Representation entity) {
        System.out.println("USING POST");

        try {
            if (entity.getMediaType()
                    .equals(MediaType.APPLICATION_WWW_FORM, true)) {
                DomRepresentation representation =
                        getFraudXMLRepresentation();
                // Returns the XML representation of this document.
                getResponse().setEntity(representation);
            }
        } catch (IOException e) {
            e.printStackTrace();
            getResponse().setEntity("ERROR " + e.getMessage(),
                    MediaType.TEXT_PLAIN);
        }
    }

    /**
     * Creates the XML that will be returned to the requesting client.
     *
     * @return - The formatted XML Document
     */
    private DomRepresentation getFraudXMLRepresentation() throws IOException {

        DomRepresentation representation =
                new DomRepresentation(MediaType.TEXT_XML);

        MappedPersistence persistence =
                new MappedCityPersistence();

        // Generate a DOM document representing the list of frauds generated..
        Document d = representation.getDocument();

        //The Root Element, with it's corresponding attribute for the total score.
        Element root = d.createElement("citystatemappings");
        root.setAttribute("numberofstates",
                String.valueOf(persistence.valueSet().size()));
        d.appendChild(root);

        //If there are no frauds, then we skip this and just return the root.
        for (String state : persistence.valueSet()) {
            Element stateElement = d.createElement("state");
            stateElement.setAttribute("name", state);

            for (String city : persistence.mappedKeys(state)) {
                Element cityElement = d.createElement("city");
                cityElement.appendChild(d.createTextNode(city));

                stateElement.appendChild(cityElement);
            }
            //tie the frauds to the root.
            root.appendChild(stateElement);
        }
        d.normalizeDocument();
        return representation;
    }
}