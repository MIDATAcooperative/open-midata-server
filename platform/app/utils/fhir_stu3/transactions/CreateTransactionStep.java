package utils.fhir_stu3.transactions;


import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.dstu3.model.DomainResource;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import models.Model;
import utils.exceptions.AppException;
import utils.fhir_stu3.FHIRServlet;
import utils.fhir_stu3.ReadWriteResourceProvider;
import utils.fhir_stu3.ResourceProvider;

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
	public CreateTransactionStep(ResourceProvider<DomainResource, Model> provider, DomainResource resource) {
		this.provider = provider;
		this.resource = resource;
	}
	
	@Override
    public void init() {
    	record = ((ReadWriteResourceProvider) provider).init();
    }
	
	@Override
	public void prepare() throws AppException { 		
		((ReadWriteResourceProvider) provider).createPrepare(record, resource);		
	}
	
	@Override
	public void execute() {
		try {
			if (result == null) {
				try {
					((ReadWriteResourceProvider) provider).createExecute(record, resource);
				} catch (AppException e) {
				  throw new InternalErrorException(e);
				}
				result = new BundleEntryComponent();
				BundleEntryResponseComponent response = new BundleEntryResponseComponent();
				response.setLastModified(((ReadWriteResourceProvider) provider).getLastUpdated(record));
				response.setStatus("201 Created");
				response.setLocation(FHIRServlet.getBaseUrl()+"/"+provider.getResourceType().getSimpleName()+"/"+record._id.toString()+"/_history/0");
				result.setResponse(response);
			}
		} catch (Exception e) {
			setResultBasedOnException(e);			
		} 
	}

	
}
