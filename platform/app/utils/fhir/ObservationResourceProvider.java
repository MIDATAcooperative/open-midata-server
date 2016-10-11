package utils.fhir;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.MidataId;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
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
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.QuantityAndListParam;
import ca.uhn.fhir.rest.param.QuantityParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.EncodingEnum;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

import org.hl7.fhir.dstu3.exceptions.FHIRException;
import org.hl7.fhir.dstu3.model.Type;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

import models.ContentInfo;
import models.Record;
import utils.AccessLog;
import utils.access.RecordManager;
import utils.auth.ExecutionInfo;
import utils.collections.CMaps;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.exceptions.AppException;

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
import org.hl7.fhir.instance.model.api.IIdType;

public class ObservationResourceProvider extends ResourceProvider<Observation> implements IResourceProvider {

	@Override
	public Class<Observation> getResourceType() {
		return Observation.class;
	}

	@Search()
	public List<Observation> getObservation(
			@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,

			@Description(shortDefinition = "The resource language") @OptionalParam(name = "_language") StringAndListParam theResourceLanguage,

			@Description(shortDefinition = "Search the contents of the resource's data using a fulltext search") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT) StringAndListParam theFtContent,

			@Description(shortDefinition = "Search the contents of the resource's narrative using a fulltext search") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_TEXT) StringAndListParam theFtText,

			@Description(shortDefinition = "Search for resources which have the given tag") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_TAG) TokenAndListParam theSearchForTag,

			@Description(shortDefinition = "Search for resources which have the given security labels") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY) TokenAndListParam theSearchForSecurity,

			@Description(shortDefinition = "Search for resources which have the given profile") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE) UriAndListParam theSearchForProfile,
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

			@Description(shortDefinition = "The value of the observation, if the value is a date or period of time") @OptionalParam(name = "value-date") DateRangeParam theValue_date,

			@Description(shortDefinition = "The value of the observation, if the value is a string, and also searches in CodeableConcept.text") @OptionalParam(name = "value-string") StringAndListParam theValue_string,

			@Description(shortDefinition = "The value of the component observation, if the value is a string, and also searches in CodeableConcept.text") @OptionalParam(name = "component-value-string") StringAndListParam theComponent_value_string,

			@Description(shortDefinition = "Obtained date/time. If the obtained element is a period, a date that falls in the period") @OptionalParam(name = "date") DateRangeParam theDate,

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

			@IncludeParam(allow = { "Observation:device", "Observation:encounter", "Observation:patient", "Observation:performer", "Observation:related-target", "Observation:specimen",				
					"Observation:subject", "*" }) Set<Include> theIncludes,

			@Sort SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", theId);
		paramMap.add("_language", theResourceLanguage);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT, theFtContent);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TEXT, theFtText);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TAG, theSearchForTag);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY, theSearchForSecurity);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE, theSearchForProfile);
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

		List<ReferenceParam> patients = builder.resolveReferences("patient", "Patient");
		if (patients != null) {
			query.putAccount("owner", referencesToIds(patients));
		}

		Set<String> codes = builder.tokensToCodeSystemStrings("code");
		if (codes != null) {
			query.putAccount("code", codes);
			builder.restriction("code", "code", "CodeableConcept", false);
		} else {
			builder.restriction("code", "code", "CodeableConcept", true);
		}

		builder.restriction("date", "effectiveDateTime", "DateTime", true);
		
		builder.restriction("code-value-quantity", "code", "valueQuantity", "CodeableConcept", "Quantity");
		builder.restriction("code-value-string", "code", "valueString", "CodeableConcept", "String");
		builder.restriction("code-value-date", "code", "valueDate", "CodeableConcept", "DateTime");
		builder.restriction("code-value-concept", "code", "valueConcept", "CodeableConcept", "CodeableConcept");
		
		builder.restriction("category", "category", "CodeableConcept", true);
		builder.restriction("component-code", "component.code", "CodeableConcept", true);
		
		builder.restriction("component-code-value-quantity", "component.code", "component.valueQuantity", "CodeableConcept", "Quantity");
		builder.restriction("component-code-value-string", "component.code", "component.valueString", "CodeableConcept", "String");
		builder.restriction("component-code-value-date", "component.code", "component.valueDate", "CodeableConcept", "DateTime");
		builder.restriction("component-code-value-concept", "component.code", "component.valueConcept", "CodeableConcept", "CodeableConcept");
		
		
		builder.restriction("data-absent-reason", "dataAbsentReason", "CodeableConcept", true);
		builder.restriction("identifier", "identifier", "CodeableConcept", true);
		builder.restriction("related-type", "related.type", "code", false);
		builder.restriction("status", "status", "code", false);
		builder.restriction("value-concept", "valueCodeableConcept", "CodeableConcept", true);
		
		builder.restriction("value-string", "valueString", "String", true);
		builder.restriction("value-quantity", "valueQuantity", "Quantity", true);
		builder.restriction("value-date", "valueDate", "DateTime", true);
		builder.restriction("component-value-string", "component.valueString", "String", true);
		builder.restriction("component-value-quantity", "component.valueQuantity", "Quantity", true);
		
		builder.restriction("device", "device", "Device", true);
		builder.restriction("encounter", "encounter", "Encounter", true);
		builder.restriction("performer", "performer", "Performer", true);
		builder.restriction("related-target", "related.target", null, true);
		builder.restriction("related", "related.type", "related.target", "code", null);
		builder.restriction("specimen", "Specimen", "specimen", true);
		
		
		return query.execute(info);
	}

	@Create
	public MethodOutcome createObservation(@ResourceParam Observation theObservation) {

		Record record = newRecord("fhir/Observation");
		prepare(record, theObservation);
		// insert
		insertRecord(record, theObservation);

		processResource(record, theObservation);				
		
		return outcome("Observation", record, theObservation);

	}
	
	public Record init() { return newRecord("fhir/Observation"); }

	@Update
	public MethodOutcome updateObservation(@IdParam IdType theId, @ResourceParam Observation theObservation) {
		Record record = fetchCurrent(theId);
		prepare(record, theObservation);		
		updateRecord(record, theObservation);		
		return outcome("Observation", record, theObservation);
	}

	public void prepare(Record record, Observation theObservation) {
		// Set Record code and content
		record.code = new HashSet<String>(); 
		String display = null;
		for (Coding coding : theObservation.getCode().getCoding()) {
			if (coding.getDisplay() != null && display == null) display = coding.getDisplay();
			if (coding.getCode() != null && coding.getSystem() != null) {
				record.code.add(coding.getSystem() + " " + coding.getCode());
			}
		}		
		try {
			ContentInfo.setRecordCodeAndContent(record, record.code, null);
		} catch (AppException e) {
			throw new InternalErrorException(e);
		}
		String date;
		try {
			date = theObservation.getEffectiveDateTimeType().asStringValue();
		} catch (FHIRException e) {
			throw new UnprocessableEntityException("Cannot process effectiveDateTime");
		}
		record.name = display != null ? (display + " / " + date) : date;

		// Set Record subtype
		Type valType = theObservation.getValue();
		if (valType == null && theObservation.getComponent() != null) {
			record.subformat = "component";
		} else if (valType == null) {
			throw new UnprocessableEntityException("Observation must have a value or component");
		} else if (valType instanceof StringType)
			record.subformat = "String";
		else if (valType instanceof Quantity)
			record.subformat = "Quantity";
		else if (valType instanceof BooleanType)
			record.subformat = "Boolean";
		else if (valType instanceof CodeableConcept)
			record.subformat = "CodeableConcept";
		else if (valType instanceof Range)
			record.subformat = "Range";
		else if (valType instanceof Ratio)
			record.subformat = "Ratio";
		else if (valType instanceof SampledData)
			record.subformat = "SampledData";
		else if (valType instanceof Attachment)
			record.subformat = "Attachment";
		else if (valType instanceof TimeType)
			record.subformat = "Time";
		else if (valType instanceof DateTimeType)
			record.subformat = "DateTime";
		else if (valType instanceof Period)
			record.subformat = "Period";		  		  		  
		else
			throw new UnprocessableEntityException("Value Type not Implemented");

		// clean
		Reference subjectRef = theObservation.getSubject();
		boolean cleanSubject = true;
		if (subjectRef != null) {
			IIdType target = subjectRef.getReferenceElement();
			if (target != null) {
				String rt = target.getResourceType();
				if (rt != null && rt.equals("Patient")) {
					String tId = target.getIdPart();
					if (! MidataId.isValid(tId)) throw new UnprocessableEntityException("Subject Reference not valid");
					record.owner = new MidataId(tId);
				} else cleanSubject = false;
			}
		}
		
		if (cleanSubject) theObservation.setSubject(null);
		clean(theObservation);

	}

	/*
	 * @Delete() public void deleteObservation(@IdParam IdType theId) { Record
	 * record = fetchCurrent(theId);
	 * RecordManager.instance.deleteRecord(info().executorId, info().targetAPS,
	 * record); }
	 */
 
	@Override
	public void processResource(Record record, Observation p) {
		super.processResource(record, p);
		if (p.getSubject().isEmpty()) {
			p.getSubject().setReferenceElement(new IdType("Patient", record.owner.toString()));
			p.getSubject().setDisplay(record.ownerName);
		}
	}

	@Override
	public void clean(Observation theObservation) {
		
		super.clean(theObservation);
	}

}