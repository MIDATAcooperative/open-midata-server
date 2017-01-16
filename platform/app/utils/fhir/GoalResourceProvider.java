package utils.fhir;

import java.util.List;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Goal;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
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
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import models.Record;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class GoalResourceProvider extends ResourceProvider<Goal> implements IResourceProvider {

	public GoalResourceProvider() {		
		searchParamNameToPathMap.put("Goal:patient", "subject");
		searchParamNameToTypeMap.put("Goal:patient", Sets.create("Patient"));		
		searchParamNameToPathMap.put("Goal:subject", "subject");	
		
		registerSearches("Goal", getClass(), "getGoal");
	}
	
	@Override
	public Class<Goal> getResourceType() {
		return Goal.class;
	}

	@Search()
	public List<IBaseResource> getGoal(
			@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,

			@Description(shortDefinition = "The resource language") @OptionalParam(name = "_language") StringAndListParam theResourceLanguage,

			/*
			@Description(shortDefinition = "Search the contents of the resource's data using a fulltext search") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT) StringAndListParam theFtContent,

			@Description(shortDefinition = "Search the contents of the resource's narrative using a fulltext search") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_TEXT) StringAndListParam theFtText,

			@Description(shortDefinition = "Search for resources which have the given tag") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_TAG) TokenAndListParam theSearchForTag,

			@Description(shortDefinition = "Search for resources which have the given security labels") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY) TokenAndListParam theSearchForSecurity,

			@Description(shortDefinition = "Search for resources which have the given profile") @OptionalParam(name = ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE) UriAndListParam theSearchForProfile,
			*/
			@Description(shortDefinition="E.g. Treatment, dietary, behavioral, etc.")
			@OptionalParam(name="category")
			TokenAndListParam theCategory, 
			  
			@Description(shortDefinition="External Ids for this goal")
			@OptionalParam(name="identifier")
			TokenAndListParam theIdentifier, 
			    
			@Description(shortDefinition="Who this goal is intended for")
			@OptionalParam(name="patient", targetTypes={  } )
			ReferenceAndListParam thePatient, 
			   
			@Description(shortDefinition="proposed | planned | accepted | rejected | in-progress | achieved | sustaining | on-hold | cancelled | on-target | ahead-of-target | behind-target | entered-in-error")
			@OptionalParam(name="status")
			TokenAndListParam theStatus, 
			    
			@Description(shortDefinition="Who this goal is intended for")
			@OptionalParam(name="subject", targetTypes={  } )
			ReferenceAndListParam theSubject, 
			    
			@Description(shortDefinition="Reach goal on or before")
			@OptionalParam(name="targetdate")
			DateRangeParam theTargetdate, 
			  
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
			Integer theCount
		

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", theId);
		paramMap.add("_language", theResourceLanguage);
		
		paramMap.add("category", theCategory);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("patient", thePatient);
		paramMap.add("status", theStatus);
		paramMap.add("subject", theSubject);
		paramMap.add("targetdate", theTargetdate);
	
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Goal");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient");
		      		
		builder.restriction("category", true, "CodeableConcept", "category");
		builder.restriction("identifier", true, "Identifier", "identifier");
		
		if (!builder.recordOwnerReference("subject", null)) builder.restriction("subject", true, null, "subject");		
		
		builder.restriction("status", true, "code", "status");
		builder.restriction("targetdate", true, "DateTime", "targetDate");				
		
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Goal theGoal) {
		return super.createResource(theGoal);
	}
	
	@Override
	protected MethodOutcome create(Goal theGoal) throws AppException {

		Record record = newRecord("fhir/Goal");
		prepare(record, theGoal);
		// insert
		insertRecord(record, theGoal);

		processResource(record, theGoal);				
		
		return outcome("Goal", record, theGoal);

	}
	
	public Record init() { return newRecord("fhir/Goal"); }

	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Goal theGoal) {
		return super.updateResource(theId, theGoal);
	}
	
	@Override
	protected MethodOutcome update(@IdParam IdType theId, @ResourceParam Goal theGoal) throws AppException {
		Record record = fetchCurrent(theId);
		prepare(record, theGoal);		
		updateRecord(record, theGoal);		
		return outcome("Goal", record, theGoal);
	}

	public void prepare(Record record, Goal theGoal) throws AppException {
		// Set Record code and content
		String display = setRecordCodeByCodeableConcept(record, null, "Goal");					
		record.name = FHIRTools.getStringFromCodeableConcept(theGoal.getDescription(), "Goal");
		
		// clean
		Reference subjectRef = theGoal.getSubject();
		if (cleanAndSetRecordOwner(record, subjectRef)) theGoal.setSubject(null);
		
		clean(theGoal);
 
	}

	
	@Override
	public void processResource(Record record, Goal p) {
		super.processResource(record, p);
		
		if (p.getSubject().isEmpty()) {
			p.getSubject().setReferenceElement(new IdType("Patient", record.owner.toString()));
			p.getSubject().setDisplay(record.ownerName);
		}
	}
	

}