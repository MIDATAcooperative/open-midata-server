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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Type;

import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import models.HPUser;
import models.HealthcareProvider;
import models.Member;
import models.MidataId;
import models.Record;
import models.TypedMidataId;
import models.User;
import models.UserGroup;
import models.enums.UserRole;
import models.enums.UserStatus;
import utils.AccessLog;
import utils.RuntimeConstants;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class FHIRTools {

	private static Set<String> PERSON = Sets.create("person");
	private static Set<String> REFERENCE = Sets.create("role", "firstname", "lastname");
	public final static String BASE64_PLACEHOLDER_FOR_STREAMING = "JiFQTEFDRcKnSE9MREVSISEz";
	
	private static DateTimeFormatter titleTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
	private static DateTimeFormatter titleDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	
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
	public static Reference getReferenceToOwner(Record record) throws AppException {
		if (record == null) return null;
		if (record.ownerType != null) return new Reference().setDisplay(record.ownerName); //.setReference(record.ownerType+"/"+record.owner.toString());	   
        return getReferenceToUser(record.owner, record.ownerName);		
	}
	
	/**
	 * Returns a FHIR reference to the specified creator. Automatically chooses Patient, Practitioner or RelatedPerson
	 * @param id
	 * @return
	 * @throws InternalServerException
	 */
	public static Reference getReferenceToCreator(Record record) throws AppException {
		if (record == null) return null;
		if (record.owner.equals(record.creator)) {
			if (record.ownerType != null) return new Reference().setDisplay(record.ownerName).setReference(record.ownerType+"/"+record.owner.toString());
			return getReferenceToUser(record.owner, record.ownerName);
		}
        return getReferenceToUser(record.creator, record.creatorName);		
	}
	
	public static Reference getReferenceToModifiedBy(Record record) throws AppException {
		if (record == null) return null;
		if (record.owner.equals(record.modifiedBy)) {
			if (record.ownerType != null) return new Reference().setDisplay(record.ownerName).setReference(record.ownerType+"/"+record.owner.toString());
			return getReferenceToUser(record.owner, record.ownerName);
		}
        return getReferenceToUser(record.modifiedBy, record.modifiedByName);		
	}
	
	/**
	 * Returns a FHIR reference to the specified user. Automatically chooses Patient, Practitioner or RelatedPerson
	 * @param id
	 * @return
	 * @throws InternalServerException
	 */
	public static Reference getReferenceToUser(MidataId id, String defName) throws AppException {
		if (id == null) return null;
	    if (RuntimeConstants.instance.publicUser.equals(id)) return null;
		if (defName != null) return new Reference().setDisplay(defName).setReference("Patient/"+id.toString());				
		
		User user = ResourceProvider.hasInfo() ? ResourceProvider.info().getRequestCache().getUserById(id) : User.getById(id, User.PUBLIC);
		if (user == null) {
			return new Reference().setDisplay(defName).setReference("Patient/"+id.toString());
			//throw new InternalServerException("error.internal", "Person not found "+id.toString());
		}
        return getReferenceToUser(user);		
	}
	
	public static Reference getReferenceToUser(User user) throws AppException {
		if (user==null) return null;
		String type = "RelatedPerson";
		switch (user.role) {
		case MEMBER : type = "Patient";break;
		case PROVIDER : type = "Practitioner";break;
		case RESEARCH : type = "Practitioner";break;
		}
		if (user.status.isDeleted()) return new Reference().setReference(type+"/"+user._id.toString());
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
		String idpart = userRef.getIdPart();
		if (!MidataId.isValid(idpart)) throw new UnprocessableEntityException("Invalid reference to person. Maybe this is an id from another platform or a placeholder?");
		MidataId id = MidataId.from(idpart);
		
		User user = ResourceProvider.hasInfo() ? ResourceProvider.info().getRequestCache().getUserById(id, true) : User.getByIdAlsoDeleted(id, Sets.create("role"));
		if (user == null) throw new UnprocessableEntityException("Invalid Person Reference");
		if (rt != null) {
				if (rt.equals("Patient") && user.role != UserRole.MEMBER) throw new UnprocessableEntityException("Invalid Patient reference");
				if (rt.equals("Practitioner") && (user.role != UserRole.PROVIDER && user.role != UserRole.RESEARCH)) throw new UnprocessableEntityException("Invalid Practitioner reference");
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
		if (ref == null) return null;
		String rt = ref.getResourceType();
		if (rt == null) return null;
		if (rt.equals("Group") || rt.equals("Organization")) {
			MidataId result = getUserGroupIdFromReference(ref);
			return result != null ? new TypedMidataId(result, rt) : null; 
		} else {
			MidataId result = getUserIdFromReference(ref);
			return result != null ? new TypedMidataId(result, rt) : null;
		}				
	}
	
	public static String getMidataLoginFromReference(Reference ref) throws AppException {
		if (ref == null) return null;
		if (ref.hasIdentifier()) {
			Identifier id = ref.getIdentifier();
			String system = id.getSystem();
			String value = id.getValue();
			if (system.equals("http://midata.coop/identifier/patient-login-or-invitation")) {
				return value;
			}
		}
		return null;
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
			} else if (system.equals("http://midata.coop/identifier/patient-login-or-invitation")) {
				target = Member.getByEmail(value, Sets.create("_id", "role", "firstname", "lastname"));
				type = "Patient";
				if (target == null) return ref;
			} else if (system.equals("http://midata.coop/identifier/practitioner-login")) {
				target = HPUser.getByEmail(value, Sets.create("_id", "role", "firstname", "lastname"));
				type = "Practitioner";
			} else if (system.equals("http://midata.coop/identifier/organization-name")) {
				HealthcareProvider hp = HealthcareProvider.getByName(value);
				if (hp == null) throw new UnprocessableEntityException("References: Referenced organization not found");
				ref.setReference("Organization/"+hp._id.toString());
				ref.setIdentifier(null);
				ref.setDisplay(hp.name);
				return ref;
			} else if (system.equals("http://midata.coop/identifier/organization-identifier")) {
				ref.setType("Organization");
				type = null;
			}
			if (type == null && ref.hasType()) {
				try {
					IBaseResource res = resolveUniqueIdentifier(ref.getType(), ref.getIdentifier().getSystem(), ref.getIdentifier().getValue());
					if (res != null) {
						ref.setReference(ref.getType()+"/"+res.getIdElement().getIdPart());
						ref.setIdentifier(null);
						return ref;
					}
				} catch (AppException e) {
					AccessLog.logException("resolve reference", e);
				}												
			}
			
			if (type == null) return ref;
			
			if (target == null) throw new UnprocessableEntityException("References: Referenced person not found");
			ref.setReference(type+"/"+target._id.toString());
			ref.setIdentifier(null);
			ref.setDisplay(target.firstname+" "+target.lastname);
		}		
		
		return ref;
	}
	
	public static IBaseResource resolveUniqueIdentifier(String resourceType, String systemValue) throws AppException {
		if (systemValue == null || resourceType == null) return null;
        int p = systemValue.indexOf('|');
        if (p > 0) {
        	return resolveUniqueIdentifier(resourceType, systemValue.substring(0, p), systemValue.substring(p+1));
        }
        return null;
	}
	
	public static MidataId resolveUniqueIdentifierToId(String resourceType, String systemValue) throws AppException {
		if (systemValue == null || resourceType == null) return null;
        int p = systemValue.indexOf('|');
        if (p > 0) {
        	IBaseResource result = resolveUniqueIdentifier(resourceType, systemValue.substring(0, p), systemValue.substring(p+1));
        	if (result != null) return MidataId.parse(result.getIdElement().getIdPart());
        }
        return null;
	}
	
	public static MidataId resolveUniqueIdentifierToId(String resourceType, String system, String value) throws AppException {	
    	IBaseResource result = resolveUniqueIdentifier(resourceType, system, value);
    	if (result != null) return MidataId.parse(result.getIdElement().getIdPart());
    
        return null;
	}
	
	public static IBaseResource resolveUniqueIdentifier(String resourceType, String system, String value) throws AppException {
		if (resourceType == null || system == null || value == null || system.trim().length() == 0 || value.trim().length() == 0) return null;
		
		AccessLog.logBegin("begin resolve unique identifier type=",resourceType," system/value=",system+"|"+value);
		ResourceProvider provider = FHIRServlet.getProvider(resourceType);
		SearchParameterMap map = new SearchParameterMap();
		map.add("identifier", new TokenParam(system, value));
		map.setCount(3);
		List<IBaseResource> resources = provider.search(map);
		AccessLog.logEnd("end resolve unique identifier #=",Integer.toString(resources.size()));
		if (resources.size() == 1) {
			return resources.get(0);
		}
		return null;
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
    
    public static String stringFromDateTime(Type date) {
    	if (date == null) return "";
    	if (date instanceof Period) {
    		Period p = (Period) date;
    		return stringFromDateTime(p.getStartElement())+" - "+stringFromDateTime(p.getEndElement());
    	}
    	if (date instanceof DateTimeType) {
    	return ((DateTimeType) date).getValue().toInstant()
               .atZone(ZoneId.systemDefault())
               .format(titleTimeFormatter);
    	}
    	if (date instanceof DateType) {
        	return ((DateType) date).getValue().toInstant()
                   .atZone(ZoneId.systemDefault())
                   .format(titleDateFormatter);
        	}
    	return "";
    }
    
    public static String fhirFromRecord(Record record) throws AppException {
    	Object resourceType = record.data.get("resourceType");
    	if (resourceType == null) return null;
    	ResourceProvider prov = FHIRServlet.myProviders.get(resourceType.toString());
    	if (prov == null) return null;
    	return prov.serialize(prov.parse(record, prov.getResourceType()));    	
    }
    
    public static IIdType convertToId(CanonicalType canonical) {
    	if (canonical == null) return null;
    	String ref = canonical.getValue();
    	return new IdType(ref);
    }
	
	/*
	public static void updatePersonRecordOfUser(String id, Person person) throws InternalServerException {
		MidataId mongoId = new MidataId(id);
		User user = User.getById(mongoId, PERSON);
		IParser parser = ResourceProvider.ctx.newJsonParser();
	    user.person = BasicDBObject.parse(parser.encodeResourceToString(person));
	    User.set(mongoId, "person", user.person);		
	}
	*/
}
