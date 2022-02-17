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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;

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
import models.ContentInfo;
import models.Record;
import utils.access.pseudo.FhirPseudonymizer;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;

public class AppointmentResourceProvider extends RecordBasedResourceProvider<Appointment> implements IResourceProvider {

	public AppointmentResourceProvider() {
		searchParamNameToPathMap.put("Appointment:actor", "participant.actor");
		searchParamNameToPathMap.put("Appointment:location", "participant.actor");
		searchParamNameToTypeMap.put("Appointment:location", Sets.create("Location"));
		searchParamNameToPathMap.put("Appointment:patient", "participant.actor");
		searchParamNameToTypeMap.put("Appointment:patient", Sets.create("Patient"));
		searchParamNameToPathMap.put("Appointment:practitioner", "participant.actor");
		searchParamNameToTypeMap.put("Appointment:practitioner", Sets.create("Practitioner"));
		searchParamNameToPathMap.put("Appointment:based-on", "basedOn");
		searchParamNameToTypeMap.put("Appointment:based-on", Sets.create("ServiceRequest"));
		searchParamNameToPathMap.put("Appointment:reason-reference", "reasonReference");
		searchParamNameToPathMap.put("Appointment:supporting-info", "supportingInformation");
		searchParamNameToPathMap.put("Appointment:slot", "slot");
		searchParamNameToTypeMap.put("Appointment:slot", Sets.create("Slot"));
		registerSearches("Appointment", getClass(), "getAppointment");
		
		FhirPseudonymizer.forR4()
		  .reset("Appointment")
		  .pseudonymizeReference("Appointment", "participant", "actor");
	}
	
	@Override
	public Class<Appointment> getResourceType() {
		return Appointment.class;
	}

