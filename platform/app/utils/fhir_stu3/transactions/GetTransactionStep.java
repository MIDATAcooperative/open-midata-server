package utils.fhir_stu3.transactions;

import org.hl7.fhir.dstu3.model.DomainResource;

import utils.fhir_stu3.ResourceProvider;

public class GetTransactionStep extends TransactionStep {

	
	public GetTransactionStep(ResourceProvider provider, DomainResource resource) {
		this.provider = provider;
		this.resource = resource;
	}
	
    public void init() {
    	//record = provider.init();
    } 
	
	public void prepare() { 
		/*try {
		  provider.prepare(record, resource);
		} catch (Exception e) {
		  setResultBasedOnException(e);
		}*/
	}
	
	public void execute() {
		//provider.insertRecord(record, resource);
	}

}