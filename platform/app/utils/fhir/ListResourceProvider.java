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

import java.util.List;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Reference;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import models.Record;
import utils.access.pseudo.FhirPseudonymizer;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;

public class ListResourceProvider extends RecordBasedResourceProvider<ListResource> implements IResourceProvider {

	// Provide one default constructor
	public ListResourceProvider() {
				
		searchParamNameToPathMap.put("List:encounter", "encounter");
		searchParamNameToPathMap.put("List:item", "entry.item");
		searchParamNameToPathMap.put("List:patient", "subject");
		searchParamNameToPathMap.put("List:subject", "subject");
		searchParamNameToPathMap.put("List:source", "source");
				
		// For each existing search parameter that has a "reference" type that cannot reference
		// to any resource add one line:
		// searchParamNameToTypeMap.put("Resource:search-name", Sets.create("TargetResourceTyp1", ...));
		
		searchParamNameToTypeMap.put("List:encounter", Sets.create("Encounter"));		
		searchParamNameToTypeMap.put("List:patient", Sets.create("Patient"));
		searchParamNameToTypeMap.put("List:subject", Sets.create("Patient","Group", "Device", "Location"));
		searchParamNameToTypeMap.put("List:source", Sets.create("Practitioner", "Device", "Patient", "PractitionerRole" ));
		
		// Use name of @Search function as last parameter
		registerSearches("List", getClass(), "getListResource");
		
		FhirPseudonymizer.forR4()
		  .reset("List")
		  .hideIfPseudonymized("List", "text")
		  .pseudonymizeReference("List", "source")
		  .pseudonymizeReference("List", "entry", "item")
		  .pseudonymizeReference("List", "note", "authorReference");
	}
	
	// Return corresponding FHIR class
	@Override
	public Class<ListResource> getResourceType() {
		return ListResource.class;
	}

