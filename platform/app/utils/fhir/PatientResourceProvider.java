package utils.fhir;

import java.util.ArrayList;
import java.util.Collections;
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

import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.IdentifierUseEnum;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;

public class PatientResourceProvider extends ResourceProvider implements IResourceProvider {
 
    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
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
    public Patient getResourceById(@IdParam IdDt theId) {    	
    	    
        Patient patient = new Patient();
        patient.addIdentifier();
        patient.getIdentifier().get(0).setSystem(new UriDt("urn:hapitest:mrns"));
        patient.getIdentifier().get(0).setValue("00002");
        patient.addName().addFamily("Test");
        patient.getName().get(0).addGiven("PatientOne");
        patient.setGender(AdministrativeGenderEnum.FEMALE);
        return patient;
    }
 
   
    @Search()
    public List<Patient> getPatient(
    		@OptionalParam(name = Patient.SP_NAME) StringParam theName,
    		@OptionalParam(name = Patient.SP_GENDER) TokenParam theGender,
    		@OptionalParam(name = Patient.SP_BIRTHDATE) DateParam theBirthdate
    		) throws AppException {
    	try {
    	ExecutionInfo info = info();
    	
    	Map<String, Object> criteria = new HashMap<String, Object>();
    	
    	if (theName != null) {
    		addRestriction(criteria, theName, "name", "family");
    	}
    	
    	if (theGender != null) {
    		addRestriction(criteria, theGender, "gender");
    	}
    	
    	if (theBirthdate != null) {
    		addRestriction(criteria, theBirthdate, "birthDate");
    	}
    	
    	AccessLog.logQuery(criteria, Sets.create("data"));
    	List<Record> result = RecordManager.instance.list(info.executorId, info.targetAPS, CMaps.map("format","fhir").map("content", "Patient").map("data", criteria), Sets.create("data"));
    	List<Patient> patients = new ArrayList<Patient>();
    	IParser parser = ctx().newJsonParser();    	
    	for (Record rec : result) {
    		
    		Patient p = parser.parseResource(Patient.class, rec.data.toString());
    		patients.add(p);
    	}
       
        return patients;
        
    	} catch (AppException e) {
    		AccessLog.debug("ERROR");
    		return null;
    	}
    }
 
}