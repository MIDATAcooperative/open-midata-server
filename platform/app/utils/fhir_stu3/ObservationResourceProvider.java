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
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
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
import utils.access.pseudo.FhirPseudonymizer;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;

// Guide for implementing a FHIR resource:
// One resource provider needs to be written for each FHIR resource
//
// FHIR data type of resource should already be available from happy fhir
//
// Choose super class : 
// RecordBasedResourceProvider for Resources that will be Midata Records
// ReadWriteResourceProvider for other writable resources
// ResourceProvider for other read only resources
//
// Register your new provider in the class:
// utils.fhir.FHIRServlet
// (search for myProviders.put )
public class ObservationResourceProvider extends RecordBasedResourceProvider<Observation> implements IResourceProvider {

	// Provide one default constructor
	public ObservationResourceProvider() {
		
		// For each existing search parameter that has a "reference" type add one line:
		// searchParamNameToPathMap.put("Resource:search-name", "path from search specification");
		searchParamNameToPathMap.put("Observation:based-on", "basedOn");
		searchParamNameToPathMap.put("Observation:context", "context");
		searchParamNameToPathMap.put("Observation:device", "device");
		searchParamNameToPathMap.put("Observation:encounter", "encounter");
		searchParamNameToPathMap.put("Observation:patient", "subject");
		
		// For each existing search parameter that has a "reference" type that cannot reference
		// to any resource add one line:
		// searchParamNameToTypeMap.put("Resource:search-name", Sets.create("TargetResourceTyp1", ...));			
		searchParamNameToTypeMap.put("Observation:patient", Sets.create("Patient"));
		
		
		searchParamNameToPathMap.put("Observation:performer", "performer");
		searchParamNameToPathMap.put("Observation:related-target", "related.target");
		searchParamNameToPathMap.put("Observation:specimen", "specimen");
		searchParamNameToPathMap.put("Observation:subject", "subject");				
		
		// Use name of @Search function as last parameter
		registerSearches("Observation", getClass(), "getObservation");
		
		FhirPseudonymizer.forSTU3()
		  .reset("Observation")		
		  .pseudonymizeReference("Observation", "performer")
		  .pseudonymizeReference("Observation", "note", "authorReference")
		  ;		  
	}
	
	// Return corresponding FHIR class
	@Override
	public Class<Observation> getResourceType() {
		return Observation.class;
	}

