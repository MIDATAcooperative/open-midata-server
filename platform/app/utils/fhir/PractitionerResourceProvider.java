package utils.fhir;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import models.HPUser;
import models.MidataId;
import models.User;
import models.enums.UserRole;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class PractitionerResourceProvider extends ResourceProvider<Practitioner, User> implements IResourceProvider {

	public  PractitionerResourceProvider() {
		registerSearches("Practitioner", getClass(), "getPractitioner");
	}
	
	@Override
	public Class<Practitioner> getResourceType() {
		return Practitioner.class;
	}

	/**
	 * Default implementation to retrieve a FHIR resource by id.
	 * @param theId ID of resource to be retrieved
	 * @return Resource read from database
	 * @throws AppException
	 */
	@Read()
	public Practitioner getResourceById(@IdParam IIdType theId) throws AppException {
		HPUser member = HPUser.getById(MidataId.from(theId.getIdPart()), User.ALL_USER);	
		if (member == null) return null;
		return practitionerFromMidataUser(member);
	}
		
	/**
	 * Convert a MIDATA User object into a FHIR person object
	 * @param userToConvert user to be converted into a FHIR object
	 * @return FHIR person
	 * @throws AppException
	 */
	public static Practitioner practitionerFromMidataUser(User userToConvert) throws AppException {
		if (!userToConvert.role.equals(UserRole.PROVIDER) && !userToConvert.role.equals(UserRole.RESEARCH)) throw new InternalServerException("error.internal", "Wrong role for practitioner");
		Practitioner p = new Practitioner();
		p.setId(userToConvert._id.toString());
		p.addName().setFamily(userToConvert.lastname).addGiven(userToConvert.firstname);
	
		//p.setBirthDate(member.birthday);
		//p.addIdentifier().setSystem("http://midata.coop/midataID").setValue(member.midataID);
		p.setGender(AdministrativeGender.valueOf(userToConvert.gender.toString()));
		if (userToConvert.email != null) p.addTelecom().setSystem(ContactPointSystem.EMAIL).setValue(userToConvert.email);
		if (userToConvert.phone != null && userToConvert.phone.length()>0) {
			p.addTelecom().setSystem(ContactPointSystem.PHONE).setValue(userToConvert.phone);
		}
		p.addAddress().setCity(userToConvert.city).setCountry(userToConvert.country).setPostalCode(userToConvert.zip).addLine(userToConvert.address1).addLine(userToConvert.address2);
			
		return p;
	}
	
	protected static String getPersonName(User theUser) {
		return theUser.firstname+" "+theUser.lastname;
	}
	
	   @Search()
	    public List<IBaseResource> getPractitioner(
	    		
	    		
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
	    	DateAndListParam theBirthdate,
	    	
	    	@OptionalParam(name="_page")
			StringParam _page,
			
			RequestDetails theDetails
	    		   	    	
	    		
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
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, null);
		
		builder.handleIdRestriction();
		builder.restriction("name", true, QueryBuilder.TYPE_STRING, "firstname", "string", "lastname");
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
		Set<HPUser> users = HPUser.getAll(properties, Sets.create("firstname","lastname","birthday","gender","email","phone","city","country","zip","address1","address2","role"));
        return new ArrayList<User>(users);
	}
 
	
	@Override
	public User fetchCurrent(IIdType theId) throws AppException {
		return HPUser.getById(MidataId.from(theId.getIdPart()), User.ALL_USER);	
	}

	@Override
	public void processResource(User record, Practitioner resource) throws AppException {
		
		
	}

	@Override
	public List<Practitioner> parse(List<User> users, Class<Practitioner> resultClass) throws AppException {
		List<Practitioner> result = new ArrayList<Practitioner>();
		for (User user : users) {
			result.add(practitionerFromMidataUser(user));
		}
		return result;
	}
}