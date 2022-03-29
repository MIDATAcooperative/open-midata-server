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

package utils.fhir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.rest.annotation.Transaction;
import ca.uhn.fhir.rest.annotation.TransactionParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.util.FhirTerser;
import models.MidataId;
import models.Model;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.PluginException;
import utils.fhir.transactions.CreateTransactionStep;
import utils.fhir.transactions.TransactionStep;
import utils.fhir.transactions.UpdateTransactionStep;

public class Transactions {

	@Transaction
	public Bundle transaction(@TransactionParam Bundle theInput) {
		
	   try {
	   BundleType type = theInput.getType();
	   if (type == null) throw new UnprocessableEntityException("No type given for Bundle!");
	   if (! (type.equals(BundleType.BATCH) || type.equals(BundleType.TRANSACTION) || type.equals(BundleType.DOCUMENT))) {
		   throw new UnprocessableEntityException("Unknown transaction type");
	   }
	   boolean isDocument = type.equals(BundleType.DOCUMENT); 
	   
	   List<TransactionStep> steps = new ArrayList<TransactionStep>();
	   for (BundleEntryComponent nextEntry : theInput.getEntry()) {
	      
		   BundleEntryRequestComponent req = nextEntry.getRequest();
		   HTTPVerb verb = null;
		   
		   if (req == null || isDocument) {
			   if (isDocument) {
				 Resource res = nextEntry.getResource();				 
				 if (res == null || res.getResourceType() == null || res.getResourceType().name() == null) throw new UnprocessableEntityException("Missing Resource Type inside Bundle entry");
				 ResourceProvider provider = FHIRServlet.myProviders.get(res.getResourceType().name());
				 if (provider == null) throw new UnprocessableEntityException("Resource Type not supported: "+res.getResourceType().name());
				 String id = res.getId();
				 Model existing = null;
				 if (id != null && MidataId.isValid(id)) {
					 try {
					   existing = provider.fetchCurrent(res.getIdElement());
					 } catch (AppException e) {
					   existing = null;
					 }					 
				 }
				 if (existing != null) {
					 steps.add(new UpdateTransactionStep(provider, (DomainResource) res, existing));
				 } else {
					 steps.add(new CreateTransactionStep(provider, (DomainResource) res));
				 }				   
			   } else throw new UnprocessableEntityException("No request in bundle");
		   } else {
			   verb = req.getMethod();
		   
			   if (verb == null) throw new UnprocessableEntityException("Missing HTTP Verb in transaction");
			   if (verb.equals(HTTPVerb.POST)) {		   
			     Resource res = nextEntry.getResource();
			     if (res == null || res.getResourceType() == null || res.getResourceType().name() == null) throw new UnprocessableEntityException("Missing Resource Type inside Bundle entry");
			     ResourceProvider provider = FHIRServlet.myProviders.get(res.getResourceType().name());
			     if (provider == null) throw new UnprocessableEntityException("Resource Type not supported: "+res.getResourceType().name());
			     steps.add(new CreateTransactionStep(provider, (DomainResource) res));
			   } else if (verb.equals(HTTPVerb.PUT)) {
				 Resource res = nextEntry.getResource();
				 if (res == null || res.getResourceType() == null || res.getResourceType().name() == null) throw new UnprocessableEntityException("Missing Resource Type inside Bundle entry");
				 ResourceProvider provider = FHIRServlet.myProviders.get(res.getResourceType().name());
				 if (provider == null) throw new UnprocessableEntityException("Resource Type not supported: "+res.getResourceType().name());
				 steps.add(new UpdateTransactionStep(provider, (DomainResource) res));
			   } else if (verb.equals(HTTPVerb.DELETE)) {
				   throw new NotImplementedOperationException("Currently no support for DELETE");
			   } else if (verb.equals(HTTPVerb.GET)) {
				   throw new NotImplementedOperationException("Currently no support for GET");
			   } else {
				   throw new UnprocessableEntityException("Unknown HTTP Verb in transaction");
			   }
		   }
		   
	   }
	   	  
	   AccessContext inf = ResourceProvider.info();
	   inf.getRequestCache().getStudyPublishBuffer().setLazy(true);
	   
	   try {
		   if (isDocument || type.equals(BundleType.TRANSACTION)) {
			   
			   for (TransactionStep step : steps) step.init();
			   resolveReferences(steps);
			   boolean failed = false;
			   
			   for (TransactionStep step : steps) {
				   try {
				     step.prepare();
				   } catch (BaseServerResponseException e) {
					 failed = true;
					 step.setResultBasedOnException(e);					 
					 throw e;
				   } catch (BadRequestException e1) {
					  failed = true;
					 step.setResultBasedOnException(e1);
					 throw new UnprocessableEntityException(e1.getMessage());
				   } catch (AppException e2) {					   
					 failed = true;
					  step.setResultBasedOnException(e2);
					 throw e2;
				   } catch (NullPointerException e3) {
					  failed = true;
					  step.setResultBasedOnException(e3);
					 throw e3;
				   }
			   }		   
			   if (!failed) {
			     for (TransactionStep step : steps) {
			    	 try {
			    	 step.execute();
			    	 } catch (Exception e) {
			    		 if (!failed && e instanceof PluginException) {
			    			 ErrorReporter.reportPluginProblem("FHIR Transaction", null, (PluginException) e); 
			    		 }
			    		 step.setResultBasedOnException(e);
			    		 failed = true;
			    	 }
			     }
			   }
		   } else {
			   boolean failed = false;
			   for (TransactionStep step : steps) step.init();
			   resolveReferences(steps);
			   for (TransactionStep step : steps) {
				   try {				 				
				     step.prepare();
				     step.execute();
				   } catch (BaseServerResponseException e) {
					  step.setResultBasedOnException(e);			
				   } catch (AppException e2) {
					   if (!failed && e2 instanceof PluginException) {
						   ErrorReporter.reportPluginProblem("FHIR Batch", null, (PluginException) e2);
					   }
					  step.setResultBasedOnException(e2);
					  failed = true;
				   }
			   }
		   }
	   } finally {
		   inf.getRequestCache().getStudyPublishBuffer().save();
	   }
	   	   
	   Bundle retVal = new Bundle();		 
	   for (TransactionStep step : steps) retVal.addEntry(step.getResult());	   
	   return retVal;
	   } catch (BaseServerResponseException e2) {
		   throw e2;
	   } catch (PluginException e3) {
		   ErrorReporter.reportPluginProblem("FHIR Transaction", null, e3);
		   throw new InternalErrorException(e3.getMessage());
	   } catch (Exception e) {
		   ErrorReporter.report("FHIR Transaction", null, e);
		   throw new InternalErrorException(e.getMessage());
		   
	   }
	}
	
	
	public void resolveReferences(List<TransactionStep> steps) {
	   
	   FhirTerser terser = ResourceProvider.ctx().newTerser();
	   Map<String, IdType> idSubstitutions = new HashMap<String, IdType>();

	   for (TransactionStep step : steps) {
		   IIdType source = step.getResource().getIdElement();
		   if (source != null && source.getIdPart() != null) {
			 IdType t = new IdType(source.getResourceType(), step.getRecord()._id.toString());
			 AccessLog.log("reg ref:"+source.getIdPart()+" -> "+t.toString());
		     idSubstitutions.put(source.getIdPart(), t);
		   }
	   }
	   
	   for (TransactionStep step : steps) {
		   resolveReferences(idSubstitutions, step.getResource(), terser);		   
	   }
	}
	
	public void resolveReferences(Map<String, IdType> idSubstitutions, IBaseResource resource, FhirTerser terser) {
		List<IBaseReference> allRefs = terser.getAllPopulatedChildElementsOfType(resource, IBaseReference.class);
		for (IBaseReference nextRef : allRefs) {
			AccessLog.log("checkref:"+nextRef.toString());
			IIdType nextId = nextRef.getReferenceElement();
			if (!nextId.hasIdPart()) {
				continue;
			}
			if (idSubstitutions.containsKey(nextId.getIdPart())) {
				IdType newId = idSubstitutions.get(nextId.getIdPart());
				AccessLog.log("set ref:"+nextRef.toString()+" -> "+newId.toString());
				nextRef.setResource(null);
				nextRef.setReference(newId.getValue());
			} 
		}
	}
}
	   
	  