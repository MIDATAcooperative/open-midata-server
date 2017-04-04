package utils.fhir.transactions;


import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.dstu3.model.DomainResource;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import utils.exceptions.AppException;
import utils.fhir.FHIRServlet;
import utils.fhir.ResourceProvider;

/**
 * Create a new FHIR resource as part of a FHIR tranction or batch
 *
 */
public class CreateTransactionStep extends TransactionStep {

	/**
	 * Creates a new resource create step using the given ResourceProvider and DomainResource
	 * @param provider the ResourceProvider to use
	 * @param resource the DomainResource from the user request
	 */
	public CreateTransactionStep(ResourceProvider<DomainResource> provider, DomainResource resource) {
		this.provider = provider;
		this.resource = resource;
	}
	
	@Override
    public void init() {
    	record = provider.init();
    }
	
	@Override
	public void prepare() throws AppException { 
		
		provider.prepare(record, resource);
		
	}
	
	@Override
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
