package utils.fhir;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Person;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IIdType;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import models.MidataId;
import models.User;
import models.enums.UserRole;
import utils.collections.Sets;
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
	public static Reference getReferenceToUser(MidataId id) throws InternalServerException {
	
		User user = User.getById(id, REFERENCE);
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
	public static MidataId getUserIdFromReference(IIdType userRef) throws UnprocessableEntityException {
		String rt = userRef.getResourceType();
		MidataId id = MidataId.from(userRef.getIdPart());
		try {
			User user = User.getById(id, Sets.create("role"));
			if (user == null) throw new UnprocessableEntityException("Invalid Person Reference");
			if (rt != null) {
				if (rt.equals("Patient") && user.role != UserRole.MEMBER) throw new UnprocessableEntityException("Invalid Patient reference");
				if (rt.equals("Practitioner") && user.role != UserRole.PROVIDER) throw new UnprocessableEntityException("Invalid Practitioner reference");
			}
			return id;
		} catch (InternalServerException e) {
			throw new InternalErrorException(e);
		}
	}
	
	public static boolean isUserFromMidata(IIdType ref) {
		String rt = ref.getResourceType();
		if (rt.equals("Patient") || rt.equals("Practitioner")) {
			// TODO check base url
			
			return true;
		}
		return false;
	}
	
	
    public static Set<String> referencesToIds(Collection<ReferenceParam> refs) {
		
		Set<String> ids = new HashSet<String>();
		for (ReferenceParam ref : refs)
			ids.add(ref.getIdPart().toString());
		return ids;				
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
