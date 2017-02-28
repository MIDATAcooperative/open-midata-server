package utils.fhir.transactions;

import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.dstu3.model.DomainResource;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionNotSpecifiedException;
import utils.exceptions.AppException;
import utils.fhir.FHIRServlet;
import utils.fhir.ResourceProvider;

public class UpdateTransactionStep extends TransactionStep {

	
	
	public UpdateTransactionStep(ResourceProvider provider, DomainResource resource) {
		this.provider = provider;
		this.resource = resource;
	}
	
    public void init() {
    	record = ResourceProvider.fetchCurrent(resource.getIdElement());
    }
	
	public void prepare() throws AppException { 				  
		  provider.prepare(record, resource);
		  if (resource.getMeta() == null || resource.getMeta().getVersionId() == null) throw new PreconditionFailedException("Resource version missing!");
		  if (!record.version.equals(resource.getMeta().getVersionId())) throw new ResourceVersionConflictException("Wrong resource version supplied!") ;
		
	}
	
	public void execute() {
		
		try {
		if (result == null) {
			provider.updateRecord(record, resource);
			result = new BundleEntryComponent();
			BundleEntryResponseComponent response = new BundleEntryResponseComponent();
			response.setLastModified(record.lastUpdated);
			response.setStatus("200 OK");
			response.setLocation(FHIRServlet.getBaseUrl()+"/"+provider.getResourceType().getSimpleName()+"/"+record._id.toString()+"/_history/"+record.version);
			result.setResponse(response);
		}
		} catch (Exception e) {
			setResultBasedOnException(e);
		}
		
	}
		
}