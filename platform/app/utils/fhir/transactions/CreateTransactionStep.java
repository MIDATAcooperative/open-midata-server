package utils.fhir.transactions;

import org.hl7.fhir.dstu3.model.BaseResource;


import utils.fhir.ResourceProvider;

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
		provider.insertRecord(record, resource);
	}
}