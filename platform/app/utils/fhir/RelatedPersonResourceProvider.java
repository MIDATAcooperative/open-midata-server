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

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.RelatedPerson;

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
import models.ContentInfo;
import models.Record;
import utils.access.pseudo.FhirPseudonymizer;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class RelatedPersonResourceProvider extends RecordBasedResourceProvider<RelatedPerson> implements IResourceProvider {

	public RelatedPersonResourceProvider() {		
		searchParamNameToPathMap.put("RelatedPerson:patient", "patient");
		searchParamNameToTypeMap.put("RelatedPerson:patient", Sets.create("Patient"));		
		
		registerSearches("RelatedPerson", getClass(), "getRelatedPerson");
		
		FhirPseudonymizer.forR4()
		  .reset("RelatedPerson");
	}
	
	@Override
	public Class<RelatedPerson> getResourceType() {
		return RelatedPerson.class;
	}

	@Search()
	public Bundle getRelatedPerson(
		
			@Description(shortDefinition="The resource identity")
			@OptionalParam(name="_id")
			StringAndListParam theId, 
			
			@Description(shortDefinition = "An Identifier of the RelatedPerson") 
			@OptionalParam(name = "identifier") TokenAndListParam theIdentifier,
					
			@Description(shortDefinition = "A server defined search that may match any of the string fields in the HumanName, including family, give, prefix, suffix, suffix, and/or text") 
			@OptionalParam(name = "name") StringAndListParam theName,
		
			//@Description(shortDefinition = "A portion of name using some kind of phonetic matching algorithm") 
			//@OptionalParam(name = "phonetic") StringAndListParam thePhonetic,

			@Description(shortDefinition = "The value in any kind of contact") 
			@OptionalParam(name = "telecom") TokenAndListParam theTelecom,

			@Description(shortDefinition = "A value in a phone contact") 
			@OptionalParam(name = "phone") TokenAndListParam thePhone,

			@Description(shortDefinition = "A value in an email contact") 
			@OptionalParam(name = "email") TokenAndListParam theEmail,

			@Description(shortDefinition = "A server defined search that may match any of the string fields in the Address, including line, city, district, state, country, postalCode, and/or text") 
			@OptionalParam(name = "address") StringAndListParam theAddress,

			@Description(shortDefinition = "A city specified in an address") 
			@OptionalParam(name = "address-city") StringAndListParam theAddress_city,

			@Description(shortDefinition = "A state specified in an address") 
			@OptionalParam(name = "address-state") StringAndListParam theAddress_state,

			@Description(shortDefinition = "A postalCode specified in an address") 
			@OptionalParam(name = "address-postalcode") StringAndListParam theAddress_postalcode,

			@Description(shortDefinition = "A country specified in an address") 
			@OptionalParam(name = "address-country") StringAndListParam theAddress_country,

			@Description(shortDefinition = "A use code specified in an address") 
			@OptionalParam(name = "address-use") TokenAndListParam theAddress_use,

			@Description(shortDefinition = "Gender of the related person") 
			@OptionalParam(name = "gender") TokenAndListParam theGender,
			
			@Description(shortDefinition = "The Related Person's date of birth") 
			@OptionalParam(name = "birthdate") DateAndListParam theBirthdate,
			
			@Description(shortDefinition = "Indicates if the related person record is active") 
			@OptionalParam(name = "active") TokenAndListParam theActive,

			@Description(shortDefinition="The patient this related person is related ton")
			@OptionalParam(name="patient", targetTypes={ Patient.class } )
			ReferenceAndListParam thePatient, 

			@Description(shortDefinition = "The relationship between the patient and the relatedperson") 
			@OptionalParam(name = "relationship") TokenAndListParam theRelationship,
			

			@IncludeParam(reverse=true)
			Set<Include> theRevIncludes,
			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
			@OptionalParam(name="_lastUpdated")
			DateRangeParam theLastUpdated, 

			@IncludeParam(allow= {					
					"RelatedPerson:patient" ,
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
		
		paramMap.add("active", theActive);	
		paramMap.add("address", theAddress);
		paramMap.add("address-city", theAddress_city);
		paramMap.add("address-country", theAddress_country);
		paramMap.add("address-postalcode", theAddress_postalcode);
		paramMap.add("address-state", theAddress_state);
		paramMap.add("address-use", theAddress_use);
		paramMap.add("birthdate", theBirthdate);
		paramMap.add("email", theEmail);
		paramMap.add("gender", theGender);
		paramMap.add("identifier", theIdentifier);	
		paramMap.add("name", theName);	
		paramMap.add("patient", thePatient);		
		paramMap.add("phone", thePhone);
		//paramMap.add("phonetic" ,thePhonetic);
		paramMap.add("relationship", theRelationship);	
		paramMap.add("telecom", theTelecom);
		
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/RelatedPerson");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", null);
				
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
				
		builder.restriction("birthdate", true, QueryBuilder.TYPE_DATE, "birthDate");				
		
		builder.restriction("name", true, QueryBuilder.TYPE_STRING, "name.given", QueryBuilder.TYPE_STRING, "name.family");
		builder.restriction("email", true, QueryBuilder.TYPE_CODE, "telecom.value");
		builder.restriction("address-city", true, QueryBuilder.TYPE_STRING, "address.city");
		builder.restriction("address-country", true, QueryBuilder.TYPE_STRING, "address.country");
		builder.restriction("address-postalcode", true, QueryBuilder.TYPE_STRING, "address.postalCode");
		builder.restriction("address-state", true, QueryBuilder.TYPE_STRING, "address.state");
		builder.restriction("address-use", false, QueryBuilder.TYPE_CODE, "address.use");

		builder.restrictionMany("address", true, QueryBuilder.TYPE_STRING, "address.text", "address.line", "address.city", "address.district", "address.state", "address.postalCode",
				"address.country");
		builder.restriction("phone", true, QueryBuilder.TYPE_CODE, "telecom.value");
		builder.restriction("telecom", true, QueryBuilder.TYPE_CODE, "telecom.value");
		builder.restriction("relationship", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "relationship");
		
		builder.restriction("gender", false, QueryBuilder.TYPE_CODE, "gender");
		builder.restriction("active", false, QueryBuilder.TYPE_BOOLEAN, "active");
																					
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam RelatedPerson theRelatedPerson) {
		return super.createResource(theRelatedPerson);
	}
		
	@Override
	public String getRecordFormat() {	
		return "fhir/RelatedPerson";
	}	
	

	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam RelatedPerson theRelatedPerson) {
		return super.updateResource(theId, theRelatedPerson);
	}
		

	public void prepare(Record record, RelatedPerson theRelatedPerson) throws AppException {
		// Set Record code and content
				
		ContentInfo.setRecordCodeAndContent(info().pluginId, record, null, "RelatedPerson");			
		record.name = theRelatedPerson.hasName() ? theRelatedPerson.getName().get(0).getNameAsSingleString() : "Related Person";
				
		if (cleanAndSetRecordOwner(record, theRelatedPerson.getPatient())) theRelatedPerson.setPatient(null);
		clean(theRelatedPerson);

	}

 
	@Override
	public void processResource(Record record, RelatedPerson p) throws AppException {
		super.processResource(record, p);
		if (p.getPatient().isEmpty()) {
			p.setPatient(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}

	@Override
	public void clean(RelatedPerson theRelatedPerson) {
		super.clean(theRelatedPerson);
	}

	@Override
	protected void convertToR4(Object in) {
				
	}	

}