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
		
		registerSearches("Condition", getClass(), "getCondition");
	}
	
	@Override
	public Class<Condition> getResourceType() {
		return Condition.class;
	}

	@Search()
	public List<IBaseResource> getCondition(
			@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,

			@Description(shortDefinition = "The resource language") @OptionalParam(name = "_language") StringAndListParam theResourceLanguage,

			
			@Description(shortDefinition="Abatement as age or age range")
			@OptionalParam(name="abatement-age")
			QuantityAndListParam theAbatement_age, 
			    
			@Description(shortDefinition="Abatement boolean (boolean is true or non-boolean values are present)")
			@OptionalParam(name="abatement-boolean")
			TokenAndListParam theAbatement_boolean, 
			   
			@Description(shortDefinition="Date-related abatements (dateTime and period)")
			@OptionalParam(name="abatement-date")
			DateAndListParam theAbatement_date, 
			   
			@Description(shortDefinition="Abatement as a string")
			@OptionalParam(name="abatement-string")
			StringAndListParam theAbatement_string, 
			    
			@Description(shortDefinition="Date record was believed accurate")
			@OptionalParam(name="asserted-date")
			DateAndListParam theAsserted_date, 
			   
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
			DateAndListParam theOnset_date, 
			   
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
		builder.recordOwnerReference("patient", "Patient", "subject");				
        builder.recordCodeRestriction("code", "code");
			
		
        builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
        builder.restriction("asserter", true, null, "asserter");
        
        if (!builder.recordOwnerReference("subject", null, "subject")) builder.restriction("subject", true, null, "subject");
        
        builder.restriction("context", true, null, "context");
        builder.restriction("category", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "category");
        
		builder.restriction("abatement-age", true, QueryBuilder.TYPE_AGE_OR_RANGE, "abatement");
		//builder.restriction("abatement-boolean", false, QueryBuilder.TYPE_BOOLEAN, "abatementBoolean");
		builder.restriction("abatement-date", true, QueryBuilder.TYPE_DATETIME_OR_PERIOD, "abatement");
		builder.restriction("abatement-string", true, QueryBuilder.TYPE_STRING, "abatementString");
		
		builder.restriction("body-site", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "bodySite");							
		builder.restriction("clinicalstatus", true, QueryBuilder.TYPE_CODE, "clinicalStatus");		
		builder.restriction("evidence", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "evidence.code");
		
		builder.restriction("onset-age", true, QueryBuilder.TYPE_AGE_OR_RANGE, "onset");
		builder.restriction("onset-date", true, QueryBuilder.TYPE_DATETIME_OR_PERIOD, "onset");
		builder.restriction("onset-info", true, QueryBuilder.TYPE_STRING, "onsetString");
		builder.restriction("severity", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "severity");
		builder.restriction("stage", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "stage.summary");					
		
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Condition theCondition) {
		return super.createResource(theCondition);
	}
		
	
	public Record init() { return newRecord("fhir/Condition"); }

	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Condition theCondition) {
		return super.updateResource(theId, theCondition);
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
	public void processResource(Record record, Condition p) throws AppException {
		super.processResource(record, p);
		
		if (p.getSubject().isEmpty()) {
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}

}