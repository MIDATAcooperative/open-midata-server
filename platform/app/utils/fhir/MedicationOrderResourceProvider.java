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

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;

public class MedicationOrderResourceProvider extends ResourceProvider<MedicationOrder> implements IResourceProvider {
 
 
    @Override
    public Class<MedicationOrder> getResourceType() {
        return MedicationOrder.class;
    }
     
   
    @Read()
    public MedicationOrder getResourceById(@IdParam IdType theId) {    	
    	    
        return null;
    }
 
    @Search()
    public List<MedicationOrder> getMedicationOrder(
    		@OptionalParam(name = MedicationOrder.SP_PATIENT) ReferenceAndListParam thePatient,
    		@OptionalParam(name = MedicationOrder.SP_STATUS) StringAndListParam theStatus
    		) throws AppException {
    	
    		    		
    		SearchParameterMap paramMap = new SearchParameterMap();						
			paramMap.add("patient", thePatient);			
			
			return search(paramMap); 
    			
    }


	@Override
	public List searchRaw(SearchParameterMap params) throws AppException {
		ExecutionInfo info = info();
    	
    	Query query = new Query();
		QueryBuilder builder = new QueryBuilder(params, query);
		
        List<ReferenceParam> patients = builder.resolveReferences("patient", "Patient");
        if (patients != null) {
        	query.putAccount("owner", referencesToIds(patients));
        }
															
		return query.execute(info);	
	}
	
	public static void processResource(Record record, MedicationOrder p) {
		ResourceProvider.processResource(record, p);
		if (p.getPatient().isEmpty()) {
			p.getPatient().setReferenceElement(new IdType("Patient", record.owner.toString()));
			p.getPatient().setDisplay(record.ownerName);
		}		
	}
 
}