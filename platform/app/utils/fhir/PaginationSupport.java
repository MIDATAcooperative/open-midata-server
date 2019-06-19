/*package utils.fhir;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;
import utils.AccessLog;

public class PaginationSupport extends InterceptorAdapter {

	@Override
	public boolean outgoingResponse(RequestDetails theRequestDetails, IBaseResource theResponseObject) {
		
		if (theResponseObject instanceof Bundle) {
			AccessLog.log("FOUND BUNDLE");
		}
		theRequestDetails.
		return true;
		
	}

	
}
*/