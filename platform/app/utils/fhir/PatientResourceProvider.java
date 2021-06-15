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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.bson.BSONObject;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.PatientCommunicationComponent;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.instance.model.api.IIdType;

import com.typesafe.config.Config;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.History;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import controllers.Application;
import controllers.Circles;
import models.Circle;
import models.Consent;
import models.HPUser;
import models.HealthcareProvider;
import models.Member;
import models.MemberKey;
import models.MidataId;
import models.Plugin;
import models.Record;
import models.Study;
import models.StudyAppLink;
import models.StudyParticipation;
import models.User;
import models.enums.AccountActionFlags;
import models.enums.AccountSecurityLevel;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.InformationType;
import models.enums.JoinMethod;
import models.enums.LinkTargetType;
import models.enums.StudyAppLinkType;
import models.enums.SubUserRole;
import models.enums.UserFeature;
import models.enums.UserRole;
import models.enums.UserStatus;
import models.enums.WritePermissionType;
import utils.AccessLog;
import utils.ApplicationTools;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.RuntimeConstants;
import utils.access.AccessContext;
import utils.access.AccountCreationAccessContext;
import utils.access.DBIterator;
import utils.access.Feature_Pseudonymization;
import utils.access.RecordManager;
import utils.audit.AuditEventBuilder;
import utils.audit.AuditManager;
import utils.auth.CodeGenerator;
import utils.auth.ExecutionInfo;
import utils.auth.KeyManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.json.JsonOutput;

public class PatientResourceProvider extends RecordBasedResourceProvider<Patient> implements IResourceProvider {

	
	public PatientResourceProvider() {
		searchParamNameToPathMap.put("Patient.general-practitioner", "generalPractitioner");
		searchParamNameToPathMap.put("Patient.link", "link.other");
		searchParamNameToPathMap.put("Patient:organization", "managingOrganization");

		registerSearches("Patient", getClass(), "getPatient");
	}

	@Override
	public Class<Patient> getResourceType() {
		return Patient.class;
	}

	@Read()
	public Patient getResourceById(@IdParam IIdType theId) throws AppException {

		String id = theId.getIdPart();
		MidataId targetId = new MidataId(id);

		ExecutionInfo info = info();
		List<Record> allRecs = RecordManager.instance.list(info.role, info.context, CMaps.map("owner", targetId).map("format", "fhir/Patient").map("data", CMaps.map("id", targetId.toString())),
				RecordManager.COMPLETE_DATA);

		if (allRecs == null || allRecs.size() == 0)
			return null;

		Record record = allRecs.get(0);
		convertToR4(record, record.data);
		IParser parser = ctx().newJsonParser();
		Patient p = parser.parseResource(getResourceType(), JsonOutput.toJsonString(record.data));
		processResource(record, p);
		return p;
	}
	
	
	@Override
	public Record fetchCurrent(IIdType theId)  {
		try {
			if (theId == null) throw new UnprocessableEntityException("id missing");
			if (theId.getIdPart() == null || theId.getIdPart().length() == 0) throw new UnprocessableEntityException("id local part missing");
			if (!isLocalId(theId)) throw new UnprocessableEntityException("id is not local resource");
			
			ExecutionInfo info = info();
			MidataId targetId = MidataId.from(theId.getIdPart());
			List<Record> allRecs = RecordManager.instance.list(info.role, info.context, CMaps.map("owner", targetId).map("format", "fhir/Patient").map("data", CMaps.map("id", targetId.toString())),
					RecordManager.COMPLETE_DATA);

			if (allRecs == null || allRecs.size() == 0)
				throw new ResourceNotFoundException("not found");

			Record record = allRecs.get(0);
			
			
			if (record == null) throw new ResourceNotFoundException("Resource "+theId.getIdPart()+" not found."); 
			if (!record.format.equals("fhir/"+theId.getResourceType())) throw new ResourceNotFoundException("Resource "+theId.getIdPart()+" has wrong resource type."); 
												
			String versionId = theId.getVersionIdPart();
			if (versionId != null) {	  
			   if (!versionId.equals(record.version)) {
			     throw new ResourceVersionConflictException("Unexpected version");
			   }
			}		
			return record;
		} catch (AppException e) {
			ErrorReporter.report("FHIR (fetch current record)", null, e);	 
			throw new InternalErrorException(e.getMessage());
		} catch (NullPointerException e2) {
			ErrorReporter.report("FHIR (fetch current record)", null, e2);	 
			throw new InternalErrorException("internal error during fetch current record version");
		}
	}
	

	@History()
	@Override
	public List<Patient> getHistory(@IdParam IIdType theId) throws AppException {
		String id = theId.getIdPart();
		MidataId targetId = new MidataId(id);

		List<Record> records = RecordManager.instance.list(info().role, info().context,
				CMaps.map("owner", targetId).map("format", "fhir/Patient").map("history", true).map("sort", "lastUpdated desc"), RecordManager.COMPLETE_DATA);
		if (records.isEmpty())
			throw new ResourceNotFoundException(theId);

		List<Patient> result = new ArrayList<Patient>(records.size());
		IParser parser = ctx().newJsonParser();
		for (Record record : records) {
			Patient p = parser.parseResource(getResourceType(), JsonOutput.toJsonString(record.data));
			processResource(record, p);
			result.add(p);
		}

		return result;
	}

