package utils.fhir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Record;
import utils.AccessLog;
import utils.access.RecordManager;
import utils.auth.ExecutionInfo;
import utils.collections.CMaps;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.exceptions.AppException;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.MedicationOrder;
import org.hl7.fhir.dstu3.model.Observation;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;

public class MedicationOrderResourceProvider extends ResourceProvider<MedicationOrder> implements IResourceProvider {
 
 
    @Override
    public Class<MedicationOrder> getResourceType() {
        return MedicationOrder.class;
    }
     
       
    @Search()
    public List<MedicationOrder> getMedicationOrder(
    		@Description(shortDefinition="The resource identity")
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
    		    
    		@Description(shortDefinition="Return prescriptions written on this date")
    		@OptionalParam(name="datewritten")
    		DateRangeParam theDatewritten, 
    		    
    		@Description(shortDefinition="Return prescriptions with this encounter identifier")
    		@OptionalParam(name="encounter", targetTypes={  } )
    		ReferenceAndListParam theEncounter, 
    		    
    		@Description(shortDefinition="Return prescriptions with this external identifier")
    		@OptionalParam(name="identifier")
    		TokenAndListParam theIdentifier, 
    		    
    		@Description(shortDefinition="Return administrations of this medication reference")
    		@OptionalParam(name="medication", targetTypes={  } )
    		ReferenceAndListParam theMedication, 
    		   
    		@Description(shortDefinition="Return administrations of this medication code")
    		@OptionalParam(name="code")
    		TokenAndListParam theCode, 
    		   
    		@Description(shortDefinition="The identity of a patient to list orders  for")
    		@OptionalParam(name="patient", targetTypes={  } )
    		ReferenceAndListParam thePatient, 
    		    
    		@Description(shortDefinition="Status of the prescription")
    		@OptionalParam(name="status")
    		TokenAndListParam theStatus, 
    		    
    		@Description(shortDefinition="")
    		@OptionalParam(name="prescriber", targetTypes={  } )
    		ReferenceAndListParam thePrescriber, 
    		 
    		@IncludeParam(reverse=true)
    		Set<Include> theRevIncludes,
    		@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
    		@OptionalParam(name="_lastUpdated")
    		DateRangeParam theLastUpdated, 
    		
    		@IncludeParam(allow= {
    					"MedicationOrder:encounter" ,"MedicationOrder:medication" ,"MedicationOrder:patient" ,	"MedicationOrder:prescriber" , 	"MedicationOrder:encounter" ,"MedicationOrder:medication" , 					"MedicationOrder:patient" , 					"MedicationOrder:prescriber" , 						"MedicationOrder:encounter" , 					"MedicationOrder:medication" , 					"MedicationOrder:patient" , 					"MedicationOrder:prescriber" , 						"MedicationOrder:encounter" , 					"MedicationOrder:medication" , 					"MedicationOrder:patient" , 					"MedicationOrder:prescriber" 					, "*"
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
    		paramMap.add("datewritten", theDatewritten);
    		paramMap.add("encounter", theEncounter);
    		paramMap.add("identifier", theIdentifier);
    		paramMap.add("medication", theMedication);
    		paramMap.add("code", theCode);
    		paramMap.add("patient", thePatient);
    		paramMap.add("status", theStatus);
    		paramMap.add("prescriber", thePrescriber);
    		paramMap.setRevIncludes(theRevIncludes);
    		paramMap.setLastUpdated(theLastUpdated);
    		paramMap.setIncludes(theIncludes);
    		paramMap.setSort(theSort);
    		paramMap.setCount(theCount);			
			
			return search(paramMap); 
    			
    }


	@Override
	public List searchRaw(SearchParameterMap params) throws AppException {
		ExecutionInfo info = info();
    	
    	Query query = new Query();
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/MedicationOrder");
		
        List<ReferenceParam> patients = builder.resolveReferences("patient", "Patient");
        if (patients != null) {
        	query.putAccount("owner", referencesToIds(patients));
        }
															
		return query.execute(info);	
	}
	
	@Override
	public void processResource(Record record, MedicationOrder p) {
		super.processResource(record, p);
		if (p.getPatient().isEmpty()) {
			p.getPatient().setReferenceElement(new IdType("Patient", record.owner.toString()));
			p.getPatient().setDisplay(record.ownerName);
		}		
	}
 
}