package utils.fhir;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Appointment;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Range;
import org.hl7.fhir.dstu3.model.Ratio;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.SampledData;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.TimeType;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

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
import ca.uhn.fhir.rest.param.CompositeAndListParam;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.QuantityAndListParam;
import ca.uhn.fhir.rest.param.QuantityParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import models.ContentInfo;
import models.MidataId;
import models.Record;
import utils.AccessLog;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class ObservationResourceProvider extends ResourceProvider<Observation> implements IResourceProvider {

	public ObservationResourceProvider() {
		searchParamNameToPathMap.put("Observation:based-on", "basedOn");
		searchParamNameToPathMap.put("Observation:context", "context");
		searchParamNameToPathMap.put("Observation:device", "device");
		searchParamNameToPathMap.put("Observation:encounter", "encounter");
		searchParamNameToPathMap.put("Observation:patient", "subject");
		searchParamNameToTypeMap.put("Observation:patient", Sets.create("Patient"));
		searchParamNameToPathMap.put("Observation:performer", "performer");
		searchParamNameToPathMap.put("Observation:related-target", "related.target");
		searchParamNameToPathMap.put("Observation:specimen", "specimen");
		searchParamNameToPathMap.put("Observation:subject", "subject");				
		
		registerSearches("Observation", getClass(), "getObservation");
	}
	
	@Override
	public Class<Observation> getResourceType() {
		return Observation.class;
	}

	@Search()
	public List<IBaseResource> getObservation(
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
			DateRangeParam theDate, 
			  
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
			DateRangeParam theValue_date, 
			  
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

			@ca.uhn.fhir.rest.annotation.Count Integer theCount

	) throws AppException {

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

		return search(paramMap);
	}

	public List<Record> searchRaw(SearchParameterMap params) throws AppException {
		ExecutionInfo info = info();

		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Observation");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient");
				
        builder.recordCodeRestriction("code", "code");
			
		builder.restriction("date", true, QueryBuilder.TYPE_DATETIME_OR_PERIOD, "effective");
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		
		if (!builder.recordOwnerReference("subject", null)) builder.restriction("subject", true, null, "subject");
		
		builder.restriction("code-value-quantity", "code", "valueQuantity", QueryBuilder.TYPE_CODEABLE_CONCEPT, QueryBuilder.TYPE_QUANTITY);
		builder.restriction("code-value-string", "code", "valueString", QueryBuilder.TYPE_CODEABLE_CONCEPT, QueryBuilder.TYPE_STRING);
		builder.restriction("code-value-date", "code", "value", QueryBuilder.TYPE_CODEABLE_CONCEPT, QueryBuilder.TYPE_DATETIME_OR_PERIOD);
		builder.restriction("code-value-concept", "code", "valueConcept", QueryBuilder.TYPE_CODEABLE_CONCEPT, QueryBuilder.TYPE_CODEABLE_CONCEPT);
		
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
		
		
		
		builder.restriction("data-absent-reason", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "dataAbsentReason");
		
		builder.restriction("related-type", false, "code", "related.type");
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
		builder.restriction("based-on", true, null, "basedOn");
		builder.restriction("related-target", true, null, "related.target");
		builder.restriction("related", "related.target", "related.type", null,  QueryBuilder.TYPE_CODE);
		builder.restriction("specimen", true, "Specimen", "specimen");
		
		
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Observation theObservation) {
		return super.createResource(theObservation);
	}
			
	public Record init() { return newRecord("fhir/Observation"); }

	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Observation theObservation) {
		return super.updateResource(theId, theObservation);
	}		

	public void prepare(Record record, Observation theObservation) throws AppException {
		// Set Record code and content
		String display = setRecordCodeByCodeableConcept(record, theObservation.getCode(), null);		
		String date = "No time";		
		if (theObservation.hasEffectiveDateTimeType()) {
			try {
				date = stringFromDateTime(theObservation.getEffectiveDateTimeType());
			} catch (Exception e) {
				throw new UnprocessableEntityException("Cannot process effectiveDateTime");
			}
		}
		record.name = display != null ? (display + " / " + date) : date;		

		// clean
		Reference subjectRef = theObservation.getSubject();
		if (cleanAndSetRecordOwner(record, subjectRef)) theObservation.setSubject(null);
		
		clean(theObservation);
 
	}	
 
	@Override
	public void processResource(Record record, Observation p) throws AppException {
		super.processResource(record, p);
		
		if (p.getSubject().isEmpty()) {			
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}
	

}