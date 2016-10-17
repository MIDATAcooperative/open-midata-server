package utils.fhir;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.ContentInfo;
import models.Record;

import models.MidataId;
import org.hl7.fhir.dstu3.exceptions.FHIRException;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Range;
import org.hl7.fhir.dstu3.model.Ratio;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.SampledData;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.TimeType;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.IIdType;

import play.Play;

import utils.auth.ExecutionInfo;
import utils.exceptions.AppException;
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
import ca.uhn.fhir.rest.param.CompositeAndListParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.QuantityAndListParam;
import ca.uhn.fhir.rest.param.QuantityParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class DocumentReferenceProvider extends ResourceProvider<DocumentReference> implements IResourceProvider {

	@Override
	public Class<DocumentReference> getResourceType() {
		return DocumentReference.class;
	}

	@Search()
	public List<DocumentReference> getDocumentReference(
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
			DateRangeParam theCreated, 
			    
			@Description(shortDefinition="")
			@OptionalParam(name="indexed")
			DateRangeParam theIndexed, 
			   
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
			DateRangeParam thePeriod, 
			  
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
			Integer theCount	

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
		return search(paramMap);
	}

	public List<Record> searchRaw(SearchParameterMap params) throws AppException {
		ExecutionInfo info = info();

		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/DocumentReference");

		
		List<ReferenceParam> patients = builder.resolveReferences("patient", "Patient");
		if (patients != null) {
			query.putAccount("owner", referencesToIds(patients));
		}

		Set<String> codes = builder.tokensToCodeSystemStrings("type");
		if (codes != null) {
			query.putAccount("code", codes);
			builder.restriction("type", "type", "CodeableConcept", false);
		} else {
			builder.restriction("type", "type", "CodeableConcept", true);
		}
		
		
		builder.restriction("identifier", "Identifier", true, "identifier", "masterIdentifier");
		builder.restriction("subject", null, true, "subject");
		
		builder.restriction("authenticator", null, true, "authenticator");
		
		builder.restriction("author", null, true, "author");
		
		builder.restriction("class", "CodeableConcept", true, "class");
		builder.restriction("created", "date", true, "created");
		builder.restriction("custodian", "Organization", true, "custodian");
		
		builder.restriction("description", "String", true, "description");
		builder.restriction("encounter", "Encounter", true, "context.encounter");
		
		builder.restriction("event", "CodeableConcept", true, "context.event");
		builder.restriction("facility", "CodeableConcept", true, "context.facilityType");
		builder.restriction("format", "CodeableConcept", true, "content.format");
		
		builder.restriction("indexed", "date", true, "indexed");
		builder.restriction("language", "code", true, "content.attachment.language");
		builder.restriction("location", "uri", true, "content.attachment.url");
		//builder.restriction("patient", "Patient", true, "subject");
		
		builder.restriction("period", "date", true, "context.period");
		builder.restriction("related-id", "Identifier", true, "context.related.identifier");
		builder.restriction("related-ref", null, true, "context.related.ref");
		
		builder.restriction("relatesto", "DocumentReference", true, "relatesTo.target");
		
		builder.restriction("relation", "code", false, "relatesTo.code");
		builder.restriction("relationship", "relatesTo.code", "relatesTo.target", "code", "DocumentReference");	
		builder.restriction("securitylabel", "CodeableConcept", true, "securityLabel");
		builder.restriction("setting", "CodeableConcept", true, "context.practiceSetting");
		builder.restriction("status", "code", true, "status");
		
		
		//builder.restriction("type", "CodeableConcept", true, "type");
		
		return query.execute(info);
	}

	@Create
	public MethodOutcome createDocumenentReference(@ResourceParam DocumentReference theDocumentReference) {

		Record record = newRecord("fhir/DocumentReference");
		prepare(record, theDocumentReference);
		// insert
		Attachment attachment = null;
		
		List<DocumentReferenceContentComponent> contents = theDocumentReference.getContent(); 
		if (contents != null && contents.size() == 1) {
			attachment = theDocumentReference.getContent().get(0).getAttachment();			
		}
		
		insertRecord(record, theDocumentReference, attachment);

		processResource(record, theDocumentReference);
		return outcome("DocumentReference", record, theDocumentReference);

	}
	
	public Record init() { return newRecord("fhir/DocumentReference"); }

	@Update
	public MethodOutcome updateDocumentReference(@IdParam IdType theId, @ResourceParam DocumentReference theDocumentReference) {
		Record record = fetchCurrent(theId);
		prepare(record, theDocumentReference);
		updateRecord(record, theDocumentReference);
		return outcome("DocumentReference", record, theDocumentReference);
	}

	public void prepare(Record record, DocumentReference theDocumentReference) {
		// Set Record code and content
		record.code = new HashSet<String>(); 
		for (Coding coding : theDocumentReference.getType().getCoding()) {			
			if (coding.getCode() != null && coding.getSystem() != null) {
				record.code.add(coding.getSystem() + " " + coding.getCode());
			}
		}
		try {
			ContentInfo.setRecordCodeAndContent(record, record.code, null);
		} catch (AppException e) {
			throw new InternalErrorException(e);
		}
		record.name = theDocumentReference.getDescription();
	
		// clean
		Reference subjectRef = theDocumentReference.getSubject();
		boolean cleanSubject = true;
		if (subjectRef != null) {
			IIdType target = subjectRef.getReferenceElement();
			if (target != null) {
				String rt = target.getResourceType();
				if (rt != null && rt.equals("Patient")) {
					String tId = target.getIdPart();
					if (! MidataId.isValid(tId)) throw new UnprocessableEntityException("Subject Reference not valid");
					record.owner = new MidataId(tId);
				} else cleanSubject = false;
			}
		}
		
		if (cleanSubject) theDocumentReference.setSubject(null);
		clean(theDocumentReference);

	}

	/*
	 * @Delete() public void deleteObservation(@IdParam IdType theId) { Record
	 * record = fetchCurrent(theId);
	 * RecordManager.instance.deleteRecord(info().executorId, info().targetAPS,
	 * record); }
	 */
 
	@Override
	public void processResource(Record record, DocumentReference p) {
		super.processResource(record, p);
		if (p.getSubject().isEmpty()) {
			p.getSubject().setReferenceElement(new IdType("Patient", record.owner.toString()));
			p.getSubject().setDisplay(record.ownerName);
		}
		for (DocumentReferenceContentComponent component : p.getContent()) {
			Attachment attachment = component.getAttachment();
			if (attachment != null && attachment.getUrl() == null && attachment.getData() == null) {	
			  String url = "https://"+Play.application().configuration().getString("platform.server")+"/v1/records/file?_id="+record._id;
			  attachment.setUrl(url);
			}
		}
	}

	@Override
	public void clean(DocumentReference theDocumentReference) {
		
		super.clean(theDocumentReference);
	}

}