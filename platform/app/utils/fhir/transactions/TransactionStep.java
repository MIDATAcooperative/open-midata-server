/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.fhir.transactions;

import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import models.Model;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
import utils.fhir.ResourceProvider;
import utils.stats.Stats;

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
	
	public IdType getReferenceId() {
	  return new IdType(getResource().fhirType(), provider.getIdForReference(getRecord()));
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
		AccessLog.log("SET RESULT BASED ON EXCEPTION:"+e.getClass().getName());
		BundleEntryResponseComponent response = new BundleEntryResponseComponent();		
		
		if (e instanceof BaseServerResponseException) {
			BaseServerResponseException e2 = (BaseServerResponseException) e;
		    response.setStatus(""+e2.getStatusCode());		
		    IBaseOperationOutcome out = e2.getOperationOutcome();
		    if (out != null) { response.setOutcome((Resource) out); }
		    else response.setOutcome(outcomeFromException(e));
		    /*else {
		    	Throwable cause = e2.getCause();
		    	if (cause != null && cause instanceof Exception) response.setOutcome(outcomeFrom)
		    }*/
		    Stats.addComment("Transaction-Error: "+e2.getStatusCode()+" "+e2.getMessage());
		} else if (e instanceof BadRequestException) {
			response.setStatus("400 "+e.getMessage());
			response.setOutcome(outcomeFromException(e));
			Stats.addComment("Transaction Bad Request: "+e.getMessage());
		} else if (e instanceof InternalServerException) {
			ErrorReporter.report("FHIR (transaction)", null, e);	
			response.setStatus("500 "+e.getMessage());	
			response.setOutcome(outcomeFromException(e));
		} else {
			ErrorReporter.report("FHIR (transaction)", null, e);
			response.setStatus("500 "+e.getMessage());
			response.setOutcome(outcomeFromException(e));
		}
		result.setResponse(response);
	}
	
	public static OperationOutcome outcomeFromException(Exception e) {
		OperationOutcome result = new OperationOutcome();
		if (e instanceof PluginException) {		
			result.addIssue()
			.setCode(IssueType.INVALID)
			.setSeverity(IssueSeverity.ERROR)
			.setDetails(new CodeableConcept().setText(e.getMessage()));
		} else if (e instanceof InternalServerException) {
			result.addIssue()
			.setCode(IssueType.INVALID)
			.setSeverity(IssueSeverity.FATAL)
			.setDetails(new CodeableConcept().setText(e.getMessage()));
		} else if (e instanceof InternalErrorException) {
			result.addIssue()
			.setCode(IssueType.INVALID)
			.setSeverity(IssueSeverity.ERROR)
			.setDetails(new CodeableConcept().setText(e.getMessage()));
		} else if (e instanceof BadRequestException) {
			result.addIssue()
			.setCode(IssueType.INVALID)
			.setSeverity(IssueSeverity.ERROR)
			.setDetails(new CodeableConcept().setText(e.getMessage()));
		} else if (e instanceof AuthException) {
			result.addIssue()
			.setCode(IssueType.FORBIDDEN)
			.setSeverity(IssueSeverity.ERROR)
			.setDetails(new CodeableConcept().setText("Authentication Problem: "+e.getMessage()));
		} else {
			result.addIssue()
			.setCode(IssueType.EXCEPTION)
			.setSeverity(IssueSeverity.FATAL)
			.setDetails(new CodeableConcept().setText(e.getClass().getName()+" "+e.getMessage()));
		}
		return result;
	}
	
}
