package utils.fhir;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Communication;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;

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

public class CommunicationResourceProvider extends RecordBasedResourceProvider<Communication> implements IResourceProvider {

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
	public Bundle getCommunication(
			
			@Description(shortDefinition="The resource identity")
			@OptionalParam(name="_id")
			StringAndListParam theId, 
			  
			@Description(shortDefinition="The resource language")
			@OptionalParam(name="_language")
			StringAndListParam theResourceLanguage, 
						
			    
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
			DateAndListParam theSent, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="received")
			DateAndListParam theReceived, 
			  
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

			@ca.uhn.fhir.rest.annotation.Count Integer theCount,
			
			@OptionalParam(name="_page")
			StringParam _page,
			
			RequestDetails theDetails

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", theId);
		paramMap.add("_language", theResourceLanguage);	
	
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
		paramMap.setFrom(_page != null ? _page.getValue() : null);

		return searchBundle(paramMap, theDetails);
		
	}

	public List<Record> searchRaw(SearchParameterMap params) throws AppException {
		ExecutionInfo info = info();
        
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Communication");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", "subject");
		
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("received", true, QueryBuilder.TYPE_DATE, "received");
		builder.restriction("sent", true, QueryBuilder.TYPE_DATE, "sent");
		builder.restriction("based-on", true, null, "basedOn");
		builder.restriction("recipient", true, null, "recipient");
		
		if (!builder.recordOwnerReference("subject", null, "subject")) builder.restriction("subject", true, null, "subject");
				
		builder.restriction("category", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "category");
		builder.restriction("context", true, null, "context");
		builder.restriction("medium", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "medium");
		builder.restriction("sender", true, null, "sender");
		builder.restriction("status", true, QueryBuilder.TYPE_CODE, "status");	
						
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Communication theCommunication) {
		return super.createResource(theCommunication);
	}
			
	public void createPrepare(Record record, Communication theCommunication) throws AppException {		
		prepareForSharing(theCommunication);
		prepare(record, theCommunication);		
	}
	
	public void createExecute(Record record, Communication theCommunication) throws AppException {
		shareRecord(record, theCommunication);
	}	
	
	public void prepareForSharing(Communication theCommunication) throws AppException {
		if (theCommunication.getSender().isEmpty()) {
			theCommunication.setSender(FHIRTools.getReferenceToUser(info().executorId, null));
		}
		if (theCommunication.getSent() == null) theCommunication.setSent(new Date());
		if (theCommunication.getRecipient().isEmpty()) throw new UnprocessableEntityException("Recipient is missing");
		
		FHIRTools.resolve(theCommunication.getSender());
		FHIRTools.resolve(theCommunication.getSubject());
	}
	
	public void shareRecord(Record record, Communication theCommunication) throws AppException {		
		ExecutionInfo inf = info();
		
		MidataId subject = record.owner;//theCommunication.getSubject().isEmpty() ? inf.executorId : FHIRTools.getUserIdFromReference(theCommunication.getSubject().getReferenceElement());
		MidataId sender = FHIRTools.getUserIdFromReference(theCommunication.getSender().getReferenceElement());
		MidataId shareFrom = insertMessageRecord(record, theCommunication);
						
		List<Reference> recipients = theCommunication.getRecipient();
		for (Reference recipient :recipients) {
						
			TypedMidataId target = FHIRTools.getMidataIdFromReference(recipient.getReferenceElement());
			Consent consent = Circles.getOrCreateMessagingConsent(inf.executorId, sender, target.getMidataId(), subject, target.getType().equals("Group"));
			RecordManager.instance.share(inf.executorId, shareFrom, consent._id, consent.owner, Collections.singleton(record._id), true);
			
		}
	}
	
	public Record init() { return newRecord("fhir/Communication"); }

	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Communication theCommunication) {
		return super.updateResource(theId, theCommunication);
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
	public void processResource(Record record, Communication p) throws AppException {
		super.processResource(record, p);
		if (p.getSubject().isEmpty()) {			
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}
	

}