package utils.fhir;

import java.util.List;
import java.util.Set;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse;
import org.hl7.fhir.dstu3.model.Reference;
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
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import models.ContentInfo;
import models.MidataId;
import models.Record;
import utils.auth.ExecutionInfo;
import utils.exceptions.AppException;

public class QuestionnaireResponseResourceProvider extends ResourceProvider<QuestionnaireResponse> implements IResourceProvider {

	@Override
	public Class<QuestionnaireResponse> getResourceType() {
		return QuestionnaireResponse.class;
	}

	@Search()
	public List<QuestionnaireResponse> getQuestionnaireResponse(
		
			@Description(shortDefinition="The resource identity")
			@OptionalParam(name="_id")
			StringAndListParam theId, 
			  
			@Description(shortDefinition="The resource language")
			@OptionalParam(name="_language")
			StringAndListParam theResourceLanguage, 
			/*
			@Description(shortDefinition="Return resources linked to by the given target")
			@OptionalParam(name="_has")
			HasAndListParam theHas, 
			 */ 
			   
			@Description(shortDefinition="The status of the questionnaire response")
			@OptionalParam(name="status")
			TokenAndListParam theStatus, 
			   
			@Description(shortDefinition="When the questionnaire was authored")
			@OptionalParam(name="authored")
			DateRangeParam theAuthored, 
			   
			@Description(shortDefinition="The subject of the questionnaire")
			@OptionalParam(name="subject", targetTypes={  } )
			ReferenceAndListParam theSubject, 
			   
			@Description(shortDefinition="The author of the questionnaire")
			@OptionalParam(name="author", targetTypes={  } )
			ReferenceAndListParam theAuthor, 
			  
			@Description(shortDefinition="The questionnaire the answers are provided for")
			@OptionalParam(name="questionnaire", targetTypes={  } )
			ReferenceAndListParam theQuestionnaire, 
			   
			@Description(shortDefinition="Encounter or episode during which questionnaire was authored")
			@OptionalParam(name="context", targetTypes={  } )
			ReferenceAndListParam theContext, 
			   
			@Description(shortDefinition="The patient that is the subject of the questionnaire")
			@OptionalParam(name="patient", targetTypes={  Patient.class   } )
			ReferenceAndListParam thePatient, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="source", targetTypes={  } )
			ReferenceAndListParam theSource, 
			   
			@Description(shortDefinition="The unique identifier for the questionnaire response")
			@OptionalParam(name="identifier")
			TokenAndListParam theIdentifier, 
			   
			@Description(shortDefinition="Plan/proposal/order fulfilled by this questionnaire response")
			@OptionalParam(name="based-on", targetTypes={  } )
			ReferenceAndListParam theBased_on, 
			   
			@Description(shortDefinition="Procedure or observation this questionnaire response is part of")
			@OptionalParam(name="parent", targetTypes={  } )
			ReferenceAndListParam theParent, 
			 
			@IncludeParam(reverse=true)
			Set<Include> theRevIncludes,
			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
			@OptionalParam(name="_lastUpdated")
			DateRangeParam theLastUpdated, 
			 
			@IncludeParam(allow= {
					"QuestionnaireResponse:author" ,
					"QuestionnaireResponse:based-on" ,
					"QuestionnaireResponse:context" ,
					"QuestionnaireResponse:parent" ,
					"QuestionnaireResponse:patient" ,
					"QuestionnaireResponse:questionnaire" ,
					"QuestionnaireResponse:source" ,
					"QuestionnaireResponse:subject" ,
					"*"
			}) 
			Set<Include> theIncludes,
			
			@Sort SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", theId);
		paramMap.add("_language", theResourceLanguage);
		
		// paramMap.add("_has", theHas);
		paramMap.add("status", theStatus);
		paramMap.add("authored", theAuthored);
		paramMap.add("subject", theSubject);
		paramMap.add("author", theAuthor);
		paramMap.add("questionnaire", theQuestionnaire);
		paramMap.add("context", theContext);
		paramMap.add("patient", thePatient);
		paramMap.add("source", theSource);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("based-on", theBased_on);
		paramMap.add("parent", theParent);
		
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

		/*
		Set<String> codes = builder.tokensToCodeSystemStrings("code");
		if (codes != null) {
			query.putAccount("code", codes);
			builder.restriction("code", "code", "CodeableConcept", false);
		} else {
			builder.restriction("code", "code", "CodeableConcept", true);
		}
        */
		
		builder.restriction("authored", "Date", true, "authored");
		builder.restriction("author", null, true, "author");
		builder.restriction("based-on", null, true, "basedOn");
		builder.restriction("context", null, true, "context");
		builder.restriction("identifier", "Identifier", true, "identifier");
		builder.restriction("parent", null, true, "parent");
		builder.restriction("questionnaire", "Questionnaire", true, "questionnaire");
		builder.restriction("source", null, true, "source");
		builder.restriction("status", "code", true, "status");
		builder.restriction("subject", null, true, "subject");
				
		return query.execute(info);
	}

	@Create
	public MethodOutcome createQuestionnaireResponse(@ResourceParam QuestionnaireResponse theQuestionnaireResponse) {

		Record record = newRecord("fhir/QuestionnaireResponse");
		prepare(record, theQuestionnaireResponse);
		// insert
		insertRecord(record, theQuestionnaireResponse);

		processResource(record, theQuestionnaireResponse);				
		
		return outcome("QuestionnaireResponse", record, theQuestionnaireResponse);

	}
	
	public Record init() { return newRecord("fhir/QuestionnaireResponse"); }

	@Update
	public MethodOutcome updateQuestionnaireResponse(@IdParam IdType theId, @ResourceParam QuestionnaireResponse theQuestionnaireResponse) {
		Record record = fetchCurrent(theId);
		prepare(record, theQuestionnaireResponse);		
		updateRecord(record, theQuestionnaireResponse);		
		return outcome("QuestionnaireResponse", record, theQuestionnaireResponse);
	}

	public void prepare(Record record, QuestionnaireResponse theQuestionnaireResponse) {
		// Set Record code and content
		
		try {
			ContentInfo.setRecordCodeAndContent(record, null, "QuestionnaireResponse");
		} catch (AppException e) {
			throw new InternalErrorException(e);
		}
		
		record.name = "Questionnaire Response";

		
		// clean
		Reference subjectRef = theQuestionnaireResponse.getSubject();
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
		
		if (cleanSubject) theQuestionnaireResponse.setSubject(null);
		clean(theQuestionnaireResponse);

	}

	/*
	 * @Delete() public void deleteObservation(@IdParam IdType theId) { Record
	 * record = fetchCurrent(theId);
	 * RecordManager.instance.deleteRecord(info().executorId, info().targetAPS,
	 * record); }
	 */
 
	@Override
	public void processResource(Record record, QuestionnaireResponse p) {
		super.processResource(record, p);
		if (p.getSubject().isEmpty()) {
			p.getSubject().setReferenceElement(new IdType("Patient", record.owner.toString()));
			p.getSubject().setDisplay(record.ownerName);
		}
	}

	@Override
	public void clean(QuestionnaireResponse theQuestionnaireResponse) {
		
		super.clean(theQuestionnaireResponse);
	}

}