	// Main search function for resource. May have another name.
	// Copy method signature from happy fhir implementation.
	// Look at http://hapifhir.io/xref-jpaserver/ca/uhn/fhir/jpa/rp/dstu3/ObservationResourceProvider.html
	// Throw out unsupported search parameters (like "_has" and all starting with "PARAM_" )
	// Replace DateRangeParam with DateAndListParam everywhere except for _lastUpdated	
	// Add non FHIR _page parameter used for the pagination mechanism
	@Search()
	public Bundle getObservation(
			@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,

			@Description(shortDefinition = "The resource language") @OptionalParam(name = "_language") StringAndListParam theResourceLanguage,

		
			@Description(shortDefinition="Reference to the test or procedure request.")
			@OptionalParam(name="based-on", targetTypes={  } )
			ReferenceAndListParam theBased_on, 
			   
			@Description(shortDefinition="The classification of the type of observation")
			@OptionalParam(name="category")
			TokenAndListParam theCategory, 
			  
			@Description(shortDefinition="The code of the observation type")
			@OptionalParam(name="code")
			TokenAndListParam theCode, 
			   
			@Description(shortDefinition="Code and coded value parameter pair")
			@OptionalParam(name="code-value-concept", compositeTypes= { TokenParam.class, TokenParam.class })
			CompositeAndListParam<TokenParam, TokenParam> theCode_value_concept,
			   
			@Description(shortDefinition="Code and date/time value parameter pair")
			@OptionalParam(name="code-value-date", compositeTypes= { TokenParam.class, DateParam.class })
			CompositeAndListParam<TokenParam, DateParam> theCode_value_date,
			  
			@Description(shortDefinition="Code and quantity value parameter pair")
			@OptionalParam(name="code-value-quantity", compositeTypes= { TokenParam.class, QuantityParam.class })
			CompositeAndListParam<TokenParam, QuantityParam> theCode_value_quantity,
			   
			@Description(shortDefinition="Code and string value parameter pair")
			@OptionalParam(name="code-value-string", compositeTypes= { TokenParam.class, StringParam.class })
			CompositeAndListParam<TokenParam, StringParam> theCode_value_string,
			   
			@Description(shortDefinition="The code of the observation type or component type")
			@OptionalParam(name="combo-code")
			TokenAndListParam theCombo_code, 
			   
			@Description(shortDefinition="Code and coded value parameter pair, including in components")
			@OptionalParam(name="combo-code-value-concept", compositeTypes= { TokenParam.class, TokenParam.class })
			CompositeAndListParam<TokenParam, TokenParam> theCombo_code_value_concept,
			   
			@Description(shortDefinition="Code and quantity value parameter pair, including in components")
			@OptionalParam(name="combo-code-value-quantity", compositeTypes= { TokenParam.class, QuantityParam.class })
			CompositeAndListParam<TokenParam, QuantityParam> theCombo_code_value_quantity,
			  
			@Description(shortDefinition="The reason why the expected value in the element Observation.value[x] or Observation.component.value[x] is missing.")
			@OptionalParam(name="combo-data-absent-reason")
			TokenAndListParam theCombo_data_absent_reason, 
			  
			@Description(shortDefinition="The value or component value of the observation, if the value is a CodeableConcept")
			@OptionalParam(name="combo-value-concept")
			TokenAndListParam theCombo_value_concept, 
			  
			@Description(shortDefinition="The value or component value of the observation, if the value is a Quantity, or a SampledData (just search on the bounds of the values in sampled data)")
			@OptionalParam(name="combo-value-quantity")
			QuantityAndListParam theCombo_value_quantity, 
			  
			@Description(shortDefinition="The component code of the observation type")
			@OptionalParam(name="component-code")
			TokenAndListParam theComponent_code, 
			  
			@Description(shortDefinition="Component code and component coded value parameter pair")
			@OptionalParam(name="component-code-value-concept", compositeTypes= { TokenParam.class, TokenParam.class })
			CompositeAndListParam<TokenParam, TokenParam> theComponent_code_value_concept,
			  
			@Description(shortDefinition="Component code and component quantity value parameter pair")
			@OptionalParam(name="component-code-value-quantity", compositeTypes= { TokenParam.class, QuantityParam.class })
			CompositeAndListParam<TokenParam, QuantityParam> theComponent_code_value_quantity,
			  
			@Description(shortDefinition="The reason why the expected value in the element Observation.component.value[x] is missing.")
			@OptionalParam(name="component-data-absent-reason")
			TokenAndListParam theComponent_data_absent_reason, 
			  
			@Description(shortDefinition="The value of the component observation, if the value is a CodeableConcept")
			@OptionalParam(name="component-value-concept")
			TokenAndListParam theComponent_value_concept, 
			  
			@Description(shortDefinition="The value of the component observation, if the value is a Quantity, or a SampledData (just search on the bounds of the values in sampled data)")
			@OptionalParam(name="component-value-quantity")
			QuantityAndListParam theComponent_value_quantity, 
			   
			@Description(shortDefinition="Healthcare event  (Episode-of-care or Encounter) related to the observation")
			@OptionalParam(name="context", targetTypes={  } )
			ReferenceAndListParam theContext, 
			  
			@Description(shortDefinition="The reason why the expected value in the element Observation.value[x] is missing.")
			@OptionalParam(name="data-absent-reason")
			TokenAndListParam theData_absent_reason, 
			  
			@Description(shortDefinition="Obtained date/time. If the obtained element is a period, a date that falls in the period")
			@OptionalParam(name="date")
			DateAndListParam theDate, 
			  
			@Description(shortDefinition="The Device that generated the observation data.")
			@OptionalParam(name="device", targetTypes={  } )
			ReferenceAndListParam theDevice, 
			  
			@Description(shortDefinition="Encounter related to the observation")
			@OptionalParam(name="encounter", targetTypes={  } )
			ReferenceAndListParam theEncounter, 
			  
			@Description(shortDefinition="The unique id for a particular observation")
			@OptionalParam(name="identifier")
			TokenAndListParam theIdentifier, 
			   
			@Description(shortDefinition="The method used for the observation")
			@OptionalParam(name="method")
			TokenAndListParam theMethod, 
			  
			@Description(shortDefinition="The subject that the observation is about (if patient)")
			@OptionalParam(name="patient", targetTypes={  } )
			ReferenceAndListParam thePatient, 
			  
			@Description(shortDefinition="Who performed the observation")
			@OptionalParam(name="performer", targetTypes={  } )
			ReferenceAndListParam thePerformer, 
			   
			@Description(shortDefinition="Related Observations - search on related-type and related-target together")
			@OptionalParam(name="related", compositeTypes= { ReferenceParam.class, TokenParam.class })
			CompositeAndListParam<ReferenceParam, TokenParam> theRelated,
			  
			@Description(shortDefinition="Resource that is related to this one")
			@OptionalParam(name="related-target", targetTypes={  } )
			ReferenceAndListParam theRelated_target, 
			  
			@Description(shortDefinition="has-member | derived-from | sequel-to | replaces | qualified-by | interfered-by")
			@OptionalParam(name="related-type")
			TokenAndListParam theRelated_type, 
			  
			@Description(shortDefinition="Specimen used for this observation")
			@OptionalParam(name="specimen", targetTypes={  } )
			ReferenceAndListParam theSpecimen, 
			  
			@Description(shortDefinition="The status of the observation")
			@OptionalParam(name="status")
			TokenAndListParam theStatus, 
			  
			@Description(shortDefinition="The subject that the observation is about")
			@OptionalParam(name="subject", targetTypes={  } )
			ReferenceAndListParam theSubject, 
			   
			@Description(shortDefinition="The value of the observation, if the value is a CodeableConcept")
			@OptionalParam(name="value-concept")
			TokenAndListParam theValue_concept, 
			   
			@Description(shortDefinition="The value of the observation, if the value is a date or period of time")
			@OptionalParam(name="value-date")
			DateAndListParam theValue_date, 
			  
			@Description(shortDefinition="The value of the observation, if the value is a Quantity, or a SampledData (just search on the bounds of the values in sampled data)")
			@OptionalParam(name="value-quantity")
			QuantityAndListParam theValue_quantity, 
			   
			@Description(shortDefinition="The value of the observation, if the value is a string, and also searches in CodeableConcept.text")
			@OptionalParam(name="value-string")
			StringAndListParam theValue_string, 
			
			@IncludeParam(reverse=true)
			Set<Include> theRevIncludes,
			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
			@OptionalParam(name="_lastUpdated")
			DateRangeParam theLastUpdated, 
			
			// This often needs to be cleaned up after copy/paste
			@IncludeParam(allow= {
					"Observation:based-on",
					"Observation:context" ,
					"Observation:device" ,
					"Observation:encounter" ,
					"Observation:patient" ,
					"Observation:performer" ,
					"Observation:related-target" ,
					"Observation:specimen" ,
					"Observation:subject" ,
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
	
		paramMap.add("based-on", theBased_on);
		paramMap.add("category", theCategory);
		paramMap.add("code", theCode);
		paramMap.add("code-value-concept", theCode_value_concept);
		paramMap.add("code-value-date", theCode_value_date);
		paramMap.add("code-value-quantity", theCode_value_quantity);
		paramMap.add("code-value-string", theCode_value_string);
		paramMap.add("combo-code", theCombo_code);
		paramMap.add("combo-code-value-concept", theCombo_code_value_concept);
		paramMap.add("combo-code-value-quantity", theCombo_code_value_quantity);
		paramMap.add("combo-data-absent-reason", theCombo_data_absent_reason);
		paramMap.add("combo-value-concept", theCombo_value_concept);
		paramMap.add("combo-value-quantity", theCombo_value_quantity);
		paramMap.add("component-code", theComponent_code);
		paramMap.add("component-code-value-concept", theComponent_code_value_concept);
		paramMap.add("component-code-value-quantity", theComponent_code_value_quantity);
		paramMap.add("component-data-absent-reason", theComponent_data_absent_reason);
		paramMap.add("component-value-concept", theComponent_value_concept);
		paramMap.add("component-value-quantity", theComponent_value_quantity);
		paramMap.add("context", theContext);
		paramMap.add("data-absent-reason", theData_absent_reason);
		paramMap.add("date", theDate);
		paramMap.add("device", theDevice);
		paramMap.add("encounter", theEncounter);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("method", theMethod);
		paramMap.add("patient", thePatient);
		paramMap.add("performer", thePerformer);
		paramMap.add("related", theRelated);
		paramMap.add("related-target", theRelated_target);
		paramMap.add("related-type", theRelated_type);
		paramMap.add("specimen", theSpecimen);
		paramMap.add("status", theStatus);
		paramMap.add("subject", theSubject);
		paramMap.add("value-concept", theValue_concept);
		paramMap.add("value-date", theValue_date);
		paramMap.add("value-quantity", theValue_quantity);
		paramMap.add("value-string", theValue_string);
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Observation");

		// Now all possible searches need to be handeled. For performance reasons it makes sense
		// to put searches that are very restrictive and frequently used first in order
	
		// Add default handling for the _id search parameter
		builder.handleIdRestriction();
		
		// Add handling for search on the "owner" of the record. 
		builder.recordOwnerReference("patient", "Patient", "subject");
				
		// Add handling for search on the field that determines the MIDATA content type used.
        builder.recordCodeRestriction("code", "code");
			
        // Add handling for a multiTYPE search: "date" may be search on effectiveDateTime or effectivePeriod
        // Note that path = "effective" and type = TYPE_DATETIME_OR_PERIOD
        // If the search was only on effectiveDateTime then
        // type would be TYPE_DATETIME and path would be "effectiveDateTime" instead
		builder.restriction("date", true, QueryBuilder.TYPE_DATETIME_OR_PERIOD, "effective");
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		
		// On some resources there are searches for "patient" and "subject" which are 
		// searches on the same field. patient is always the record owner
		// subject may be something different than a person and may not be the record owner
		// in that case. Add a record owner search if possible and a normal search otherwise:		
		if (!builder.recordOwnerReference("subject", null, "subject")) builder.restriction("subject", true, null, "subject");
	
		// Many examples for COMPOSITE type searches:
		builder.restriction("code-value-quantity", "code", "valueQuantity", QueryBuilder.TYPE_CODEABLE_CONCEPT, QueryBuilder.TYPE_QUANTITY);
		builder.restriction("code-value-string", "code", "valueString", QueryBuilder.TYPE_CODEABLE_CONCEPT, QueryBuilder.TYPE_STRING);
		builder.restriction("code-value-date", "code", "value", QueryBuilder.TYPE_CODEABLE_CONCEPT, QueryBuilder.TYPE_DATETIME_OR_PERIOD);
		builder.restriction("code-value-concept", "code", "valueConcept", QueryBuilder.TYPE_CODEABLE_CONCEPT, QueryBuilder.TYPE_CODEABLE_CONCEPT);
		
		// Example for a restriction on a Codeable Concept:
		builder.restriction("category", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "category");
		builder.restriction("component-code", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "component.code");
		
		builder.restriction("component-code-value-quantity", "component.code", "component.valueQuantity", QueryBuilder.TYPE_CODEABLE_CONCEPT, QueryBuilder.TYPE_QUANTITY);
		builder.restriction("component-code-value-string", "component.code", "component.valueString", QueryBuilder.TYPE_CODEABLE_CONCEPT, QueryBuilder.TYPE_STRING);
		builder.restriction("component-code-value-date", "component.code", "component.value", QueryBuilder.TYPE_CODEABLE_CONCEPT, QueryBuilder.TYPE_DATETIME_OR_PERIOD);
		builder.restriction("component-code-value-concept", "component.code", "component.valueConcept", QueryBuilder.TYPE_CODEABLE_CONCEPT, QueryBuilder.TYPE_CODEABLE_CONCEPT);
		
		builder.restriction("combo-code", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "code", QueryBuilder.TYPE_CODEABLE_CONCEPT, "component.code");				
		builder.restriction("combo-data-absent-reason", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "dataAbsentReason", QueryBuilder.TYPE_CODEABLE_CONCEPT, "component.dataAbsentReason");
	    builder.restriction("combo-value-concept", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "valueCodeableConcept", QueryBuilder.TYPE_CODEABLE_CONCEPT, "component.valueCodeableConcept");
	    builder.restriction("combo-value-quantity", true, QueryBuilder.TYPE_QUANTITY, "valueQuantity", QueryBuilder.TYPE_QUANTITY, "component.valueQuantity");
		
	    builder.restriction("method", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "method");
				
		builder.restriction("data-absent-reason", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "dataAbsentReason");
		
		builder.restriction("related-type", false, "code", "related.type");
		
		// Example for a search on a code field (not codeable concept)
		builder.restriction("status", false, QueryBuilder.TYPE_CODE, "status");
				
		builder.restriction("value-concept", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "valueCodeableConcept");
		
		builder.restriction("value-string", true, QueryBuilder.TYPE_STRING, "valueString");
		builder.restriction("value-quantity", true, QueryBuilder.TYPE_QUANTITY, "valueQuantity");
		builder.restriction("value-date", true, QueryBuilder.TYPE_DATETIME_OR_PERIOD, "value");
		builder.restriction("component-value-string", true, QueryBuilder.TYPE_STRING, "component.valueString");
		builder.restriction("component-value-quantity", true, QueryBuilder.TYPE_QUANTITY, "component.valueQuantity");
		
		builder.restriction("device", true, "Device", "device");
		builder.restriction("encounter", true, "Encounter", "encounter");
		builder.restriction("performer", true, "Performer", "performer");
		
		// Example for a "reference" type search where the target type of the reference is unknown:
		builder.restriction("based-on", true, null, "basedOn");
		builder.restriction("related-target", true, null, "related.target");
		builder.restriction("related", "related.target", "related.type", null,  QueryBuilder.TYPE_CODE);
		
		// Example for a "reference" type search where the target type is known:
		builder.restriction("specimen", true, "Specimen", "specimen");
		
		// At last execute the constructed query
		return query.execute(info);
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Observation theObservation) {
		return super.createResource(theObservation);
	}
		
	@Override
	public String getRecordFormat() {	
		return "fhir/Observation";
	}
		

	// This method is required if it is allowed to update the resource.
	// Just change the resource type
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Observation theObservation) {
		return super.updateResource(theId, theObservation);
	}		

	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical type "content" set. 
	// b) Each record must have a "name" that will be shown to the user in the record tree.
	//    The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the FHIR representation
	public void prepare(Record record, Observation theObservation) throws AppException {
		// Task a : Set Record "content" field by using a code from the resource (or a fixed value or something else useful)
		String display = setRecordCodeByCodeableConcept(record, theObservation.getCode(), null);
		
		// Task b : Create record name
		String date = "No time";		
		if (theObservation.hasEffective()) {
			try {
				date = FHIRTools.stringFromDateTime(theObservation.getEffective());
			} catch (Exception e) {
				throw new UnprocessableEntityException("Cannot process effectiveDateTime");
			}
		}
		record.name = display != null ? (display + " / " + date) : date;		

		// Task c : Set record owner based on subject and clean subject if this was possible
		Reference subjectRef = theObservation.getSubject();
		if (cleanAndSetRecordOwner(record, subjectRef)) theObservation.setSubject(null);
		
		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta" section
		clean(theObservation);
 
	}	
 
	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, Observation p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);
		
