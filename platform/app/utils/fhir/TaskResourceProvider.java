package utils.fhir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Communication;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Task;
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
import ca.uhn.fhir.rest.param.HasAndListParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import controllers.Circles;
import models.Consent;
import models.ContentInfo;
import models.MidataId;
import models.Record;
import utils.ErrorReporter;
import utils.access.RecordManager;
import utils.auth.ExecutionInfo;
import utils.exceptions.AppException;

public class TaskResourceProvider extends ResourceProvider<Task> implements IResourceProvider {

	@Override
	public Class<Task> getResourceType() {
		return Task.class;
	}

	@Search()
	public List<Task> getTask(
			@Description(shortDefinition="The resource identity")
			@OptionalParam(name="_id")
			StringAndListParam theId, 
			  
			@Description(shortDefinition="The resource language")
			@OptionalParam(name="_language")
			StringAndListParam theResourceLanguage, 
			  
			@Description(shortDefinition="Search the contents of the resource's data using a fulltext search")
			@OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT)
			StringAndListParam theFtContent, 
			 
			@Description(shortDefinition="Search the contents of the resource's narrative using a fulltext search")
			@OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_TEXT)
			StringAndListParam theFtText, 
			 
			@Description(shortDefinition="Search for resources which have the given tag")
			@OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_TAG)
			TokenAndListParam theSearchForTag, 
			 
			@Description(shortDefinition="Search for resources which have the given security labels")
			@OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY)
			TokenAndListParam theSearchForSecurity, 
			  
			@Description(shortDefinition="Search for resources which have the given profile")
			@OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE)
			UriAndListParam theSearchForProfile, 
			  
			/*
			@Description(shortDefinition="Return resources linked to by the given target")
			@OptionalParam(name="_has")
			HasAndListParam theHas, 
			 */ 
			   
			@Description(shortDefinition="Search for a task instance by its business identifier")
			@OptionalParam(name="identifier")
			TokenAndListParam theIdentifier, 
			   
			@Description(shortDefinition="Search by task code")
			@OptionalParam(name="code")
			TokenAndListParam theCode, 
			    
			@Description(shortDefinition="Search by recommended type of performer (e.g., Requester, Performer, Scheduler).")
			@OptionalParam(name="performer")
			TokenAndListParam thePerformer, 
			   
			@Description(shortDefinition="Search by task priority")
			@OptionalParam(name="priority")
			TokenAndListParam thePriority, 
			   
			@Description(shortDefinition="Search by task status")
			@OptionalParam(name="status")
			TokenAndListParam theStatus, 
			    
			@Description(shortDefinition="Search by status reason")
			@OptionalParam(name="statusreason")
			TokenAndListParam theStatusreason, 
			    
			@Description(shortDefinition="Search by task focus")
			@OptionalParam(name="focus", targetTypes={  } )
			ReferenceAndListParam theFocus, 
			 
			/*
			@Description(shortDefinition="Search by task definition")
			@OptionalParam(name="definition")
			UriAndListParam theDefinition, 
			*/  			   			
			   
			@Description(shortDefinition="Search by last modification date")
			@OptionalParam(name="modified")
			DateRangeParam theModified, 
			   
			@Description(shortDefinition="Search by task owner")
			@OptionalParam(name="owner", targetTypes={  } )
			ReferenceAndListParam theOwner, 
			  
			@Description(shortDefinition="Search by task requester")
			@OptionalParam(name="requester", targetTypes={  } )
			ReferenceAndListParam theRequester, 
			   
			/*
			@Description(shortDefinition="Search by parent task")
			@OptionalParam(name="parent", targetTypes={  } )
			ReferenceAndListParam theParent, 
			  */
			  
			@Description(shortDefinition="Search by patient")
			@OptionalParam(name="patient", targetTypes={  Patient.class   } )
			ReferenceAndListParam thePatient, 
			   
			/*
			@Description(shortDefinition="Search by task stage")
			@OptionalParam(name="stage")
			TokenAndListParam theStage, 
			 */
			@IncludeParam(reverse=true)
			Set<Include> theRevIncludes,
			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
			@OptionalParam(name="_lastUpdated")
			DateRangeParam theLastUpdated, 
			 
			@IncludeParam(allow= {
						"Task:focus" ,
						"Task:owner" ,
						"Task:parent" ,
						"Task:patient" ,
						"Task:requester" ,
						"*"
			}) 
			
			Set<Include> theIncludes,
			 									
			@Sort SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", theId);
		paramMap.add("_language", theResourceLanguage);
		
	    //paramMap.add("_has", theHas);
        paramMap.add("identifier", theIdentifier);
        paramMap.add("code", theCode);
        paramMap.add("performer", thePerformer);
        paramMap.add("priority", thePriority);
		paramMap.add("status", theStatus);
		paramMap.add("statusreason", theStatusreason);
		paramMap.add("focus", theFocus);
		//paramMap.add("definition", theDefinition);
		
		paramMap.add("modified", theModified);
		paramMap.add("owner", theOwner);
		paramMap.add("requester", theRequester);
		//paramMap.add("parent", theParent);
		paramMap.add("patient", thePatient);
		//paramMap.add("stage", theStage);
	
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Task");
		builder.recordOwnerReference("patient", "Patient");
		builder.recordCodeRestriction("code", "code");
		
		builder.restriction("identifier", "Identifier", true, "identifier");
		builder.restriction("performer", "CodeableConcept", true, "performerType");        
        builder.restriction("priority", "code", false, "priority");
        builder.restriction("status", "code", false, "status");
		builder.restriction("statusreason", "CodeableConcept", true, "statusReason");
		builder.restriction("focus", null, true, "focus");
		//builder.restriction("definition", type, indexing, paths);
		
		builder.restriction("modified", "date", true, "lastModified");
		builder.restriction("owner", null, true, "owner");
		builder.restriction("requester", null, true, "requester.agent");
		
		//paramMap.add("parent", theParent);		
		//paramMap.add("stage", theStage);
							   
		return query.execute(info);
	}

	@Create
	public MethodOutcome createTask(@ResourceParam Task theTask) throws AppException {

		Record record = newRecord("fhir/Task");
		
		prepare(record, theTask);		
		// insert
		insertRecord(record, theTask);
        shareRecord(record, theTask);
		processResource(record, theTask);				
		
		return outcome("Task", record, theTask);

	}
			
	public void shareRecord(Record record, Task theTask) throws AppException {		
		ExecutionInfo inf = info();
		List<IIdType> personRefs = new ArrayList<IIdType>();
		
		if (theTask.getOwner() != null && !theTask.getOwner().isEmpty()) {
			personRefs.add(theTask.getOwner().getReferenceElement());
		}
					
		shareWithPersons(record, personRefs);
	}
	
	public Record init() { return newRecord("fhir/Task"); }

	@Update
	public MethodOutcome updateTask(@IdParam IdType theId, @ResourceParam Task theTask) {
		Record record = fetchCurrent(theId);
		prepare(record, theTask);		
		updateRecord(record, theTask);	
		try {
		  shareRecord(record, theTask);
		} catch (AppException e) {
			ErrorReporter.report("FHIR (update Task)", null, e);	
			throw new InternalErrorException(e);
		}
		return outcome("Task", record, theTask);
	}

	public void prepare(Record record, Task theTask) {
		// Set Record code and content
		String display = setRecordCodeByCodeableConcept(record, theTask.getCode(), "Task");
		record.name = display != null ? display : "Task";

		if (cleanAndSetRecordOwner(record, theTask.getFor())) theTask.setFor(null);		
		clean(theTask);
	}
	
 
	@Override
	public void processResource(Record record, Task p) {
		super.processResource(record, p);
		
		if (p.getFor().isEmpty()) {
			p.getFor().setReferenceElement(new IdType("Patient", record.owner.toString()));
			p.getFor().setDisplay(record.ownerName);
		}
	}

	@Override
	public void clean(Task theTask) {
		
		super.clean(theTask);
	}

}