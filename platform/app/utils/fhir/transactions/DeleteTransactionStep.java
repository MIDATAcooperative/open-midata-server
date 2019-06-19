package utils.fhir.transactions;

import org.hl7.fhir.r4.model.DomainResource;

import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import models.Model;
import utils.fhir.ResourceProvider;

/**
 * Deleted a FHIR resource as part of a FHIR tranction or batch
 * NOT IMPLEMENTED YET
 */
public class DeleteTransactionStep extends TransactionStep {

	/**
	 * Creates a new delete transaction step for the given ResourceProvider and DomainResource from the user request
	 * @param provider the ResourceProvider to use
	 * @param resource the DomainResource from the user request
	 */
	public DeleteTransactionStep(ResourceProvider<DomainResource, Model> provider, DomainResource resource) {
		this.provider = provider;
		this.resource = resource;
	}
	
	@Override
    public void init() {
    	throw new NotImplementedOperationException("delete not implemented");    	
    }
	
	@Override
	public void prepare() { 
		throw new NotImplementedOperationException("delete not implemented");
	}
	
	@Override
	public void execute() {
		throw new NotImplementedOperationException("delete not implemented");
	}
	
}