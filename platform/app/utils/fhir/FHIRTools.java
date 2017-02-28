package utils.fhir;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Person;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IIdType;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import models.HPUser;
import models.Member;
import models.MidataId;
import models.TypedMidataId;
import models.User;
import models.UserGroup;
import models.enums.UserRole;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class FHIRTools {

	private static Set<String> PERSON = Sets.create("person");
	private static Set<String> REFERENCE = Sets.create("role", "firstname", "lastname");
	
/*
	public static Person getPersonRecordOfUser(String id) throws InternalServerException {
		MidataId mongoId = new MidataId(id);
		User user = User.getById(mongoId, PERSON);
		IParser parser = ResourceProvider.ctx.newJsonParser();    	
    	Person p = parser.parseResource(Person.class, user.person.toString());
    	return p;
	}
	*/
	
	/**
	 * Returns a FHIR reference to the specified user. Automatically chooses Patient, Practitioner or RelatedPerson
	 * @param id
	 * @return
	 * @throws InternalServerException
	 */
	public static Reference getReferenceToUser(MidataId id) throws AppException {
	
		User user = ResourceProvider.info().cache.getUserById(id);
		if (user == null) throw new InternalServerException("error.internal", "Person not found");
		String type = "RelatedPerson";
		switch (user.role) {
		case MEMBER : type = "Patient";break;
		case PROVIDER : type = "Practitioner";break;
		}
		return new Reference().setDisplay(user.firstname+" "+user.lastname).setReference(type+"/"+user._id.toString());
	}
	
	/**
	 * Returns the MidataId of a user represented by a FHIR reference
	 * @param userRef The FHIR reference to be checked and converted
	 * @return the MidataId 
	 * @throws UnprocessableEntityException if the reference could not be resolved.
	 * @throws InternalServerException
	 */
	public static MidataId getUserIdFromReference(IIdType userRef) throws AppException {
		String rt = userRef.getResourceType();
		MidataId id = MidataId.from(userRef.getIdPart());
		
		User user = User.getById(id, Sets.create("role"));
		if (user == null) throw new UnprocessableEntityException("Invalid Person Reference");
		if (rt != null) {
				if (rt.equals("Patient") && user.role != UserRole.MEMBER) throw new UnprocessableEntityException("Invalid Patient reference");
				if (rt.equals("Practitioner") && user.role != UserRole.PROVIDER) throw new UnprocessableEntityException("Invalid Practitioner reference");
		}
		return id;
		
	}
	
	/**
	 * Returns the MidataId of a usergroup represented by a FHIR reference
	 * @param userRef The FHIR reference to be checked and converted
	 * @return the MidataId 
	 * @throws UnprocessableEntityException if the reference could not be resolved.
	 * @throws InternalServerException
	 */
	public static MidataId getUserGroupIdFromReference(IIdType userRef) throws AppException {
		String rt = userRef.getResourceType();
		MidataId id = MidataId.from(userRef.getIdPart());
		
		UserGroup usergroup = UserGroup.getById(id, Sets.create("name"));
		if (usergroup == null) throw new UnprocessableEntityException("Invalid Group Reference");
		
		return id;
		
	}
	
	public static TypedMidataId getMidataIdFromReference(IIdType ref) throws AppException {
		String rt = ref.getResourceType();
		if (rt.equals("Group")) {
			MidataId result = getUserGroupIdFromReference(ref);
			return result != null ? new TypedMidataId(result, rt) : null; 
		} else {
			MidataId result = getUserIdFromReference(ref);
			return result != null ? new TypedMidataId(result, rt) : null;
		}				
	}
	
	public static boolean isUserFromMidata(IIdType ref) {
		String rt = ref.getResourceType();
		if (rt == null) return false;
		if (rt.equals("Patient") || rt.equals("Practitioner")) {
			// TODO check base url
			
			return true;
		}
		if (rt.equals("Group")) {
			// TODO check base url
			
			return true;
		}
		return false;
	}
	
	public static Reference resolve(Reference ref) throws InternalServerException {
		if (ref == null) return null;
		if (ref.hasIdentifier()) {
			Identifier id = ref.getIdentifier();
			String system = id.getSystem();
			String value = id.getValue();
			
			User target = null;
			String type = null;
			if (system.equals("http://midata.coop/identifier/patient-login")) {
				target = Member.getByEmail(value, Sets.create("_id", "role", "firstname", "lastname"));
				type = "Patient";
			} else if (system.equals("http://midata.coop/identifier/practitioner-login")) {
				target = HPUser.getByEmail(value, Sets.create("_id", "role", "firstname", "lastname"));
				type = "Practitioner";
			}
			if (type == null) return ref;
			
			if (target == null) throw new UnprocessableEntityException("References Patient not found");
			ref.setReference(type+"/"+target._id.toString());
			ref.setIdentifier(null);
			ref.setDisplay(target.firstname+" "+target.lastname);
		}		
		
		return ref;
	}
	
	/**
	 * Checks if all references point to resources of given resource types
	 * @param refs list of references to check
	 * @param types allowed types
	 * @return
	 */
	public static boolean areAllOfType(Collection<ReferenceParam> refs, Set<String> types) {
		for (ReferenceParam ref : refs) {
			String type = ref.getResourceType();
			if (type != null && ! types.contains(type)) return false;
		}
	    return true;
	}
	
    public static Set<String> referencesToIds(Collection<ReferenceParam> refs) {
		
		Set<String> ids = new HashSet<String>();
		for (ReferenceParam ref : refs)
			ids.add(ref.getIdPart().toString());
		return ids;				
	}
    
    public static String getStringFromCodeableConcept(CodeableConcept cc, String defaultValue) {
    	if (cc.hasText()) return cc.getText();
    	for (Coding coding : cc.getCoding()) {
    	  if (coding.hasDisplay()) return coding.getDisplay();
    	}
    	return defaultValue;
    }
	
	/*
	public static void updatePersonRecordOfUser(String id, Person person) throws InternalServerException {
		MidataId mongoId = new MidataId(id);
		User user = User.getById(mongoId, PERSON);
		IParser parser = ResourceProvider.ctx.newJsonParser();
	    user.person = (DBObject) JSON.parse(parser.encodeResourceToString(person));
	    User.set(mongoId, "person", user.person);		
	}
	*/
}
