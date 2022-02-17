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

package utils.fhir_stu3;

import java.util.List;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Questionnaire;

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
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import models.Record;
import utils.context.AccessContext;
import utils.exceptions.AppException;

public class QuestionnaireResourceProvider extends RecordBasedResourceProvider<Questionnaire> implements IResourceProvider {

	public QuestionnaireResourceProvider() {
		
		registerSearches("Questionnaire", getClass(), "getQuestionnaire");
	}
	
	@Override
	public Class<Questionnaire> getResourceType() {
		return Questionnaire.class;
	}

	@Search()
	public Bundle getQuestionnaire(
		
			@Description(shortDefinition="The resource identity")
			@OptionalParam(name="_id")
			StringAndListParam theId, 
			  
			@Description(shortDefinition="The resource language")
			@OptionalParam(name="_language")
			StringAndListParam theResourceLanguage, 
			
			@Description(shortDefinition="A code that corresponds to one of its items in the questionnaire")
			@OptionalParam(name="code")
			TokenAndListParam theCode, 
			    
			@Description(shortDefinition="The questionnaire publication date")
			@OptionalParam(name="date")
			DateRangeParam theDate, 
			   
			@Description(shortDefinition="The description of the questionnaire")
			@OptionalParam(name="description")
			StringAndListParam theDescription, 
			   
			@Description(shortDefinition="The time during which the questionnaire is intended to be in use")
			@OptionalParam(name="effective")
			DateAndListParam theEffective, 
			   
			@Description(shortDefinition="External identifier for the questionnaire")
			@OptionalParam(name="identifier")
			TokenAndListParam theIdentifier, 
			   
			@Description(shortDefinition="Intended jurisdiction for the questionnaire")
			@OptionalParam(name="jurisdiction")
			TokenAndListParam theJurisdiction, 
			   
			@Description(shortDefinition="Computationally friendly name of the questionnaire")
			@OptionalParam(name="name")
			StringAndListParam theName, 
			   
			@Description(shortDefinition="Name of the publisher of the questionnaire")
			@OptionalParam(name="publisher")
			StringAndListParam thePublisher, 
			  
			@Description(shortDefinition="The current status of the questionnaire")
			@OptionalParam(name="status")
			TokenAndListParam theStatus, 
			 
			@Description(shortDefinition="The human-friendly name of the questionnaire")
			@OptionalParam(name="title")
			StringAndListParam theTitle, 
			  
			@Description(shortDefinition="The uri that identifies the questionnaire")
			@OptionalParam(name="url")
			UriAndListParam theUrl, 
			  
			@Description(shortDefinition="The business version of the questionnaire")
			@OptionalParam(name="version")
			TokenAndListParam theVersion, 
			
			@IncludeParam(reverse=true)
			Set<Include> theRevIncludes,
			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
			@OptionalParam(name="_lastUpdated")
			DateRangeParam theLastUpdated, 
			 
			@IncludeParam(allow= {
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
		
		paramMap.add("code", theCode);
		paramMap.add("date", theDate);
		paramMap.add("description", theDescription);
		paramMap.add("effective", theEffective);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("jurisdiction", theJurisdiction);
		paramMap.add("name", theName);
		paramMap.add("publisher", thePublisher);
		paramMap.add("status", theStatus);
		paramMap.add("title", theTitle);
		paramMap.add("url", theUrl);
		paramMap.add("version", theVersion);
		
		
		paramMap.setRevIncludes(theRevIncludes);
		paramMap.setLastUpdated(theLastUpdated);
		paramMap.setIncludes(theIncludes);
		paramMap.setSort(theSort);
		paramMap.setCount(theCount);
		paramMap.setFrom(_page != null ? _page.getValue() : null);

		return searchBundle(paramMap, theDetails);
		
	}

	public List<Record> searchRaw(SearchParameterMap params) throws AppException {
		AccessContext info = info();

		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Questionnaire");

		builder.handleIdRestriction();
						
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("code", true, QueryBuilder.TYPE_CODING, "item.code");	
		builder.restriction("date", true, QueryBuilder.TYPE_DATETIME, "date");	
		builder.restriction("description", true, QueryBuilder.TYPE_MARKDOWN, "description");
		builder.restriction("effective", true, QueryBuilder.TYPE_PERIOD, "effectivePeriod");
		builder.restriction("jurisdiction", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "jurisdiction");	
		builder.restriction("name", true, QueryBuilder.TYPE_STRING, "name");	
        builder.restriction("publisher", true, QueryBuilder.TYPE_STRING, "publisher");			
        builder.restriction("status", true, QueryBuilder.TYPE_CODE, "status");			
		builder.restriction("title", true, QueryBuilder.TYPE_STRING, "title");	
	    builder.restriction("url", true, QueryBuilder.TYPE_URI, "url");
		builder.restriction("version", true, QueryBuilder.TYPE_STRING, "version");						
				
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Questionnaire theQuestionnaire) {
		return super.createResource(theQuestionnaire);
	}
		
	@Override
	public String getRecordFormat() {	
		return "fhir/Questionnaire";
	}
	

	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Questionnaire theQuestionnaire) {
		return super.updateResource(theId, theQuestionnaire);
	}
	

	public void prepare(Record record, Questionnaire theQuestionnaire) throws AppException {
		// Set Record code and content
		String display = setRecordCodeByCodings(record, theQuestionnaire.getCode(), "Questionnaire");			
				
		record.name = theQuestionnaire.getTitle();
		if (record.name == null) record.name = "Questionnaire";
						
		clean(theQuestionnaire);

	}
	
 
	@Override
	public void processResource(Record record, Questionnaire p) throws AppException {
		super.processResource(record, p);
		
	}

	@Override
	public void clean(Questionnaire theQuestionnaire) {
		
		super.clean(theQuestionnaire);
	}

}