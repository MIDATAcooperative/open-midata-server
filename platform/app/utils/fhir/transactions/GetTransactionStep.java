package utils.fhir.transactions;

import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;

import utils.fhir.ResourceProvider;

public class GetTransactionStep extends TransactionStep {

	
	public GetTransactionStep(ResourceProvider provider, BaseResource resource) {
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
		//provider.insertRecord(record, resource);
	}

}