	@Search()
	public Bundle getPatient(@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,

			@Description(shortDefinition = "The resource language") @OptionalParam(name = "_language") StringAndListParam theResourceLanguage,

			/*
			 * @Description(
			 * shortDefinition="Search the contents of the resource's data using a fulltext search"
			 * )
			 * 
			 * @OptionalParam(name=ca.uhn.fhir.rest.server.Constants.
			 * PARAM_CONTENT) StringAndListParam theFtContent,
			 * 
			 * @Description(
			 * shortDefinition="Search the contents of the resource's narrative using a fulltext search"
			 * )
			 * 
			 * @OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_TEXT)
			 * StringAndListParam theFtText,
			 * 
			 * @Description(
			 * shortDefinition="Search for resources which have the given tag")
			 * 
			 * @OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_TAG)
			 * TokenAndListParam theSearchForTag,
			 * 
			 * @Description(
			 * shortDefinition="Search for resources which have the given security labels"
			 * )
			 * 
			 * @OptionalParam(name=ca.uhn.fhir.rest.server.Constants.
			 * PARAM_SECURITY) TokenAndListParam theSearchForSecurity,
			 * 
			 * @Description(
			 * shortDefinition="Search for resources which have the given profile"
			 * )
			 * 
			 * @OptionalParam(name=ca.uhn.fhir.rest.server.Constants.
			 * PARAM_PROFILE) UriAndListParam theSearchForProfile,
			 */
			/*
			 * @Description(
			 * shortDefinition="Return resources linked to by the given target")
			 * 
			 * @OptionalParam(name="_has") HasAndListParam theHas,
			 */

			@Description(shortDefinition = "A patient identifier") @OptionalParam(name = "identifier") TokenAndListParam theIdentifier,

			@Description(shortDefinition = "A portion of either family or given name of the patient") @OptionalParam(name = "name") StringAndListParam theName,

			@Description(shortDefinition = "A portion of the family name of the patient") @OptionalParam(name = "family") StringAndListParam theFamily,

			@Description(shortDefinition = "A portion of the given name of the patient") @OptionalParam(name = "given") StringAndListParam theGiven,

			@Description(shortDefinition = "A portion of either family or given name using some kind of phonetic matching algorithm") @OptionalParam(name = "phonetic") StringAndListParam thePhonetic,

			@Description(shortDefinition = "The value in any kind of telecom details of the patient") @OptionalParam(name = "telecom") TokenAndListParam theTelecom,

			@Description(shortDefinition = "A value in a phone contact") @OptionalParam(name = "phone") TokenAndListParam thePhone,

			@Description(shortDefinition = "A value in an email contact") @OptionalParam(name = "email") TokenAndListParam theEmail,

			@Description(shortDefinition = "An address in any kind of address/part of the patient") @OptionalParam(name = "address") StringAndListParam theAddress,

			@Description(shortDefinition = "A city specified in an address") @OptionalParam(name = "address-city") StringAndListParam theAddress_city,

			@Description(shortDefinition = "A state specified in an address") @OptionalParam(name = "address-state") StringAndListParam theAddress_state,

			@Description(shortDefinition = "A postalCode specified in an address") @OptionalParam(name = "address-postalcode") StringAndListParam theAddress_postalcode,

			@Description(shortDefinition = "A country specified in an address") @OptionalParam(name = "address-country") StringAndListParam theAddress_country,

			@Description(shortDefinition = "A use code specified in an address") @OptionalParam(name = "address-use") TokenAndListParam theAddress_use,

			@Description(shortDefinition = "Gender of the patient") @OptionalParam(name = "gender") TokenAndListParam theGender,

			@Description(shortDefinition = "Language code (irrespective of use value)") @OptionalParam(name = "language") TokenAndListParam theLanguage,

			@Description(shortDefinition = "The patient's date of birth") @OptionalParam(name = "birthdate") DateAndListParam theBirthdate,

			@Description(shortDefinition = "The organization at which this person is a patient") @OptionalParam(name = "organization", targetTypes = {}) ReferenceAndListParam theOrganization,

			@Description(shortDefinition = "Patient's nominated care provider, could be a care manager, not the organization that manages the record") @OptionalParam(name = "general-practitioner", targetTypes = {}) ReferenceAndListParam theCareprovider,

			@Description(shortDefinition = "Whether the patient record is active") @OptionalParam(name = "active") TokenAndListParam theActive,

			@Description(shortDefinition = "The species for animal patients") @OptionalParam(name = "animal-species") TokenAndListParam theAnimal_species,

			@Description(shortDefinition = "The breed for animal patients") @OptionalParam(name = "animal-breed") TokenAndListParam theAnimal_breed,

			@Description(shortDefinition = "All patients linked to the given patient") @OptionalParam(name = "link", targetTypes = {}) ReferenceAndListParam theLink,

			@Description(shortDefinition = "This patient has been marked as deceased, or as a death date entered") @OptionalParam(name = "deceased") TokenAndListParam theDeceased,

			@Description(shortDefinition = "The date of death has been provided and satisfies this search value") @OptionalParam(name = "deathdate") DateAndListParam theDeathdate,

			@IncludeParam(reverse = true) Set<Include> theRevIncludes,
			@Description(shortDefinition = "Only return resources which were last updated as specified by the given range") @OptionalParam(name = "_lastUpdated") DateRangeParam theLastUpdated,

			@IncludeParam(allow = { "Patient:general-practitioner", "Patient:link", "Patient:organization", "*" }) Set<Include> theIncludes,

			@Sort SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount,
			
			@OptionalParam(name="_page")
			StringParam _page,
			
			RequestDetails theDetails) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();
		paramMap.add("_id", theId);
		paramMap.add("_language", theResourceLanguage);
		/*
		 * paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT,
		 * theFtContent);
		 * paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TEXT,
		 * theFtText); paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TAG,
		 * theSearchForTag);
		 * paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY,
		 * theSearchForSecurity);
		 * paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE,
		 * theSearchForProfile);
		 */
		// paramMap.add("_has", theHas);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("name", theName);
		paramMap.add("family", theFamily);
		paramMap.add("given", theGiven);
		paramMap.add("phonetic", thePhonetic);
		paramMap.add("telecom", theTelecom);
		paramMap.add("phone", thePhone);
		paramMap.add("email", theEmail);
		paramMap.add("address", theAddress);
		paramMap.add("address-city", theAddress_city);
		paramMap.add("address-state", theAddress_state);
		paramMap.add("address-postalcode", theAddress_postalcode);
		paramMap.add("address-country", theAddress_country);
		paramMap.add("address-use", theAddress_use);
		paramMap.add("gender", theGender);
		paramMap.add("language", theLanguage);
		paramMap.add("birthdate", theBirthdate);
		paramMap.add("organization", theOrganization);
		paramMap.add("general-practitioner", theCareprovider);
		paramMap.add("active", theActive);
		paramMap.add("animal-species", theAnimal_species);
		paramMap.add("animal-breed", theAnimal_breed);
		paramMap.add("link", theLink);
		paramMap.add("deceased", theDeceased);
		paramMap.add("deathdate", theDeathdate);
		paramMap.setRevIncludes(theRevIncludes);
		paramMap.setLastUpdated(theLastUpdated);
		paramMap.setIncludes(theIncludes);
		paramMap.setSort(theSort);
		paramMap.setCount(theCount);
		paramMap.setFrom(_page != null ? _page.getValue() : null);

