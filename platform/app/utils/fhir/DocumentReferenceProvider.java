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

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Base64BinaryType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
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
import utils.access.pseudo.FhirPseudonymizer;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class DocumentReferenceProvider extends RecordBasedResourceProvider<DocumentReference> implements IResourceProvider {

	public DocumentReferenceProvider() {
		searchParamNameToPathMap.put("DocumentReference:authenticator", "authenticator");
		searchParamNameToPathMap.put("DocumentReference:author", "author");
		searchParamNameToPathMap.put("DocumentReference:custodian", "custodian");
		searchParamNameToTypeMap.put("DocumentReference:custodian", Sets.create("Organization"));
		searchParamNameToPathMap.put("DocumentReference:encounter", "encounter");
		searchParamNameToTypeMap.put("DocumentReference:encounter", Sets.create("Encounter","EpisodeOfCare"));
		searchParamNameToPathMap.put("DocumentReference:patient", "subject");
		searchParamNameToTypeMap.put("DocumentReference:patient", Sets.create("Patient"));
		searchParamNameToPathMap.put("DocumentReference:related", "context.related");
		searchParamNameToPathMap.put("DocumentReference:relatesto", "relatesTo.target");
		searchParamNameToPathMap.put("DocumentReference:subject", "subject");	
		
		registerSearches("DocumentReference", getClass(), "getDocumentReference");
		
		FhirPseudonymizer.forR4()
		  .reset("DocumentReference")		
		  .pseudonymizeReference("DocumentReference", "author")
		  .pseudonymizeReference("DocumentReference", "context", "sourcePatientInfo");
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
			 
			@Description(shortDefinition="Who/what authenticated the document")
  			@OptionalParam(name="authenticator", targetTypes={  } )
  			ReferenceAndListParam theAuthenticator, 
    
  			@Description(shortDefinition="Who and/or what authored the document")
  			@OptionalParam(name="author", targetTypes={  } )
  			ReferenceAndListParam theAuthor, 
    
  			@Description(shortDefinition="Categorization of document")
  			@OptionalParam(name="category")
  			TokenAndListParam theCategory,
    
  			@Description(shortDefinition="Mime type of the content, with charset etc.")
  			@OptionalParam(name="contenttype")
  			TokenAndListParam theContenttype,
    
  			@Description(shortDefinition="Organization which maintains the document")
  			@OptionalParam(name="custodian", targetTypes={  } )
  			ReferenceAndListParam theCustodian, 
    
  			@Description(shortDefinition="When this document reference was created")
  			@OptionalParam(name="date")
  			DateAndListParam theDate, 
    
  			@Description(shortDefinition="Human-readable description")
  			@OptionalParam(name="description")
  			StringAndListParam theDescription, 
    
  			@Description(shortDefinition="Context of the document  content")
  			@OptionalParam(name="encounter", targetTypes={  } )
  			ReferenceAndListParam theEncounter, 
    
 			@Description(shortDefinition="Main clinical acts documented")
 			@OptionalParam(name="event")
 			TokenAndListParam theEvent,
   
 			@Description(shortDefinition="Kind of facility where patient was seen")
 			@OptionalParam(name="facility")
 			TokenAndListParam theFacility,
   
 			@Description(shortDefinition="Format/content rules for the document")
 			@OptionalParam(name="format")
 			TokenAndListParam theFormat,
   
 			@Description(shortDefinition="Master Version Specific Identifier")
 			@OptionalParam(name="identifier")
 			TokenAndListParam theIdentifier,
   
 			@Description(shortDefinition="Human language of the content (BCP-47)")
 			@OptionalParam(name="language")
 			TokenAndListParam theLanguage,
   
 			@Description(shortDefinition="Uri where the data can be found")
 			@OptionalParam(name="location")
 			UriAndListParam theLocation, 
   
 			@Description(shortDefinition="Who/what is the subject of the document")
 			@OptionalParam(name="patient", targetTypes={  } )
 			ReferenceAndListParam thePatient, 
   
 			@Description(shortDefinition="Time of service that is being documented")
 			@OptionalParam(name="period")
			DateAndListParam thePeriod, 
   
 			@Description(shortDefinition="Related identifiers or resources")
 			@OptionalParam(name="related", targetTypes={  } )
 			ReferenceAndListParam theRelated, 
   
 			@Description(shortDefinition="Target of the relationship")
 			@OptionalParam(name="relatesto", targetTypes={  } )
 			ReferenceAndListParam theRelatesto, 
   
 			@Description(shortDefinition="replaces | transforms | signs | appends")
 			@OptionalParam(name="relation")
 			TokenAndListParam theRelation,
   
 			@Description(shortDefinition="Combination of relation and relatesTo")
 			@OptionalParam(name="relationship", compositeTypes= { ReferenceParam.class, TokenParam.class })
 			CompositeAndListParam<ReferenceParam, TokenParam> theRelationship,
   
 			@Description(shortDefinition="Document security-tags")
 			@OptionalParam(name="security-label")
 			TokenAndListParam theSecurity_label,
   
 			@Description(shortDefinition="Additional details about where the content was created (e.g. clinical specialty)")
 			@OptionalParam(name="setting")
 			TokenAndListParam theSetting,
   
 			@Description(shortDefinition="current | superseded | entered-in-error")
 			@OptionalParam(name="status")
 			TokenAndListParam theStatus,
   
 			@Description(shortDefinition="Who/what is the subject of the document")
 			@OptionalParam(name="subject", targetTypes={  } )
 			ReferenceAndListParam theSubject, 
   
 			@Description(shortDefinition="Kind of document (LOINC if possible)")
 			@OptionalParam(name="type")
 			TokenAndListParam theType,
  		 
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
 					"DocumentReference:related" ,
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
		paramMap.add("authenticator", theAuthenticator);
		paramMap.add("author", theAuthor);
		paramMap.add("category", theCategory);
		paramMap.add("contenttype", theContenttype);
		paramMap.add("custodian", theCustodian);
		paramMap.add("date", theDate);
		paramMap.add("description", theDescription);
		paramMap.add("encounter", theEncounter);
		paramMap.add("event", theEvent);
		paramMap.add("facility", theFacility);
		paramMap.add("format", theFormat);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("language", theLanguage);
		paramMap.add("location", theLocation);
		paramMap.add("patient", thePatient);
		paramMap.add("period", thePeriod);
		paramMap.add("related", theRelated);
		paramMap.add("relatesto", theRelatesto);
		paramMap.add("relation", theRelation);
		paramMap.add("relationship", theRelationship);
		paramMap.add("security-label", theSecurity_label);
		paramMap.add("setting", theSetting);
		paramMap.add("status", theStatus);
		paramMap.add("subject", theSubject);
		paramMap.add("type", theType);
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
				
		builder.restriction("category", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "category|class");
		
		builder.restriction("date", true, QueryBuilder.TYPE_DATETIME, "date|indexed");	
		builder.restriction("custodian", true, "Organization", "custodian");
		
		builder.restriction("description", true, QueryBuilder.TYPE_STRING, "description");
		builder.restriction("encounter", true, null, "context.encounter");
		
		builder.restriction("event", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "context.event");
		builder.restriction("facility", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "context.facilityType");
		builder.restriction("format", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "content.format");
		
		builder.restriction("indexed", true, QueryBuilder.TYPE_DATETIME, "indexed");
		builder.restriction("language", true, QueryBuilder.TYPE_CODE, "content.attachment.language");
		builder.restriction("location", true, QueryBuilder.TYPE_URI, "content.attachment.url");
		//builder.restriction("patient", "Patient", true, "subject");
		
		builder.restriction("period", true, QueryBuilder.TYPE_PERIOD, "context.period");
		builder.restriction("related", true, null, "context.related");		
		
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

	@Override
	protected void convertToR4(Object in) {
		FHIRVersionConvert.rename(in, "class", "category");
		FHIRVersionConvert.rename(in, "indexed", "date");
		
	}

}