package utils.fhir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import org.hl7.fhir.dstu3.model.Bundle.HTTPVerb;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.rest.annotation.Transaction;
import ca.uhn.fhir.rest.annotation.TransactionParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.util.FhirTerser;
import utils.ErrorReporter;
import utils.fhir.transactions.CreateTransactionStep;
import utils.fhir.transactions.TransactionStep;
import utils.fhir.transactions.UpdateTransactionStep;

public class Transactions {

	@Transaction
	public Bundle transaction(@TransactionParam Bundle theInput) {
		
	   try {
	   BundleType type = theInput.getType();
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
		     steps.add(new CreateTransactionStep(provider, (BaseResource) res));
		   } else if (verb.equals(HTTPVerb.PUT)) {
			 Resource res = nextEntry.getResource();
			 ResourceProvider provider = FHIRServlet.myProviders.get(res.getResourceType().name());		   
			 steps.add(new UpdateTransactionStep(provider, (BaseResource) res));
		   } else if (verb.equals(HTTPVerb.DELETE)) {
			   throw new NotImplementedOperationException("Currently no support for DELETE");
		   } else if (verb.equals(HTTPVerb.GET)) {
			   throw new NotImplementedOperationException("Currently no support for GET");
		   } else {
			   throw new UnprocessableEntityException("Unknown HTTP Verb in transaction");
		   }
		   
	   }
	   
	   /*
	   BundleEntryComponent cmp = new BundleEntryComponent();
	   BundleEntryResponseComponent res = new BundleEntryResponseComponent();	   
	   cmp.setResponse(res);
	   */
	   
	   if (type.equals(BundleType.TRANSACTION)) {
		   for (TransactionStep step : steps) step.init();
		   resolveReferences(steps);
		   for (TransactionStep step : steps) step.prepare();
		   for (TransactionStep step : steps) step.execute();
	   } else {
		   for (TransactionStep step : steps) step.init();
		   resolveReferences(steps);
		   for (TransactionStep step : steps) {
			   step.prepare();
			   step.execute();			   
		   }
	   }
	   	   
	   Bundle retVal = new Bundle();
	   // Populate return bundle
	   //retVal.addEntry(t)
	   return retVal;
	   
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
			IIdType nextId = nextRef.getReferenceElement();
			if (!nextId.hasIdPart()) {
				continue;
			}
			if (idSubstitutions.containsKey(nextId)) {
				IdType newId = idSubstitutions.get(nextId.getIdPart());				
				nextRef.setReference(newId.getValue());
			} 
		}
	}
}
	   
	  