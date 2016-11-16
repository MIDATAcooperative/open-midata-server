package utils.fhir;

import java.util.Set;

import org.hl7.fhir.dstu3.model.Person;
import org.hl7.fhir.dstu3.model.Reference;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

import ca.uhn.fhir.parser.IParser;
import models.MidataId;
import models.User;
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
