package utils.fhir.transactions;

import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import utils.fhir.FHIRServlet;
import utils.fhir.ResourceProvider;

/**
 * Create a new FHIR resource as part of a FHIR tranction or batch
 *
 */
public class CreateTransactionStep extends TransactionStep {

	
	public CreateTransactionStep(ResourceProvider provider, BaseResource resource) {
		this.provider = provider;
		this.resource = resource;
	}
	
    public void init() {
    	record = provider.init();
    }
	
	public void prepare() { 		
		provider.prepare(record, resource);
	}
	
	public void execute() {
		try {
			if (result == null) {
				provider.insertRecord(record, resource);
				result = new BundleEntryComponent();
				BundleEntryResponseComponent response = new BundleEntryResponseComponent();
				response.setLastModified(record.created);
				response.setStatus("201 Created");
				response.setLocation(FHIRServlet.getBaseUrl()+"/"+provider.getResourceType().getSimpleName()+"/"+record._id.toString()+"/_history/0");
				result.setResponse(response);
			}
		} catch (BaseServerResponseException e) {
			setResultBasedOnException(e);			
		}
	}

	
}
