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

package utils.fhir_stu3;

import java.util.List;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Base64BinaryType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Media;
import org.hl7.fhir.dstu3.model.Patient;

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
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import models.Record;
import utils.InstanceConfig;
import utils.access.pseudo.FhirPseudonymizer;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;

public class MediaResourceProvider extends RecordBasedResourceProvider<Media> implements IResourceProvider {

	public MediaResourceProvider() {
		searchParamNameToPathMap.put("Media:operator", "operator");
		searchParamNameToPathMap.put("Media:patient", "subject");
		searchParamNameToTypeMap.put("Media:patient", Sets.create("Patient"));
		searchParamNameToPathMap.put("Media:subject", "subject");
		
		registerSearches("Media", getClass(), "getMedia");
		
		FhirPseudonymizer.forSTU3()
		  .reset("Media")		
		  .pseudonymizeReference("Media", "operator")
		  .pseudonymizeReference("Media", "note", "authorReference");	
	}
	
	@Override
	public Class<Media> getResourceType() {
		return Media.class;
	}

	@Search()
	public Bundle getMedia(
			@Description(shortDefinition="The resource identity")
			@OptionalParam(name="_id")
			StringAndListParam theId, 
			  
			@Description(shortDefinition="The resource language")
			@OptionalParam(name="_language")
			StringAndListParam theResourceLanguage, 
			 			
			    
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

	) {

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
		paramMap.setFrom(_page != null ? _page.getValue() : null);

		return searchBundle(paramMap, theDetails);
		
	}

	public List<Record> searchRaw(SearchParameterMap params) throws AppException {
		AccessContext info = info();

		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Media");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", "subject");		
		
		builder.recordCodeRestriction("view", "view");
				
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("created", true, QueryBuilder.TYPE_DATETIME, "content.creation");
		
		if (!builder.recordOwnerReference("subject", null, "subject")) builder.restriction("subject", true, null, "subject");
		
		//builder.restriction("subject", null, true, "subject");
		builder.restriction("operator", true, "Practitioner", "operator");		
		builder.restriction("subtype", false, QueryBuilder.TYPE_CODEABLE_CONCEPT, "subtype");
		builder.restriction("type", false, QueryBuilder.TYPE_CODE, "type");		
		
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Media theMedia) {
		return super.createResource(theMedia);
	}
	
	@Override
	public void createExecute(Record record, Media theMedia) throws AppException {
		Attachment attachment = null; 		
		attachment = theMedia.getContent();						
		insertRecord(record, theMedia, attachment);		
	}	
	
	@Override
	public String getRecordFormat() {	
		return "fhir/Media";
	}
	

	/*
	@Update
	public MethodOutcome updateMedia(@IdParam IdType theId, @ResourceParam Media theMedia) {
		Record record = fetchCurrent(theId);
		prepare(record, theMedia);
		updateRecord(record, theMedia);
		return outcome("Media", record, theMedia);
	}
	*/

	public void prepare(Record record, Media theMedia) throws AppException {
		// Set Record code and content
		setRecordCodeByCodeableConcept(record, theMedia.getView(), "Media");		
		record.name = theMedia.getContent().getTitle();
			
		if (cleanAndSetRecordOwner(record, theMedia.getSubject())) theMedia.setSubject(null);
		clean(theMedia);

	}
	
 
	@Override
	public void processResource(Record record, Media p) throws AppException {
		super.processResource(record, p);
		if (p.getSubject().isEmpty()) {
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
		
		Attachment attachment = p.getContent();
		if (attachment != null && attachment.getUrl() == null && attachment.getData() == null) {	
		  String url = "https://"+InstanceConfig.getInstance().getPlatformServer()+"/v1/records/file?_id="+record._id;
		  attachment.setUrl(url);
		}
		
	}

	@Override
	public void clean(Media theMedia) {		
		super.clean(theMedia);
	}
	
	public String serialize(Media theMedia) {
		Attachment att = theMedia.getContent();
		if (att != null) {
			
			att.setUrl(null);
			att.setDataElement(new Base64BinaryType(FHIRTools.BASE64_PLACEHOLDER_FOR_STREAMING));
		}
    	return ctx.newJsonParser().encodeResourceToString(theMedia);
    }

}