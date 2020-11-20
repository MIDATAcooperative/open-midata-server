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

package utils.fhir;

import java.util.List;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Goal;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;

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
import models.Record;
import utils.access.pseudo.FhirPseudonymizer;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class GoalResourceProvider extends RecordBasedResourceProvider<Goal> implements IResourceProvider {

	public GoalResourceProvider() {		
		searchParamNameToPathMap.put("Goal:patient", "subject");
		searchParamNameToTypeMap.put("Goal:patient", Sets.create("Patient"));		
		searchParamNameToPathMap.put("Goal:subject", "subject");	
		
		registerSearches("Goal", getClass(), "getGoal");
		
		FhirPseudonymizer.forR4()
		  .reset("Goal")	
		  .pseudonymizeReference("Goal", "expressedBy")
		  .pseudonymizeReference("Goal", "note", "authorReference");
	}
	
	@Override
	public Class<Goal> getResourceType() {
		return Goal.class;
	}

	@Search()
	public Bundle getGoal(
			@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,

			@Description(shortDefinition = "The resource language") @OptionalParam(name = "_language") StringAndListParam theResourceLanguage,

			
  			@Description(shortDefinition="in-progress | improving | worsening | no-change | achieved | sustaining | not-achieved | no-progress | not-attainable")
  			@OptionalParam(name="achievement-status")
  			TokenAndListParam theAchievement_status,
    
  			@Description(shortDefinition="E.g. Treatment, dietary, behavioral, etc.")
  			@OptionalParam(name="category")
  			TokenAndListParam theCategory,
    
  			@Description(shortDefinition="External Ids for this goal")
  			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier,
    
  			@Description(shortDefinition="proposed | planned | accepted | active | on-hold | completed | cancelled | entered-in-error | rejected")
  			@OptionalParam(name="lifecycle-status")
  			TokenAndListParam theLifecycle_status,
    
  			@Description(shortDefinition="Who this goal is intended for")
  			@OptionalParam(name="patient", targetTypes={  } )
  			ReferenceAndListParam thePatient, 
    
  			@Description(shortDefinition="When goal pursuit begins")
  			@OptionalParam(name="start-date")
  			DateAndListParam theStart_date, 
    
  			@Description(shortDefinition="Who this goal is intended for")
  			@OptionalParam(name="subject", targetTypes={  } )
  			ReferenceAndListParam theSubject, 
    
  			@Description(shortDefinition="Reach goal on or before")
  			@OptionalParam(name="target-date")
  			DateAndListParam theTarget_date, 
   			 
 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= {
 					"Goal:patient" ,
 					"Goal:subject" ,
 					"*"
 			}) 
 			Set<Include> theIncludes,
						
			@Sort 
			SortSpec theSort,
			 			
			@ca.uhn.fhir.rest.annotation.Count
			Integer theCount,
			
			@OptionalParam(name="_page")
			StringParam _page,
			
			RequestDetails theDetails
		

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", theId);
		paramMap.add("_language", theResourceLanguage);
		
		paramMap.add("achievement-status", theAchievement_status);
		paramMap.add("category", theCategory);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("lifecycle-status", theLifecycle_status);
		paramMap.add("patient", thePatient);
		paramMap.add("start-date", theStart_date);
		paramMap.add("subject", theSubject);
		paramMap.add("target-date", theTarget_date);
	
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Goal");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", "subject");
		      		
		builder.restriction("category", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "category");
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		
		if (!builder.recordOwnerReference("subject", null, "subject")) builder.restriction("subject", true, null, "subject");		
		
		builder.restriction("achievement-status", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "achievementStatus");
		builder.restriction("lifecycle-status", true, QueryBuilder.TYPE_CODE, "lifecycleStatus|status");
		builder.restriction("start-date", true, QueryBuilder.TYPE_DATETIME, "startDate");
		builder.restriction("target-date", true, QueryBuilder.TYPE_DATETIME, "target.dueDate");
		
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Goal theGoal) {
		return super.createResource(theGoal);
	}
	
	@Override
	public String getRecordFormat() {	
		return "fhir/Goal";
	}
			

	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Goal theGoal) {
		return super.updateResource(theId, theGoal);
	}
		

	public void prepare(Record record, Goal theGoal) throws AppException {
		// Set Record code and content
		String display = setRecordCodeByCodings(record, null, "Goal");					
		record.name = FHIRTools.getStringFromCodeableConcept(theGoal.getDescription(), "Goal");
		
		// clean
		Reference subjectRef = theGoal.getSubject();
		if (cleanAndSetRecordOwner(record, subjectRef)) theGoal.setSubject(null);
		
		clean(theGoal);
 
	}

	
	@Override
	public void processResource(Record record, Goal p) throws AppException {
		super.processResource(record, p);
		
		if (p.getSubject().isEmpty()) {
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}

	@Override
	protected void convertToR4(Object in) {
		FHIRVersionConvert.rename(in, "status", "lifecycleStatus");
		
	}
	

}