		return searchBundle(paramMap, theDetails);
	}

	/*
	 * private List<Patient> patientsFromUserAccounts(Map<String, Object>
	 * properties) throws AppException { Set<Member> members =
	 * Member.getAll(properties, Sets.create("_id", "email", "firstname",
	 * "lastname", "gender", "birthday", "midataID", "phone", "city", "country",
	 * "zip", "address1", "address2"), 0); List<Patient> result = new
	 * ArrayList<Patient>(); for (Member member : members) { Patient p = new
	 * Patient(); p.setId(member._id.toString());
	 * p.addName().addFamily(member.lastname).addGiven(member.firstname);
	 * p.setBirthDate(member.birthday);
	 * p.addIdentifier().setSystem("http://midata.coop/midataID").setValue(
	 * member.midataID);
	 * p.setGender(AdministrativeGender.valueOf(member.gender.toString()));
	 * p.addTelecom().setSystem(ContactPointSystem.EMAIL).setValue(member.email)
	 * ; if (member.phone != null && member.phone.length()>0) {
	 * p.addTelecom().setSystem(ContactPointSystem.PHONE).setValue(member.phone)
	 * ; } p.addAddress().setCity(member.city).setCountry(member.country).
	 * setPostalCode(member.zip).addLine(member.address1).addLine(member.
	 * address2); result.add(p); } return result; }
	 */

	@Override
	public List<Record> searchRaw(SearchParameterMap params) throws AppException {
		ExecutionInfo info = info();

		Query query = new Query();
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Patient");
        query.removeAccount("limit");
		
		if (params.containsKey("_id")) {
			Set<String> ids = builder.paramToStrings("_id");
			if (ids != null)
				query.putAccount("owner", ids);
		}

		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("family", true, QueryBuilder.TYPE_STRING, "name.family");
		builder.restriction("birthdate", true, QueryBuilder.TYPE_DATE, "birthDate");
		builder.restriction("deathdate", true, QueryBuilder.TYPE_DATETIME, "deceasedDateTime");
		builder.restriction("given", true, QueryBuilder.TYPE_STRING, "name.given");
		builder.restriction("gender", false, QueryBuilder.TYPE_CODE, "gender");

		builder.restriction("name", true, QueryBuilder.TYPE_STRING, "name.given", QueryBuilder.TYPE_STRING, "name.family");
		builder.restriction("email", true, QueryBuilder.TYPE_CODE, "telecom.value");
		builder.restriction("address-city", true, QueryBuilder.TYPE_STRING, "address.city");
		builder.restriction("address-country", true, QueryBuilder.TYPE_STRING, "address.country");
		builder.restriction("address-postalcode", true, QueryBuilder.TYPE_STRING, "address.postalCode");
		builder.restriction("address-state", true, QueryBuilder.TYPE_STRING, "address.state");
		builder.restriction("address-use", false, QueryBuilder.TYPE_CODE, "address.use");

		builder.restrictionMany("address", true, QueryBuilder.TYPE_STRING, "address.text", "address.line", "address.city", "address.district", "address.state", "address.postalCode",
				"address.country");

		builder.restriction("language", false, QueryBuilder.TYPE_CODE, "communication.language");
		builder.restriction("phone", true, QueryBuilder.TYPE_CODE, "telecom.value");
		builder.restriction("telecom", true, QueryBuilder.TYPE_CODE, "telecom.value");
		builder.restriction("active", false, QueryBuilder.TYPE_BOOLEAN, "active");

		DBIterator<Record> recs = query.executeIterator(info);
		List<Record> result = new ArrayList<Record>();
		int limit = params.getCount() != null ? params.getCount() : Integer.MAX_VALUE;
		while (recs.hasNext() && result.size() <= limit) {
		    Record record = recs.next();
			if (record.data == null)
				continue;
			Object id = record.data.get("id");
			// 
			if (id.equals(record.owner.toString())) {
				if (record.creator != null && record.creator.equals(record.owner)) record.creator = null; 
				result.add(record);
			//AccessLog.log("taken:"+record.content+" / "+record.name);
			} else {
				//AccessLog.log(id.toString()+" vs "+record.owner.toString());
				//AccessLog.log(record.content+" / "+record.name);
			}
		}
		AccessLog.log("RESULT AFTER FILTER="+result.size());
		return result;
	}
	
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Patient thePatient) {
		return super.createResource(thePatient);
	}
	
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Patient thePatient) {
		return super.updateResource(theId, thePatient);
	}	

	public Patient generatePatientForAccount(Member member) {
		Patient p = new Patient();
		p.setId(member._id.toString());
		p.addName().setFamily(member.lastname).addGiven(member.firstname);
		p.setBirthDate(member.birthday);
		if (member.status == UserStatus.ACTIVE || member.status == UserStatus.NEW || member.status == UserStatus.BLOCKED) {
			p.setActive(true);
		} else {
			p.setActive(false);
		}
		p.addIdentifier().setSystem("http://midata.coop/identifier/midata-id").setValue(member.midataID);
		if (member.email != null)
			p.addIdentifier().setSystem("http://midata.coop/identifier/patient-login").setValue(member.emailLC);
		p.setGender(AdministrativeGender.valueOf(member.gender.toString()));
		if (member.email != null)
			p.addTelecom().setSystem(ContactPointSystem.EMAIL).setValue(member.email);
		if (member.phone != null && member.phone.length() > 0) {
			p.addTelecom().setSystem(ContactPointSystem.PHONE).setValue(member.phone);
		}
		if (member.language != null)
			p.addCommunication().setPreferred(true).setLanguage(new CodeableConcept().addCoding(new Coding().setCode(member.language)));
		p.addAddress().setCity(member.city).setCountry(member.country).setPostalCode(member.zip).addLine(member.address1).addLine(member.address2);
		return p;
	}

	public void updatePatientForAccount(Member member) throws AppException {
		if (!member.role.equals(UserRole.MEMBER)) return;
		AccessContext context = RecordManager.instance.createSharingContext(info().context, member._id);
		List<Record> allExisting = RecordManager.instance.list(info().role, context,
				CMaps.map("format", "fhir/Patient").map("owner", member._id).map("data", CMaps.map("id", member._id.toString())), Record.ALL_PUBLIC);

		if (allExisting.isEmpty()) {
			Patient patient = generatePatientForAccount(member);
			Record record = newRecord("fhir/Patient");
			prepare(record, patient);
			record.owner = member._id;
			insertRecord(record, patient);
		} else {
			Patient patient = generatePatientForAccount(member);
			Record existing = allExisting.get(0);
			patient.getMeta().setVersionId(existing.version);
			prepare(existing, patient);
			updateRecord(existing, patient);
		}
	}

	public static void updatePatientForAccount(MidataId who) throws AppException {
		PatientResourceProvider patientProvider = (PatientResourceProvider) FHIRServlet.myProviders.get("Patient");

		try {
			info();
		} catch (AuthenticationException e) {
			ExecutionInfo inf = new ExecutionInfo(who, UserRole.MEMBER);
			patientProvider.setExecutionInfo(inf);
		}

		Member member = Member.getById(who, Sets.create(User.ALL_USER, "firstname", "lastname", "birthday", "midataID", "gender", "email", "phone", "city", "country", "zip", "address1", "address2", "emailLC", "language", "role"));
		patientProvider.updatePatientForAccount(member);
	}

	public static Patient generatePatientForStudyParticipation(MidataId pseudo, String ownerName, Member member, Study study) {

	

		Patient p = new Patient();		
		p.addName().setText(ownerName);
		
		if (study.requiredInformation != InformationType.NONE) {
			if (member.birthday != null) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(member.birthday);
				cal.set(Calendar.MONTH, 0);
				cal.set(Calendar.DAY_OF_MONTH, 1);
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
				
				p.setBirthDate(cal.getTime());
			}
			if (member.gender != null) p.setGender(AdministrativeGender.valueOf(member.gender.toString()));
		}

		p.addIdentifier(new Identifier().setValue(ownerName).setSystem("http://midata.coop/identifier/participant-name"));
		p.addIdentifier(new Identifier().setValue(pseudo.toString()).setSystem("http://midata.coop/identifier/participant-id"));

		return p;
	}

	public static void createPatientForStudyParticipation(ExecutionInfo inf, Study study, StudyParticipation part, Member member) throws AppException {

		AccessContext context = RecordManager.instance.createSharingContext(inf.context, part.owner);
		PatientResourceProvider patientProvider = (PatientResourceProvider) FHIRServlet.myProviders.get("Patient");
		PatientResourceProvider.setExecutionInfo(inf);
		String userName = "P-" + CodeGenerator.nextUniqueCode();			
		Record record = PatientResourceProvider.newRecord("fhir/Patient");
		record.owner = context.getOwner();
		MidataId pseudo = CodeGenerator.nextMidataId();
		Patient patient = generatePatientForStudyParticipation(pseudo, userName, member, study);
		patient.setId(pseudo.toString());
		patientProvider.prepare(record, patient);
		record.content = "PseudonymizedPatient";
		
		patientProvider.insertRecord(record, patient, context);

		Feature_Pseudonymization.addPseudonymization(context, part._id, pseudo, userName);
		RecordManager.instance.share(context, member._id, part._id, Collections.singleton(record._id), false);
	}

	public void prepare(Record record, Patient thePatient) {
		record.content = "Patient";
		record.name = thePatient.getName().get(0).getNameAsSingleString();
		if (record.name == null || record.name.length() == 0)
			record.name = thePatient.getName().get(0).getText();
	}

	public void processResource(Record record, Patient resource) throws AppException {
		IdType old = resource.getIdElement();
		super.processResource(record, resource);
		resource.setIdElement(old);
		if (record.ownerName != null && record.content.equals("Patient")) {
			resource.addIdentifier(new Identifier().setValue(record.ownerName).setSystem("http://midata.coop/identifier/participant-name"));
		}
				
		if (info().ownerId.equals(record.owner) && info().pluginId != null) {
		  List<Study> studies = determineStudyIdentifiers();
		  populateIdentifiers(record.owner, resource, studies);
		  
		  /*Plugin plugin = Plugin.getById(info().pluginId);	
		  Set<StudyAppLink> links = StudyAppLink.getByApp(plugin._id);
		  for (StudyAppLink sal : links) {
			  if (sal.isConfirmed() && (sal.type.contains(StudyAppLinkType.REQUIRE_P) || sal.type.contains(StudyAppLinkType.OFFER_P))) {
				  StudyParticipation part = StudyParticipation.getByStudyAndMember(sal.studyId, record.owner, Sets.create("_id", "ownerName"));			  
				  if (part != null && part.getOwnerName() != null) {
					  resource.addIdentifier(new Identifier().setValue(part.getOwnerName()).setSystem("http://midata.coop/identifier/participant-name"));
					  resource.addIdentifier(new Identifier().setValue(part._id.toString()).setSystem("http://midata.coop/identifier/participant-id"));
				  }  
			  }
		  }*/
		}
		
		// resource.setId(record.owner.toString());
	}
	
	
	@Override
	public String getRecordFormat() {	
		return "fhir/Patient";
	}
	
	
	@Override
	public void updatePrepare(Record record, Patient theResource) throws AppException {	
		AccessContext context = ApplicationTools.actAsRepresentative(info().context, record.owner);
		if (context == null) throw new NotImplementedOperationException("update on Patient not implemented.");
	}
	
	
	
	@Override
	public void updateExecute(Record record, Patient thePatient) throws AppException {
		String lang = null;
		for (PatientCommunicationComponent comm : thePatient.getCommunication()) {
			if (comm.getPreferred()) {
				lang = comm.getLanguage().getCodingFirstRep().getCode();
			}
		}
		Member user = Member.getById(record.owner, User.ALL_USER);
		boolean changes = false;
		boolean welcome = false;
		
		// Account email address may only be set via API if not present
		String loginEmail = user.email;
		if (loginEmail == null || loginEmail.trim().length()==0) {
			for (Identifier identifier : thePatient.getIdentifier()) {
				if (identifier.getSystem().equals("http://midata.coop/identifier/patient-login")) {
					loginEmail = identifier.getValue();					
					changes = true;
				}
			}
		}
		if (loginEmail == null || loginEmail.trim().length()==0) {
			for (ContactPoint cp : thePatient.getTelecom()) {
				if (ContactPointSystem.EMAIL.equals(cp.getSystem())) {
					loginEmail = cp.getValue();
				}
			}
		}
		
		if (loginEmail != null && !loginEmail.equals(user.emailLC)) {
		   user.email = loginEmail;
		   user.emailLC = user.email.toLowerCase();
		   user.set("email", user.email);
		   user.set("emailLC", user.emailLC);
		   changes = true;		
		   welcome = true;
		   AuditManager.instance.addAuditEvent(
				   AuditEventBuilder
				     .withType(AuditEventType.USER_EMAIL_CHANGE)
				     .withActorUser(info().cache.getUserById(info().executorId))
				     .withModifiedUser(user)
				     .withApp(info().pluginId)
		   );
		}
		
		// ------------
		
		
		if (lang != null && !lang.equals(user.language)) {			
			user.language = lang;
			User.set(user._id, "language", user.language);
			changes = true;						
		}
		
		if (changes) {
			updatePatientForAccount(user._id);
		}
		if (welcome) {
			Application.sendWelcomeMail(info().pluginId, user, info().cache.getUserById(info().executorId));
		}
		AuditManager.instance.success();
	}

	@Override
	public void createPrepare(Record record, Patient thePatient) throws AppException {
		if (!thePatient.hasName()) throw new UnprocessableEntityException("Name required for patient");
		if (!thePatient.hasGender()) throw new UnprocessableEntityException("Gender required for patient");
		if (!thePatient.hasBirthDate()) throw new UnprocessableEntityException("Birth date required for patient");
		if (!thePatient.hasAddress()) throw new UnprocessableEntityException("Country required for patient");
		
		/*
		Config config = InstanceConfig.getInstance().getConfig();
		String terms = "midata-terms-of-use--" + (config.hasPath("versions.midata-terms-of-use") ? config.getString("versions.midata-terms-of-use") : "1.0");
		String ppolicy = "midata-privacy-policy--" + (config.hasPath("versions.midata-privacy-policy") ? config.getString("versions.midata-privacy-policy") : "1.0");
		boolean termsOk = false;
		boolean ppolicyOk = false;

		for (Extension ext : thePatient.getExtensionsByUrl("http://midata.coop/extensions/terms-agreed")) {
			String agreed = ext.getValue().primitiveValue();
			if (agreed.equals(terms))
				termsOk = true;
			if (agreed.equals(ppolicy))
				ppolicyOk = true;
		}

		if (!termsOk || !ppolicyOk)
			throw new UnprocessableEntityException("Patient must approve terms of use and privacy policy");
       */
		
		super.createPrepare(record, thePatient);
	}

	@Override
	public void createExecute(Record record, Patient thePatient) throws AppException {

		
		// create the user
		Member user = new Member();

		for (HumanName name : thePatient.getName()) {
			if (name.getPeriod() == null || !name.getPeriod().hasEnd()) {
				user.firstname = name.getGivenAsSingleString();
				user.lastname = name.getFamily();
			}
		}

		for (Address address : thePatient.getAddress()) {
			if (!address.hasPeriod() || !address.getPeriod().hasEnd()) {
				user.city = address.getCity();
				user.country = address.getCountry();
				user.zip = address.getPostalCode();
				List<StringType> lines = address.getLine();
				if (lines.size() > 0)
					user.address1 = lines.get(0).asStringValue();
				if (lines.size() > 1)
					user.address2 = lines.get(0).asStringValue();
			}
		}

		boolean foundEmail = false;
		boolean foundLoginId = false;
		boolean foundMidataId = false;

		for (ContactPoint point : thePatient.getTelecom()) {

			if (!point.hasPeriod() || !point.getPeriod().hasEnd()) {
				if (point.getSystem().equals(ContactPointSystem.EMAIL)) {
					user.email = point.getValue();
					user.emailLC = user.email.toLowerCase();
					foundEmail = true;
				} else if (point.getSystem().equals(ContactPointSystem.PHONE)) {
					user.phone = point.getValue();
				} else if (point.getSystem().equals(ContactPointSystem.SMS)) {
					user.mobile = point.getValue();
				}
			}
		}

		for (Identifier identifier : thePatient.getIdentifier()) {
			if (identifier.getSystem().equals("http://midata.coop/identifier/patient-login")) {
				user.email = identifier.getValue();
				user.emailLC = user.email.toLowerCase();
				foundLoginId = true;
			}
		}

		user.name = user.firstname + " " + user.lastname;
		user.subroles = EnumSet.noneOf(SubUserRole.class);
		switch (thePatient.getGender()) {
		case FEMALE:
			user.gender = Gender.FEMALE;
			break;
		case MALE:
			user.gender = Gender.MALE;
			break;
		case OTHER:
			user.gender = Gender.OTHER;
			break;
		default:
		}
		user.birthday = thePatient.getBirthDate();
		user.language = InstanceConfig.getInstance().getDefaultLanguage();
		for (PatientCommunicationComponent comm : thePatient.getCommunication()) {
			if (comm.getPreferred()) {
				user.language = comm.getLanguage().getCodingFirstRep().getCode();
			}
		}
		user.initialApp = info().pluginId;

		if (user.firstname == null)
			throw new UnprocessableEntityException("Patient 'given' name not given.");
		if (user.lastname == null)
			throw new UnprocessableEntityException("Patient family name not given.");
		// if (user.email == null) throw new
		// UnprocessableEntityException("Patient email not given.");
		if (user.country == null)
			throw new UnprocessableEntityException("Patient country not given.");
		if (user.gender == null)
			throw new UnprocessableEntityException("Patient gender not given.");
		if (user.birthday == null)
			throw new UnprocessableEntityException("Patient birth date not given.");

		String password = null;
		for (Extension ext : thePatient.getExtensionsByUrl("http://midata.coop/extensions/account-password")) {
			password = ext.getValue().primitiveValue();
		}
		// if (password == null) throw new UnprocessableEntityException("Patient
		// account password not given.");

		if (password != null)
			user.password = Member.encrypt(password);

		
		if (!foundEmail && user.email != null) {
			thePatient.addTelecom().setSystem(ContactPointSystem.EMAIL).setValue(user.email);
		}
		if (!foundLoginId && user.email != null) {
			thePatient.addIdentifier().setSystem("http://midata.coop/identifier/patient-login").setValue(user.emailLC);
		}

		MidataId studyId = null;
		StudyParticipation part = null;

		for (Extension ext : thePatient.getExtensionsByUrl("http://midata.coop/extensions/join-study")) {

			String studyName = ((Coding) ext.getValue()).getCode();
			Study study = Study.getByCodeFromMember(studyName, Study.ALL);
			if (study == null)
				throw new BadRequestException("error.invalid.code", "Unknown code for study.");

			studyId = study._id;
		}

		thePatient.getExtension().clear();

		Member existing = user.email != null ? Member.getByEmail(user.email, Member.ALL_USER) : null;
		MidataId executorId = info().executorId;
		ExecutionInfo info = info();

		BSONObject query = null;
		Plugin plugin = Plugin.getById(info().pluginId);
		if (plugin.targetUserRole.equals(UserRole.RESEARCH)) {
			AccessLog.log("is researcher app");
			query = RecordManager.instance.getMeta(info().context, info().context.getTargetAps(), "_query");
			AccessLog.log("q=" + query.toString());
		}

		AccountCreationAccessContext tempContext = null;
		
		if (existing == null) {

			Application.registerSetDefaultFields(user, false);
						
			//if (thePatient.getActive()) {
			  user.status = UserStatus.ACTIVE;
			//} else {
			//  user.status = UserStatus.PRECREATED;
			//}
			
			user.emailStatus = (user.email != null && user.status == UserStatus.ACTIVE) ? EMailStatus.EXTERN_VALIDATED : EMailStatus.UNVALIDATED;

			user.emailStatus = user.emailStatus != null ? EMailStatus.EXTERN_VALIDATED : EMailStatus.UNVALIDATED;			

			user.flags = EnumSet.of(AccountActionFlags.CHANGE_PASSWORD);
			
			thePatient.setId(user._id.toString());
			AuditManager.instance.addAuditEvent(AuditEventType.USER_REGISTRATION, info().pluginId, info().ownerId, user);

			user.security = AccountSecurityLevel.KEY;
			user.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKey(user._id);
			Member.add(user);
			
			for (Extension ext : thePatient.getExtensionsByUrl("http://midata.coop/extensions/terms-agreed")) {
				String agreed = ext.getValue().primitiveValue();
				user.agreedToTerms(agreed, info.pluginId);
			}
						
			KeyManager.instance.unlock(user._id, null);

			tempContext = new AccountCreationAccessContext(info().context, user._id);
			
			/*RecordManager.instance.clearCache();
			executorId = user._id;
			RecordManager.instance.setAccountOwner(user._id, user._id);
			*/
			
			user.myaps = RecordManager.instance.createPrivateAPS(tempContext.getCache(), user._id, user._id);
			Member.set(user._id, "myaps", user.myaps);

			//Record record = newRecord("fhir/Patient");
			record.owner = user._id;
			prepare(record, thePatient);
			info = new ExecutionInfo(user._id, UserRole.MEMBER, tempContext);
			insertRecord(info, record, thePatient, tempContext);

			// if (user.emailLC!=null) Circles.fetchExistingConsents(user._id,
			// user.emailLC);
		} else {
			user = existing;

			Set<Consent> exist = Consent.getAllActiveByAuthorizedAndOwners(info().ownerId, Collections.singleton(user._id));
			if (!exist.isEmpty())
				throw new UnprocessableEntityException("Already exists.");
		}

		String consentName = plugin.name;
		HPUser hpuser = HPUser.getById(info().ownerId, Sets.create("provider", "firstname", "lastname"));
		if (hpuser != null) {
			consentName = hpuser.firstname + " " + hpuser.lastname;
			if (hpuser.provider != null) {
				HealthcareProvider prov = HealthcareProvider.getById(hpuser.provider, HealthcareProvider.ALL);
				if (prov != null)
					consentName = prov.name;
			}
		/*}

		if (plugin.targetUserRole.equals(UserRole.PROVIDER)) {*/
			Consent consent = new MemberKey();
			consent.writes = WritePermissionType.WRITE_ANY;
			consent.owner = user._id;
			consent.name = consentName;
			consent.creatorApp = info.pluginId;
			consent.authorized = new HashSet<MidataId>();
			consent.status = existing == null ? ConsentStatus.ACTIVE : ConsentStatus.UNCONFIRMED;
			consent.authorized.add(info().ownerId);
			consent.sharingQuery = new HashMap<String, Object>();
			consent.sharingQuery.put("owner", "self");
			consent.sharingQuery.put("app", plugin.filename);

			Circles.addConsent(tempContext != null ? tempContext : info.context, consent, false, null, true);
			
			if (consent.status == ConsentStatus.UNCONFIRMED) {
				try {
			      String serviceUrl = InstanceConfig.getInstance().getServiceURL()+"?consent="+consent._id+"&login="+URLEncoder.encode(user.email, "UTF-8");
			      thePatient.addExtension(new Extension("http://midata.coop/extensions/service-url", new UriType(serviceUrl)));
				} catch (UnsupportedEncodingException e) {}
			}
		} else {
			User executorUser = info().cache.getUserById(info().ownerId);
			if (executorUser.getRole()==UserRole.MEMBER) {
				Consent consent = new Circle();
				consent.type = ConsentType.REPRESENTATIVE;
				consent.owner = user._id;
				consent.name = executorUser.getPublicIdentifier();
				consent.creatorApp = info.pluginId;
				consent.authorized = new HashSet<MidataId>();
				consent.status = existing == null ? ConsentStatus.ACTIVE : ConsentStatus.UNCONFIRMED;
				consent.authorized.add(info().ownerId);
				consent.writes=WritePermissionType.UPDATE_AND_CREATE;
				consent.sharingQuery = new HashMap<String, Object>();
				consent.sharingQuery.put("group", "all");
				consent.sharingQuery.put("group-system", "v1");
				Set<MidataId> observers = ApplicationTools.getObserversForApp(info().pluginId);
				consent.observers = observers;
			
				Circles.addConsent(tempContext != null ? tempContext : info.context, consent, false, null, true);
				if (consent.status == ConsentStatus.UNCONFIRMED) {
					try {
				      String serviceUrl = InstanceConfig.getInstance().getServiceURL()+"?consent="+consent._id+"&login="+URLEncoder.encode(user.email, "UTF-8");
				      thePatient.addExtension(new Extension("http://midata.coop/extensions/service-url", new UriType(serviceUrl)));
					} catch (UnsupportedEncodingException e) {}
				}
			}
		}

		if (query != null && query.containsField("link-study")) {
			Map<String, Object> q = query.toMap();
			studyId = MidataId.from(q.get("link-study"));
			AccessLog.log("found linked study:" + studyId);
		}
		if (studyId != null) {
			Set<UserFeature> studyReq = controllers.members.Studies.precheckRequestParticipation(null, studyId);
			AccessLog.log("request part");
			if (existing == null) {
				part = controllers.members.Studies.requestParticipation(info, user._id, studyId, plugin._id, info().pluginId.equals(RuntimeConstants.instance.portalPlugin) ? JoinMethod.RESEARCHER : JoinMethod.APP, null);
			} else {
				part = controllers.members.Studies.match(info.context, user._id, studyId, plugin._id, info().pluginId.equals(RuntimeConstants.instance.portalPlugin) ? JoinMethod.RESEARCHER : JoinMethod.APP);
			}
			AccessLog.log("end request part");
		}

		if (existing == null) {
			User executorUser = info().cache.getUserById(info().ownerId);
			RecordManager.instance.clearCache();
			if (user.status == UserStatus.ACTIVE) Application.sendWelcomeMail(info().pluginId, user, executorUser);
			// if
			// (InstanceConfig.getInstance().getInstanceType().notifyAdminOnRegister()
			// && user.developer == null)
			// Application.sendAdminNotificationMail(user);
		}

		thePatient.setId(user._id.toString());
		
		if (part != null) {
			if (part.ownerName != null) {
				Pair<MidataId, String> pseudo = Feature_Pseudonymization.pseudonymizeUser(info().context, part);
				thePatient.addIdentifier().setSystem("http://midata.coop/identifier/participant-name").setValue(pseudo.getRight());
				thePatient.addIdentifier().setSystem("http://midata.coop/identifier/participant-id").setValue(pseudo.getLeft().toString());				
			}
		}
		
		if (tempContext != null) tempContext.close();
		
		AuditManager.instance.success();
	}
	
	protected void populateIdentifiers(MidataId owner, Patient thePatient, List<Study> studies) throws AppException {
		if (studies == null || studies.isEmpty()) return;
		for (Study study : studies) {
			StudyParticipation sp = StudyParticipation.getByStudyAndMember(study._id, owner, Sets.create("status", "pstatus", "ownerName"));
			if (sp != null) {
				Pair<MidataId, String> pseudo = Feature_Pseudonymization.pseudonymizeUser(info().context, sp);
				thePatient.addIdentifier().setSystem("http://midata.coop/identifier/participant-name").setValue(pseudo.getRight()).setType(new CodeableConcept(new Coding("http://midata.coop/codesystems/study-code",study.code, study.name)));
				thePatient.addIdentifier().setSystem("http://midata.coop/identifier/participant-id").setValue(pseudo.getLeft().toString()).setType(new CodeableConcept(new Coding("http://midata.coop/codesystems/study-code",study.code, study.name)));
			}
		}
	}
	
	protected List<Study> determineStudyIdentifiers() throws AppException {
		MidataId pluginId = info().pluginId;
		if (pluginId == null) return null;
		Set<StudyAppLink> sals = StudyAppLink.getByApp(pluginId);
		if (sals.isEmpty()) return null;
		List<Study> studies = new ArrayList<Study>();
		for (StudyAppLink sal : sals) {
			if (sal.isConfirmed() && sal.active && sal.linkTargetType == LinkTargetType.STUDY && (sal.type.contains(StudyAppLinkType.REQUIRE_P) || sal.type.contains(StudyAppLinkType.OFFER_P))) {
				studies.add(Study.getById(sal.studyId, Sets.create("_id","code","name")));
			}
		}
		return studies;
	}

	@Override
	protected void convertToR4(Object in) {
		// Nothing to do
		
	}

}