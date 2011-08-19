package com.dg.demo.restlet.application;

import com.dg.demo.restlet.resource.CityXMLResource;
import com.dg.demo.restlet.resource.MapResource;
import com.dg.demo.restlet.resource.SystemResource;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.Router;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.StringRepresentation;

/**
 * The Application Root Class.  Maps all of the resources.
 */
public class DemoApplication extends Application {

    /**
     * Creates a new DemoApplication object.
     */
    public DemoApplication() {
        //empty
    }

    /**
     * Public Constructor to create an instance of DemoApplication.
     *
     * @param parentContext - the org.restlet.Context instance
     */
    public DemoApplication(Context parentContext) {
        super(parentContext);
    }

    /**
     * The Restlet instance that will call the correct resource
     * depending up on URL mapped to it.
     *
     * @return -- The resource Restlet mapped to the URL.
     */
    @Override
    public Restlet createRoot() {
        Router router = new Router(getContext());

        router.attach("/sys", SystemResource.class);
        router.attach("/{key}/maps", MapResource.class);
        router.attach("/xml", CityXMLResource.class);

        Restlet mainpage = new Restlet() {
            @Override
            public void handle(Request request, Response response) {
                StringBuilder stringBuilder = new StringBuilder();

                stringBuilder.append("<html>");
                stringBuilder.append("<head><title>Sample Application " +
                        "Servlet Page</title></head>");
                stringBuilder.append("<body bgcolor=white>");
                stringBuilder.append("<table border=\"0\">");
                stringBuilder.append("<tr>");
                stringBuilder.append("<td>");
                stringBuilder.append("<h3>available REST calls</h3>");
                stringBuilder.append("<ol><li>/sys --> returns system " +
                        "up and date string</li>");
                stringBuilder.append("<li>/all/maps --> returns a list" +
                        " of all the cities and states</li>");
                stringBuilder.append("<li>/{key}/maps --> returns a list " +
                        "of cities for a particular state (CA,IL,TX)<br/>");
                stringBuilder.append("using one of the keys from the \"all\" " +
                        "call<br/> pasted in place as the {key}.</li>");
                stringBuilder.append("<li>/xml --> POST or GET URL or web " +
                        "form elements as XML to this</li>");
                stringBuilder.append("</ol>");
                stringBuilder.append("</td>");
                stringBuilder.append("</tr>");
                stringBuilder.append("</table>");
                stringBuilder.append("</body>");
                stringBuilder.append("</html>");

                response.setEntity(new StringRepresentation(
                        stringBuilder.toString(),
                        MediaType.TEXT_HTML));
            }
        };
        router.attach("", mainpage);
        return router;
    }
}