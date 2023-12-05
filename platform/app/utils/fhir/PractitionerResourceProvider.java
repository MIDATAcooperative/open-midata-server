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

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Practitioner;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import models.ContentInfo;
import models.Record;
import models.User;
import utils.exceptions.AppException;

public class PractitionerResourceProvider extends HybridTypeResourceProvider<Practitioner, Record, User> {

	public PractitionerResourceProvider() {
		super(Record.class, new RecordPractitionerResourceProvider(), User.class, new MidataPractitionerResourceProvider(), false);
		registerSearches("Practitioner", getClass(), "getPractitioner");
	}
	
	  @Search()
	    public List<IBaseResource> getPractitioner(
	    		
	    		
	    	@Description(shortDefinition="The resource identity")
	    	@OptionalParam(name="_id")
	    	StringParam theId, 
	    		  
	    	@Description(shortDefinition="A person Identifier")
	    	@OptionalParam(name="identifier")
	    	TokenAndListParam theIdentifier, 
	    	    
	    	@Description(shortDefinition="A server defined search that may match any of the string fields in the HumanName, including family, give, prefix, suffix, suffix, and/or text")
	    	@OptionalParam(name="name")
	    	StringAndListParam theName, 
	    	    
	    	@Description(shortDefinition="A value in an email contact")
	    	@OptionalParam(name="email")
	    	TokenParam theEmail, 
	    		    
	    	@Description(shortDefinition="A server defined search that may match any of the string fields in the Address, including line, city, state, country, postalCode, and/or text")
	    	@OptionalParam(name="address")
	    	StringAndListParam theAddress, 
	    	   
	    	@Description(shortDefinition="A city specified in an address")
	    	@OptionalParam(name="address-city")
	    	StringAndListParam theAddress_city, 
	    	   
	    	/*
	    	@Description(shortDefinition="A state specified in an address")
	    	@OptionalParam(name="address-state")
	    	StringAndListParam theAddress_state, 
	    	*/
	    	
	    	@Description(shortDefinition="A postal code specified in an address")
	    	@OptionalParam(name="address-postalcode")
	    	StringParam theAddress_postalcode, 
	    		
	    	@Description(shortDefinition="A country specified in an address")
	    	@OptionalParam(name="address-country")
	    	StringParam theAddress_country, 
	    		   	    			    		   
	    	@Description(shortDefinition="The gender of the person")
	    	@OptionalParam(name="gender")
	    	TokenParam theGender, 
	    		 
	    	@Description(shortDefinition="The person's date of birth")
	    	@OptionalParam(name="birthdate")
	    	DateAndListParam theBirthdate,
	    	
	    	@OptionalParam(name="_page")
			StringParam _page,
			
			RequestDetails theDetails
	    		   	    	
	    		
	    		) throws AppException {
	    	
	    	SearchParameterMap paramMap = new SearchParameterMap();
	    	paramMap.add("_id", theId);	    	
	    	paramMap.add("identifier", theIdentifier);
	    	paramMap.add("name", theName);
	    	paramMap.add("email", theEmail);
	    	paramMap.add("address", theAddress);
	    	paramMap.add("address-city", theAddress_city);
	    	//paramMap.add("address-state", theAddress_state);
	    	paramMap.add("address-postalcode", theAddress_postalcode);
	    	paramMap.add("address-country", theAddress_country);
	    	
	    	paramMap.add("gender", theGender);
	    	paramMap.add("birthdate", theBirthdate);
	    	//paramMap.add("organization", theOrganization);
	    	    		    	
	    	return search(paramMap);    	    	    	
	    }
	  
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Practitioner thePractitioner) {
		return super.createResource(thePractitioner);
	}
	
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Practitioner thePractitioner) {
		return super.updateResource(theId, thePractitioner);
	}	

	@Override
	public boolean handleWithFirstProvider(Practitioner resource) {
		return (resource.getMeta().getSecurity("http://midata.coop/codesystems/security", "generated") == null);
	}

	@Override
	public Class<Practitioner> getResourceType() {
		return Practitioner.class;
	}

	@Override
	protected void convertToR4(Object in) {
				
	}
	
}

class RecordPractitionerResourceProvider extends RecordBasedResourceProvider<Practitioner> {

	@Override
	public String getRecordFormat() {
		return "fhir/Practitioner";
	}

	@Override
	public Class<Practitioner> getResourceType() {
		return Practitioner.class;
	}

	@Override
	protected void convertToR4(Object in) {
		
		
	}

	@Override
	public Query buildQuery(SearchParameterMap params) throws AppException {
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Practitioner");
		query.putAccount("content", "Practitioner/Extern");
		builder.handleIdRestriction();
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("name", true, QueryBuilder.TYPE_STRING, "name.given", QueryBuilder.TYPE_STRING, "name.family");
		builder.restriction("email", true, QueryBuilder.TYPE_CODE, "telecom.value");
		builder.restriction("address-city", true, QueryBuilder.TYPE_STRING, "address.city");
		builder.restriction("address-country", true, QueryBuilder.TYPE_STRING, "address.country");
		builder.restriction("address-postalcode", true, QueryBuilder.TYPE_STRING, "address.postalCode");
		builder.restriction("address-state", true, QueryBuilder.TYPE_STRING, "address.state");
		builder.restriction("address-use", false, QueryBuilder.TYPE_CODE, "address.use");
		builder.restriction("birthdate", true, QueryBuilder.TYPE_DATE, "birthDate");						
		builder.restriction("gender", false, QueryBuilder.TYPE_CODE, "gender");
		
		return query;
	}
	
	public void prepare(Record record, Practitioner thePractitioner) throws AppException {
		// Set Record code and content
				
		ContentInfo.setRecordCodeAndContent(info().getUsedPlugin(), record, null, "Practitioner/Extern");			
		record.name = "Practitioner";
								
		clean(thePractitioner);

	}
	
}