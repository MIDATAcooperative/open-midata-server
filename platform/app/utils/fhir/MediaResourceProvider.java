package utils.fhir;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.ContentInfo;
import models.MidataId;
import models.Record;

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Media;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IIdType;

import play.Play;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.auth.ExecutionInfo;
import utils.exceptions.AppException;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class MediaResourceProvider extends ResourceProvider<Media> implements IResourceProvider {

	@Override
	public Class<Media> getResourceType() {
		return Media.class;
	}

	@Search()
	public List<Media> getMedia(
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
			    
			@Description(shortDefinition="")
			@OptionalParam(name="type")
			TokenAndListParam theType, 
			    
			@Description(shortDefinition="")
			@OptionalParam(name="subtype")
			TokenAndListParam theSubtype, 
			    
			@Description(shortDefinition="")
			@OptionalParam(name="identifier")
			TokenAndListParam theIdentifier, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="created")
			DateRangeParam theCreated, 
			    
			@Description(shortDefinition="")
			@OptionalParam(name="subject", targetTypes={  } )
			ReferenceAndListParam theSubject, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="operator", targetTypes={  } )
			ReferenceAndListParam theOperator, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="view")
			TokenAndListParam theView, 
			    
			@Description(shortDefinition="")
			@OptionalParam(name="patient", targetTypes={  Patient.class   } )
			ReferenceAndListParam thePatient, 
			 
			@IncludeParam(reverse=true)
			Set<Include> theRevIncludes,
			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
			@OptionalParam(name="_lastUpdated")
			DateRangeParam theLastUpdated, 
			 
			@IncludeParam(allow= {
						"Media:operator" ,
						"Media:patient" ,
						"Media:subject" ,
						"Media:operator" ,
						"*"
			}) 
			Set<Include> theIncludes,
			 			
			@Sort 
			SortSpec theSort,
			 			
			@ca.uhn.fhir.rest.annotation.Count
			Integer theCount	

	) {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", theId);
		paramMap.add("_language", theResourceLanguage);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT, theFtContent);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TEXT, theFtText);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TAG, theSearchForTag);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY, theSearchForSecurity);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE, theSearchForProfile);
		//paramMap.add("_has", theHas);
		
		paramMap.add("identifier", theIdentifier);
		paramMap.add("subject", theSubject);
				
		paramMap.add("type", theType);
		paramMap.add("subtype", theSubtype);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("created", theCreated);
		paramMap.add("subject", theSubject);
		paramMap.add("operator", theOperator);
		paramMap.add("view", theView);
		paramMap.add("patient", thePatient);
		
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Media");

		
		List<ReferenceParam> patients = builder.resolveReferences("patient", "Patient");
		if (patients != null) {
			query.putAccount("owner", referencesToIds(patients));
		}

		
		Set<String> codes = builder.tokensToCodeSystemStrings("view");
		if (codes != null) {
			query.putAccount("code", codes);
			builder.restriction("view", "view", "CodeableConcept", false);
		} else {
			builder.restriction("view", "view", "CodeableConcept", true);
		}
		
		
		//builder.restriction("type", "CodeableConcept", true, "type");
		
		return query.execute(info);
	}

	@Create
	public MethodOutcome createMedia(@ResourceParam Media theMedia) {

		Record record = newRecord("fhir/Media");
		prepare(record, theMedia);
		// insert
		Attachment attachment = null;
				 		
		attachment = theMedia.getContent();			
				
		insertRecord(record, theMedia, attachment);
		
		try {
		  processResource(record, theMedia);
		
		  MethodOutcome out = outcome("Media", record, theMedia);
		  
		 
		  return out;
		} catch (Exception e) {
			ErrorReporter.report("test", null, e);
			return null;
		}

	}
	
	public Record init() { return newRecord("fhir/Media"); }

	/*
	@Update
	public MethodOutcome updateMedia(@IdParam IdType theId, @ResourceParam Media theMedia) {
		Record record = fetchCurrent(theId);
		prepare(record, theMedia);
		updateRecord(record, theMedia);
		return outcome("Media", record, theMedia);
	}
	*/

	public void prepare(Record record, Media theMedia) {
		// Set Record code and content
		record.code = new HashSet<String>();
		try {
			if (!theMedia.getView().isEmpty()) {
				for (Coding coding : theMedia.getView().getCoding()) {			
					if (coding.getCode() != null && coding.getSystem() != null) {
						record.code.add(coding.getSystem() + " " + coding.getCode());
					}
				}
			
				ContentInfo.setRecordCodeAndContent(record, record.code, null);
			
			} else {
				ContentInfo.setRecordCodeAndContent(record, null, "Media");
			}
		} catch (AppException e) {
			throw new InternalErrorException(e);
		}
		record.name = theMedia.getContent().getTitle();
	
		// clean
		Reference subjectRef = theMedia.getSubject();
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
		
		if (cleanSubject) theMedia.setSubject(null);
		clean(theMedia);

	}
	
 
	@Override
	public void processResource(Record record, Media p) {
		super.processResource(record, p);
		if (p.getSubject().isEmpty()) {
			p.getSubject().setReferenceElement(new IdType("Patient", record.owner.toString()));
			p.getSubject().setDisplay(record.ownerName);
		}
		
		Attachment attachment = p.getContent();
		if (attachment != null && attachment.getUrl() == null && attachment.getData() == null) {	
		  String url = "https://"+Play.application().configuration().getString("platform.server")+"/v1/records/file?_id="+record._id;
		  attachment.setUrl(url);
		}
		
	}

	@Override
	public void clean(Media theMedia) {		
		super.clean(theMedia);
	}

}