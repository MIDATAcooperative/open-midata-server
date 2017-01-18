package utils.fhir;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Person;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

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
import ca.uhn.fhir.rest.param.CompositeAndListParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import controllers.UserGroups;
import models.MidataId;
import models.Record;
import models.User;
import models.UserGroup;
import models.UserGroupMember;
import models.enums.UserStatus;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class GroupResourceProvider extends ResourceProvider<Group> implements IResourceProvider {

	@Override
	public Class<Group> getResourceType() {
		return Group.class;
	}

	/**
	 * Default implementation to retrieve a FHIR resource by id.
	 * @param theId ID of resource to be retrieved
	 * @return Resource read from database
	 * @throws AppException
	 */
	@Read()
	public Group getResourceById(@IdParam IIdType theId) throws AppException {
		UserGroup group = UserGroup.getById(MidataId.from(theId.getIdPart()), UserGroup.FHIR);	
		if (group == null) return null;
		return readGroupFromMidataUserGroup(group);
	}
	
	/**
	 * Convert a MIDATA User object into a FHIR person object
	 * @param userToConvert user to be converted into a FHIR object
	 * @return FHIR person
	 * @throws AppException
	 */
	public Group readGroupFromMidataUserGroup(UserGroup groupToConvert) throws AppException {
		
		IParser parser = ctx().newJsonParser();
		AccessLog.log(groupToConvert.fhirGroup.toString());
		Group p = parser.parseResource(getResourceType(), groupToConvert.fhirGroup.toString());
				
		Set<UserGroupMember> members = UserGroups.listUserGroupMembers(groupToConvert._id);
		for (UserGroupMember member : members) {
		  p.addMember().setEntity(FHIRTools.getReferenceToUser(member.member));
		}
		return p;
	}
	
	public static void updateMidataUserGroup(UserGroup groupToConvert) throws AppException {
		Group p = new Group();

		p.setId(groupToConvert._id.toString());
		p.setName(groupToConvert.name);
		p.setActual(true);
		p.setActive(groupToConvert.status == UserStatus.ACTIVE);
		//p.setIdentifier(theIdentifier)
		
		String encoded = ctx.newJsonParser().encodeResourceToString(p);		
		groupToConvert.fhirGroup = (DBObject) JSON.parse(encoded);				
	}
			
	   @Search()
	    public List<IBaseResource> getGroup(
	    		@Description(shortDefinition="The resource identity")
	    		@OptionalParam(name="_id")
	    		StringAndListParam theId, 
	    		 
	    		@Description(shortDefinition="The resource language")
	    		@OptionalParam(name="_language")
	    		StringAndListParam theResourceLanguage, 
	    		   
	    		@Description(shortDefinition="Descriptive or actual")
	    		@OptionalParam(name="actual")
	    		TokenAndListParam theActual, 
	    		    
	    		@Description(shortDefinition="Kind of characteristic")
	    		@OptionalParam(name="characteristic")
	    		TokenAndListParam theCharacteristic, 
	    		    
	    		@Description(shortDefinition="A composite of both characteristic and value")
	    		@OptionalParam(name="characteristic-value", compositeTypes= { TokenParam.class, TokenParam.class })
	    		CompositeAndListParam<TokenParam, TokenParam> theCharacteristic_value,
	    		    
	    		@Description(shortDefinition="The kind of resources contained")
	    		@OptionalParam(name="code")
	    		TokenAndListParam theCode, 
	    		    
	    		@Description(shortDefinition="Group includes or excludes")
	    		@OptionalParam(name="exclude")
	    		TokenAndListParam theExclude, 
	    		    
	    		@Description(shortDefinition="Unique id")
	    		@OptionalParam(name="identifier")
	    		TokenAndListParam theIdentifier, 
	    		    
	    		@Description(shortDefinition="Reference to the group member")
	    		@OptionalParam(name="member", targetTypes={  } )
	    		ReferenceAndListParam theMember, 
	    		   
	    		@Description(shortDefinition="The type of resources the group contains")
	    		@OptionalParam(name="type")
	    		TokenAndListParam theType, 
	    		   
	    		@Description(shortDefinition="Value held by characteristic")
	    		@OptionalParam(name="value")
	    		TokenAndListParam theValue, 
	    		 
	    		@IncludeParam(reverse=true)
	    		Set<Include> theRevIncludes,
	    		@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
	    		@OptionalParam(name="_lastUpdated")
	    		DateRangeParam theLastUpdated, 
	    		 
	    		@IncludeParam(allow= {
	    						"Group:member", "*"
	    		}) 
	    		Set<Include> theIncludes,
	    		 			
	    		@Sort 
	    		SortSpec theSort,
	    					
	    		@ca.uhn.fhir.rest.annotation.Count
	    		Integer theCount	
	    
	    		) throws AppException {
	    	
	    	SearchParameterMap paramMap = new SearchParameterMap();
	    	
	    	paramMap.add("actual", theActual);
	    	paramMap.add("characteristic", theCharacteristic);
	    	paramMap.add("characteristic-value", theCharacteristic_value);
	    	paramMap.add("code", theCode);
	    	paramMap.add("exclude", theExclude);
	    	paramMap.add("identifier", theIdentifier);
	    	paramMap.add("member", theMember);
	    	paramMap.add("type", theType);
	    	paramMap.add("value", theValue);
	    	paramMap.setRevIncludes(theRevIncludes);
	    	paramMap.setLastUpdated(theLastUpdated);
	    	paramMap.setIncludes(theIncludes);
	    	paramMap.setSort(theSort);
	    	paramMap.setCount(theCount);	    		    	
	    	    		    	
	    	return search(paramMap);    	    	    	
	    }
	
	@Override
	public List<Record> searchRaw(SearchParameterMap params) throws AppException {		
		return null;
	}
 
	@Override
	public List<IBaseResource> search(SearchParameterMap params) {
		try {
					
			//ExecutionInfo info = info();
	
			Query query = new Query();		
			QueryBuilder builder = new QueryBuilder(params, query, null);
						
			builder.restriction("actual", false, "boolean", "fhirGroup.actual");
			builder.restriction("characteristic", false, "CodeableConcept", "fhirGroup.characteristic.code");
			builder.restriction("code", false, "CodeableConcept", "fhirGroup.code");
			builder.restriction("exclude", false, "boolean", "fhirGroup.characteristic.exclude");
			builder.restriction("identifier", false, "Identifier", "fhirGroup.identifier");
			builder.restriction("type", false, "code", "fhirGroup.type");
			//builder.restriction("characteristic-value", "CodeableConcept", "valueDate", "CodeableConcept", "DateTime");
			//builder.restriction("value", "CodeableConcept", false, "fhirGroup.characteristic.value");																				
			
			Map<String, Object> properties = query.retrieveAsNormalMongoQuery();
			builder.restriction("identifier", true, "string", "nameLC");
			
			/*Object keywords = query.retrieveIndexValues();
			if (keywords != null) properties.put("keywordsLC", keywords);
			properties.put("searchable", true);
			properties.put("status", User.NON_DELETED);
			*/
			Set<UserGroup> groups = UserGroup.getAllUserGroup(properties, UserGroup.FHIR);
			List<IBaseResource> result = new ArrayList<IBaseResource>();
			for (UserGroup group : groups) {
				result.add(readGroupFromMidataUserGroup(group));
			}
			
			return result;
			
		 } catch (AppException e) {
		       ErrorReporter.report("FHIR (search)", null, e);	       
			   return null;
		 } catch (NullPointerException e2) {
		   	    ErrorReporter.report("FHIR (search)", null, e2);	 
				throw new InternalErrorException(e2);
		}
		
	}
}