package utils.fhir;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Enumerations.ResourceType;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.client.exceptions.NonFhirResponseException;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnclassifiedServerFailureException;
import ca.uhn.fhir.util.OperationOutcomeUtil;
import utils.AccessLog;
import utils.auth.ExecutionInfo;
import utils.messaging.SubscriptionManager;

public class MessageProcessor {

	
		@Operation(name="$process-message")
		public Bundle processMessage(				  
		   @OperationParam(name="content") Bundle content,
		   @OptionalParam(name="async") TokenParam async
		) {
			
			BundleEntryComponent bec = content.getEntryFirstRep();
			if (bec == null) throw new InvalidRequestException("Bundle has no entries.");
			
			Resource mh = bec.getResource();
			if (mh==null || mh.getResourceType() == null || !mh.getResourceType().toString().equals("MessageHeader")) throw new InvalidRequestException("Missing MessageHeader at beginning of bundle");
			
			MessageHeader mhr = (MessageHeader) mh;
			String eventCode = mhr.hasEventCoding() ? mhr.getEventCoding().getCode() : mhr.getEventUriType().getValue();
			
			String inputBundle = ResourceProvider.ctx.newJsonParser().encodeResourceToString(content);
			
			boolean doasync = async != null && async.getValue() != null && async.getValue().equals("true");
			ExecutionInfo inf = ResourceProvider.info();
			String result = SubscriptionManager.messageToProcess(inf.executorId, inf.pluginId, eventCode, inputBundle, doasync);
			
			if (doasync) {			
				Bundle resultBundle = new Bundle();				
				return resultBundle;
			}
			AccessLog.log(result);
			
			Bundle resultBundle = new Bundle();
			
			if (result != null) {
				IBaseResource retVal = ResourceProvider.ctx.newJsonParser().parseResource(result);
				if (retVal instanceof Bundle) return (Bundle) retVal;
							
				resultBundle.addEntry().setResource((Resource) retVal);
			}
			return resultBundle;
		}	

		
}
