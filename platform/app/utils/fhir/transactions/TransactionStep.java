package utils.fhir.transactions;

import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.dstu3.model.Resource;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import models.Record;
import utils.fhir.FHIRServlet;
import utils.fhir.ResourceProvider;

/**
 * One action during a FHIR transaction or batch
 *
 */
public abstract class TransactionStep {

	/**
	 * the resource that shall be worked with
	 */
	protected BaseResource resource;
	
	/**
	 * the record that shall be worked with
	 */
	protected Record record;
	
	/**
	 * the resource provider belonging to the resource
	 */
	protected ResourceProvider<BaseResource> provider;
	
	/**
	 * The response created by this transaction step
	 */
	protected BundleEntryComponent result;
	
	/**
	 * returns the resource belonging to this action
	 * @return FHIR resource
	 */
	public BaseResource getResource() {
		return resource;
	}

	/**
	 * returns the record belonging to this action
	 * @return record
	 */
	public Record getRecord() {
		return record;
	}

	/**
	 * initialize this action
	 */
	public void init() {}
	
	/**
	 * prepare this action for execution
	 */
	public void prepare() {}
	
	/**
	 * execute this action
	 */
	public void execute() {}
	
	/**
	 * Returns the response to this transaction step
	 * @return response as BundleEntryComponent
	 */
	public BundleEntryComponent getResult() {
		return result;
	}
	
	/**
	 * Convert an store an exception from HAPI FHIR into a bundle entry component to be returned for transactions/batches
	 * @param e
	 */
	public void setResultBasedOnException(BaseServerResponseException e) {
		result = new BundleEntryComponent();
		BundleEntryResponseComponent response = new BundleEntryResponseComponent();		
		response.setStatus(""+e.getStatusCode());		
		response.setOutcome((Resource) e.getOperationOutcome());
		result.setResponse(response);
	}
	
}
