package utils.fhir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Record;
import utils.access.AccessLog;
import utils.access.RecordManager;
import utils.auth.ExecutionInfo;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;

public class ObservationResourceProvider extends ResourceProvider implements IResourceProvider {
 
    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    @Override
    public Class<Observation> getResourceType() {
        return Observation.class;
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
    public Observation getResourceById(@IdParam IdDt theId) {    	
    	    
        return null;
    }
 
    @Search()
    public List<Observation> getObservation(
    		@OptionalParam(name = Observation.SP_PATIENT) ReferenceParam thePatient
    		) throws AppException {
    	try {
    	ExecutionInfo info = info();
    	
    	Map<String, Object> criteria = new HashMap<String, Object>();
    	
    	if (thePatient != null) {
    		addRestriction(criteria, "Patient", thePatient, "subject", "reference");
    	}
    	    	
    	
    	AccessLog.logQuery(criteria, Sets.create("data"));
    	List<Record> result = RecordManager.instance.list(info.executorId, info.targetAPS, CMaps.map("format","fhir").map("content", "Observation").map("data", criteria), Sets.create("data"));
    	List<Observation> patients = new ArrayList<Observation>();
    	IParser parser = ctx().newJsonParser();    	
    	for (Record rec : result) {
    		
    		Observation p = parser.parseResource(Observation.class, rec.data.toString());
    		patients.add(p);
    	}
       
        return patients;
        
    	} catch (AppException e) {
    		AccessLog.debug("ERROR");
    		return null;
    	}
    }
 
}