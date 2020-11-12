/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.fhir_stu3.transactions;

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
import utils.fhir_stu3.ResourceProvider;

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
	public void execute() throws AppException {}
	
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
