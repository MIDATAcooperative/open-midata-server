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

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Location;

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
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.QuantityAndListParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import models.ContentInfo;
import models.Record;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class LocationResourceProvider extends RecordBasedResourceProvider<Location> implements IResourceProvider {

	public LocationResourceProvider() {
		searchParamNameToPathMap.put("Location:endpoint", "endpoint");
		searchParamNameToTypeMap.put("Location:endpoint", Sets.create("Endpoint"));
		searchParamNameToPathMap.put("Location:organization", "managingOrganization");
		searchParamNameToTypeMap.put("Location:organization", Sets.create("Organization"));
		searchParamNameToPathMap.put("Location:partof", "partOf");
		searchParamNameToTypeMap.put("Location:partof", Sets.create("Location"));				
		
		registerSearches("Location", getClass(), "getLocation");
	}
	
	@Override
	public Class<Location> getResourceType() {
		return Location.class;
	}

	@Search()
	public Bundle getLocation(
		
			@Description(shortDefinition="The resource identity")
			@OptionalParam(name="_id")
			StringAndListParam theId, 
			  
			@Description(shortDefinition="A (part of the) address of the location")
  			@OptionalParam(name="address")
  			StringAndListParam theAddress, 
    
  			@Description(shortDefinition="A city specified in an address")
  			@OptionalParam(name="address-city")
  			StringAndListParam theAddress_city, 
    
  			@Description(shortDefinition="A country specified in an address")
  			@OptionalParam(name="address-country")
  			StringAndListParam theAddress_country, 
    
  			@Description(shortDefinition="A postal code specified in an address")
  			@OptionalParam(name="address-postalcode")
  			StringAndListParam theAddress_postalcode, 
    
  			@Description(shortDefinition="A state specified in an address")
  			@OptionalParam(name="address-state")
  			StringAndListParam theAddress_state, 
    
  			@Description(shortDefinition="A use code specified in an address")
  			@OptionalParam(name="address-use")
  			TokenAndListParam theAddress_use,
    
  			@Description(shortDefinition="Technical endpoints providing access to services operated for the location")
  			@OptionalParam(name="endpoint", targetTypes={  } )
  			ReferenceAndListParam theEndpoint, 
    
  			@Description(shortDefinition="An identifier for the location")
  			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier,
    
 			@Description(shortDefinition="A portion of the location's name or alias")
 			@OptionalParam(name="name")
 			StringAndListParam theName,     			
   
 			@Description(shortDefinition="Searches for locations (typically bed/room) that have an operational status (e.g. contaminated, housekeeping)")
 			@OptionalParam(name="operational-status")
 			TokenAndListParam theOperational_status,
   
 			@Description(shortDefinition="Searches for locations that are managed by the provided organization")
 			@OptionalParam(name="organization", targetTypes={  } )
 			ReferenceAndListParam theOrganization, 
   
 			@Description(shortDefinition="A location of which this location is a part")
 			@OptionalParam(name="partof", targetTypes={  } )
 			ReferenceAndListParam thePartof, 
   
 			@Description(shortDefinition="Searches for locations with a specific kind of status")
 			@OptionalParam(name="status")
 			TokenAndListParam theStatus,
   
 			@Description(shortDefinition="A code for the type of location")
 			@OptionalParam(name="type")
 			TokenAndListParam theType,
			
			@IncludeParam(reverse=true)
			Set<Include> theRevIncludes,
			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
			@OptionalParam(name="_lastUpdated")
			DateRangeParam theLastUpdated, 
			
			@IncludeParam(allow= {					
		 			"Location:endpoint" ,
		 			"Location:organization" ,
		 			"Location:partof", 					
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
		
		paramMap.add("address", theAddress);
		paramMap.add("address-city", theAddress_city);
		paramMap.add("address-country", theAddress_country);
		paramMap.add("address-postalcode", theAddress_postalcode);
		paramMap.add("address-state", theAddress_state);
		paramMap.add("address-use", theAddress_use);
		paramMap.add("endpoint", theEndpoint);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("name", theName);
		//paramMap.add("near", theNear);
		//paramMap.add("near-distance", theNear_distance);
		paramMap.add("operational-status", theOperational_status);
		paramMap.add("organization", theOrganization);
		paramMap.add("partof", thePartof);
		paramMap.add("status", theStatus);
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Location");

		builder.handleIdRestriction();
					
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("name", true, QueryBuilder.TYPE_STRING, "name", QueryBuilder.TYPE_STRING, "alias");
		
		builder.restriction("address-city", true, QueryBuilder.TYPE_STRING, "address.city");
		builder.restriction("address-country", true, QueryBuilder.TYPE_STRING, "address.country");
		builder.restriction("address-postalcode", true, QueryBuilder.TYPE_STRING, "address.postalcode");
		builder.restriction("address-state", true, QueryBuilder.TYPE_STRING, "address.state");
		builder.restriction("address-use", true, QueryBuilder.TYPE_CODE, "address.use");
		
		builder.restrictionMany("address", true, QueryBuilder.TYPE_STRING, "address.text", "address.line", "address.city", "address.district", "address.state", "address.postalCode",
				"address.country");
		
		builder.restriction("endpoint", true, "Endpoint", "endpoint");

		builder.restriction("operational-status", true, QueryBuilder.TYPE_CODING, "operationalStatus");
		builder.restriction("organization", true, "Organization", "managingOrganization");
		builder.restriction("partof", true, "Location", "partOf");
		builder.restriction("status", true, QueryBuilder.TYPE_CODE, "status");
		builder.restriction("type", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "type");																														
		
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Location theLocation) {
		return super.createResource(theLocation);
	}
		
	@Override
	public String getRecordFormat() {	
		return "fhir/Location";
	}
	
	

	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Location theLocation) {
		return super.updateResource(theId, theLocation);
	}
		

	public void prepare(Record record, Location theLocation) throws AppException {
		// Set Record code and content
				
		ContentInfo.setRecordCodeAndContent(info().pluginId, record, null, "Location");			
		record.name = "Location";
						
		clean(theLocation);

	}

 
	@Override
	public void processResource(Record record, Location p) throws AppException {
		super.processResource(record, p);		
	}

	@Override
	public void clean(Location theLocation) {
		
		super.clean(theLocation);
	}

}