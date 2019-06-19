package utils.fhir_stu3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Task;
import org.hl7.fhir.dstu3.model.Task.TaskRestrictionComponent;
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
import models.MidataId;
import models.Record;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class TaskResourceProvider extends RecordBasedResourceProvider<Task> implements IResourceProvider {

	public TaskResourceProvider() {
		
		searchParamNameToPathMap.put("Task:focus", "focus");
		searchParamNameToPathMap.put("Task:owner", "owner");
		searchParamNameToPathMap.put("Task:patient", "for");
		searchParamNameToTypeMap.put("Task:patient", Sets.create("Patient"));
		searchParamNameToPathMap.put("Task:requester", "requester");	
		
		registerSearches("Task", getClass(), "getTask");
	}
	
	@Override
	public Class<Task> getResourceType() {
		return Task.class;
	}

	@Search()
	public Bundle getTask(
			@Description(shortDefinition="The resource identity")
			@OptionalParam(name="_id")
			StringAndListParam theId, 
			  
			@Description(shortDefinition="The resource language")
			@OptionalParam(name="_language")
			StringAndListParam theResourceLanguage, 
			 /* 
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
			  */
			@Description(shortDefinition="Search by creation date")
			@OptionalParam(name="authored-on")
			DateAndListParam theAuthored_on, 
			   
			@Description(shortDefinition="Search by requests this task is based on")
			@OptionalParam(name="based-on", targetTypes={  } )
			ReferenceAndListParam theBased_on, 
			   
			@Description(shortDefinition="Search by business status")
			@OptionalParam(name="business-status")
			TokenAndListParam theBusiness_status, 
			    
			@Description(shortDefinition="Search by task code")
			@OptionalParam(name="code")
			TokenAndListParam theCode, 
			   
			@Description(shortDefinition="Search by encounter or episode")
			@OptionalParam(name="context", targetTypes={  } )
			ReferenceAndListParam theContext, 
			   
			@Description(shortDefinition="Search by task definition as a Reference")
			@OptionalParam(name="definition-ref", targetTypes={  } )
			ReferenceAndListParam theDefinition_ref, 
			    
			@Description(shortDefinition="Search by task focus")
			@OptionalParam(name="focus", targetTypes={  } )
			ReferenceAndListParam theFocus, 
			  
			@Description(shortDefinition="Search by group identifier")
			@OptionalParam(name="group-identifier")
			TokenAndListParam theGroup_identifier, 
			   
			@Description(shortDefinition="Search for a task instance by its business identifier")
			@OptionalParam(name="identifier")
			TokenAndListParam theIdentifier, 
			  
			@Description(shortDefinition="Search by task intent")
			@OptionalParam(name="intent")
			TokenAndListParam theIntent, 
			   
			@Description(shortDefinition="Search by last modification date")
			@OptionalParam(name="modified")
			DateAndListParam theModified, 
			   
			@Description(shortDefinition="Search by responsible organization")
			@OptionalParam(name="organization", targetTypes={  } )
			ReferenceAndListParam theOrganization, 
			  
			@Description(shortDefinition="Search by task owner")
			@OptionalParam(name="owner", targetTypes={  } )
			ReferenceAndListParam theOwner, 
			  
			@Description(shortDefinition="Search by task this task is part of")
			@OptionalParam(name="part-of", targetTypes={  } )
			ReferenceAndListParam thePart_of, 
			   
			@Description(shortDefinition="Search by patient")
			@OptionalParam(name="patient", targetTypes={  } )
			ReferenceAndListParam thePatient, 
			   
			@Description(shortDefinition="Search by recommended type of performer (e.g., Requester, Performer, Scheduler).")
			@OptionalParam(name="performer")
			TokenAndListParam thePerformer, 
			   
			@Description(shortDefinition="Search by period Task is/was underway")
			@OptionalParam(name="period")
			DateAndListParam thePeriod, 
			   
			@Description(shortDefinition="Search by task priority")
			@OptionalParam(name="priority")
			TokenAndListParam thePriority, 
			   
			@Description(shortDefinition="Search by task requester")
			@OptionalParam(name="requester", targetTypes={  } )
			ReferenceAndListParam theRequester, 
			   
			@Description(shortDefinition="Search by task status")
			@OptionalParam(name="status")
			TokenAndListParam theStatus, 
			   
			@Description(shortDefinition="Search by status reason")
			@OptionalParam(name="statusreason")
			TokenAndListParam theStatusreason, 
			   
			@Description(shortDefinition="Search by subject")
			@OptionalParam(name="subject", targetTypes={  } )
			ReferenceAndListParam theSubject, 
			 
			@IncludeParam(reverse=true)
			Set<Include> theRevIncludes,
			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
			@OptionalParam(name="_lastUpdated")
			DateRangeParam theLastUpdated, 
			 
			@IncludeParam(allow= {
				"Task:based-on" ,
				"Task:context" ,
				"Task:definition-ref" ,
				"Task:focus" ,
				"Task:organization" ,
				"Task:owner" ,
				"Task:part-of" ,
				"Task:patient" ,
				"Task:requester" ,
				"Task:subject" ,
				"*"
			}) 
			Set<Include> theIncludes,
			 			
			@Sort 
			SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount,
			
			@OptionalParam(name="_page")
			StringParam _page,
			
			RequestDetails theDetails

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", theId);
		paramMap.add("_language", theResourceLanguage);
		
	    //paramMap.add("_has", theHas);
		paramMap.add("authored-on", theAuthored_on);
		paramMap.add("based-on", theBased_on);
		paramMap.add("business-status", theBusiness_status);
		paramMap.add("code", theCode);
		paramMap.add("context", theContext);
		paramMap.add("definition-ref", theDefinition_ref);
		paramMap.add("focus", theFocus);
		paramMap.add("group-identifier", theGroup_identifier);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("intent", theIntent);
		paramMap.add("modified", theModified);
		paramMap.add("organization", theOrganization);
		paramMap.add("owner", theOwner);
		paramMap.add("part-of", thePart_of);
		paramMap.add("patient", thePatient);
		paramMap.add("performer", thePerformer);
		paramMap.add("period", thePeriod);
		paramMap.add("priority", thePriority);
		paramMap.add("requester", theRequester);
		paramMap.add("status", theStatus);
		paramMap.add("statusreason", theStatusreason);
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Task");
		
		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", "subject");		
		builder.recordCodeRestriction("code", "code");
		
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("part-of", true, "Task", "partOf");
		builder.restriction("owner", true, null, "owner");
		
		if (!builder.recordOwnerReference("subject", null, "subject")) builder.restriction("subject", true, null, "for");
		  
        											
		builder.restriction("authored-on", true, QueryBuilder.TYPE_DATETIME, "authoredOn");
		builder.restriction("based-on", true, null, "basedOn");
		builder.restriction("business-status",  true,  QueryBuilder.TYPE_CODEABLE_CONCEPT, "businessStatus");
		builder.restriction("context", true, null, "context");
		builder.restriction("definition-ref", true, "ActivityDefinition", "definitionReference");	
		builder.restriction("focus", true, null, "focus");			
		builder.restriction("group-identifier", true, QueryBuilder.TYPE_IDENTIFIER, "groupIdentifier");
		
		builder.restriction("intent", true, QueryBuilder.TYPE_CODE, "intent");
		builder.restriction("modified", true, QueryBuilder.TYPE_DATETIME, "lastModified");
		builder.restriction("organization", true, "Organization", "requester.onBehalfOf");
		
		
		builder.restriction("performer", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "performerType"); 
		builder.restriction("period", true, QueryBuilder.TYPE_PERIOD, "executionPeriod");
		builder.restriction("priority", false, QueryBuilder.TYPE_CODE, "priority");	
		builder.restriction("requester", true, null, "requester.agent");	
		builder.restriction("status", false, QueryBuilder.TYPE_CODE, "status");
		builder.restriction("statusreason", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "statusReason");					
							   
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Task theTask) {
		return super.createResource(theTask);
	}
	
	@Override
	public void createExecute(Record record, Task theTask) throws AppException {
		MidataId consent = insertMessageRecord(record, theTask);
        shareRecord(record, theTask, consent);
	}	
	
			
	public void shareRecord(Record record, Task theTask, MidataId consent) throws AppException {		
		ExecutionInfo inf = info();
		List<IIdType> personRefs = new ArrayList<IIdType>();
		
		if (theTask.getOwner() != null && !theTask.getOwner().isEmpty()) {
			personRefs.add(theTask.getOwner().getReferenceElement());
		}
		
		TaskRestrictionComponent trc = theTask.getRestriction();
		if (trc != null && trc.getRecipient() != null) {
			for (Reference ref : trc.getRecipient()) {
				personRefs.add(ref.getReferenceElement());
			}
		}
				
					
		shareWithPersons(record, personRefs, consent);
	}
	
	@Override
	public String getRecordFormat() {	
		return "fhir/Task";
	}	
		
	
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Task theTask) {
		return super.updateResource(theId, theTask);
	}
	
	
	@Override
	public void updateExecute(Record record, Task theTask) throws AppException {
		updateRecord(record, theTask);
		shareRecord(record, theTask, info().executorId); // XXX To be checked
	}

	public void prepare(Record record, Task theTask) throws AppException {
		// Set Record code and content
		String display = setRecordCodeByCodeableConcept(record, theTask.getCode(), "Task");
		record.name = display != null ? display : "Task";

		FHIRTools.resolve(theTask.getOwner());
		FHIRTools.resolve(theTask.getFor());
		TaskRestrictionComponent trc = theTask.getRestriction();
		if (trc != null && trc.getRecipient() != null) {
			for (Reference ref : trc.getRecipient()) {
				FHIRTools.resolve(ref);
			}
		}
		
		if (cleanAndSetRecordOwner(record, theTask.getFor())) theTask.setFor(null);		
		clean(theTask);
	}
	
 
	@Override
	public void processResource(Record record, Task p) throws AppException {
		super.processResource(record, p);
		
		if (p.getFor().isEmpty()) {			
			p.setFor(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}

	@Override
	public void clean(Task theTask) {
		
		super.clean(theTask);
	}

}