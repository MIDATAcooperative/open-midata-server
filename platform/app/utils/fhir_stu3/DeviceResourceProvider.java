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
import org.hl7.fhir.dstu3.model.Device;
import org.hl7.fhir.dstu3.model.IdType;

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

public class DeviceResourceProvider extends RecordBasedResourceProvider<Device> implements IResourceProvider {

	public DeviceResourceProvider() {
		searchParamNameToPathMap.put("Device:location", "location");
		searchParamNameToTypeMap.put("Device:location", Sets.create("Location"));
		searchParamNameToPathMap.put("Device:organization", "owner");
		searchParamNameToTypeMap.put("Device:organization", Sets.create("Organization"));
		searchParamNameToPathMap.put("Device:patient", "patient");
		searchParamNameToTypeMap.put("Device:patient", Sets.create("Patient"));		
		
		registerSearches("Device", getClass(), "getDevice");
		
		FhirPseudonymizer.forSTU3()
		  .reset("Device")		  
		  .pseudonymizeReference("Device", "note", "authorReference");
	}
	
	@Override
	public Class<Device> getResourceType() {
		return Device.class;
	}

	@Search()
	public Bundle getDevice(
		
			@Description(shortDefinition="The resource identity")
			@OptionalParam(name="_id")
			StringAndListParam theId, 
			  
			@Description(shortDefinition="Instance id from manufacturer, owner, and others")
			@OptionalParam(name="identifier")
			TokenAndListParam theIdentifier, 
			    
			@Description(shortDefinition="A location, where the resource is found")
			@OptionalParam(name="location", targetTypes={  } )
			ReferenceAndListParam theLocation, 
			   
			@Description(shortDefinition="The manufacturer of the device")
			@OptionalParam(name="manufacturer")
			StringAndListParam theManufacturer, 
			   
			@Description(shortDefinition="The model of the device")
			@OptionalParam(name="model")
			StringAndListParam theModel, 
			   
			@Description(shortDefinition="The organization responsible for the device")
			@OptionalParam(name="organization", targetTypes={  } )
			ReferenceAndListParam theOrganization, 
			   
			@Description(shortDefinition="Patient information, if the resource is affixed to a person")
			@OptionalParam(name="patient", targetTypes={  } )
			ReferenceAndListParam thePatient, 
			   
			@Description(shortDefinition="The type of the device")
			@OptionalParam(name="type")
			TokenAndListParam theType, 
			   
			@Description(shortDefinition="Barcode string (udi)")
			@OptionalParam(name="udicarrier")
			TokenAndListParam theUdicarrier, 
			   
			@Description(shortDefinition="Network address to contact device")
			@OptionalParam(name="url")
			UriAndListParam theUrl, 
			
			@IncludeParam(reverse=true)
			Set<Include> theRevIncludes,
			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
			@OptionalParam(name="_lastUpdated")
			DateRangeParam theLastUpdated, 
			
			@IncludeParam(allow= {
					"Device:location" ,
					"Device:organization" ,
					"Device:patient" ,
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
		
		paramMap.add("identifier", theIdentifier);
		paramMap.add("location", theLocation);
		paramMap.add("manufacturer", theManufacturer);
		paramMap.add("model", theModel);
		paramMap.add("organization", theOrganization);
		paramMap.add("patient", thePatient);
		paramMap.add("type", theType);
		paramMap.add("udicarrier", theUdicarrier);
		paramMap.add("url", theUrl);
		
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Device");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", null);
				
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("device-name", false, QueryBuilder.TYPE_STRING, "udi", QueryBuilder.TYPE_STRING, "type.coding.display", QueryBuilder.TYPE_STRING, "type.text");
        builder.restriction("location", true, "Location", "location");
		builder.restriction("manufacturer", true, QueryBuilder.TYPE_STRING, "manufacturer");	
		builder.restriction("model", true, QueryBuilder.TYPE_STRING, "model");
		builder.restriction("organization", true, "Organization", "owner");
		builder.restriction("status", true, QueryBuilder.TYPE_CODE, "status");	
		builder.restriction("type", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "type");
		builder.restriction("udi-carrier", true, QueryBuilder.TYPE_STRING, "udi.carrierHRF", QueryBuilder.TYPE_STRING, "udi.carrierAIDC");
		builder.restriction("udi-di", true, QueryBuilder.TYPE_STRING, "udi.deviceIdentifier");
		builder.restriction("url", true, QueryBuilder.TYPE_URI, "url");
														
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Device theDevice) {
		return super.createResource(theDevice);
	}
		
	@Override
	public String getRecordFormat() {	
		return "fhir/Device";
	}	
	

	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Device theDevice) {
		return super.updateResource(theId, theDevice);
	}
		

	public void prepare(Record record, Device theDevice) throws AppException {
		// Set Record code and content
				
		ContentInfo.setRecordCodeAndContent(info().pluginId, record, null, "Device");			
		record.name = "Device";
				
		if (cleanAndSetRecordOwner(record, theDevice.getPatient())) theDevice.setPatient(null);
		clean(theDevice);

	}

 
	@Override
	public void processResource(Record record, Device p) throws AppException {
		super.processResource(record, p);
		if (p.getPatient().isEmpty()) {
			p.setPatient(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}

	@Override
	public void clean(Device theDevice) {
		
		super.clean(theDevice);
	}

}