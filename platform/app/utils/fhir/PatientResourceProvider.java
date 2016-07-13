package utils.fhir;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Consent;
import models.Member;

import org.bson.types.ObjectId;

import utils.AccessLog;
import utils.auth.ExecutionInfo;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContactPointSystemEnum;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
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
    public Patient getResourceById(@IdParam IdDt theId) throws AppException {    	
    	    
    	String id = theId.getIdPart();
    	ObjectId targetId = new ObjectId(id);
    	
    	List<Patient> result = patientsFromUserAccounts(CMaps.map("_id", targetId));    	
        return result.iterator().next();
    }
 
   
    @Search()
    public List<Patient> getPatient(
    		@OptionalParam(name = Patient.SP_NAME) StringParam theName,
    		@OptionalParam(name = Patient.SP_GENDER) TokenParam theGender,
    		@OptionalParam(name = Patient.SP_BIRTHDATE) DateParam theBirthdate
    		) throws AppException {
    	try {
    	ExecutionInfo info = info();
    	
    	/*Map<String, Object> criteria = new HashMap<String, Object>();
    	
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
    	List<Record> result = RecordManager.instance.list(info.executorId, info.targetAPS, CMaps.map("format","fhir/Observation").map("content", "Patient").map("data", criteria), Sets.create("data"));
    	*/
    	List<Patient> patients = getAllAccessiblePatients(info.executorId);
    	
    	/*IParser parser = ctx().newJsonParser();    	
    	for (Record rec : result) {
    		
    		Patient p = parser.parseResource(Patient.class, rec.data.toString());
    		patients.add(p);
    	}*/
       
        return patients;
        
    	} catch (AppException e) {
    		AccessLog.log("ERROR");
    		return null;
    	}
    }
    
    private List<Patient> patientsFromUserAccounts(Map<String, Object> properties) throws AppException {
    	Set<Member> members = Member.getAll(properties, Sets.create("_id", "email", "firstname", "lastname", "gender", "birthday", "midataID", "phone", "city", "country", "zip", "address1", "address2"), 0);
    	List<Patient> result = new ArrayList<Patient>();
    	for (Member member : members) {
    		Patient p = new Patient();
    		p.setId(member._id.toString());
    		p.addName().addFamily(member.lastname).addGiven(member.firstname);
    		p.setBirthDateWithDayPrecision(member.birthday);
    		p.addIdentifier().setSystem("http://midata.coop/midataID").setValue(member.midataID);
    		p.setGender(AdministrativeGenderEnum.valueOf(member.gender.toString()));
    		p.addTelecom().setSystem(ContactPointSystemEnum.EMAIL).setValue(member.email);
    		if (member.phone != null && member.phone.length()>0) {
    			p.addTelecom().setSystem(ContactPointSystemEnum.PHONE).setValue(member.phone);
    		}
    		p.addAddress().setCity(member.city).setCountry(member.country).setPostalCode(member.zip).addLine(member.address1).addLine(member.address2);
    		result.add(p);
    	}
    	return result;
    }
    
    private Set<ObjectId> accessableAccounts(ObjectId executor) throws AppException {
    	Set<Consent> consents = Consent.getAllActiveByAuthorized(executor);
    	Set<ObjectId> result = new HashSet<ObjectId>();
    	for (Consent consent : consents) {
    		result.add(consent.owner);
    	}
    	return result;
    }
    
    private List<Patient> getAllAccessiblePatients(ObjectId executor) throws AppException {
       Set<ObjectId> acc = accessableAccounts(executor);
       acc.add(executor);
       return patientsFromUserAccounts(CMaps.map("_id", acc));
    }
 
}