	@Search()
	public Bundle getAppointment(
			@Description(shortDefinition="The resource identity")
			@OptionalParam(name="_id")
			StringAndListParam theId, 
			  
			@Description(shortDefinition="The resource language")
			@OptionalParam(name="_language")
			StringAndListParam theResourceLanguage, 
  			@Description(shortDefinition="Any one of the individuals participating in the appointment")
  			@OptionalParam(name="actor", targetTypes={  } )
  			ReferenceAndListParam theActor, 
    
  			@Description(shortDefinition="The style of appointment or patient that has been booked in the slot (not service type)")
  			@OptionalParam(name="appointment-type")
  			TokenAndListParam theAppointment_type,
    
  			@Description(shortDefinition="The service request this appointment is allocated to assess")
  			@OptionalParam(name="based-on", targetTypes={  } )
  			ReferenceAndListParam theBased_on, 
    
  			@Description(shortDefinition="Appointment date/time.")
  			@OptionalParam(name="date")
  			DateAndListParam theDate, 
    
  			@Description(shortDefinition="An Identifier of the Appointment")
  			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier,
    
  			@Description(shortDefinition="This location is listed in the participants of the appointment")
  			@OptionalParam(name="location", targetTypes={  } )
  			ReferenceAndListParam theLocation, 
    
  			@Description(shortDefinition="The Participation status of the subject, or other participant on the appointment. Can be used to locate participants that have not responded to meeting requests.")
  			@OptionalParam(name="part-status")
  			TokenAndListParam thePart_status,
    
  			@Description(shortDefinition="One of the individuals of the appointment is this patient")
  			@OptionalParam(name="patient", targetTypes={  } )
  			ReferenceAndListParam thePatient, 
    
 			@Description(shortDefinition="One of the individuals of the appointment is this practitioner")
 			@OptionalParam(name="practitioner", targetTypes={  } )
 			ReferenceAndListParam thePractitioner, 
   
 			@Description(shortDefinition="Coded reason this appointment is scheduled")
 			@OptionalParam(name="reason-code")
 			TokenAndListParam theReason_code,
   
 			@Description(shortDefinition="Reason the appointment is to take place (resource)")
 			@OptionalParam(name="reason-reference", targetTypes={  } )
 			ReferenceAndListParam theReason_reference, 
   
 			@Description(shortDefinition="A broad categorization of the service that is to be performed during this appointment")
 			@OptionalParam(name="service-category")
 			TokenAndListParam theService_category,
   
 			@Description(shortDefinition="The specific service that is to be performed during this appointment")
 			@OptionalParam(name="service-type")
 			TokenAndListParam theService_type,
   
 			@Description(shortDefinition="The slots that this appointment is filling")
 			@OptionalParam(name="slot", targetTypes={  } )
 			ReferenceAndListParam theSlot, 
   
 			@Description(shortDefinition="The specialty of a practitioner that would be required to perform the service requested in this appointment")
 			@OptionalParam(name="specialty")
 			TokenAndListParam theSpecialty,
   
 			@Description(shortDefinition="The overall status of the appointment")
 			@OptionalParam(name="status")
 			TokenAndListParam theStatus,
   
 			@Description(shortDefinition="Additional information to support the appointment")
 			@OptionalParam(name="supporting-info", targetTypes={  } )
 			ReferenceAndListParam theSupporting_info, 
  		
 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= {
 					"Appointment:actor" ,
 					"Appointment:based-on" ,
 					"Appointment:location" ,
 					"Appointment:patient" ,
 					"Appointment:practitioner" ,
 					"Appointment:reason-reference" ,
 					"Appointment:slot" ,
 					"Appointment:supporting-info" ,
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
		
		paramMap.add("actor", theActor);
		paramMap.add("appointment-type", theAppointment_type);
		paramMap.add("based-on", theBased_on);
		paramMap.add("date", theDate);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("location", theLocation);
		paramMap.add("part-status", thePart_status);
		paramMap.add("patient", thePatient);
		paramMap.add("practitioner", thePractitioner);
		paramMap.add("reason-code", theReason_code);
		paramMap.add("reason-reference", theReason_reference);
		paramMap.add("service-category", theService_category);
		paramMap.add("service-type", theService_type);
		paramMap.add("slot", theSlot);
		paramMap.add("specialty", theSpecialty);
		paramMap.add("status", theStatus);
		paramMap.add("supporting-info", theSupporting_info);
	
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Appointment");
		
		builder.handleIdRestriction();
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("date", true, QueryBuilder.TYPE_DATE, "start");
		builder.restriction("actor", true, null, "participant.actor");
		builder.restriction("status", false, QueryBuilder.TYPE_CODE, "status");
		builder.restriction("part-status", false, QueryBuilder.TYPE_CODE, "participant.status");
		builder.restriction("patient", true, "Patient", "participant.actor");
		builder.restriction("practitioner", true, "Practitioner", "participant.actor");
		builder.restriction("location", true, "Location", "participant.actor");
		builder.restriction("appointment-type", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "appointmentType");
		builder.restriction("service-type", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "serviceType");
		builder.restriction("service-category", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "serviceCategory");
		builder.restriction("based-on", true, "ServiceRequest", "basedOn");
		builder.restriction("reason-code", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "reasonCode");
		builder.restriction("reason-reference", true, null, "reasonReference");
		builder.restriction("slot", true, "Slot", "slot");
		builder.restriction("specialty", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "specialty");
		builder.restriction("supporting-info", true, null, "supportingInformation");
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Appointment theAppointment) {
		return super.createResource(theAppointment);
	}
	

	@Override
	public void createExecute(Record record, Appointment theAppointment) throws AppException {
		insertRecord(record, theAppointment);
		shareRecord(record, theAppointment);
	}		
	
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Appointment theAppointment) {
		return super.updateResource(theId, theAppointment);
	}
	
	
	@Override
	public void updateExecute(Record record, Appointment theAppointment) throws AppException {
		updateRecord(record, theAppointment, getAttachments(theAppointment));			
		shareRecord(record, theAppointment);
	}
			
	public void shareRecord(Record record, Appointment theAppointment) throws AppException {
		
		List<IIdType> persons = new ArrayList<IIdType>();
		List<AppointmentParticipantComponent> participants = theAppointment.getParticipant();		
		for (AppointmentParticipantComponent participant :participants) { 
			persons.add(participant.getActor().getReferenceElement()); 
		}		
		shareWithPersons(record, persons, info().getAccessor());				
				
	}
	
	@Override
	public String getRecordFormat() {	
		return "fhir/Appointment";
	}		
	

	public void prepare(Record record, Appointment theAppointment) throws AppException {
		// Set Record code and content
		
		ContentInfo.setRecordCodeAndContent(info().getUsedPlugin(), record, null, "Appointment");								
		
		String display = theAppointment.getDescription();
		record.name = display != null ? display : "Appointment";
		
		List<AppointmentParticipantComponent> participants = theAppointment.getParticipant();
		if (participants != null) {
			for (AppointmentParticipantComponent participant :participants) { 
				FHIRTools.resolve(participant.getActor()); 
			}	
		}
		
		clean(theAppointment);
	}
	
 
	@Override
	public void processResource(Record record, Appointment p) throws AppException {
		super.processResource(record, p);
		
		
	}

	@Override
	protected void convertToR4(Object in) {
		// TODO Auto-generated method stub
		
	}


}