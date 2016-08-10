package utils.fhir.transactions;

import models.Record;

import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.instance.model.api.IBaseResource;


import utils.fhir.ResourceProvider;

public class TransactionStep {

	protected BaseResource resource;
	protected Record record;
	protected ResourceProvider provider;
	
	
	public BaseResource getResource() {
		return resource;
	}

	public Record getRecord() {
		return record;
	}

	public void init() {}
	
	public void prepare() {}
	
	public void execute() {}
	
}