	// Main search function for resource. May have another name.
	// Copy method signature from happy fhir implementation.
	// Look at http://hapifhir.io/xref-jpaserver/ca/uhn/fhir/jpa/rp/dstu3/ObservationResourceProvider.html
	// Throw out unsupported search parameters (like "_has" and all starting with "PARAM_" )
	// Replace DateRangeParam with DateAndListParam everywhere except for _lastUpdated	
	// Add non FHIR _page parameter used for the pagination mechanism
	@Search()
	public Bundle getListResource(
			@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,
		
			@Description(shortDefinition="What the purpose of this list is")
  			@OptionalParam(name="code")
  			TokenAndListParam theCode,
			
  			@Description(shortDefinition="When the list was prepared")
 			@OptionalParam(name="date")
			DateAndListParam theDate, 
			
			@Description(shortDefinition="Why list is empty")
  			@OptionalParam(name="empty-reason")
  			TokenAndListParam theEmptyReason,
  			
  			@Description(shortDefinition="Context in which list was created")
 			@OptionalParam(name="encounter", targetTypes={  } )
 			ReferenceAndListParam theEncounter, 
			
 			@Description(shortDefinition="Business identifier")
 			@OptionalParam(name="identifier")
 			TokenAndListParam theIdentifier,
 			
 			@Description(shortDefinition="Actual entry")
 			@OptionalParam(name="item", targetTypes={  } )
 			ReferenceAndListParam theItem, 
				
 			@Description(shortDefinition="The annotation - text content (as markdown)")
 			@OptionalParam(name="notes")
 			StringAndListParam theNotes, 
				
 			@Description(shortDefinition="If all resources have the same subject")
 			@OptionalParam(name="patient", targetTypes={  } )
 			ReferenceAndListParam thePatient, 
			
 			@Description(shortDefinition="Who and/or what defined the list contents (aka Author)")
 			@OptionalParam(name="source", targetTypes={  } )
 			ReferenceAndListParam theSource, 
			
 			@Description(shortDefinition="current | retired | entered-in-error")
 			@OptionalParam(name="status")
 			TokenAndListParam theStatus,
			
 			@Description(shortDefinition="If all resources have the same subject")
 			@OptionalParam(name="subject", targetTypes={  } )
 			ReferenceAndListParam theSubject, 
			
 			@Description(shortDefinition="Descriptive name for the list")
 			@OptionalParam(name="title")
 			StringAndListParam theTitle, 
				
 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= {
 					"List:encounter" ,
 					"List:item" ,
 					"List:patient" ,
 					"List:source" ,
 					"List:subject" , 					 
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
	
	    paramMap.add("code", theCode);
		paramMap.add("date", theDate);
		paramMap.add("empty-reaseon", theEmptyReason);
		paramMap.add("encounter", theEncounter);	
		paramMap.add("identifier", theIdentifier);
		paramMap.add("item", theItem);
		paramMap.add("notes", theNotes);	
		paramMap.add("patient", thePatient);	
		paramMap.add("source", theSource);
		paramMap.add("status", theStatus);
		paramMap.add("subject", theSubject);
		paramMap.add("title", theTitle);		
				
		paramMap.setRevIncludes(theRevIncludes);
		paramMap.setLastUpdated(theLastUpdated);
		paramMap.setIncludes(theIncludes);
		paramMap.setSort(theSort);
		paramMap.setCount(theCount);
		
		// The last lines are different than the happy fhir version
		paramMap.setFrom(_page != null ? _page.getValue() : null);
		return searchBundle(paramMap, theDetails);
		
	}
	
	public Query buildQuery(SearchParameterMap params) throws AppException {
		
		info();

		// construct empty query and a builder for that query
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/List");

		// Now all possible searches need to be handeled. For performance reasons it makes sense
		// to put searches that are very restrictive and frequently used first in order
	
		// Add default handling for the _id search parameter
		builder.handleIdRestriction();
		
		// Add handling for search on the "owner" of the record. 
		builder.recordOwnerReference("patient", "Patient", "subject");
		if (!builder.recordOwnerReference("subject", null, "subject")) builder.restriction("subject", true, null, "subject");
				
		// Add handling for search on the field that determines the MIDATA content type used.
        builder.recordCodeRestriction("code", "code");
		
        builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
        
        builder.restriction("date", true, QueryBuilder.TYPE_DATETIME, "date");
        builder.restriction("title", true, QueryBuilder.TYPE_STRING, "title");
        builder.restriction("encounter", true, null, "encounter");
        builder.restriction("source", true, null, "source");
        builder.restriction("empty-reason", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "emptyReason");                
        builder.restriction("item", true, null, "entry.item");
        builder.restriction("notes", true, QueryBuilder.TYPE_MARKDOWN, "note.text");        
        builder.restriction("status", true, QueryBuilder.TYPE_CODE, "status");	                                
        			
		// At last execute the constructed query
		return query;
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam ListResource theListResource) {
		return super.createResource(theListResource);
	}
		
	@Override
	public String getRecordFormat() {	
		return "fhir/List";
	}
		

	// This method is required if it is allowed to update the resource.
	// Just change the resource type
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam ListResource theListResource) {
		return super.updateResource(theId, theListResource);
	}		

	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical type "content" set. 
	// b) Each record must have a "name" that will be shown to the user in the record tree.
	//    The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the FHIR representation
	public void prepare(Record record, ListResource theListResource) throws AppException {
		// Task a : Set Record "content" field by using a code from the resource (or a fixed value or something else useful)
		String display = setRecordCodeByCodeableConcept(record, theListResource.getCode(), "List");
		
		// Task b : Create record name
		String date = "No time";		
		if (theListResource.hasDate()) {
			try {
				date = FHIRTools.stringFromDateTime(theListResource.getDateElement());
			} catch (Exception e) {
				throw new UnprocessableEntityException("Cannot process date");
			}
		}
		record.name = display != null ? (display + " / " + date) : date;		

		// Task c : Set record owner based on subject and clean subject if this was possible
		Reference subjectRef = theListResource.getSubject();
		if (cleanAndSetRecordOwner(record, subjectRef)) {
			theListResource.setSubject(null);
		}
		
		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta" section
		clean(theListResource);
 
	}	
 
	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, ListResource p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);
		
		// Add subject field from record owner field if it is not already there		
		if (p.getSubject().isEmpty()) {			
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}
		

	@Override
	protected void convertToR4(Object in) {
		
	}
	

}