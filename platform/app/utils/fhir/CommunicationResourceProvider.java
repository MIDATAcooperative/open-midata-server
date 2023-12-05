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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Communication;
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
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import controllers.Circles;
import models.Consent;
import models.ContentInfo;
import models.MidataId;
import models.Record;
import models.TypedMidataId;
import models.enums.AuditEventType;
import utils.access.RecordManager;
import utils.access.pseudo.FhirPseudonymizer;
import utils.audit.AuditHeaderTool;
import utils.audit.AuditManager;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;

public class CommunicationResourceProvider extends RecordBasedResourceProvider<Communication> implements IResourceProvider {

	public CommunicationResourceProvider() {
		searchParamNameToPathMap.put("Communication:based-on", "basedOn");
		searchParamNameToPathMap.put("Communication:part-of", "partOf");
		searchParamNameToPathMap.put("Communication:instantiates-canonical", "instantiatesCanonical");
		searchParamNameToPathMap.put("Communication:encounter", "encounter");
		searchParamNameToPathMap.put("Communication:patient", "subject");
		searchParamNameToTypeMap.put("Communication:patient", Sets.create("Patient"));
		searchParamNameToPathMap.put("Communication:recipient", "recipient");
		searchParamNameToPathMap.put("Communication:sender", "sender");
		searchParamNameToPathMap.put("Communication:subject", "subject");		
		
		registerSearches("Communication", getClass(), "getCommunication");
		
		FhirPseudonymizer.forR4()
		  .reset("Communication")
		  .hideIfPseudonymized("Communication", "text")
		  .pseudonymizeReference("Communication", "recipient")
		  .pseudonymizeReference("Communication", "sender")
		  .pseudonymizeReference("Communication", "note", "authorReference")
		  .pseudonymizeReference("Communication", "subject");
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
			  											 
  			@Description(shortDefinition="Request fulfilled by this communication")
  			@OptionalParam(name="based-on", targetTypes={  } )
  			ReferenceAndListParam theBased_on, 
    
  			@Description(shortDefinition="Message category")
  			@OptionalParam(name="category")
  			TokenAndListParam theCategory,
    
  			@Description(shortDefinition="Encounter created as part of")
  			@OptionalParam(name="encounter", targetTypes={  } )
  			ReferenceAndListParam theEncounter, 
    
  			@Description(shortDefinition="Unique identifier")
  			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier,
    
  			@Description(shortDefinition="Instantiates FHIR protocol or definition")
  			@OptionalParam(name="instantiates-canonical", targetTypes={  } )
  			ReferenceAndListParam theInstantiates_canonical, 
    
  			@Description(shortDefinition="Instantiates external protocol or definition")
  			@OptionalParam(name="instantiates-uri")
  			UriAndListParam theInstantiates_uri, 
    
  			@Description(shortDefinition="A channel of communication")
  			@OptionalParam(name="medium")
  			TokenAndListParam theMedium,
    
  			@Description(shortDefinition="Part of this action")
  			@OptionalParam(name="part-of", targetTypes={  } )
  			ReferenceAndListParam thePart_of, 
    
 			@Description(shortDefinition="Focus of message")
 			@OptionalParam(name="patient", targetTypes={  } )
 			ReferenceAndListParam thePatient, 
   
 			@Description(shortDefinition="When received")
 			@OptionalParam(name="received")
 			DateAndListParam theReceived, 
   
 			@Description(shortDefinition="Message recipient")
 			@OptionalParam(name="recipient", targetTypes={  } )
 			ReferenceAndListParam theRecipient, 
   
 			@Description(shortDefinition="Message sender")
 			@OptionalParam(name="sender", targetTypes={  } )
 			ReferenceAndListParam theSender, 
   
 			@Description(shortDefinition="When sent")
 			@OptionalParam(name="sent")
			DateAndListParam theSent, 
   
 			@Description(shortDefinition="preparation | in-progress | not-done | suspended | aborted | completed | entered-in-error")
 			@OptionalParam(name="status")
 			TokenAndListParam theStatus,
   
 			@Description(shortDefinition="Focus of message")
 			@OptionalParam(name="subject", targetTypes={  } )
 			ReferenceAndListParam theSubject, 
  		
 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= {
 					"Communication:based-on" ,
 					"Communication:encounter" ,
 					"Communication:instantiates-canonical" ,
 					"Communication:part-of" ,
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
	
		paramMap.add("category", theCategory);
		paramMap.add("encounter", theEncounter);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("instantiates-canonical", theInstantiates_canonical);
		paramMap.add("instantiates-uri", theInstantiates_uri);
		paramMap.add("medium", theMedium);
		paramMap.add("part-of", thePart_of);
		paramMap.add("patient", thePatient);
		paramMap.add("received", theReceived);
		paramMap.add("recipient", theRecipient);
		paramMap.add("sender", theSender);
		paramMap.add("sent", theSent);
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

	public Query buildQuery(SearchParameterMap params) throws AppException {
		info();
		
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
		builder.restriction("encounter", true, "Encounter", "encounter");
		builder.restriction("instantiates-canonical", true, null, "instantiatesCanonical");
		builder.restriction("instantiates-uri", true, QueryBuilder.TYPE_URI, "instantiatesUri");
		builder.restriction("part-of", true, null, "partOf");
		builder.restriction("medium", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "medium");
		builder.restriction("sender", true, null, "sender");
		builder.restriction("status", true, QueryBuilder.TYPE_CODE, "status");	
						
		return query;
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
	
	public Communication createExecute(Record record, Communication theCommunication) throws AppException {
		boolean audit = AuditHeaderTool.createAuditEntryFromHeaders(info(), AuditEventType.REST_CREATE, record.owner);
		shareRecord(record, theCommunication);
		if (audit) AuditManager.instance.success();
		return theCommunication;
	}	
	
	public void prepareForSharing(Communication theCommunication) throws AppException {
		if (theCommunication.getSender().isEmpty()) {
			theCommunication.setSender(FHIRTools.getReferenceToUser(info().getAccessor(), null));
		}
		if (theCommunication.getSent() == null) theCommunication.setSent(new Date());
		if (theCommunication.getRecipient().isEmpty()) throw new UnprocessableEntityException("Recipient is missing");
		
		FHIRTools.resolve(theCommunication.getSender());
		FHIRTools.resolve(theCommunication.getSubject());
	}
	
	public void shareRecord(Record record, Communication theCommunication) throws AppException {		
		AccessContext inf = info();
		
		MidataId subject = record.owner;//theCommunication.getSubject().isEmpty() ? inf.getAccessor() : FHIRTools.getUserIdFromReference(theCommunication.getSubject().getReferenceElement());
		MidataId sender = FHIRTools.getUserIdFromReference(theCommunication.getSender().getReferenceElement());
		MidataId shareFrom = insertMessageRecord(record, theCommunication);
						
		List<Reference> recipients = theCommunication.getRecipient();
		for (Reference recipient :recipients) {
						
			TypedMidataId target = FHIRTools.getMidataIdFromReference(recipient.getReferenceElement());
			Consent consent = Circles.getOrCreateMessagingConsent(inf, sender, target.getMidataId(), subject, target.getType().equals("Group"));
			RecordManager.instance.share(inf, shareFrom, consent._id, consent.owner, Collections.singleton(record._id), false);
			
		}
	}
	
	@Override
	public String getRecordFormat() {	
		return "fhir/Communication";
	}

	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Communication theCommunication) {
		return super.updateResource(theId, theCommunication);
	}
			

	public void prepare(Record record, Communication theCommunication) throws AppException {
		// Set Record code and content
		
		
		ContentInfo.setRecordCodeAndContent(info().getUsedPlugin(), record, null, "Communication");
		
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
			p.setSubject(FHIRTools.getReferenceToOwner(record));
		}
	}
	
	

	@Override
	public List<Attachment> getAttachments(Communication resource) {
		List<Attachment> result = new ArrayList<Attachment>();
		for (Communication.CommunicationPayloadComponent payload : resource.getPayload()) {
			if (payload.hasContentAttachment()) {
				result.add(payload.getContentAttachment());
			}
			
		}
		return result;
	}

	@Override
	protected void convertToR4(Object in) {
		// No action
		
	}
	

}