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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Media;

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
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import models.Record;
import utils.access.pseudo.FhirPseudonymizer;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;

public class MediaResourceProvider extends RecordBasedResourceProvider<Media> implements IResourceProvider {

	public MediaResourceProvider() {
		searchParamNameToPathMap.put("Media:based-on", "basedOn");
		searchParamNameToTypeMap.put("Media:based-on", Sets.create("CarePlan", "ServiceRequest"));
		
		searchParamNameToPathMap.put("Media:device", "device");
		searchParamNameToTypeMap.put("Media:device", Sets.create("Device", "DeviceMetric"));
		
		searchParamNameToPathMap.put("Media:encounter", "encounter");
		searchParamNameToTypeMap.put("Media:encounter", Sets.create("Encounter"));
		
		searchParamNameToPathMap.put("Media:operator", "operator");
		searchParamNameToPathMap.put("Media:patient", "subject");
		searchParamNameToTypeMap.put("Media:patient", Sets.create("Patient"));
		searchParamNameToPathMap.put("Media:subject", "subject");
		
		registerSearches("Media", getClass(), "getMedia");
		
		FhirPseudonymizer.forR4()
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
			  					 		
			    
 			@Description(shortDefinition="Procedure that caused this media to be created")
  			@OptionalParam(name="based-on", targetTypes={  } )
  			ReferenceAndListParam theBased_on, 
    
  			@Description(shortDefinition="When Media was collected")
  			@OptionalParam(name="created")
  			DateAndListParam theCreated, 
    
  			@Description(shortDefinition="Observing Device")
  			@OptionalParam(name="device", targetTypes={  } )
  			ReferenceAndListParam theDevice, 
    
  			@Description(shortDefinition="Encounter associated with media")
  			@OptionalParam(name="encounter", targetTypes={  } )
  			ReferenceAndListParam theEncounter, 
    
  			@Description(shortDefinition="Identifier(s) for the image")
  			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier,
    
  			@Description(shortDefinition="The type of acquisition equipment/process")
  			@OptionalParam(name="modality")
  			TokenAndListParam theModality,
    
  			@Description(shortDefinition="The person who generated the image")
  			@OptionalParam(name="operator", targetTypes={  } )
  			ReferenceAndListParam theOperator, 
    
  			@Description(shortDefinition="Who/What this Media is a record of")
  			@OptionalParam(name="patient", targetTypes={  } )
  			ReferenceAndListParam thePatient, 
    
 			@Description(shortDefinition="Observed body part")
 			@OptionalParam(name="site")
 			TokenAndListParam theSite,
   
 			@Description(shortDefinition="preparation | in-progress | not-done | suspended | aborted | completed | entered-in-error | unknown")
 			@OptionalParam(name="status")
 			TokenAndListParam theStatus,
   
 			@Description(shortDefinition="Who/What this Media is a record of")
 			@OptionalParam(name="subject", targetTypes={  } )
 			ReferenceAndListParam theSubject, 
   
 			@Description(shortDefinition="Classification of media as image, video, or audio")
 			@OptionalParam(name="type")
 			TokenAndListParam theType,
   
 			@Description(shortDefinition="Imaging view, e.g. Lateral or Antero-posterior")
 			@OptionalParam(name="view")
 			TokenAndListParam theView,
  			 
 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= {
 					"Media:based-on" ,
 					"Media:device" ,
 					"Media:encounter" ,
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
		/*
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT, theFtContent);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TEXT, theFtText);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TAG, theSearchForTag);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY, theSearchForSecurity);
		paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE, theSearchForProfile);
		*/
		//paramMap.add("_has", theHas);
		
		paramMap.add("based-on", theBased_on);
		paramMap.add("created", theCreated);
		paramMap.add("device", theDevice);
		paramMap.add("encounter", theEncounter);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("modality", theModality);
		paramMap.add("operator", theOperator);
		paramMap.add("patient", thePatient);
		paramMap.add("site", theSite);
		paramMap.add("status", theStatus);
		paramMap.add("subject", theSubject);
		paramMap.add("type", theType);
		paramMap.add("view", theView);
		
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Media");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", "subject");		
		
		builder.recordCodeRestriction("view", "view");
				
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("created", true, QueryBuilder.TYPE_DATETIME_OR_PERIOD, "created");
		
		if (!builder.recordOwnerReference("subject", null, "subject")) builder.restriction("subject", true, null, "subject");
		
		builder.restriction("based-on", true, null, "basedOn");
		builder.restriction("device", true, null, "device");
		builder.restriction("encounter", true, "Encounter", "encounter");
		builder.restriction("modality", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "modality");	
		builder.restriction("operator", true, null, "operator");				
		builder.restriction("type", false, QueryBuilder.TYPE_CODEABLE_CONCEPT, "type");		
		
		return query;
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Media theMedia) {
		return super.createResource(theMedia);
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
	}
	
	
	@Override
	public List<Attachment> getAttachments(Media resource) {	
		return Collections.singletonList(resource.getContent());
	}

	@Override
	public void clean(Media theMedia) {		
		super.clean(theMedia);
	}
		
	@Override
	protected void convertToR4(Object in) {
		FHIRVersionConvert.rename(in, "subtype", "modality");
		
	}

}