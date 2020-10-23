/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.fhir_stu3;

import java.util.List;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Base64BinaryType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.DocumentReference.DocumentReferenceContentComponent;
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
import ca.uhn.fhir.rest.param.CompositeAndListParam;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import models.Record;
import utils.InstanceConfig;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class DocumentReferenceProvider extends RecordBasedResourceProvider<DocumentReference> implements IResourceProvider {

	public DocumentReferenceProvider() {
		searchParamNameToPathMap.put("DocumentReference:authenticator", "authenticator");
		searchParamNameToPathMap.put("DocumentReference:author", "author");
		searchParamNameToPathMap.put("DocumentReference:custodian", "custodian");
		searchParamNameToPathMap.put("DocumentReference:encounter", "encounter");
		searchParamNameToPathMap.put("DocumentReference:patient", "subject");
		searchParamNameToTypeMap.put("DocumentReference:patient", Sets.create("Patient"));
		searchParamNameToPathMap.put("DocumentReference:related-ref", "context.related.ref");
		searchParamNameToPathMap.put("DocumentReference:relatesto", "relatesTo.target");
		searchParamNameToPathMap.put("DocumentReference:subject", "subject");	
		
		registerSearches("DocumentReference", getClass(), "getDocumentReference");
	}
	
	@Override
	public Class<DocumentReference> getResourceType() {
		return DocumentReference.class;
	}

	@Search()
	public Bundle getDocumentReference(
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
			@OptionalParam(name="subject", targetTypes={  } )
			ReferenceAndListParam theSubject, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="type")
			TokenAndListParam theType, 
			  
			@Description(shortDefinition="")
			@OptionalParam(name="class")
			TokenAndListParam theClass, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="author", targetTypes={  } )
			ReferenceAndListParam theAuthor, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="custodian", targetTypes={  } )
			ReferenceAndListParam theCustodian, 
			
			@Description(shortDefinition="")
			@OptionalParam(name="authenticator", targetTypes={  } )
			ReferenceAndListParam theAuthenticator, 
			  
			@Description(shortDefinition="")
			@OptionalParam(name="created")
			DateAndListParam theCreated, 
			    
			@Description(shortDefinition="")
			@OptionalParam(name="indexed")
			DateAndListParam theIndexed, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="status")
			TokenAndListParam theStatus, 
			  
			@Description(shortDefinition="")
			@OptionalParam(name="relatesto", targetTypes={  } )
			ReferenceAndListParam theRelatesto, 
			  
			@Description(shortDefinition="")
			@OptionalParam(name="relation")
			TokenAndListParam theRelation, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="description")
			StringAndListParam theDescription, 
			  
			@Description(shortDefinition="")
			@OptionalParam(name="securitylabel")
			TokenAndListParam theSecuritylabel, 
			  
			@Description(shortDefinition="")
			@OptionalParam(name="format")
			TokenAndListParam theFormat, 
			  
			@Description(shortDefinition="")
			@OptionalParam(name="language")
			TokenAndListParam theLanguage, 
			  
			@Description(shortDefinition="")
			@OptionalParam(name="location")
			UriAndListParam theLocation, 
			 
			@Description(shortDefinition="")
			@OptionalParam(name="event")
			TokenAndListParam theEvent, 
			  
			@Description(shortDefinition="")
			@OptionalParam(name="period")
			DateAndListParam thePeriod, 
			  
			@Description(shortDefinition="")
			@OptionalParam(name="facility")
			TokenAndListParam theFacility, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="patient", targetTypes={  Patient.class   } )
			ReferenceAndListParam thePatient, 
			  
			@Description(shortDefinition="")
			@OptionalParam(name="setting")
			TokenAndListParam theSetting, 
			  
			@Description(shortDefinition="")
			@OptionalParam(name="related-id")
			TokenAndListParam theRelated_id, 
			  
			@Description(shortDefinition="")
			@OptionalParam(name="related-ref", targetTypes={  } )
			ReferenceAndListParam theRelated_ref, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="encounter", targetTypes={  } )
			ReferenceAndListParam theEncounter, 
			   
			@Description(shortDefinition="Combination of relation and relatesTo")
			@OptionalParam(name="relatesto-relation", compositeTypes= { ReferenceParam.class, TokenParam.class })
			CompositeAndListParam<ReferenceParam, TokenParam> theRelatesto_relation,
			
			@IncludeParam(reverse=true)
			Set<Include> theRevIncludes,
			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
			@OptionalParam(name="_lastUpdated")
			DateRangeParam theLastUpdated, 
			 
			@IncludeParam(allow= {
						"DocumentReference:authenticator" ,
						"DocumentReference:author" ,
						"DocumentReference:custodian" ,
						"DocumentReference:encounter" ,
						"DocumentReference:patient" ,
						"DocumentReference:related-ref" ,
						"DocumentReference:relatesto" ,
						"DocumentReference:subject" ,
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
		/*
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT, theFtContent);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TEXT, theFtText);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TAG, theSearchForTag);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY, theSearchForSecurity);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE, theSearchForProfile);
		*/
		//paramMap.add("_has", theHas);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("subject", theSubject);
		paramMap.add("type", theType);
		paramMap.add("class", theClass);
		paramMap.add("author", theAuthor);
		paramMap.add("custodian", theCustodian);
		paramMap.add("authenticator", theAuthenticator);
		paramMap.add("created", theCreated);
		paramMap.add("indexed", theIndexed);
		paramMap.add("status", theStatus);
		paramMap.add("relatesto", theRelatesto);
		paramMap.add("relation", theRelation);
		paramMap.add("description", theDescription);
		paramMap.add("securitylabel", theSecuritylabel);
		paramMap.add("format", theFormat);
		paramMap.add("language", theLanguage);
		paramMap.add("location", theLocation);
		paramMap.add("event", theEvent);
		paramMap.add("period", thePeriod);
		paramMap.add("facility", theFacility);
		paramMap.add("patient", thePatient);
		paramMap.add("setting", theSetting);
		paramMap.add("related-id", theRelated_id);
		paramMap.add("related-ref", theRelated_ref);
		paramMap.add("encounter", theEncounter);
		paramMap.add("relatesto-relation", theRelatesto_relation);
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/DocumentReference");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", "subject");
		
		
		builder.recordCodeRestriction("type", "type");
				
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier", QueryBuilder.TYPE_IDENTIFIER, "masterIdentifier");		
		
		if (!builder.recordOwnerReference("subject", null, "subject")) builder.restriction("subject", true, null, "subject");
		
		builder.restriction("authenticator", true, null, "authenticator");
		
		builder.restriction("author", true, null, "author");
		
		builder.restriction("class", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "class");
		builder.restriction("created", true, QueryBuilder.TYPE_DATETIME, "created");
		builder.restriction("custodian", true, "Organization", "custodian");
		
		builder.restriction("description", true, QueryBuilder.TYPE_STRING, "description");
		builder.restriction("encounter", true, "Encounter", "context.encounter");
		
		builder.restriction("event", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "context.event");
		builder.restriction("facility", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "context.facilityType");
		builder.restriction("format", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "content.format");
		
		builder.restriction("indexed", true, QueryBuilder.TYPE_DATETIME, "indexed");
		builder.restriction("language", true, QueryBuilder.TYPE_CODE, "content.attachment.language");
		builder.restriction("location", true, QueryBuilder.TYPE_URI, "content.attachment.url");
		//builder.restriction("patient", "Patient", true, "subject");
		
		builder.restriction("period", true, QueryBuilder.TYPE_PERIOD, "context.period");
		builder.restriction("related-id", true, QueryBuilder.TYPE_IDENTIFIER, "context.related.identifier");
		builder.restriction("related-ref", true, null, "context.related.ref");
		
		builder.restriction("relatesto", true, "DocumentReference", "relatesTo.target");
		
		builder.restriction("relation", false, QueryBuilder.TYPE_CODE, "relatesTo.code");
		builder.restriction("relationship", "relatesTo.code", "relatesTo.target", QueryBuilder.TYPE_CODE, "DocumentReference");	
		builder.restriction("securitylabel", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "securityLabel");
		builder.restriction("setting", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "context.practiceSetting");
		builder.restriction("status", true, QueryBuilder.TYPE_CODE, "status");
		
		
		//builder.restriction("type", "CodeableConcept", true, "type");
		
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam DocumentReference theDocumentReference) {
		return super.createResource(theDocumentReference);
	}
		
	@Override
	public void createExecute(Record record, DocumentReference theDocumentReference) throws AppException {
        Attachment attachment = null;
		
		List<DocumentReferenceContentComponent> contents = theDocumentReference.getContent(); 
		if (contents != null && contents.size() == 1) {
			attachment = theDocumentReference.getContent().get(0).getAttachment();			
		}
		
		insertRecord(record, theDocumentReference, attachment);
	}	
	
	@Override
	public String getRecordFormat() {	
		return "fhir/DocumentReference";
	}	


	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam DocumentReference theDocumentReference) {
		return super.updateResource(theId, theDocumentReference);
	}
	
	public void prepare(Record record, DocumentReference theDocumentReference) throws AppException {
		// Set Record code and content
		setRecordCodeByCodeableConcept(record, theDocumentReference.getType(), "DocumentReference");		
		record.name = theDocumentReference.getDescription();
	
		// clean
		Reference subjectRef = theDocumentReference.getSubject();	
		if (cleanAndSetRecordOwner(record, theDocumentReference.getSubject())) theDocumentReference.setSubject(null);
		clean(theDocumentReference);

	}

	/*
	 * @Delete() public void deleteObservation(@IdParam IdType theId) { Record
	 * record = fetchCurrent(theId);
	 * RecordManager.instance.deleteRecord(info().executorId, info().targetAPS,
	 * record); }
	 */
 
	@Override
	public void processResource(Record record, DocumentReference p) throws AppException {
		super.processResource(record, p);
		if (p.getSubject().isEmpty()) {
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
		for (DocumentReferenceContentComponent component : p.getContent()) {
			Attachment attachment = component.getAttachment();
			if (attachment != null && attachment.getUrl() == null && attachment.getData() == null) {	
			  String url = "https://"+InstanceConfig.getInstance().getPlatformServer()+"/v1/records/file?_id="+record._id;
			  attachment.setUrl(url);
			}
		}
	}

	@Override
	public void clean(DocumentReference theDocumentReference) {
		
		super.clean(theDocumentReference);
	}
	
	public String serialize(DocumentReference theDocumentReference) {
		for (DocumentReferenceContentComponent component : theDocumentReference.getContent()) {
			Attachment attachment = component.getAttachment();
			if (attachment != null && attachment.getUrl() != null && attachment.getUrl().startsWith("https://"+InstanceConfig.getInstance().getPlatformServer())) {	
				attachment.setUrl(null);
				attachment.setDataElement(new Base64BinaryType(FHIRTools.BASE64_PLACEHOLDER_FOR_STREAMING));
			}
		}
		
    	return ctx.newJsonParser().encodeResourceToString(theDocumentReference);
    }

}