/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.fhir;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Enumerations.ResourceType;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;

import java.util.HashMap;
import java.util.Map;

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
		   @OperationParam(name="async") TokenParam async
		) {
			
			BundleEntryComponent bec = content.getEntryFirstRep();
			if (bec == null) throw new InvalidRequestException("Bundle has no entries.");
			
			Resource mh = bec.getResource();
			if (mh==null || mh.getResourceType() == null || !mh.getResourceType().toString().equals("MessageHeader")) throw new InvalidRequestException("Missing MessageHeader at beginning of bundle");
			
			MessageHeader mhr = (MessageHeader) mh;
			String eventCode = mhr.hasEventCoding() ? mhr.getEventCoding().getCode() : mhr.getEventUriType().getValue();
			
			String destination = mhr.hasDestination() ? mhr.getDestinationFirstRep().getEndpoint() : null;
			
			String inputBundle = ResourceProvider.ctx.newJsonParser().encodeResourceToString(content);
			
			Map<String, String> params = null;
			if (content.getEntry().size()>=2) {
				Resource r = content.getEntry().get(1).getResource();
				if (r instanceof Parameters) {
					Parameters parameters = (Parameters) r;
					params = new HashMap<String, String>();
					for (ParametersParameterComponent entry : parameters.getParameter()) {
						params.put(entry.getName(), entry.getValue().primitiveValue());
					}
				}
			}
			
			boolean doasync = async != null && async.getValue() != null && async.getValue().equals("true");
			ExecutionInfo inf = ResourceProvider.info();
			String result = SubscriptionManager.messageToProcess(inf.executorId, inf.pluginId, eventCode, destination, "4.0", inputBundle, params, doasync);
			
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
