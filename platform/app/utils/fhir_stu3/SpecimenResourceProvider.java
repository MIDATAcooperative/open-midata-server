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
import org.hl7.fhir.dstu3.model.Specimen;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;

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
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class SpecimenResourceProvider extends RecordBasedResourceProvider<Specimen> implements IResourceProvider {

	// Provide one default constructor
	public SpecimenResourceProvider() {
		
		// For each existing search parameter that has a "reference" type add one line:
		// searchParamNameToPathMap.put("Resource:search-name", "path from search specification");
		searchParamNameToPathMap.put("Specimen:collector", "collector.collector");
		searchParamNameToPathMap.put("Specimen:parent", "parent");		
		searchParamNameToPathMap.put("Specimen:patient", "subject");
		searchParamNameToPathMap.put("Specimen:subject", "subject");
		
		// For each existing search parameter that has a "reference" type that cannot reference
		// to any resource add one line:
		// searchParamNameToTypeMap.put("Resource:search-name", Sets.create("TargetResourceTyp1", ...));			
		searchParamNameToTypeMap.put("Specimen:collector", Sets.create("Practitioner"));				
		searchParamNameToTypeMap.put("Specimen:parent", Sets.create("Specimen"));
		searchParamNameToTypeMap.put("Specimen:patient", Sets.create("Patient"));
		searchParamNameToTypeMap.put("Specimen:subject", Sets.create("Group", "Device", "Patient", "Substance"));
								
		// Use name of @Search function as last parameter
		registerSearches("Specimen", getClass(), "getSpecimen");
	}
	
	// Return corresponding FHIR class
	@Override
	public Class<Specimen> getResourceType() {
		return Specimen.class;
	}

	// Main search function for resource. May have another name.
	// Copy method signature from happy fhir implementation.
	// Look at http://hapifhir.io/xref-jpaserver/ca/uhn/fhir/jpa/rp/dstu3/ObservationResourceProvider.html
	// Throw out unsupported search parameters (like "_has" and all starting with "PARAM_" )
	// Replace DateRangeParam with DateAndListParam everywhere except for _lastUpdated	
	// Add non FHIR _page parameter used for the pagination mechanism
	@Search()
	public Bundle getSpecimen(
			@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,

			@Description(shortDefinition = "The resource language") @OptionalParam(name = "_language") StringAndListParam theResourceLanguage,

		
			@Description(shortDefinition="The ID of the resource")
  			@OptionalParam(name="_id")
  			TokenAndListParam the_id,
    
  			@Description(shortDefinition="The language of the resource")
  			@OptionalParam(name="_language")
  			StringAndListParam the_language, 
    
  			@Description(shortDefinition="The accession number associated with the specimen")
  			@OptionalParam(name="accession")
  			TokenAndListParam theAccession,
    
  			@Description(shortDefinition="The code for the body site from where the specimen originated")
  			@OptionalParam(name="bodysite")
  			TokenAndListParam theBodysite,
    
  			@Description(shortDefinition="The date the specimen was collected")
  			@OptionalParam(name="collected")
  			DateAndListParam theCollected, 
    
  			@Description(shortDefinition="Who collected the specimen")
  			@OptionalParam(name="collector", targetTypes={  } )
  			ReferenceAndListParam theCollector, 
    
  			@Description(shortDefinition="The kind of specimen container")
  			@OptionalParam(name="container")
  			TokenAndListParam theContainer,
    
  			@Description(shortDefinition="The unique identifier associated with the specimen container")
  			@OptionalParam(name="container-id")
  			TokenAndListParam theContainer_id,
    
  			@Description(shortDefinition="The unique identifier associated with the specimen")
  			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier,
    
  			@Description(shortDefinition="The parent of the specimen")
  			@OptionalParam(name="parent", targetTypes={  } )
  			ReferenceAndListParam theParent, 
    
 			@Description(shortDefinition="The patient the specimen comes from")
 			@OptionalParam(name="patient", targetTypes={  } )
 			ReferenceAndListParam thePatient, 
   
 			@Description(shortDefinition="available | unavailable | unsatisfactory | entered-in-error")
 			@OptionalParam(name="status")
 			TokenAndListParam theStatus,
   
 			@Description(shortDefinition="The subject of the specimen")
 			@OptionalParam(name="subject", targetTypes={  } )
 			ReferenceAndListParam theSubject, 
   
 			@Description(shortDefinition="The specimen type")
 			@OptionalParam(name="type")
 			TokenAndListParam theType,  			
 
 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= {
 					"Specimen:collector" ,
 					"Specimen:parent" ,
 					"Specimen:patient" ,
 					"Specimen:subject" ,
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
	
		paramMap.add("_language", theResourceLanguage);
	
		paramMap.add("_id", the_id);
		paramMap.add("_language", the_language);
		paramMap.add("accession", theAccession);
		paramMap.add("bodysite", theBodysite);
		paramMap.add("collected", theCollected);
		paramMap.add("collector", theCollector);
		paramMap.add("container", theContainer);
		paramMap.add("container-id", theContainer_id);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("parent", theParent);
		paramMap.add("patient", thePatient);
		paramMap.add("status", theStatus);
		paramMap.add("subject", theSubject);
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Specimen");

		// Now all possible searches need to be handeled. For performance reasons it makes sense
		// to put searches that are very restrictive and frequently used first in order
	
		// Add default handling for the _id search parameter
		builder.handleIdRestriction();
		
		// Add handling for search on the "owner" of the record. 
		builder.recordOwnerReference("patient", "Patient", "subject");
				
		// Add handling for search on the field that determines the MIDATA content type used.
        builder.recordCodeRestriction("type", "type");
			
        // Add handling for a multiTYPE search: "date" may be search on effectiveDateTime or effectivePeriod
        // Note that path = "effective" and type = TYPE_DATETIME_OR_PERIOD
        // If the search was only on effectiveDateTime then
        // type would be TYPE_DATETIME and path would be "effectiveDateTime" instead
		builder.restriction("date", true, QueryBuilder.TYPE_DATETIME_OR_PERIOD, "collection.collected");
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		
		// On some resources there are searches for "patient" and "subject" which are 
		// searches on the same field. patient is always the record owner
		// subject may be something different than a person and may not be the record owner
		// in that case. Add a record owner search if possible and a normal search otherwise:		
		if (!builder.recordOwnerReference("subject", null, "subject")) builder.restriction("subject", true, null, "subject");
	
		builder.restriction("accession", true, QueryBuilder.TYPE_IDENTIFIER, "accessionIdentifier");
		builder.restriction("bodysite", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "collection.bodySite");	
		builder.restriction("collector", true, "Practitioner", "collection.collector");	
		builder.restriction("container", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "container.type");	
		builder.restriction("container-id", true, QueryBuilder.TYPE_IDENTIFIER, "container.identifier");	
		builder.restriction("parent", true, "Specimen", "parent");
		builder.restriction("status", false, QueryBuilder.TYPE_CODE, "status");									
		
		// At last execute the constructed query
		return query.execute(info);
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Specimen theSpecimen) {
		return super.createResource(theSpecimen);
	}
			
	@Override
	public String getRecordFormat() {	
		return "fhir/Specimen";
	}	
			

	// This method is required if it is allowed to update the resource.
	// Just change the resource type
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Specimen theSpecimen) {
		return super.updateResource(theId, theSpecimen);
	}		

	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical type "content" set. 
	// b) Each record must have a "name" that will be shown to the user in the record tree.
	//    The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the FHIR representation
	public void prepare(Record record, Specimen theSpecimen) throws AppException {
		// Task a : Set Record "content" field by using a code from the resource (or a fixed value or something else useful)
		String display = setRecordCodeByCodeableConcept(record, theSpecimen.getType(), null);
		
		// Task b : Create record name
		String date = "No time";		
		if (theSpecimen.getCollection().hasCollectedDateTimeType()) {
			try {
				date = FHIRTools.stringFromDateTime(theSpecimen.getCollection().getCollectedDateTimeType());
			} catch (Exception e) {
				throw new UnprocessableEntityException("Cannot process effectiveDateTime");
			}
		}
		record.name = display != null ? (display + " / " + date) : date;		

		// Task c : Set record owner based on subject and clean subject if this was possible
		Reference subjectRef = theSpecimen.getSubject();
		if (cleanAndSetRecordOwner(record, subjectRef)) theSpecimen.setSubject(null);
		
		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta" section
		clean(theSpecimen);
 
	}	
 
	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, Specimen p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);
		
		// Add subject field from record owner field if it is not already there
		if (p.getSubject().isEmpty()) {			
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}
		
	

}