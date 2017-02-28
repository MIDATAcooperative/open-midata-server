package utils.fhir;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import models.Member;
import models.MidataId;
import models.Record;
import utils.access.RecordManager;
import utils.auth.ExecutionInfo;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;


public class PatientResourceProvider extends ResourceProvider<Patient> implements IResourceProvider {
 
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
    	List<Record> allRecs = RecordManager.instance.list(info.executorId, info.targetAPS, CMaps.map("owner", targetId).map("format",  "fhir/Patient"), Record.ALL_PUBLIC);
    	
    	if (allRecs == null || allRecs.size() == 0) return null;
    	
    	Record record = allRecs.get(0);
    	
    	IParser parser = ctx().newJsonParser();
		Patient p = parser.parseResource(getResourceType(), record.data.toString());
		processResource(record, p);		
		return p;    	
    }
 
   
    @Search()
    public List<IBaseResource> getPatient(
    		@Description(shortDefinition="The resource identity")
    		@OptionalParam(name="_id")
    		StringAndListParam theId, 
    		 
    		@Description(shortDefinition="The resource language")
    		@OptionalParam(name="_language")
    		StringAndListParam theResourceLanguage, 
    		  
    		/*
    		@Description(shortDefinition="Search the contents of the resource's data using a fulltext search")
    		@OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT)
    		StringAndListParam theFtContent, 
    		 
    		@Description(shortDefinition="Search the contents of the resource's narrative using a fulltext search")
    		@OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_TEXT)
    		StringAndListParam theFtText, 
    		  
    		@Description(shortDefinition="Search for resources which have the given tag")
    		@OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_TAG)
    		TokenAndListParam theSearchForTag, 
    		 
    		@Description(shortDefinition="Search for resources which have the given security labels")
    		@OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY)
    		TokenAndListParam theSearchForSecurity, 
    		    
    		@Description(shortDefinition="Search for resources which have the given profile")
    		@OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE)
    		UriAndListParam theSearchForProfile, 
    		*/
    		/*
    		@Description(shortDefinition="Return resources linked to by the given target")
    		@OptionalParam(name="_has")
    		HasAndListParam theHas, 
    		 */
    		   
    		@Description(shortDefinition="A patient identifier")
    		@OptionalParam(name="identifier")
    		TokenAndListParam theIdentifier, 
    		   
    		@Description(shortDefinition="A portion of either family or given name of the patient")
    		@OptionalParam(name="name")
    		StringAndListParam theName, 
    		   
    		@Description(shortDefinition="A portion of the family name of the patient")
    		@OptionalParam(name="family")
    		StringAndListParam theFamily, 
    		   
    		@Description(shortDefinition="A portion of the given name of the patient")
    		@OptionalParam(name="given")
    		StringAndListParam theGiven, 
    		   
    		@Description(shortDefinition="A portion of either family or given name using some kind of phonetic matching algorithm")
    		@OptionalParam(name="phonetic")
    		StringAndListParam thePhonetic, 
    		    
    		@Description(shortDefinition="The value in any kind of telecom details of the patient")
    		@OptionalParam(name="telecom")
    		TokenAndListParam theTelecom, 
    		    
    		@Description(shortDefinition="A value in a phone contact")
    		@OptionalParam(name="phone")
    		TokenAndListParam thePhone, 
    		    
    		@Description(shortDefinition="A value in an email contact")
    		@OptionalParam(name="email")
    		TokenAndListParam theEmail, 
    		   
    		@Description(shortDefinition="An address in any kind of address/part of the patient")
    		@OptionalParam(name="address")
    		StringAndListParam theAddress, 
    		   
    		@Description(shortDefinition="A city specified in an address")
    		@OptionalParam(name="address-city")
    		StringAndListParam theAddress_city, 
    		
    		@Description(shortDefinition="A state specified in an address")
    		@OptionalParam(name="address-state")
    		StringAndListParam theAddress_state, 
    		   
    		@Description(shortDefinition="A postalCode specified in an address")
    		@OptionalParam(name="address-postalcode")
    		StringAndListParam theAddress_postalcode, 
    		  
    		@Description(shortDefinition="A country specified in an address")
    		@OptionalParam(name="address-country")
    		StringAndListParam theAddress_country, 
    		   
    		@Description(shortDefinition="A use code specified in an address")
    		@OptionalParam(name="address-use")
    		TokenAndListParam theAddress_use, 
    		   
    		@Description(shortDefinition="Gender of the patient")
    		@OptionalParam(name="gender")
    		TokenAndListParam theGender, 
    		   
    		@Description(shortDefinition="Language code (irrespective of use value)")
    		@OptionalParam(name="language")
    		TokenAndListParam theLanguage, 
    		   
    		@Description(shortDefinition="The patient's date of birth")
    		@OptionalParam(name="birthdate")
    		DateRangeParam theBirthdate, 
    		   
    		@Description(shortDefinition="The organization at which this person is a patient")
    		@OptionalParam(name="organization", targetTypes={  } )
    		ReferenceAndListParam theOrganization, 
    		   
    		@Description(shortDefinition="Patient's nominated care provider, could be a care manager, not the organization that manages the record")
    		@OptionalParam(name="general-practitioner", targetTypes={  } )
    		ReferenceAndListParam theCareprovider, 
    		   
    		@Description(shortDefinition="Whether the patient record is active")
    		@OptionalParam(name="active")
    		TokenAndListParam theActive, 
    		   
    		@Description(shortDefinition="The species for animal patients")
    		@OptionalParam(name="animal-species")
    		TokenAndListParam theAnimal_species, 
    		   
    		@Description(shortDefinition="The breed for animal patients")
    		@OptionalParam(name="animal-breed")
    		TokenAndListParam theAnimal_breed, 
    		   
    		@Description(shortDefinition="All patients linked to the given patient")
    		@OptionalParam(name="link", targetTypes={  } )
    		ReferenceAndListParam theLink, 
    		   
    		@Description(shortDefinition="This patient has been marked as deceased, or as a death date entered")
    		@OptionalParam(name="deceased")
    		TokenAndListParam theDeceased, 
    		  
    		@Description(shortDefinition="The date of death has been provided and satisfies this search value")
    		@OptionalParam(name="deathdate")
    		DateRangeParam theDeathdate, 
    		
    		@IncludeParam(reverse=true)
    		Set<Include> theRevIncludes,
    		@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
    		@OptionalParam(name="_lastUpdated")
    		DateRangeParam theLastUpdated, 
    		 
    		@IncludeParam(allow= {
    				"Patient:general-practitioner",
    				"Patient:link",
    				"Patient:organization",
    				"*"
    		}) 
    		Set<Include> theIncludes,
    		 			
    		@Sort 
    		SortSpec theSort,
    		 			
    		@ca.uhn.fhir.rest.annotation.Count
    			Integer theCount
    		) throws AppException {
    	
    	SearchParameterMap paramMap = new SearchParameterMap();
    	paramMap.add("_id", theId);
    	paramMap.add("_language", theResourceLanguage);
    	/*
    	paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT, theFtContent);
    	paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TEXT, theFtText);
    	paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TAG, theSearchForTag);
    	paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY, theSearchForSecurity);
    	paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE, theSearchForProfile);
    	*/
    	//paramMap.add("_has", theHas);
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
    	
    	return search(paramMap);    	    	    	
    }
    
    /*
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
    */
    
    @Override
    public List<Record> searchRaw(SearchParameterMap params) throws AppException {
		ExecutionInfo info = info();

		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Patient");
		
		if (params.containsKey("_id")) {
	           Set<String> ids = builder.paramToStrings("_id");
	           if (ids != null) query.putAccount("owner", ids);
		}
		
		builder.restriction("identifier", true, "Identifier", "identifier");
		builder.restriction("family", true, "string", "name.family");
		builder.restriction("birthdate", true, "DateTime", "birthDate");
		builder.restriction("deathdate", true, "DateTime", "deceasedDateTime");
		builder.restriction("given", true, "string", "name.given");
		builder.restriction("gender", false, "code", "gender");
		
		builder.restriction("name", true, "string", "name.given", "string", "name.family");
		builder.restriction("email", true, "code", "telecom.value");
		builder.restriction("address-city", true, "string", "address.city");
		builder.restriction("address-country", true, "string", "address.country");
		builder.restriction("address-postalcode", true, "string", "address.postalCode");
		builder.restriction("address-state", true, "string", "address.state");
		builder.restriction("address-use", false, "code", "address.use");
		
		builder.restrictionMany("address", true, "string", "address.text", "address.line", "address.city", "address.district", "address.state", "address.postalCode", "address.country");
		
		builder.restriction("language", false, "code", "communication.language");
		builder.restriction("phone", true, "code", "telecom.value");
		builder.restriction("telecom", true, "code", "telecom.value");
		builder.restriction("active", false, "boolean", "active");
		
		return query.execute(info);
	}
    
    public Patient generatePatientForAccount(Member member) {
    	Patient p = new Patient();
		p.setId(member._id.toString());
		p.addName().setFamily(member.lastname).addGiven(member.firstname);
		p.setBirthDate(member.birthday);
		p.addIdentifier().setSystem("http://midata.coop/midataID").setValue(member.midataID);
		p.setGender(AdministrativeGender.valueOf(member.gender.toString()));
		p.addTelecom().setSystem(ContactPointSystem.EMAIL).setValue(member.email);
		if (member.phone != null && member.phone.length()>0) {
			p.addTelecom().setSystem(ContactPointSystem.PHONE).setValue(member.phone);
		}
		p.addAddress().setCity(member.city).setCountry(member.country).setPostalCode(member.zip).addLine(member.address1).addLine(member.address2);
		return p;		
    }
    
    public void updatePatientForAccount(Member member) throws AppException {
    	List<Record> allExisting = RecordManager.instance.list(member._id, member._id, CMaps.map("format", "fhir/Patient").map("owner", "self"), Record.ALL_PUBLIC);
    	
    	if (allExisting.isEmpty()) {    	
    	  Patient patient = generatePatientForAccount(member);
    	  Record record = newRecord("fhir/Patient");
		  prepare(record, patient);		
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
      ExecutionInfo inf = new ExecutionInfo();
      inf.executorId = who;
      inf.targetAPS = who;
      inf.ownerId = who;
      inf.pluginId = MidataId.from("588b53a7aed64509f5095def");
      PatientResourceProvider patientProvider = (PatientResourceProvider) FHIRServlet.myProviders.get("Patient");
      patientProvider.setExecutionInfo(inf);
      Member member = Member.getById(who, Sets.create("firstname", "lastname", "birthday", "midataID", "gender", "email", "phone", "city", "country", "zip", "address1", "address2"));
      patientProvider.updatePatientForAccount(member);
    }
    
    public void prepare(Record record, Patient thePatient) {
    	record.content = "Patient";    	
    	record.name=thePatient.getName().get(0).getNameAsSingleString();
    }
    
    public void processResource(Record record, Patient resource) throws AppException {
    	super.processResource(record, resource);
		resource.setId(record.owner.toString());		
	}
    /*
    private Set<MidataId> accessableAccounts(MidataId executor) throws AppException {
    	Set<Consent> consents = Consent.getAllActiveByAuthorized(executor);
    	Set<MidataId> result = new HashSet<MidataId>();
    	for (Consent consent : consents) {
    		result.add(consent.owner);
    	}
    	return result;
    }
    
    private List<Patient> getAllAccessiblePatients(MidataId executor) throws AppException {
       Set<MidataId> acc = accessableAccounts(executor);
       acc.add(executor);
       return patientsFromUserAccounts(CMaps.map("_id", acc));
    }
    */


}