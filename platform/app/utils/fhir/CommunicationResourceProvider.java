package utils.fhir;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Appointment;
import org.hl7.fhir.dstu3.model.Communication;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
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
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import controllers.Circles;
import models.Consent;
import models.ContentInfo;
import models.MidataId;
import models.Record;
import models.TypedMidataId;
import utils.access.RecordManager;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class CommunicationResourceProvider extends ResourceProvider<Communication> implements IResourceProvider {

	public CommunicationResourceProvider() {
		searchParamNameToPathMap.put("Communication:based-on", "basedOn");
		searchParamNameToPathMap.put("Communication:context", "context");
		searchParamNameToPathMap.put("Communication:patient", "subject");
		searchParamNameToTypeMap.put("Communication:patient", Sets.create("Patient"));
		searchParamNameToPathMap.put("Communication:recipient", "recipient");
		searchParamNameToPathMap.put("Communication:sender", "sender");
		searchParamNameToPathMap.put("Communication:subject", "subject");		
		
		registerSearches("Communication", getClass(), "getCommunication");
	}
	
	@Override
	public Class<Communication> getResourceType() {
		return Communication.class;
	}

	@Search()
	public List<IBaseResource> getCommunication(
			
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
			    
			@Description(shortDefinition="")
			@OptionalParam(name="identifier")
			TokenAndListParam theIdentifier, 
			  
			@Description(shortDefinition="")
			@OptionalParam(name="category")
			TokenAndListParam theCategory, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="sender", targetTypes={  } )
			ReferenceAndListParam theSender, 
			    
			@Description(shortDefinition="")
			@OptionalParam(name="recipient", targetTypes={  } )
			ReferenceAndListParam theRecipient, 
			    
			@Description(shortDefinition="")
			@OptionalParam(name="medium")
			TokenAndListParam theMedium, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="status")
			TokenAndListParam theStatus, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="context", targetTypes={  } )
			ReferenceAndListParam theContext, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="sent")
			DateRangeParam theSent, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="received")
			DateRangeParam theReceived, 
			  
			@Description(shortDefinition="")
			@OptionalParam(name="subject", targetTypes={  } )
			ReferenceAndListParam theSubject, 
			  
			@Description(shortDefinition="")
			@OptionalParam(name="patient", targetTypes={  Patient.class   } )
			ReferenceAndListParam thePatient, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="based-on", targetTypes={  } )
			ReferenceAndListParam theBased_on, 
			 
			@IncludeParam(reverse=true)
			Set<Include> theRevIncludes,
			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
			@OptionalParam(name="_lastUpdated")
			DateRangeParam theLastUpdated, 
			 
			@IncludeParam(allow= {
					"Communication:based-on" ,
					"Communication:context" ,
					"Communication:patient" ,
					"Communication:recipient" ,
					"Communication:sender" ,
					"Communication:subject" ,
					"*"
			}) 
			Set<Include> theIncludes,
			 									
			@Sort SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", theId);
		paramMap.add("_language", theResourceLanguage);
		/*paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT, theFtContent);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TEXT, theFtText);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TAG, theSearchForTag);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY, theSearchForSecurity);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE, theSearchForProfile);*/
		// paramMap.add("_has", theHas);
	
		paramMap.add("identifier", theIdentifier);
		paramMap.add("category", theCategory);
		paramMap.add("sender", theSender);
		paramMap.add("recipient", theRecipient);
		paramMap.add("medium", theMedium);
		paramMap.add("status", theStatus);
		paramMap.add("context", theContext);
		paramMap.add("sent", theSent);
		paramMap.add("received", theReceived);
		paramMap.add("subject", theSubject);
		paramMap.add("patient", thePatient);
		paramMap.add("based-on", theBased_on);
		
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Communication");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient");
		
		builder.restriction("identifier", true, "Identifier", "identifier");
		builder.restriction("received", true, "Date", "received");
		builder.restriction("sent", true, "Date", "sent");
		builder.restriction("based-on", true, null, "basedOn");
		builder.restriction("recipient", true, null, "recipient");
		
		if (!builder.recordOwnerReference("subject", null)) builder.restriction("subject", true, null, "subject");
				
		builder.restriction("category", true, "CodeableConcept", "category");
		builder.restriction("context", true, null, "context");
		builder.restriction("medium", true, "CodeableConcept", "medium");
		builder.restriction("sender", true, null, "sender");
		builder.restriction("status", true, "code", "status");	
						
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Communication theCommunication) {
		return super.createResource(theCommunication);
	}
	
	@Override
	protected MethodOutcome create(Communication theCommunication) throws AppException {

		Record record = newRecord("fhir/Communication");
		prepareForSharing(theCommunication);
		prepare(record, theCommunication);		
		// insert
		//insertRecord(record, theCommunication);
        shareRecord(record, theCommunication);
		processResource(record, theCommunication);				
		
		return outcome("Communication", record, theCommunication);

	}
	
	public void prepareForSharing(Communication theCommunication) throws AppException {
		if (theCommunication.getSender().isEmpty()) {
			theCommunication.setSender(FHIRTools.getReferenceToUser(info().executorId));
		}
		if (theCommunication.getSent() == null) theCommunication.setSent(new Date());
		if (theCommunication.getRecipient().isEmpty()) throw new UnprocessableEntityException("Recipient is missing");
	}
	
	public void shareRecord(Record record, Communication theCommunication) throws AppException {		
		ExecutionInfo inf = info();
		
		MidataId subject = theCommunication.getSubject().isEmpty() ? inf.executorId : FHIRTools.getUserIdFromReference(theCommunication.getSubject().getReferenceElement());
		MidataId sender = FHIRTools.getUserIdFromReference(theCommunication.getSender().getReferenceElement());
		MidataId shareFrom = subject;
		if (!subject.equals(sender)) {
			Consent consent = Circles.getOrCreateMessagingConsent(inf.executorId, sender, sender, subject, false);
			insertRecord(record, theCommunication, consent._id);
			shareFrom = consent._id;
		} else {
			insertRecord(record, theCommunication);
		}
		
		
		List<Reference> recipients = theCommunication.getRecipient();
		for (Reference recipient :recipients) {
						
			TypedMidataId target = FHIRTools.getMidataIdFromReference(recipient.getReferenceElement());
			Consent consent = Circles.getOrCreateMessagingConsent(inf.executorId, sender, target.getMidataId(), subject, target.getType().equals("Group"));
			RecordManager.instance.share(inf.executorId, shareFrom, consent._id, Collections.singleton(record._id), true);
			
		}
	}
	
	public Record init() { return newRecord("fhir/Communication"); }

	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Communication theCommunication) {
		return super.updateResource(theId, theCommunication);
	}
		
	@Override
	public MethodOutcome update(@IdParam IdType theId, @ResourceParam Communication theCommunication) throws AppException {
		Record record = fetchCurrent(theId);
		prepare(record, theCommunication);		
		updateRecord(record, theCommunication);		
		processResource(record, theCommunication);
		
		return outcome("Communication", record, theCommunication);
	}

	public void prepare(Record record, Communication theCommunication) throws AppException {
		// Set Record code and content
		
		
		ContentInfo.setRecordCodeAndContent(record, null, "Communication");
		
		String date = theCommunication.hasSentElement() ? theCommunication.getSentElement().toHumanDisplay() : "Not sent";			
		record.name = date;
		
		List<Reference> recipients = theCommunication.getRecipient();
		if (recipients != null) {
			for (Reference recipient :recipients) {
			    FHIRTools.resolve(recipient);	
			}
		}
		
		
		// clean
		if (cleanAndSetRecordOwner(record, theCommunication.getSubject())) theCommunication.setSubject(null);
		
		clean(theCommunication);

	}
	
 
	@Override
	public void processResource(Record record, Communication p) {
		super.processResource(record, p);
		if (p.getSubject().isEmpty()) {
			p.getSubject().setReferenceElement(new IdType("Patient", record.owner.toString()));
			p.getSubject().setDisplay(record.ownerName);
		}
	}

	@Override
	public void clean(Communication theCommunication) {
		
		super.clean(theCommunication);
	}

}