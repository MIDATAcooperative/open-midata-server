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

package utils.fhir;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
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
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import models.Record;
import utils.access.pseudo.FhirPseudonymizer;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class QuestionnaireResponseResourceProvider extends RecordBasedResourceProvider<QuestionnaireResponse> implements IResourceProvider {

	public QuestionnaireResponseResourceProvider() {
		searchParamNameToPathMap.put("QuestionnaireResponse:author", "author");
		searchParamNameToPathMap.put("QuestionnaireResponse:based-on", "basedOn");
		searchParamNameToTypeMap.put("QuestionnaireResponse:based-on", Sets.create("CarePlan", "ServiceRequest"));
		
		searchParamNameToPathMap.put("QuestionnaireResponse:encounter", "encounter");
		searchParamNameToTypeMap.put("QuestionnaireResponse:encounter", Sets.create("Encounter"));
		
		searchParamNameToPathMap.put("QuestionnaireResponse:part-of", "partOf");
		searchParamNameToTypeMap.put("QuestionnaireResponse:part-of", Sets.create("Observation", "Procedure"));
		
		
		searchParamNameToPathMap.put("QuestionnaireResponse:parent", "parent");
		searchParamNameToPathMap.put("QuestionnaireResponse:patient", "subject");
		searchParamNameToTypeMap.put("QuestionnaireResponse:patient", Sets.create("Patient"));
		searchParamNameToPathMap.put("QuestionnaireResponse:questionnaire", "questionnaire");
		searchParamNameToTypeMap.put("QuestionnaireResponse:questionnaire", Sets.create("Questionnaire"));
		searchParamNameToPathMap.put("QuestionnaireResponse:source", "source");
		searchParamNameToTypeMap.put("QuestionnaireResponse:source", Sets.create("Practitioner", "Patient", "PractitionerRole", "RelatedPerson"));
		searchParamNameToPathMap.put("QuestionnaireResponse:subject", "subject");	
		
		registerSearches("QuestionnaireResponse", getClass(), "getQuestionnaireResponse");
		
		FhirPseudonymizer.forR4()
		  .reset("QuestionnaireResponse")
		  .pseudonymizeReference("QuestionnaireResponse", "source")
		  .pseudonymizeReference("QuestionnaireResponse", "author");
	}
	
	@Override
	public Class<QuestionnaireResponse> getResourceType() {
		return QuestionnaireResponse.class;
	}

	@Search()
	public Bundle getQuestionnaireResponse(
		
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
			   
 			@Description(shortDefinition="The author of the questionnaire response")
  			@OptionalParam(name="author", targetTypes={  } )
  			ReferenceAndListParam theAuthor, 
    
  			@Description(shortDefinition="When the questionnaire response was last changed")
  			@OptionalParam(name="authored")
  			DateAndListParam theAuthored, 
    
  			@Description(shortDefinition="Plan/proposal/order fulfilled by this questionnaire response")
  			@OptionalParam(name="based-on", targetTypes={  } )
  			ReferenceAndListParam theBased_on, 
    
  			@Description(shortDefinition="Encounter associated with the questionnaire response")
  			@OptionalParam(name="encounter", targetTypes={  } )
  			ReferenceAndListParam theEncounter, 
    
  			@Description(shortDefinition="The unique identifier for the questionnaire response")
  			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier,
    
  			@Description(shortDefinition="Procedure or observation this questionnaire response was performed as a part of")
  			@OptionalParam(name="part-of", targetTypes={  } )
  			ReferenceAndListParam thePart_of, 
    
  			@Description(shortDefinition="The patient that is the subject of the questionnaire response")
  			@OptionalParam(name="patient", targetTypes={  } )
  			ReferenceAndListParam thePatient, 
    
  			@Description(shortDefinition="The questionnaire the answers are provided for")
  			@OptionalParam(name="questionnaire" )
  			TokenAndListParam theQuestionnaire, 
    
 			@Description(shortDefinition="The individual providing the information reflected in the questionnaire respose")
 			@OptionalParam(name="source", targetTypes={  } )
 			ReferenceAndListParam theSource, 
   
 			@Description(shortDefinition="The status of the questionnaire response")
 			@OptionalParam(name="status")
 			TokenAndListParam theStatus,
   
 			@Description(shortDefinition="The subject of the questionnaire response")
 			@OptionalParam(name="subject", targetTypes={  } )
 			ReferenceAndListParam theSubject,   		
 
 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= {
 					"QuestionnaireResponse:author" ,
 					"QuestionnaireResponse:based-on" ,
 					"QuestionnaireResponse:encounter" ,
 					"QuestionnaireResponse:part-of" ,
 					"QuestionnaireResponse:patient" ,
 					"QuestionnaireResponse:questionnaire" ,
 					"QuestionnaireResponse:source" ,
 					"QuestionnaireResponse:subject" ,
 					"*"
 			}) 
 			Set<Include> theIncludes,
			
			@Sort SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount,
			
			@OptionalParam(name="_page")
			StringParam _page,
			
			RequestDetails theDetails

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", theId);
		paramMap.add("_language", theResourceLanguage);
		
		// paramMap.add("_has", theHas);
		paramMap.add("author", theAuthor);
		paramMap.add("authored", theAuthored);
		paramMap.add("based-on", theBased_on);
		paramMap.add("encounter", theEncounter);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("part-of", thePart_of);
		paramMap.add("patient", thePatient);
		paramMap.add("questionnaire", theQuestionnaire);
		paramMap.add("source", theSource);
		paramMap.add("status", theStatus);
		paramMap.add("subject", theSubject);
		
		paramMap.setRevIncludes(theRevIncludes);
		paramMap.setLastUpdated(theLastUpdated);
		paramMap.setIncludes(theIncludes);
		paramMap.setSort(theSort);
		paramMap.setCount(theCount);
		paramMap.setFrom(_page != null ? _page.getValue() : null);

		return searchBundle(paramMap, theDetails);
		
	}

	public List<Record> searchRaw(SearchParameterMap params) throws AppException {
		ExecutionInfo info = info();

		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/QuestionnaireResponse");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", "subject");
				
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		if (!builder.recordOwnerReference("subject", null, "subject")) builder.restriction("subject", true, null, "subject");
				
		builder.restriction("authored", true, QueryBuilder.TYPE_DATETIME, "authored");
		builder.restriction("author", true, null, "author");
		builder.restriction("based-on", true, null, "basedOn");
		builder.restriction("encounter", true, "Encounter", "encounter");
		builder.restriction("part-of", true, null, "partOf");
		builder.restriction("parent", true, null, "parent");
		builder.restriction("questionnaire", true, QueryBuilder.TYPE_CANONICAL, "questionnaire");
		builder.restriction("source", true, null, "source");
		builder.restriction("status", true, QueryBuilder.TYPE_CODE, "status");		
				
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam QuestionnaireResponse theQuestionnaireResponse) {
		return super.createResource(theQuestionnaireResponse);
	}
		
	@Override
	public String getRecordFormat() {	
		return "fhir/QuestionnaireResponse";
	}	

	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam QuestionnaireResponse theQuestionnaireResponse) {
		return super.updateResource(theId, theQuestionnaireResponse);
	}
	

	public void prepare(Record record, QuestionnaireResponse theQuestionnaireResponse) throws AppException {
		// Set Record code and content
		

		List<Coding> codings = new ArrayList<Coding>();
		for (Extension ext : theQuestionnaireResponse.getExtensionsByUrl("http://midata.coop/extensions/response-code")) {
			  Coding coding = (Coding) ext.getValue();			 
			  if (coding == null) throw new InvalidRequestException("Missing coding in extension for response-code");
			  codings.add(coding);
		}
		if (codings.isEmpty()) {
			IIdType questions = FHIRTools.convertToId(theQuestionnaireResponse.getQuestionnaireElement());
			if (questions != null && !questions.isEmpty()) {				
				Questionnaire q = (Questionnaire) FHIRServlet.myProviders.get("Questionnaire").getResourceById(questions);
				codings = q.getCode();
				if (codings != null) {
				  for (Coding c : codings) {			
					theQuestionnaireResponse.addExtension().setUrl("http://midata.coop/extensions/response-code").setValue(c);
				  }
				}
			}
		}
		
		setRecordCodeByCodings(record, codings, "QuestionnaireResponse");
						
		record.name = "Questionnaire Response";
				
		if (cleanAndSetRecordOwner(record, theQuestionnaireResponse.getSubject())) theQuestionnaireResponse.setSubject(null);
		clean(theQuestionnaireResponse);

	}

	/*
	 * @Delete() public void deleteObservation(@IdParam IdType theId) { Record
	 * record = fetchCurrent(theId);
	 * RecordManager.instance.deleteRecord(info().executorId, info().targetAPS,
	 * record); }
	 */
 
	@Override
	public void processResource(Record record, QuestionnaireResponse p) throws AppException {
		super.processResource(record, p);
		if (p.getSubject().isEmpty()) {
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}

	@Override
	public void clean(QuestionnaireResponse theQuestionnaireResponse) {
		
		super.clean(theQuestionnaireResponse);
	}

	@Override
	protected void convertToR4(Object in) {
		// No action
		
	}

}