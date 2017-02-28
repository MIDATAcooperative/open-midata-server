package utils.fhir.transactions;

import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.DomainResource;

import utils.fhir.ResourceProvider;

public class DeleteTransactionStep extends TransactionStep {

	
	public DeleteTransactionStep(ResourceProvider provider, DomainResource resource) {
		this.provider = provider;
		this.resource = resource;
	}
	
    public void init() {
    	record = ResourceProvider.fetchCurrent(resource.getIdElement());
    }
	
	public void prepare() { 
		//provider.prepare(record, resource); 
	}
	
	public void execute() {
		//provider.updateRecord(record, resource);
	}
	
}