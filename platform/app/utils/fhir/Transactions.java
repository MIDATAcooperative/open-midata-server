package utils.fhir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import org.hl7.fhir.dstu3.model.Bundle.HTTPVerb;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.rest.annotation.Transaction;
import ca.uhn.fhir.rest.annotation.TransactionParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.util.FhirTerser;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.fhir.transactions.CreateTransactionStep;
import utils.fhir.transactions.TransactionStep;
import utils.fhir.transactions.UpdateTransactionStep;

public class Transactions {

	@Transaction
	public Bundle transaction(@TransactionParam Bundle theInput) {
		
	   try {
	   BundleType type = theInput.getType();
	   if (type == null) throw new UnprocessableEntityException("No type given for Bundle!");
	   if (! (type.equals(BundleType.BATCH) || type.equals(BundleType.TRANSACTION))) {
		   throw new UnprocessableEntityException("Unknown transaction type");
	   }
	   
	   List<TransactionStep> steps = new ArrayList<TransactionStep>();
	   for (BundleEntryComponent nextEntry : theInput.getEntry()) {
	      
		   BundleEntryRequestComponent req = nextEntry.getRequest();
		   if (req == null) throw new UnprocessableEntityException("No request in bundle");
		   
		   HTTPVerb verb = req.getMethod();
		   if (verb == null) throw new UnprocessableEntityException("Missing HTTP Verb in transaction");
		   if (verb.equals(HTTPVerb.POST)) {		   
		     Resource res = nextEntry.getResource();
		     ResourceProvider provider = FHIRServlet.myProviders.get(res.getResourceType().name());
		     if (provider == null) throw new UnprocessableEntityException("Resource Type not supported: "+res.getResourceType().name());
		     steps.add(new CreateTransactionStep(provider, (DomainResource) res));
		   } else if (verb.equals(HTTPVerb.PUT)) {
			 Resource res = nextEntry.getResource();
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
	   	  
	   
	   if (type.equals(BundleType.TRANSACTION)) {
		   
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
		     for (TransactionStep step : steps) step.execute();
		   }
	   } else {
		   for (TransactionStep step : steps) step.init();
		   resolveReferences(steps);
		   for (TransactionStep step : steps) {
			   try {				 				
			     step.prepare();
			     step.execute();
			   } catch (BaseServerResponseException e) {
				  step.setResultBasedOnException(e);			
			   } catch (AppException e2) {
				  step.setResultBasedOnException(e2);
			   }
		   }
	   }
	   	   
	   Bundle retVal = new Bundle();		 
	   for (TransactionStep step : steps) retVal.addEntry(step.getResult());	   
	   return retVal;
	   } catch (BaseServerResponseException e2) {
		   throw e2;
	   } catch (Exception e) {
		   ErrorReporter.report("FHIR Transaction", null, e);
		   throw new InternalErrorException(e);
		   
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
	   
	  