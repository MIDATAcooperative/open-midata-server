package utils.fhir;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Consent;
import models.Member;

import org.bson.types.ObjectId;
import org.hl7.fhir.dstu3.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;

import utils.AccessLog;
import utils.auth.ExecutionInfo;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;


public class PatientResourceProvider extends ResourceProvider<Patient> implements IResourceProvider {
 
   
    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }
     
   
    @Read()
    public Patient getResourceById(@IdParam IIdType theId) throws AppException {    	
    	    
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
    	
    
    	List<Patient> patients = getAllAccessiblePatients(info.executorId);
    	AccessLog.log("# Patient="+patients.size());
    
       
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
    		p.setBirthDate(member.birthday);
    		p.addIdentifier().setSystem("http://midata.coop/midataID").setValue(member.midataID);
    		p.setGender(AdministrativeGender.valueOf(member.gender.toString()));
    		p.addTelecom().setSystem(ContactPointSystem.EMAIL).setValue(member.email);
    		if (member.phone != null && member.phone.length()>0) {
    			p.addTelecom().setSystem(ContactPointSystem.PHONE).setValue(member.phone);
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


	@Override
	public List searchRaw(SearchParameterMap params) throws AppException {
		// TODO Auto-generated method stub
		return null;
	}

}