/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.fhir_stu3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Person;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import models.MidataId;
import models.User;
import models.enums.AuditEventType;
import models.enums.Gender;
import utils.audit.AuditEventBuilder;
import utils.audit.AuditManager;
import utils.collections.Sets;
import utils.exceptions.AppException;

/**
 * FHIR resource provider for the "Person" resource.
 * Unlike other resources this resource type triggers a user search on the account database.
 * Creates, updates and deletes are not allowed.
 *
 */
public class PersonResourceProvider extends ResourceProvider<Person, User> implements IResourceProvider {

	public  PersonResourceProvider() {
		registerSearches("Person", getClass(), "getPerson");
	}
	
	@Override
	public Class<Person> getResourceType() {
		return Person.class;
	}

	/**
	 * Default implementation to retrieve a FHIR resource by id.
	 * @param theId ID of resource to be retrieved
	 * @return Resource read from database
	 * @throws AppException
	 */
	@Read()
	public Person getResourceById(@IdParam IIdType theId) throws AppException {
		if (!checkAccessible()) throw new ResourceNotFoundException(theId);
		User member = User.getById(MidataId.from(theId.getIdPart()), User.ALL_USER);	
		if (member == null) return null;
		return personFromMidataUser(member);
	}		
	
	/**
	 * Convert a MIDATA User object into a FHIR person object
	 * @param userToConvert user to be converted into a FHIR object
	 * @return FHIR person
	 * @throws AppException
	 */
	public Person personFromMidataUser(User userToConvert) throws AppException {
		Person p = new Person();
		p.setId(userToConvert._id.toString());
		p.addName().setFamily(userToConvert.lastname).addGiven(userToConvert.firstname);
	
		//p.setBirthDate(member.birthday);
		//p.addIdentifier().setSystem("http://midata.coop/midataID").setValue(member.midataID);
		String gender = userToConvert.gender != null ? userToConvert.gender.toString() : Gender.UNKNOWN.toString();
		p.setGender(AdministrativeGender.valueOf(gender));
		
		if (userToConvert.email != null) p.addTelecom().setSystem(ContactPointSystem.EMAIL).setValue(userToConvert.email);
		if (userToConvert.phone != null && userToConvert.phone.length()>0) {
			p.addTelecom().setSystem(ContactPointSystem.PHONE).setValue(userToConvert.phone);
		}
		p.addAddress().setCity(userToConvert.city).setCountry(userToConvert.country).setPostalCode(userToConvert.zip).addLine(userToConvert.address1).addLine(userToConvert.address2);

		switch (userToConvert.role) {
		case MEMBER:
			p.addLink().setTarget(new Reference().setDisplay(getPersonName(userToConvert)).setReference("Patient/"+userToConvert._id.toString()));
			break;
		case PROVIDER:
		case RESEARCH:
			p.addLink().setTarget(new Reference().setDisplay(getPersonName(userToConvert)).setReference("Practitioner/"+userToConvert._id.toString()));
			break;
		default:
			p.addLink().setTarget(new Reference().setDisplay(getPersonName(userToConvert)).setReference("RelatedPerson/"+userToConvert._id.toString()));
			break;
		} 
		return p;
	}
	
	protected static String getPersonName(User theUser) {
		return theUser.firstname+" "+theUser.lastname;
	}
	
