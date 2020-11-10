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

package utils.fhir_stu3;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import org.hl7.fhir.dstu3.model.Composition;
import org.hl7.fhir.dstu3.model.Composition.CompositionAttesterComponent;
import org.hl7.fhir.dstu3.model.Composition.CompositionEventComponent;
import org.hl7.fhir.dstu3.model.Composition.SectionComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.CompositeAndListParam;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateOrListParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.QuantityAndListParam;
import ca.uhn.fhir.rest.param.QuantityParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import models.Record;
import models.RecordsInfo;
import models.enums.AggregationType;
import utils.access.RecordManager;
import utils.access.pseudo.FhirPseudonymizer;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class CompositionResourceProvider extends RecordBasedResourceProvider<Composition> implements IResourceProvider {

	// Provide one default constructor
	public CompositionResourceProvider() {
		
		// For each existing search parameter that has a "reference" type add one line:
		// searchParamNameToPathMap.put("Resource:search-name", "path from search specification");
		searchParamNameToPathMap.put("Composition:attester", "attester.party");
		searchParamNameToPathMap.put("Composition:author", "author");
		searchParamNameToPathMap.put("Composition:encounter", "encounter");
		searchParamNameToPathMap.put("Composition:entry", "section.entry");
		searchParamNameToPathMap.put("Composition:patient", "subject");
		searchParamNameToPathMap.put("Composition:related-ref", "relatesTo.targetReference");
		searchParamNameToPathMap.put("Composition:subject", "subject");
		
		// For each existing search parameter that has a "reference" type that cannot reference
		// to any resource add one line:
		// searchParamNameToTypeMap.put("Resource:search-name", Sets.create("TargetResourceTyp1", ...));			
		searchParamNameToTypeMap.put("Composition:attester", Sets.create("Practitioner","Organization", "Patient"));
		searchParamNameToTypeMap.put("Composition:author", Sets.create("Practitioner", "Device", "Patient", "RelatedPerson"));
		searchParamNameToTypeMap.put("Composition:encounter", Sets.create("Encounter"));
		searchParamNameToTypeMap.put("Composition:patient", Sets.create("Patient"));
		searchParamNameToTypeMap.put("Composition:related-ref", Sets.create("Composition"));
										
		// Use name of @Search function as last parameter
		registerSearches("Composition", getClass(), "getComposition");
		
		FhirPseudonymizer.forSTU3()
		  .reset("Composition")
		  .pseudonymizeReference("Composition", "attester", "party")
		  .pseudonymizeReference("Composition", "author");		
	}
	
	// Return corresponding FHIR class
	@Override
	public Class<Composition> getResourceType() {
		return Composition.class;
	}

	// Main search function for resource. May have another name.
	// Copy method signature from happy fhir implementation.
	// Look at http://hapifhir.io/xref-jpaserver/ca/uhn/fhir/jpa/rp/dstu3/ObservationResourceProvider.html
	// Throw out unsupported search parameters (like "_has" and all starting with "PARAM_" )
	// Replace DateRangeParam with DateAndListParam everywhere except for _lastUpdated	
	// Add non FHIR _page parameter used for the pagination mechanism
	@Search()
	public Bundle getComposition(
			@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,

			@Description(shortDefinition = "The resource language") @OptionalParam(name = "_language") StringAndListParam theResourceLanguage,

			@Description(shortDefinition="Who attested the composition")
  			@OptionalParam(name="attester", targetTypes={  } )
  			ReferenceAndListParam theAttester, 
    
  			@Description(shortDefinition="Who and/or what authored the composition")
  			@OptionalParam(name="author", targetTypes={  } )
  			ReferenceAndListParam theAuthor, 
    
  			@Description(shortDefinition="Categorization of Composition")
  			@OptionalParam(name="class")
  			TokenAndListParam theClass,
    
  			@Description(shortDefinition="As defined by affinity domain")
  			@OptionalParam(name="confidentiality")
  			TokenAndListParam theConfidentiality,
    
  			@Description(shortDefinition="Code(s) that apply to the event being documented")
  			@OptionalParam(name="context")
  			TokenAndListParam theContext,
    
  			@Description(shortDefinition="Composition editing time")
  			@OptionalParam(name="date")
  			DateAndListParam theDate, 
    
  			@Description(shortDefinition="Context of the Composition")
  			@OptionalParam(name="encounter", targetTypes={  } )
  			ReferenceAndListParam theEncounter, 
    
  			@Description(shortDefinition="A reference to data that supports this section")
  			@OptionalParam(name="entry", targetTypes={  } )
  			ReferenceAndListParam theEntry, 
    
 			@Description(shortDefinition="Logical identifier of composition (version-independent)")
 			@OptionalParam(name="identifier")
 			TokenAndListParam theIdentifier,
   
 			@Description(shortDefinition="Who and/or what the composition is about")
 			@OptionalParam(name="patient", targetTypes={  } )
 			ReferenceAndListParam thePatient, 
   
 			@Description(shortDefinition="The period covered by the documentation")
 			@OptionalParam(name="period")
 			DateAndListParam thePeriod, 
   
 			@Description(shortDefinition="Target of the relationship")
 			@OptionalParam(name="related-id")
 			TokenAndListParam theRelated_id,
   
 			@Description(shortDefinition="Target of the relationship")
 			@OptionalParam(name="related-ref", targetTypes={  } )
 			ReferenceAndListParam theRelated_ref, 
   
 			@Description(shortDefinition="Classification of section (recommended)")
 			@OptionalParam(name="section")
 			TokenAndListParam theSection,
   
 			@Description(shortDefinition="preliminary | final | amended | entered-in-error")
 			@OptionalParam(name="status")
 			TokenAndListParam theStatus,
   
 			@Description(shortDefinition="Who and/or what the composition is about")
 			@OptionalParam(name="subject", targetTypes={  } )
 			ReferenceAndListParam theSubject, 
   
 			@Description(shortDefinition="Human Readable name/title")
 			@OptionalParam(name="title")
 			StringAndListParam theTitle, 
   
 			@Description(shortDefinition="Kind of composition (LOINC if possible)")
 			@OptionalParam(name="type")
 			TokenAndListParam theType,
									
			@IncludeParam(reverse=true)
			Set<Include> theRevIncludes,
			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
			@OptionalParam(name="_lastUpdated")
			DateRangeParam theLastUpdated, 
			
			// This often needs to be cleaned up after copy/paste
			@IncludeParam(allow= {
					"Composition:attester",
					"Composition:author" ,
					"Composition:encounter" ,
					"Composition:entry" ,
					"Composition:patient" ,
					"Composition:related-ref" ,
					"Composition:subject" , 					
					"*"
			}) 
			Set<Include> theIncludes,
								
			@Sort SortSpec theSort,		
			
			@ca.uhn.fhir.rest.annotation.Count Integer theCount,
			
			// Non FHIR parameter used for pagination
			@OptionalParam(name="_page")
			StringParam _page,
			
			RequestDetails theDetails

	) throws AppException {

		// The implementation of this method may also be copied from happy fhir except for the last lines
		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", theId);
		paramMap.add("_language", theResourceLanguage);
			
		paramMap.add("attester", theAttester);
		paramMap.add("author", theAuthor);
		paramMap.add("class", theClass);
		paramMap.add("confidentiality", theConfidentiality);
		paramMap.add("context", theContext);
		paramMap.add("date", theDate);
		paramMap.add("encounter", theEncounter);
		paramMap.add("entry", theEntry);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("patient", thePatient);
		paramMap.add("period", thePeriod);
		paramMap.add("related-id", theRelated_id);
		paramMap.add("related-ref", theRelated_ref);
		paramMap.add("section", theSection);
		paramMap.add("status", theStatus);
		paramMap.add("subject", theSubject);
		paramMap.add("title", theTitle);
		paramMap.add("type", theType);
		
		paramMap.setRevIncludes(theRevIncludes);
		paramMap.setLastUpdated(theLastUpdated);
		paramMap.setIncludes(theIncludes);
		paramMap.setSort(theSort);
		paramMap.setCount(theCount);
		
		// The last lines are different than the happy fhir version
		paramMap.setFrom(_page != null ? _page.getValue() : null);
		return searchBundle(paramMap, theDetails);
		
	}

	// The actual search method implementation.
	// Basically this "maps" the FHIR query to a MIDATA query and executes it
	public List<Record> searchRaw(SearchParameterMap params) throws AppException {
		
		// get execution context (which user, which app)
		ExecutionInfo info = info();

		// construct empty query and a builder for that query
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Composition");

		// Now all possible searches need to be handeled. For performance reasons it makes sense
		// to put searches that are very restrictive and frequently used first in order
	
		// Add default handling for the _id search parameter
		builder.handleIdRestriction();
		
		// Add handling for search on the "owner" of the record. 
		builder.recordOwnerReference("patient", "Patient", "subject");
				
		// Add handling for search on the field that determines the MIDATA content type used.
        builder.recordCodeRestriction("type", "type");
		
        builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
        builder.restriction("date", true, QueryBuilder.TYPE_DATETIME, "date");	
        
        if (!builder.recordOwnerReference("subject", null, "subject")) builder.restriction("subject", true, null, "subject");
    	
        builder.restriction("title", true, QueryBuilder.TYPE_STRING, "title");
        
        builder.restriction("attester", true, null, "attester.party");
        builder.restriction("author", true, null, "author");	
        builder.restriction("class", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "class");	
        builder.restriction("confidentiality", true, QueryBuilder.TYPE_CODE, "confidentiality");	
        builder.restriction("context", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "event.code");
        
        builder.restriction("encounter", true, "Encounter", "encounter");
        builder.restriction("entry", true, null, "section.entry"); // MAY BE NESTED
        	
        builder.restriction("period", true, QueryBuilder.TYPE_PERIOD, "event.period");
        builder.restriction("related-id", true, QueryBuilder.TYPE_IDENTIFIER, "relatesTo.targetIdentifier");	
        builder.restriction("related-ref", true, null, "relatesTo.targetReference");
        builder.restriction("section", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "section.code");	
        builder.restriction("status", false, QueryBuilder.TYPE_CODE, "status");
        
        
		// At last execute the constructed query
		return query.execute(info);
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Composition theComposition) {
		return super.createResource(theComposition);
	}
		
	@Override
	public String getRecordFormat() {	
		return "fhir/Composition";
	}
		

	// This method is required if it is allowed to update the resource.
	// Just change the resource type
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Composition theComposition) {
		return super.updateResource(theId, theComposition);
	}		

	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical type "content" set. 
	// b) Each record must have a "name" that will be shown to the user in the record tree.
	//    The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the FHIR representation
	public void prepare(Record record, Composition theComposition) throws AppException {
		// Task a : Set Record "content" field by using a code from the resource (or a fixed value or something else useful)
		setRecordCodeByCodeableConcept(record, theComposition.getType(), null);
		
		record.name = theComposition.getTitle();
		if (record.name == null && theComposition.hasDate()) {
			try {
				record.name = FHIRTools.stringFromDateTime(theComposition.getDateElement());
			} catch (Exception e) {				
			}			
		}
		if (record.name == null) record.name = "Composition";

		// Task c : Set record owner based on subject and clean subject if this was possible
		Reference subjectRef = theComposition.getSubject();
		if (cleanAndSetRecordOwner(record, subjectRef)) theComposition.setSubject(null);
		
		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta" section
		clean(theComposition);
 
	}	
 
	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, Composition p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);
		
		// Add subject field from record owner field if it is not already there
		if (p.getSubject().isEmpty()) {			
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}
	
	
	@Operation(name="$document", idempotent=true)
	public Bundle document(				  
		 @IdParam IIdType theId,
		 @OperationParam(name="persist") TokenParam thePersist
	   ) throws AppException {
		Bundle retVal = new Bundle();
		retVal.setType(BundleType.DOCUMENT);
		retVal.getMeta().setLastUpdated(new Date());
		Composition composition = this.getResourceById(theId);
		
		addBundle(retVal, composition);
		
		if (composition.hasSubject()) {
		   addReference(retVal, composition.getSubject());	
		}
		
		if (composition.hasEncounter()) {
		   addReference(retVal, composition.getEncounter());
		}
		
		if (composition.hasAttester()) {
			for (CompositionAttesterComponent cac : composition.getAttester()) {
				addReference(retVal, cac.getParty());
			}
		}
		
		if (composition.hasCustodian()) {
			addReference(retVal, composition.getCustodian());
		}
		
		if (composition.hasEvent()) {
			for (CompositionEventComponent cec : composition.getEvent()) {
				addReferences(retVal, cec.getDetail());
			}
		}
		
		if (composition.hasSection()) {
			processSections(retVal, composition.getSection());			
		}
		
 	    return retVal;
	}
	
	private void processSections(Bundle retVal, List<SectionComponent> sections) throws AppException {
		for (SectionComponent component : sections) {
			List<Reference> refs = component.getEntry();
			addReferences(retVal, refs);
			if (component.hasSection()) {				
				processSections(retVal, component.getSection());
			}
		}
	}
	
	private void addReferences(Bundle retVal, List<Reference> refs) throws AppException {
		if (refs != null && !refs.isEmpty()) {
			for (Reference ref : refs) {
				addReference(retVal, ref);						   
			}
		}
	}
	
	private void addReference(Bundle retVal, Reference ref) throws AppException {
	    IIdType refElem = ref.getReferenceElement();
	    if (refElem != null) {
		   ResourceProvider prov = FHIRServlet.myProviders.get(refElem.getResourceType());
		   if (prov != null) {
			   IBaseResource result = prov.getResourceById(refElem);
			   addBundle(retVal, (Resource) result);
		   }
	    }
	}
	
	private void addBundle(Bundle retVal, Resource resource) {
		BundleEntryComponent entry = retVal.addEntry();
		entry.setResource(resource);
		entry.setFullUrl(FHIRServlet.getBaseUrl()+"/"+resource.getId());
	}
	

}