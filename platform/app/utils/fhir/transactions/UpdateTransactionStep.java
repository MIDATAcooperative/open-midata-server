package utils.fhir.transactions;

import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.dstu3.model.DomainResource;

import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import models.Model;
import utils.exceptions.AppException;
import utils.fhir.FHIRServlet;
import utils.fhir.ReadWriteResourceProvider;
import utils.fhir.ResourceProvider;

/**
 * Transaction step containing a resource UPDATE
 *
 */
public class UpdateTransactionStep extends TransactionStep {

	/**
	 * Creates update transaction step for a given ResourceProvider and DomainResource from request 
	 * @param provider the resource provider to use
	 * @param resource the domain resource provided in the request
	 */
	public UpdateTransactionStep(ResourceProvider<DomainResource, Model> provider, DomainResource resource) {
		this.provider = provider;
		this.resource = resource;
	}
	
	@Override
    public void init() throws AppException {
    	record = provider.fetchCurrent(resource.getIdElement());
    }
	
	@Override
	public void prepare() throws AppException { 				  
		((ReadWriteResourceProvider) provider).updatePrepare(record, resource);
		  if (resource.getMeta() == null || resource.getMeta().getVersionId() == null) throw new PreconditionFailedException("Resource version missing!");
		  if (!((ReadWriteResourceProvider) provider).getVersion(record).equals(resource.getMeta().getVersionId())) throw new ResourceVersionConflictException("Wrong resource version supplied!") ;
		
	}
	
	@Override
	public void execute() {
		
		try {
		if (result == null) {
			((ReadWriteResourceProvider) provider).updateExecute(record, resource);
			result = new BundleEntryComponent();
			BundleEntryResponseComponent response = new BundleEntryResponseComponent();
			response.setLastModified(((ReadWriteResourceProvider) provider).getLastUpdated(record));
			response.setStatus("200 OK");
			response.setLocation(FHIRServlet.getBaseUrl()+"/"+provider.getResourceType().getSimpleName()+"/"+record._id.toString()+"/_history/"+((ReadWriteResourceProvider) provider).getVersion(record));
			result.setResponse(response);
		}
		} catch (Exception e) {
			setResultBasedOnException(e);
		}
		
	}
		
}