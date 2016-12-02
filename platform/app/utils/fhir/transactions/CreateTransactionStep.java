package utils.fhir.transactions;

import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import utils.exceptions.AppException;
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
		try {
		  provider.prepare(record, resource);
		} catch (AppException e) {
		  
		}
	}
	
	public void execute() {
		try {
			if (result == null) {
				try {
				  provider.insertRecord(record, resource);
				} catch (AppException e) {
				  throw new InternalErrorException(e);
				}
				result = new BundleEntryComponent();
				BundleEntryResponseComponent response = new BundleEntryResponseComponent();
				response.setLastModified(record.created);
				response.setStatus("201 Created");
				response.setLocation(FHIRServlet.getBaseUrl()+"/"+provider.getResourceType().getSimpleName()+"/"+record._id.toString()+"/_history/0");
				result.setResponse(response);
			}
		} catch (Exception e) {
			setResultBasedOnException(e);			
		} 
	}

	
}
