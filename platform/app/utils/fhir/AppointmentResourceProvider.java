package utils.fhir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Appointment;
import org.hl7.fhir.dstu3.model.Appointment.AppointmentParticipantComponent;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Task;
import org.hl7.fhir.instance.model.api.IBaseResource;
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
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import controllers.Circles;
import models.Consent;
import models.ContentInfo;
import models.MidataId;
import models.Record;
import utils.ErrorReporter;
import utils.access.RecordManager;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class AppointmentResourceProvider extends ResourceProvider<Appointment> implements IResourceProvider {

	public AppointmentResourceProvider() {
		searchParamNameToPathMap.put("Appointment:actor", "participant.actor");
		searchParamNameToPathMap.put("Appointment:location", "participant.actor");
		searchParamNameToTypeMap.put("Appointment:location", Sets.create("Location"));
		searchParamNameToPathMap.put("Appointment:patient", "participant.actor");
		searchParamNameToTypeMap.put("Appointment:patient", Sets.create("Patient"));
		searchParamNameToPathMap.put("Appointment:practitioner", "participant.actor");
		searchParamNameToTypeMap.put("Appointment:practitioner", Sets.create("Practitioner"));
	}
	
	@Override
	public Class<Appointment> getResourceType() {
		return Appointment.class;
	}

	@Search()
	public List<IBaseResource> getAppointment(
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
			/*
			@Description(shortDefinition="Return resources linked to by the given target")
			@OptionalParam(name="_has")
			HasAndListParam theHas, 
			 */
			   
			@Description(shortDefinition="Appointment date/time.")
			@OptionalParam(name="date")
			DateRangeParam theDate, 
			   
			@Description(shortDefinition="The overall status of the appointment")
			@OptionalParam(name="status")
			TokenAndListParam theStatus, 
			    
			@Description(shortDefinition="Any one of the individuals participating in the appointment")
			@OptionalParam(name="actor", targetTypes={  } )
			ReferenceAndListParam theActor, 
			    
			@Description(shortDefinition="The Participation status of the subject, or other participant on the appointment. Can be used to locate participants that have not responded to meeting requests.")
			@OptionalParam(name="part-status")
			TokenAndListParam thePart_status, 
			    
			@Description(shortDefinition="One of the individuals of the appointment is this patient")
			@OptionalParam(name="patient", targetTypes={  Patient.class   } )
			ReferenceAndListParam thePatient, 
			    
			@Description(shortDefinition="One of the individuals of the appointment is this practitioner")
			@OptionalParam(name="practitioner", targetTypes={  Practitioner.class   } )
			ReferenceAndListParam thePractitioner, 
			   
			@Description(shortDefinition="This location is listed in the participants of the appointment")
			@OptionalParam(name="location", targetTypes={  Location.class   } )
			ReferenceAndListParam theLocation, 
			    
			@Description(shortDefinition="An Identifier of the Appointment")
			@OptionalParam(name="identifier")
			TokenAndListParam theIdentifier, 
			    
			@Description(shortDefinition="")
			@OptionalParam(name="appointment-type")
			TokenAndListParam theAppointment_type, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="service-type")
			TokenAndListParam theService_type, 
			 
			@IncludeParam(reverse=true)
			Set<Include> theRevIncludes,
			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
		    @OptionalParam(name="_lastUpdated")
			DateRangeParam theLastUpdated, 
			 
			@IncludeParam(allow= {
						"Appointment:actor" ,
						"Appointment:location" ,
						"Appointment:patient" ,
						"Appointment:practitioner" ,
						"*"
			})   
			
			
			Set<Include> theIncludes,
			 									
			@Sort SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", theId);
		paramMap.add("_language", theResourceLanguage);
		
		paramMap.add("date", theDate);
		paramMap.add("status", theStatus);
		paramMap.add("actor", theActor);
		paramMap.add("part-status", thePart_status);
		paramMap.add("patient", thePatient);
		paramMap.add("practitioner", thePractitioner);
		paramMap.add("location", theLocation);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("appointment-type", theAppointment_type);
		paramMap.add("service-type", theService_type);
	
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Appointment");
		
		builder.handleIdRestriction();
		builder.restriction("identifier", "Identifier", true, "identifier");
		builder.restriction("date", "date", true, "start");
		builder.restriction("actor", null, true, "participant.actor");
		builder.restriction("status", "code", false, "status");
		builder.restriction("part-status", "code", false, "participant.status");
		builder.restriction("patient", "Patient", true, "participant.actor");
		builder.restriction("practitioner", "Practitioner", true, "participant.actor");
		builder.restriction("location", "Location", true, "participant.actor");
		builder.restriction("appointment-type", "CodeableConcept", true, "appointmentType");
		builder.restriction("service-type", "CodeableConcept", true, "serviceType");
								  
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Appointment theAppointment) {
		return super.createResource(theAppointment);
	}
	
	@Override
	protected MethodOutcome create(Appointment theAppointment) throws AppException {
		Record record = newRecord("fhir/Appointment");
		
		prepare(record, theAppointment);		
		// insert
		insertRecord(record, theAppointment);
        shareRecord(record, theAppointment);
		processResource(record, theAppointment);				
		
		return outcome("Appointment", record, theAppointment);

	}
	
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Appointment theAppointment) {
		return super.updateResource(theId, theAppointment);
	}
	
	@Override
	protected MethodOutcome update(IdType theId, Appointment theAppointment) throws AppException {
		Record record = fetchCurrent(theId);
		prepare(record, theAppointment);		
		updateRecord(record, theAppointment);			
		shareRecord(record, theAppointment);
		
		return outcome("Appointment", record, theAppointment);
	}
			
	public void shareRecord(Record record, Appointment theAppointment) throws AppException {
		
		List<IIdType> persons = new ArrayList<IIdType>();
		List<AppointmentParticipantComponent> participants = theAppointment.getParticipant();		
		for (AppointmentParticipantComponent participant :participants) { 
			persons.add(participant.getActor().getReferenceElement()); 
		}		
		shareWithPersons(record, persons);				
				
	}
	
		
	public Record init() { return newRecord("fhir/Appointment"); }

	

	public void prepare(Record record, Appointment theAppointment) throws AppException {
		// Set Record code and content
		
		ContentInfo.setRecordCodeAndContent(record, null, "Appointment");								
		
		String display = theAppointment.getDescription();
		record.name = display != null ? display : "Appointment";
		
		clean(theAppointment);
	}
	
 
	@Override
	public void processResource(Record record, Appointment p) {
		super.processResource(record, p);
		
		
	}


}