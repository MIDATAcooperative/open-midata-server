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

package utils.fhir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Practitioner;

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
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import models.HPUser;
import models.MidataId;
import models.User;
import models.enums.Gender;
import models.enums.UserRole;
import utils.QueryTagTools;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class MidataPractitionerResourceProvider extends ResourceProvider<Practitioner, User> implements IResourceProvider {

	public  MidataPractitionerResourceProvider() {
		
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
		if (!checkAccessible()) throw new ResourceNotFoundException(theId);
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
		String gender = userToConvert.gender != null ? userToConvert.gender.toString() : Gender.UNKNOWN.toString();
		p.setGender(AdministrativeGender.valueOf(gender));
		if (userToConvert.email != null) p.addTelecom().setSystem(ContactPointSystem.EMAIL).setValue(userToConvert.email);
		if (userToConvert.phone != null && userToConvert.phone.length()>0) {
			p.addTelecom().setSystem(ContactPointSystem.PHONE).setValue(userToConvert.phone);
		}
		p.addAddress().setCity(userToConvert.city).setCountry(userToConvert.country).setPostalCode(userToConvert.zip).addLine(userToConvert.address1).addLine(userToConvert.address2);
			
		p.getMeta().addSecurity("http://midata.coop/codesystems/security", "generated", "Generated Resource");
		p.getMeta().addSecurity("http://midata.coop/codesystems/security", "platform-mapped", "Platform mapped");
		return p;
	}
	
	protected static String getPersonName(User theUser) {
		return theUser.firstname+" "+theUser.lastname;
	}
	
	public static Query buildPractitionerQuery(SearchParameterMap params) throws AppException {
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, null);
		
		builder.handleIdRestriction();
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("name", true, QueryBuilder.TYPE_STRING, "firstname", "string", "lastname");
		builder.restriction("email", true, QueryBuilder.TYPE_STRING, "emailLC");
		builder.restrictionMany("address", true, QueryBuilder.TYPE_STRING, "address1", "address2", "city", "country", "zip");
		builder.restriction("address-city", true, QueryBuilder.TYPE_STRING, "city");
		builder.restriction("address-postalcode", true, QueryBuilder.TYPE_STRING, "zip");
		builder.restriction("address-country", true, QueryBuilder.TYPE_STRING, "country");
		builder.restriction("birthdate", false, QueryBuilder.TYPE_DATE, "birthdate");						
		builder.restriction("gender", false, QueryBuilder.TYPE_CODE, "gender");
		
		return query;
	}
	
	@Override
	public List<User> searchRaw(SearchParameterMap params) throws AppException {	
		if (!checkAccessible()) return Collections.emptyList();
		
		Query query = buildPractitionerQuery(params);
		
		Map<String, Object> properties = query.retrieveAsNormalMongoQuery();
		Object keywords = query.retrieveIndexValues();
		if (keywords != null) properties.put("keywordsLC", keywords);
		if (!info().getAccessor().toString().equals(properties.get("_id"))) {
			properties.put("searchable", true);
		}
		properties.put("status", User.NON_DELETED);
		Set<HPUser> users = HPUser.getAll(properties, Sets.create("firstname","lastname","birthday","gender","email","phone","city","country","zip","address1","address2","role"));
        return new ArrayList<User>(users);
	}
 
	
	@Override
	public User fetchCurrent(IIdType theId, Practitioner r) throws AppException {
		return HPUser.getById(MidataId.from(theId.getIdPart()), User.ALL_USER);	
	}

	@Override
	public void processResource(User record, Practitioner resource) throws AppException {
		addSecurityTag(resource, QueryTagTools.SECURITY_PLATFORM_MAPPED);
		
	}

	@Override
	public List<Practitioner> parse(List<User> users, Class<Practitioner> resultClass) throws AppException {
		List<Practitioner> result = new ArrayList<Practitioner>();
		for (User user : users) {
			result.add(practitionerFromMidataUser(user));
		}
		return result;
	}

	@Override
	protected void convertToR4(Object in) {
		// Not needed. Generation on the fly		
	}
}