package utils.fhir.transactions;

import org.hl7.fhir.dstu3.model.BaseResource;

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
		provider.updateRecord(record, resource);
	}
}