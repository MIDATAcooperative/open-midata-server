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

			/*
			@Description(shortDefinition = "Search the contents of the resource's data using a fulltext search") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT) StringAndListParam theFtContent,

			@Description(shortDefinition = "Search the contents of the resource's narrative using a fulltext search") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_TEXT) StringAndListParam theFtText,

			@Description(shortDefinition = "Search for resources which have the given tag") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_TAG) TokenAndListParam theSearchForTag,

			@Description(shortDefinition = "Search for resources which have the given security labels") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY) TokenAndListParam theSearchForSecurity,

			@Description(shortDefinition = "Search for resources which have the given profile") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE) UriAndListParam theSearchForProfile,
			*/
			/*
			 * @Description(shortDefinition=
			 * "Return resources linked to by the given target")
			 * 
			 * @OptionalParam(name="_has") HasAndListParam theHas,
			 */

			@Description(shortDefinition = "The code of the observation type") @OptionalParam(name = "code") TokenAndListParam theCode,

			@Description(shortDefinition = "The component code of the observation type") @OptionalParam(name = "component-code") TokenAndListParam theComponent_code,

			@Description(shortDefinition = "The value of the observation, if the value is a Quantity, or a SampledData (just search on the bounds of the values in sampled data)") @OptionalParam(name = "value-quantity") QuantityAndListParam theValue_quantity,

			@Description(shortDefinition = "The value of the component observation, if the value is a Quantity, or a SampledData (just search on the bounds of the values in sampled data)") @OptionalParam(name = "component-value-quantity") QuantityAndListParam theComponent_value_quantity,

			@Description(shortDefinition = "The value of the observation, if the value is a CodeableConcept") @OptionalParam(name = "value-concept") TokenAndListParam theValue_concept,

			@Description(shortDefinition = "The value of the component observation, if the value is a CodeableConcept") @OptionalParam(name = "component-value-concept") TokenAndListParam theComponent_value_concept,

			@Description(shortDefinition = "The value of the observation, if the value is a date or period of time") @OptionalParam(name = "value-date") DateAndListParam theValue_date,

			@Description(shortDefinition = "The value of the observation, if the value is a string, and also searches in CodeableConcept.text") @OptionalParam(name = "value-string") StringAndListParam theValue_string,

			@Description(shortDefinition = "The value of the component observation, if the value is a string, and also searches in CodeableConcept.text") @OptionalParam(name = "component-value-string") StringAndListParam theComponent_value_string,

			@Description(shortDefinition = "Obtained date/time. If the obtained element is a period, a date that falls in the period") @OptionalParam(name = "date") DateAndListParam theDate,

			@Description(shortDefinition = "The status of the observation") @OptionalParam(name = "status") TokenAndListParam theStatus,

			@Description(shortDefinition = "The subject that the observation is about") @OptionalParam(name = "subject", targetTypes = {}) ReferenceAndListParam theSubject,

			@Description(shortDefinition = "Who performed the observation") @OptionalParam(name = "performer", targetTypes = {}) ReferenceAndListParam thePerformer,

			@Description(shortDefinition = "") @OptionalParam(name = "specimen", targetTypes = {}) ReferenceAndListParam theSpecimen,

			@Description(shortDefinition = "") @OptionalParam(name = "related-type") TokenAndListParam theRelated_type,

			@Description(shortDefinition = "") @OptionalParam(name = "related-target", targetTypes = {}) ReferenceAndListParam theRelated_target,

			@Description(shortDefinition = "Healthcare event related to the observation") @OptionalParam(name = "encounter", targetTypes = {}) ReferenceAndListParam theEncounter,

			@Description(shortDefinition = "The reason why the expected value in the element Observation.value[x] is missing.") @OptionalParam(name = "data-absent-reason") TokenAndListParam theData_absent_reason,

			@Description(shortDefinition = "The reason why the expected value in the element Observation.component.value[x] is missing.") @OptionalParam(name = "component-data-absent-reason") TokenAndListParam theComponent_data_absent_reason,

			@Description(shortDefinition = "The subject that the observation is about (if patient)") @OptionalParam(name = "patient", targetTypes = { Patient.class }) ReferenceAndListParam thePatient,

			@Description(shortDefinition = "The unique id for a particular observation") @OptionalParam(name = "identifier") TokenAndListParam theIdentifier,

			@Description(shortDefinition = "The Device that generated the observation data.") @OptionalParam(name = "device", targetTypes = {}) ReferenceAndListParam theDevice,

			@Description(shortDefinition = "The classification of the type of observation") @OptionalParam(name = "category") TokenAndListParam theCategory,

			@Description(shortDefinition = "Both code and one of the value parameters") @OptionalParam(name = "code-value-quantity", compositeTypes = { TokenParam.class, QuantityParam.class }) CompositeAndListParam<TokenParam, QuantityParam> theCode_value_quantity,

			@Description(shortDefinition = "Both code and one of the value parameters") @OptionalParam(name = "code-value-concept", compositeTypes = { TokenParam.class, TokenParam.class }) CompositeAndListParam<TokenParam, TokenParam> theCode_value_concept,

			@Description(shortDefinition = "Both code and one of the value parameters") @OptionalParam(name = "code-value-date", compositeTypes = { TokenParam.class, DateParam.class }) CompositeAndListParam<TokenParam, DateParam> theCode_value_date,

			@Description(shortDefinition = "Both code and one of the value parameters") @OptionalParam(name = "code-value-string", compositeTypes = { TokenParam.class, StringParam.class }) CompositeAndListParam<TokenParam, StringParam> theCode_value_string,

			@Description(shortDefinition = "Both component code and one of the component value parameters") @OptionalParam(name = "component-code-component-value-quantity", compositeTypes = {
					TokenParam.class, QuantityParam.class }) CompositeAndListParam<TokenParam, QuantityParam> theComponent_code_component_value_quantity,

			@Description(shortDefinition = "Both component code and one of the component value parameters") @OptionalParam(name = "component-code-component-value-concept", compositeTypes = {
					TokenParam.class, TokenParam.class }) CompositeAndListParam<TokenParam, TokenParam> theComponent_code_component_value_concept,

			@Description(shortDefinition = "Both component code and one of the component value parameters") @OptionalParam(name = "component-code-component-value-string", compositeTypes = {
					TokenParam.class, StringParam.class }) CompositeAndListParam<TokenParam, StringParam> theComponent_code_component_value_string,

			@Description(shortDefinition = "Related Observations - search on related-type and related-target together") @OptionalParam(name = "related-target-related-type", compositeTypes = {
					ReferenceParam.class, TokenParam.class }) CompositeAndListParam<ReferenceParam, TokenParam> theRelated_target_related_type,

			@IncludeParam(reverse = true) Set<Include> theRevIncludes,
			@Description(shortDefinition = "Only return resources which were last updated as specified by the given range") @OptionalParam(name = "_lastUpdated") DateRangeParam theLastUpdated,

			@IncludeParam(allow = { 
					"Observation:device", 
					"Observation:encounter", 
					"Observation:patient", 
					"Observation:performer", 
					"Observation:related-target", 
					"Observation:specimen",				
					"Observation:subject", 
					"*" }) Set<Include> theIncludes,

			@Sort SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", theId);
		paramMap.add("_language", theResourceLanguage);
		/*
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT, theFtContent);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TEXT, theFtText);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TAG, theSearchForTag);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY, theSearchForSecurity);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE, theSearchForProfile);
		*/
		// paramMap.add("_has", theHas);
		paramMap.add("code", theCode);
		paramMap.add("component-code", theComponent_code);
		paramMap.add("value-quantity", theValue_quantity);
		paramMap.add("component-value-quantity", theComponent_value_quantity);
		paramMap.add("value-concept", theValue_concept);
		paramMap.add("component-value-concept", theComponent_value_concept);
		paramMap.add("value-date", theValue_date);
		paramMap.add("value-string", theValue_string);
		paramMap.add("component-value-string", theComponent_value_string);
		paramMap.add("date", theDate);
		paramMap.add("status", theStatus);
		paramMap.add("subject", theSubject);
		paramMap.add("performer", thePerformer);
		paramMap.add("specimen", theSpecimen);
		paramMap.add("related-type", theRelated_type);
		paramMap.add("related-target", theRelated_target);
		paramMap.add("encounter", theEncounter);
		paramMap.add("data-absent-reason", theData_absent_reason);
		paramMap.add("component-data-absent-reason", theComponent_data_absent_reason);
		paramMap.add("patient", thePatient);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("device", theDevice);
		paramMap.add("category", theCategory);
		paramMap.add("code-value-quantity", theCode_value_quantity);
		paramMap.add("code-value-concept", theCode_value_concept);
		paramMap.add("code-value-date", theCode_value_date);
		paramMap.add("code-value-string", theCode_value_string);
		paramMap.add("component-code-component-value-quantity", theComponent_code_component_value_quantity);
		paramMap.add("component-code-component-value-concept", theComponent_code_component_value_concept);
		paramMap.add("component-code-component-value-string", theComponent_code_component_value_string);
		paramMap.add("related-target-related-type", theRelated_target_related_type);
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
			
		builder.restriction("date", true, "DateTime|Period", "effective");
		builder.restriction("identifier", true, "Identifier", "identifier");
		
		if (!builder.recordOwnerReference("subject", null)) builder.restriction("subject", true, null, "subject");
		
		builder.restriction("code-value-quantity", "code", "valueQuantity", "CodeableConcept", "Quantity");
		builder.restriction("code-value-string", "code", "valueString", "CodeableConcept", "String");
		builder.restriction("code-value-date", "code", "value", "CodeableConcept", "DateTime|Period");
		builder.restriction("code-value-concept", "code", "valueConcept", "CodeableConcept", "CodeableConcept");
		
		builder.restriction("category", true, "CodeableConcept", "category");
		builder.restriction("component-code", true, "CodeableConcept", "component.code");
		
		builder.restriction("component-code-value-quantity", "component.code", "component.valueQuantity", "CodeableConcept", "Quantity");
		builder.restriction("component-code-value-string", "component.code", "component.valueString", "CodeableConcept", "String");
		builder.restriction("component-code-value-date", "component.code", "component.value", "CodeableConcept", "DateTime|Period");
		builder.restriction("component-code-value-concept", "component.code", "component.valueConcept", "CodeableConcept", "CodeableConcept");
		
		
		builder.restriction("data-absent-reason", true, "CodeableConcept", "dataAbsentReason");
		
		builder.restriction("related-type", false, "code", "related.type");
		builder.restriction("status", false, "code", "status");
		builder.restriction("value-concept", true, "CodeableConcept", "valueCodeableConcept");
		
		builder.restriction("value-string", true, "string", "valueString");
		builder.restriction("value-quantity", true, "Quantity", "valueQuantity");
		builder.restriction("value-date", true, "DateTime|Period", "value");
		builder.restriction("component-value-string", true, "string", "component.valueString");
		builder.restriction("component-value-quantity", true, "Quantity", "component.valueQuantity");
		
		builder.restriction("device", true, "Device", "device");
		builder.restriction("encounter", true, "Encounter", "encounter");
		builder.restriction("performer", true, "Performer", "performer");
		builder.restriction("related-target", true, null, "related.target");
		builder.restriction("related", "related.type", "related.target", "code", null);
		builder.restriction("specimen", true, "Specimen", "specimen");
		
		
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Observation theObservation) {
		return super.createResource(theObservation);
	}
	
	@Override
	protected MethodOutcome create(Observation theObservation) throws AppException {

		Record record = newRecord("fhir/Observation");
		prepare(record, theObservation);
		// insert
		insertRecord(record, theObservation);

		processResource(record, theObservation);				
		
		return outcome("Observation", record, theObservation);

	}
	
	public Record init() { return newRecord("fhir/Observation"); }

	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Observation theObservation) {
		return super.updateResource(theId, theObservation);
	}
	
	@Override
	protected MethodOutcome update(@IdParam IdType theId, @ResourceParam Observation theObservation) throws AppException {
		Record record = fetchCurrent(theId);
		prepare(record, theObservation);		
		updateRecord(record, theObservation);	
		processResource(record, theObservation);
		
		return outcome("Observation", record, theObservation);
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

	/*
	 * @Delete() public void deleteObservation(@IdParam IdType theId) { Record
	 * record = fetchCurrent(theId);
	 * RecordManager.instance.deleteRecord(info().executorId, info().targetAPS,
	 * record); }
	 */
 
	@Override
	public void processResource(Record record, Observation p) throws AppException {
		super.processResource(record, p);
		
		if (p.getSubject().isEmpty()) {			
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}

	@Override
	public void clean(Observation theObservation) {
		
		super.clean(theObservation);
	}

}