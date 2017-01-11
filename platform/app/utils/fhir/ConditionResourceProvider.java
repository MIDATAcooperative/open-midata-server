package utils.fhir;

import java.util.List;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Condition;
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
import org.hl7.fhir.instance.model.api.IBaseResource;

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
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import models.Record;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class ConditionResourceProvider extends ResourceProvider<Condition> implements IResourceProvider {

	public ConditionResourceProvider() {		
		searchParamNameToPathMap.put("Condition:asserter", "asserter");
		searchParamNameToPathMap.put("Condition:context", "context");
		searchParamNameToPathMap.put("Condition:patient", "subject");
		searchParamNameToTypeMap.put("Condition:patient", Sets.create("Patient"));		
		searchParamNameToPathMap.put("Condition:subject", "subject");		
	}
	
	@Override
	public Class<Condition> getResourceType() {
		return Condition.class;
	}

	@Search()
	public List<IBaseResource> getCondition(
			@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,

			@Description(shortDefinition = "The resource language") @OptionalParam(name = "_language") StringAndListParam theResourceLanguage,

			/*
			@Description(shortDefinition = "Search the contents of the resource's data using a fulltext search") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT) StringAndListParam theFtContent,

			@Description(shortDefinition = "Search the contents of the resource's narrative using a fulltext search") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_TEXT) StringAndListParam theFtText,

			@Description(shortDefinition = "Search for resources which have the given tag") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_TAG) TokenAndListParam theSearchForTag,

			@Description(shortDefinition = "Search for resources which have the given security labels") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY) TokenAndListParam theSearchForSecurity,

			@Description(shortDefinition = "Search for resources which have the given profile") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE) UriAndListParam theSearchForProfile,
			*/
			
			@Description(shortDefinition="Abatement as age or age range")
			@OptionalParam(name="abatement-age")
			QuantityAndListParam theAbatement_age, 
			    
			@Description(shortDefinition="Abatement boolean (boolean is true or non-boolean values are present)")
			@OptionalParam(name="abatement-boolean")
			TokenAndListParam theAbatement_boolean, 
			   
			@Description(shortDefinition="Date-related abatements (dateTime and period)")
			@OptionalParam(name="abatement-date")
			DateRangeParam theAbatement_date, 
			   
			@Description(shortDefinition="Abatement as a string")
			@OptionalParam(name="abatement-string")
			StringAndListParam theAbatement_string, 
			    
			@Description(shortDefinition="Date record was believed accurate")
			@OptionalParam(name="asserted-date")
			DateRangeParam theAsserted_date, 
			   
			@Description(shortDefinition="Person who asserts this condition")
			@OptionalParam(name="asserter", targetTypes={  } )
			ReferenceAndListParam theAsserter, 
			    
			@Description(shortDefinition="Anatomical location, if relevant")
			@OptionalParam(name="body-site")
			TokenAndListParam theBody_site, 
			   
			@Description(shortDefinition="The category of the condition")
			@OptionalParam(name="category")
			TokenAndListParam theCategory, 
			   
			@Description(shortDefinition="The clinical status of the condition")
			@OptionalParam(name="clinicalstatus")
			TokenAndListParam theClinicalstatus, 
			   
			@Description(shortDefinition="Code for the condition")
			@OptionalParam(name="code")
			TokenAndListParam theCode, 
			   
			@Description(shortDefinition="Encounter when condition first asserted")
			@OptionalParam(name="context", targetTypes={  } )
			ReferenceAndListParam theContext, 
			   
			@Description(shortDefinition="Manifestation/symptom")
			@OptionalParam(name="evidence")
			TokenAndListParam theEvidence, 
			  
			@Description(shortDefinition="A unique identifier of the condition record")
			@OptionalParam(name="identifier")
			TokenAndListParam theIdentifier, 
			   
			@Description(shortDefinition="Onsets as age or age range")
			@OptionalParam(name="onset-age")
			QuantityAndListParam theOnset_age, 
			   
			@Description(shortDefinition="Date related onsets (dateTime and Period)")
			@OptionalParam(name="onset-date")
			DateRangeParam theOnset_date, 
			   
			@Description(shortDefinition="Onsets as a string")
			@OptionalParam(name="onset-info")
			StringAndListParam theOnset_info, 
			  
			@Description(shortDefinition="Who has the condition?")
			@OptionalParam(name="patient", targetTypes={  } )
			ReferenceAndListParam thePatient, 
			   
			@Description(shortDefinition="The severity of the condition")
			@OptionalParam(name="severity")
			TokenAndListParam theSeverity, 
			   
			@Description(shortDefinition="Simple summary (disease specific)")
			@OptionalParam(name="stage")
			TokenAndListParam theStage, 
			   
			@Description(shortDefinition="Who has the condition?")
			@OptionalParam(name="subject", targetTypes={  } )
			ReferenceAndListParam theSubject, 
			 
			@IncludeParam(reverse=true)
			Set<Include> theRevIncludes,
			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
			@OptionalParam(name="_lastUpdated")
			DateRangeParam theLastUpdated, 
			 
			@IncludeParam(allow= {
						"Condition:asserter" ,
						"Condition:context" ,
						"Condition:patient" ,
						"Condition:subject" ,
						"*"
			}) 
			Set<Include> theIncludes,
			 			
			@Sort 
			SortSpec theSort,
			 			
			@ca.uhn.fhir.rest.annotation.Count
			Integer theCount
			
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
		paramMap.add("abatement-age", theAbatement_age);
		paramMap.add("abatement-boolean", theAbatement_boolean);
		paramMap.add("abatement-date", theAbatement_date);
		paramMap.add("abatement-string", theAbatement_string);
		paramMap.add("asserted-date", theAsserted_date);
		paramMap.add("asserter", theAsserter);
		paramMap.add("body-site", theBody_site);
		paramMap.add("category", theCategory);
		paramMap.add("clinicalstatus", theClinicalstatus);
		paramMap.add("code", theCode);
		paramMap.add("context", theContext);
		paramMap.add("evidence", theEvidence);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("onset-age", theOnset_age);
		paramMap.add("onset-date", theOnset_date);
		paramMap.add("onset-info", theOnset_info);
		paramMap.add("patient", thePatient);
		paramMap.add("severity", theSeverity);
		paramMap.add("stage", theStage);
		paramMap.add("subject", theSubject);
		
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Condition");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient");
		builder.recordOwnerReference("subject", null);
        builder.recordCodeRestriction("code", "code");
			
		
        builder.restriction("identifier", "Identifier", true, "identifier");
        builder.restriction("asserter", null, true, "asserter");
        builder.restriction("context", null, true, "context");
        builder.restriction("category", "CodeableConcept", true, "category");
        
		builder.restriction("abatement-age", "Quantity", true, "abatementAge", "abatementRange");
		builder.restriction("abatement-boolean", "Boolean", false, "abatementBoolean");
		builder.restriction("abatement-date", "DateTime", true, "abatementDateTime", "abatementPeriod");
		builder.restriction("abatement-string", "String", true, "abatementString");
		
		builder.restriction("body-site", "CodeableConcept", true, "bodySite");							
		builder.restriction("clinicalstatus", "code", true, "clinicalStatus");		
		builder.restriction("evidence", "CodeableConcept", true, "evidence.code");
		
		builder.restriction("onset-age", "Quantity", true, "onsetAge", "onsetRange");
		builder.restriction("onset-date", "DateTime", true, "onsetDateTime", "onsetPeriod");
		builder.restriction("onset-info", "String", true, "onsetString");
		builder.restriction("severity", "CodeableConcept", true, "severity");
		builder.restriction("stage", "CodeableConcept", true, "stage.summary");					
		
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Condition theCondition) {
		return super.createResource(theCondition);
	}
	
	@Override
	protected MethodOutcome create(Condition theCondition) throws AppException {

		Record record = newRecord("fhir/Condition");
		prepare(record, theCondition);
		// insert
		insertRecord(record, theCondition);

		processResource(record, theCondition);				
		
		return outcome("Condition", record, theCondition);

	}
	
	public Record init() { return newRecord("fhir/Condition"); }

	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Condition theCondition) {
		return super.updateResource(theId, theCondition);
	}
	
	@Override
	protected MethodOutcome update(@IdParam IdType theId, @ResourceParam Condition theCondition) throws AppException {
		Record record = fetchCurrent(theId);
		prepare(record, theCondition);		
		updateRecord(record, theCondition);		
		return outcome("Condition", record, theCondition);
	}

	public void prepare(Record record, Condition theCondition) throws AppException {
		// Set Record code and content
		String display = setRecordCodeByCodeableConcept(record, theCondition.getCode(), null);		
		
		record.name = display != null ? display : "Condition";
		
		// clean
		Reference subjectRef = theCondition.getSubject();
		if (cleanAndSetRecordOwner(record, subjectRef)) theCondition.setSubject(null);
		
		clean(theCondition);
 
	}
	
 
	@Override
	public void processResource(Record record, Condition p) {
		super.processResource(record, p);
		
		if (p.getSubject().isEmpty()) {
			p.getSubject().setReferenceElement(new IdType("Patient", record.owner.toString()));
			p.getSubject().setDisplay(record.ownerName);
		}
	}

}