		// Add subject field from record owner field if it is not already there
		if (p.getSubject().isEmpty()) {			
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}
	
	// This is the implementation of an Observation specific operation called $lastn
	@Operation(name="$lastn", idempotent=true)
	public Bundle lastn(				  
	   @OperationParam(name="max") IntegerType theMax,
	   @OperationParam(name="patient") ReferenceParam thePatient,
	   @OperationParam(name="subject") ReferenceParam theSubject,
	   @OperationParam(name="code") TokenAndListParam theCode
	   ) throws AppException {
		SearchParameterMap paramMap = new SearchParameterMap();
		Bundle retVal = new Bundle();
		int count = theMax != null ? theMax.getValue() : 1;		
		paramMap.setSort(new SortSpec("date", SortOrderEnum.DESC));
		long now = System.currentTimeMillis();
		ExecutionInfo inf = info();
		
		// Prepare search parameters
		paramMap.add("subject", theSubject);
		paramMap.add("patient", thePatient);
		paramMap.add("code", theCode);
		
		// Build a query...
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(paramMap, query, "fhir/Observation");		
		builder.recordOwnerReference("patient", "Patient", "subject");		
        builder.recordCodeRestriction("code", "code");					
		if (!builder.recordOwnerReference("subject", null, "subject")) builder.restriction("subject", true, null, "subject");
		
		// and use it for a summary query by content type
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("include-records", true);
		Object owner = query.getAccountCriteria().get("owner");
		if (owner != null) properties.put("owner", owner);
		Object content = query.getAccountCriteria().get("code");
		if (content != null) properties.put("code", content);
		
		
		Collection<RecordsInfo> groups = RecordManager.instance.info(inf.executorId, inf.role, inf.targetAPS, inf.context, properties, AggregationType.CONTENT);
		
        // For each found content type...						
		for (RecordsInfo code : groups) {
			paramMap.setCount(count);
			paramMap.setFrom(null);
			paramMap.setContent(code.contents.iterator().next());
			
			// If there are only few records execute query without restriction
			if (code.count < Math.max(count * 2, 30)) {
				paramMap.remove("date");
				addBundle(paramMap, retVal, code.count);
			} else {
				
				// Otherwise determine effective date of last written record
				Observation ref = parse(code.newestRecordContent, getResourceType());				
				now = code.newest.getTime();
				try {
					DateTimeType t1 = ref.getEffectiveDateTimeType();
					if (t1 != null && t1.getValue() != null) now = t1.getValue().getTime();
					else {
						Period p = ref.getEffectivePeriod();
						if (p != null && p.getStart() != null) now = p.getStart().getTime();
					}
				} catch (FHIRException e) {}
				
				// How long should we look into the past?
				long range = 1000l*60l*60l*24l*(count+3);
				
				// Apply date restriction
				Date limit = new Date(now - range);
				paramMap.remove("date");
				paramMap.add("date", (IQueryParameterType) new DateParam(ParamPrefixEnum.STARTS_AFTER, limit));
				
				// Search
				int found = addBundle(paramMap, retVal, code.count);
				int retries = 3;
				
				// If we did not find enough results retry 3 times
				while (found<count && retries>=0) {
					retries--;
					
					// Search only for the remaining records
					paramMap.setCount(count-found);			
					
					DateAndListParam daterange = new DateAndListParam();
					daterange.addValue(new DateOrListParam().add(new DateParam(ParamPrefixEnum.LESSTHAN, limit)));
					
					// Increase search range
					range = range * 2 + 1000l*60l*60l*24l*32l;
					limit = new Date(now-range);
					
					// If last try search from the beginning
					if (retries>0) daterange.addValue(new DateOrListParam().add(new DateParam(ParamPrefixEnum.STARTS_AFTER, limit)));
					paramMap.remove("date");
					paramMap.add("date", daterange);
					
					// Search again
					found += addBundle(paramMap, retVal, code.count);				
				}
			}
		}
				
		retVal.setTotal(retVal.getEntry().size());
 	    return retVal;
	}
	
	// This is used by the $lastn implementation
	private int addBundle(SearchParameterMap paramMap, Bundle retVal, int count) {
		List<IBaseResource> partResult = search(paramMap);												
		for (IBaseResource res : partResult) {
			BundleEntryComponent cmp = retVal.addEntry();
			cmp.setResource((Resource) res);
			cmp.addExtension("http://midata.coop/Extensions/total-count", new IntegerType(count));
		}		
		return partResult.size();
	}
	

}