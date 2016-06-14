package utils.fhir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Record;
import utils.AccessLog;
import utils.access.RecordManager;
import utils.auth.ExecutionInfo;
import utils.collections.CMaps;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.exceptions.AppException;
import ca.uhn.fhir.model.dstu2.resource.MedicationOrder;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;

public class MedicationOrderResourceProvider extends ResourceProvider implements IResourceProvider {
 
    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    @Override
    public Class<MedicationOrder> getResourceType() {
        return MedicationOrder.class;
    }
     
    /**
     * The "@Read" annotation indicates that this method supports the
     * read operation. Read operations should return a single resource
     * instance.
     *
     * @param theId
     *    The read operation takes one parameter, which must be of type
     *    IdDt and must be annotated with the "@Read.IdParam" annotation.
     * @return
     *    Returns a resource matching this identifier, or null if none exists.
     */
    @Read()
    public MedicationOrder getResourceById(@IdParam IdDt theId) {    	
    	    
        return null;
    }
 
    @Search()
    public List<MedicationOrder> getMedicationOrder(
    		@OptionalParam(name = MedicationOrder.SP_PATIENT) ReferenceOrListParam thePatient,
    		@OptionalParam(name = MedicationOrder.SP_STATUS) StringOrListParam theCode
    		) throws AppException {
    	try {
    	ExecutionInfo info = info();
    	
    	Map<String, Object> criteria = new HashMap<String, Object>();
    	Map<String, Object> accountCriteria = CMaps.map("format","fhir/MedicationOrder");
    	
    	if (thePatient != null) {    		
    		accountCriteria.put("owner", refsToObjectIds(thePatient));
    	}
    	    	    	    	
    	AccessLog.logQuery(criteria, Sets.create("data"));
    	List<Record> result = RecordManager.instance.list(info.executorId, info.targetAPS, accountCriteria, Sets.create("owner", "ownerName", "version", "created", "lastUpdated", "data"));
    	ReferenceTool.resolveOwners(result, true, false);
    	List<MedicationOrder> patients = new ArrayList<MedicationOrder>();
    	IParser parser = ctx().newJsonParser();    	
    	for (Record rec : result) {
    		try {
    		MedicationOrder p = parser.parseResource(MedicationOrder.class, rec.data.toString());
    		processResource(rec, p);
    		if (p.getPatient().isEmpty()) {
    		  p.getPatient().setReference(new IdDt("Patient", rec.owner.toString()));
    		  p.getPatient().setDisplay(rec.ownerName);
    		}
    		patients.add(p);
    		} catch (DataFormatException e) {}
    	}
       
        return patients;
        
    	} catch (AppException e) {
    		AccessLog.log("ERROR");
    		return null;
    	}
    }
 
}