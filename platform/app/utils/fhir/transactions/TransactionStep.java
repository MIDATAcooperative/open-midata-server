package utils.fhir.transactions;

import org.hl7.fhir.dstu3.model.BaseResource;

import models.Record;
import utils.fhir.ResourceProvider;

/**
 * One action during a FHIR transaction or batch
 *
 */
public class TransactionStep {

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
	
}
