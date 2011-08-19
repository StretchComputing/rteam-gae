package com.stretchcom.rteam.server;  
  
import org.restlet.resource.Get;  
import org.restlet.resource.ServerResource;  
  
/** 
 * Resource which has only one representation. 
 *  
 */  
public class HelloWorldResource extends ServerResource {  
  
    @Get  
    public String represent() {  
        return "now running using Restlets!!!!!! After system crash ...";  
    }  
  
}  