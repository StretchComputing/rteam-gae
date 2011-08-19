package com.stretchcom.rteam.server;


import java.util.List;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
  
/** 
 * Resource that manages a list of items. 
 *  
 */  
public class MobileCarriersResource extends ServerResource {  
	private static final Logger log = Logger.getLogger(MobileCarriersResource.class.getName());

    // Handles 'Get Mobile Carriers List' API  
    @Get("json")
    public JsonRepresentation getList(Variant variant) {
        log.info("MobileCarriersResource:getList() entered");
        JSONObject jsonReturn = new JSONObject();
		
		String apiStatus = ApiStatusCode.SUCCESS;
		this.setStatus(Status.SUCCESS_OK);
		try {
			List<MobileCarrier> mobileCarriers = MobileCarrier.getList();
			JSONArray jsonArray = new JSONArray();
			for(MobileCarrier mc : mobileCarriers) {
				JSONObject jsonMobileCarrierObj = new JSONObject();
				jsonMobileCarrierObj.put("name", mc.getName());
				jsonMobileCarrierObj.put("code", mc.getCode());
				jsonArray.put(jsonMobileCarrierObj);
			}
			jsonReturn.put("mobileCarriers", jsonArray);
			log.info("number of mobile carriers returned = " + mobileCarriers.size());
		} catch (JSONException e) {
			log.severe("error converting json representation into a JSON object");
			e.printStackTrace();
			this.setStatus(Status.SERVER_ERROR_INTERNAL);
		}
		
		try {
			jsonReturn.put("apiStatus", apiStatus);
		} catch (JSONException e) {
			log.severe("error converting json representation into a JSON object");
		}
		return new JsonRepresentation(jsonReturn);
    }
}  
