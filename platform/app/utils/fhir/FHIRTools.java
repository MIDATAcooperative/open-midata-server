package utils.fhir;

import java.util.Set;

import models.Record;
import models.User;

import org.bson.types.ObjectId;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

import utils.collections.Sets;
import utils.exceptions.InternalServerException;

import ca.uhn.fhir.model.dstu2.resource.Person;
import ca.uhn.fhir.parser.IParser;

public class FHIRTools {

	private static Set<String> PERSON = Sets.create("person");
	
	public static Person getPersonRecordOfUser(String id) throws InternalServerException {
		ObjectId mongoId = new ObjectId(id);
		User user = User.getById(mongoId, PERSON);
		IParser parser = ResourceProvider.ctx.newJsonParser();    	
    	Person p = parser.parseResource(Person.class, user.person.toString());
    	return p;
	}
	
	public static void updatePersonRecordOfUser(String id, Person person) throws InternalServerException {
		ObjectId mongoId = new ObjectId(id);
		User user = User.getById(mongoId, PERSON);
		IParser parser = ResourceProvider.ctx.newJsonParser();
	    user.person = (DBObject) JSON.parse(parser.encodeResourceToString(person));
	    User.set(mongoId, "person", user.person);		
	}
}