	   @Search()
	    public List<IBaseResource> getPerson(
	    		
	    		
	    	@Description(shortDefinition="The resource identity")
	    	@OptionalParam(name="_id")
	    	StringParam theId, 
	    		  
	    	@Description(shortDefinition="A person Identifier")
	    	@OptionalParam(name="identifier")
	    	TokenAndListParam theIdentifier, 
	    	    
	    	@Description(shortDefinition="A server defined search that may match any of the string fields in the HumanName, including family, give, prefix, suffix, suffix, and/or text")
	    	@OptionalParam(name="name")
	    	StringAndListParam theName, 
	    	    
	    	@Description(shortDefinition="A value in an email contact")
	    	@OptionalParam(name="email")
	    	TokenParam theEmail, 
	    		    
	    	@Description(shortDefinition="A server defined search that may match any of the string fields in the Address, including line, city, state, country, postalCode, and/or text")
	    	@OptionalParam(name="address")
	    	StringAndListParam theAddress, 
	    	   
	    	@Description(shortDefinition="A city specified in an address")
	    	@OptionalParam(name="address-city")
	    	StringAndListParam theAddress_city, 
	    	   
	    	/*
	    	@Description(shortDefinition="A state specified in an address")
	    	@OptionalParam(name="address-state")
	    	StringAndListParam theAddress_state, 
	    	*/
	    	
	    	@Description(shortDefinition="A postal code specified in an address")
	    	@OptionalParam(name="address-postalcode")
	    	StringParam theAddress_postalcode, 
	    		
	    	@Description(shortDefinition="A country specified in an address")
	    	@OptionalParam(name="address-country")
	    	StringParam theAddress_country, 
	    		   	    			    		   
	    	@Description(shortDefinition="The gender of the person")
	    	@OptionalParam(name="gender")
	    	TokenParam theGender, 
	    		 
	    	@Description(shortDefinition="The person's date of birth")
	    	@OptionalParam(name="birthdate")
	    	DateAndListParam theBirthdate
	    		   
	    	/*
	    	@Description(shortDefinition="The organization at which this person record is being managed")
	    	@OptionalParam(name="organization", targetTypes={  } )
	    	ReferenceAndListParam theOrganization
	    		*/
	    		/*
	    		126 			@Description(shortDefinition="Any link has this Patient, Person, RelatedPerson or Practitioner reference")
	    		127 			@OptionalParam(name="link", targetTypes={  } )
	    		128 			ReferenceAndListParam theLink, 
	    		129   
	    		130 			@Description(shortDefinition="The Person links to this Patient")
	    		131 			@OptionalParam(name="patient", targetTypes={  Patient.class   } )
	    		132 			ReferenceAndListParam thePatient, 
	    		133   
	    		134 			@Description(shortDefinition="The Person links to this Practitioner")
	    		135 			@OptionalParam(name="practitioner", targetTypes={  Practitioner.class   } )
	    		136 			ReferenceAndListParam thePractitioner, 
	    		137   
	    		138 			@Description(shortDefinition="The Person links to this RelatedPerson")
	    		139 			@OptionalParam(name="relatedperson", targetTypes={  RelatedPerson.class   } )
	    		140 			ReferenceAndListParam theRelatedperson,
	    		*/ 	    		
	    		
	    		) throws AppException {
	    	
	    	SearchParameterMap paramMap = new SearchParameterMap();
	    	paramMap.add("_id", theId);	    	
	    	paramMap.add("identifier", theIdentifier);
	    	paramMap.add("name", theName);
	    	paramMap.add("email", theEmail);
	    	paramMap.add("address", theAddress);
	    	paramMap.add("address-city", theAddress_city);
	    	//paramMap.add("address-state", theAddress_state);
	    	paramMap.add("address-postalcode", theAddress_postalcode);
	    	paramMap.add("address-country", theAddress_country);
	    	
	    	paramMap.add("gender", theGender);
	    	paramMap.add("birthdate", theBirthdate);
	    	//paramMap.add("organization", theOrganization);
	    	    		    	
	    	return search(paramMap);    	    	    	
	    }
	
	@Override
	public List<User> searchRaw(SearchParameterMap params) throws AppException {	
		if (!checkAccessible()) return Collections.emptyList();
		
		// For security reasons access to Person endpoint cannot be exposed as public endpoint
		User current = info().getRequestCache().getUserById(info().getLegacyOwner());		
		if (current == null) return Collections.emptyList();
		//
				
		if (
			 !params.containsKey("email") &&
		    (!params.containsKey("name") || !params.containsKey("birthdate")) &&
		     !params.containsKey("_id")
		) {
			throw new InvalidRequestException("Person must be restricted by _id or email or name and birthdate!");
		}
		
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, null);
						
		builder.handleIdRestriction();
		builder.restriction("name", true, QueryBuilder.TYPE_STRING, "firstname", QueryBuilder.TYPE_STRING, "lastname");
		builder.restriction("email", true, QueryBuilder.TYPE_STRING, "emailLC");
		builder.restrictionMany("address", true, QueryBuilder.TYPE_STRING, "address1", "address2", "city", "country", "zip");
		builder.restriction("address-city", true, QueryBuilder.TYPE_STRING, "city");
		builder.restriction("address-postalcode", true, QueryBuilder.TYPE_STRING, "zip");
		builder.restriction("address-country", true, QueryBuilder.TYPE_STRING, "country");
		builder.restriction("birthdate", false, QueryBuilder.TYPE_DATE, "birthdate");						
		builder.restriction("gender", false, QueryBuilder.TYPE_CODE, "gender");
		
		Map<String, Object> properties = query.retrieveAsNormalMongoQuery();
		
		Object keywords = query.retrieveIndexValues();
		if (keywords != null) properties.put("keywordsLC", keywords);
		properties.put("searchable", true);
		properties.put("status", User.NON_DELETED);
		Set<User> users = User.getAllUser(properties, Sets.create("firstname","lastname","birthday","gender","email",/*"phone",*/"city","country","zip",/*"address1","address2"*/"role"));
		if (users.size() > 5) throw new InvalidRequestException("Person search must be more specific!");
		
		for (User result : users) {
			if (!result._id.equals(info().getActor())) {
				AuditManager.instance.addAuditEvent(
						AuditEventBuilder
						.withType(AuditEventType.USER_SEARCHED)
						.withActor(info(), info().getActor())
						.withApp(info().getUsedPlugin())
				        .withModifiedActor(result));
			}
		}
		AuditManager.instance.success();
		return new ArrayList<User>(users);

	} 	

	@Override
	public User fetchCurrent(IIdType theId) throws AppException {
		return User.getById(MidataId.from(theId.getIdPart()), User.ALL_USER);
	}

	@Override
	public void processResource(User record, Person resource) throws AppException {
		
		
	}

	@Override
	public List<Person> parse(List<User> users, Class<Person> resultClass) throws AppException {
		List<Person> result = new ArrayList<Person>();
		for (User user : users) {
			result.add(personFromMidataUser(user));
		}
		return result;
	}
}