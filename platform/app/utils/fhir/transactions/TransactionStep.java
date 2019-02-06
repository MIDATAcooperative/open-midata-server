package utils.fhir.transactions;

import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Resource;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import models.Model;
import utils.ErrorReporter;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.fhir.ResourceProvider;

/**
 * One action during a FHIR transaction or batch
 *
 */
public abstract class TransactionStep {

	/**
	 * the resource that shall be worked with
	 */
	protected DomainResource resource;
	
	/**
	 * the record that shall be worked with
	 */
	protected Model record;
	
	/**
	 * the resource provider belonging to the resource
	 */
	protected ResourceProvider<DomainResource, Model> provider;
	
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
	public Model getRecord() {
		return record;
	}

	/**
	 * initialize this action
	 */
	public void init() throws AppException {}
	
	/**
	 * prepare this action for execution
	 * @throws AppException
	 */
	public void prepare() throws AppException {}
	
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
	 * Convert and store an exception from HAPI FHIR into a bundle entry component to be returned for transactions/batches
	 * @param e
	 */
	public void setResultBasedOnException(Exception e) {
		result = new BundleEntryComponent();
		BundleEntryResponseComponent response = new BundleEntryResponseComponent();		
		
		if (e instanceof BaseServerResponseException) {
			BaseServerResponseException e2 = (BaseServerResponseException) e;
		    response.setStatus(""+e2.getStatusCode());		
		    response.setOutcome((Resource) e2.getOperationOutcome());
		} else if (e instanceof BadRequestException) {
			response.setStatus("400 "+e.getMessage());				    
		} else if (e instanceof InternalServerException) {
			ErrorReporter.report("FHIR (transaction)", null, e);	
			response.setStatus("500 "+e.getMessage());			
		} else {
			ErrorReporter.report("FHIR (transaction)", null, e);
			response.setStatus("500 "+e.getMessage());
		}
		result.setResponse(response);
	}
	
}
