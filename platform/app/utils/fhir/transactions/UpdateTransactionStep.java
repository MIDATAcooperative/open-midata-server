package utils.fhir.transactions;

import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryResponseComponent;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import utils.fhir.FHIRServlet;
import utils.fhir.ResourceProvider;

public class UpdateTransactionStep extends TransactionStep {

	
	
	public UpdateTransactionStep(ResourceProvider provider, BaseResource resource) {
		this.provider = provider;
		this.resource = resource;
	}
	
    public void init() {
    	record = ResourceProvider.fetchCurrent(resource.getIdElement());
    }
	
	public void prepare() { 	
		provider.prepare(record, resource);		
	}
	
	public void execute() {
		
		if (result == null) {
			provider.updateRecord(record, resource);
			result = new BundleEntryComponent();
			BundleEntryResponseComponent response = new BundleEntryResponseComponent();
			response.setLastModified(record.lastUpdated);
			response.setStatus("200 OK");
			response.setLocation(FHIRServlet.getBaseUrl()+"/"+provider.getResourceType().getSimpleName()+"/"+record._id.toString()+"/_history/"+record.version);
			result.setResponse(response);
		}
		
	}
		
}