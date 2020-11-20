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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.Group.GroupMemberComponent;
import org.hl7.fhir.dstu3.model.Group.GroupType;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Elements;
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
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.CompositeAndListParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import controllers.UserGroups;
import models.MidataId;
import models.Record;
import models.UserGroup;
import models.UserGroupMember;
import models.enums.ConsentStatus;
import models.enums.UserStatus;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.stats.Stats;

public class GroupResourceProvider extends RecordBasedResourceProvider<Group> implements IResourceProvider {

	public GroupResourceProvider() {
		registerSearches("Group", getClass(), "getGroup");
	}
	
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
		if (group != null) return readGroupFromMidataUserGroup(group, true);
		return super.getResourceById(theId);		
	}
	
    @History()
    @Override
	public List<Group> getHistory(@IdParam IIdType theId) throws AppException {
    	throw new ResourceNotFoundException("No history kept for Group resource"); 
    }
    
	/**
	 * Convert a MIDATA User object into a FHIR person object
	 * @param userToConvert user to be converted into a FHIR object
	 * @return FHIR person
	 * @throws AppException
	 */
	public Group readGroupFromMidataUserGroup(UserGroup groupToConvert, boolean addMembers) throws AppException {
		
		IParser parser = ctx().newJsonParser();
		AccessLog.log(groupToConvert.fhirGroup.toString());
		Group p = parser.parseResource(getResourceType(), groupToConvert.fhirGroup.toString());
				
		if (addMembers) {
			Set<UserGroupMember> members = UserGroups.listUserGroupMembers(groupToConvert._id);
			for (UserGroupMember member : members) {
			  GroupMemberComponent gmc = p.addMember();
			  gmc.setEntity(FHIRTools.getReferenceToUser(member.member, null));
			  if (member.status != ConsentStatus.ACTIVE) gmc.setInactive(true);
			  if (member.startDate != null) gmc.getPeriod().setStart(member.startDate);
			  if (member.endDate != null) gmc.getPeriod().setEnd(member.endDate);
			}
		}
		return p;
	}
	
	public static void updateMidataUserGroup(UserGroup groupToConvert) throws AppException {
		Group p = new Group();

		p.setId(groupToConvert._id.toString());
		p.setName(groupToConvert.name);
		p.setActual(true);
		p.setActive(groupToConvert.status == UserStatus.ACTIVE);
		p.setType(GroupType.PRACTITIONER);
		p.setCode(new CodeableConcept().addCoding(new Coding().setSystem("http://midata.coop").setCode("team").setDisplay("Team")));
		p.addIdentifier().setSystem("http://midata.coop/identifier/group-name").setValue(groupToConvert.name);
		
		String encoded = ctx.newJsonParser().encodeResourceToString(p);		
		groupToConvert.fhirGroup = (DBObject) JSON.parse(encoded);				
	}
			
	   @Search()
	    public Bundle getGroup(
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
	    		Integer theCount,
	    		
	    		SummaryEnum theSummary, // will receive the summary (no annotation required)
	    	    @Elements Set<String> theElements,
	    	    
	    	    @OptionalParam(name="_page")
				StringParam _page,
				
				RequestDetails theDetails
	    
	    		) throws AppException {
	    	
	    	SearchParameterMap paramMap = new SearchParameterMap();
	    	
	    	paramMap.add("_id", theId);
			paramMap.add("_language", theResourceLanguage);
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
	    	paramMap.setElements(theElements);
	    	paramMap.setSummary(theSummary);
	    	    		    	
	    	return searchBundle(paramMap, theDetails);    	    	    	
	    }
	
	@Override
	public List<Record> searchRaw(SearchParameterMap params) throws AppException {		
		ExecutionInfo info = info();

		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Group");

		builder.handleIdRestriction();
		
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("code", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "code");
		builder.restriction("member", true, null, "member.entity");	
		builder.restriction("type", true, QueryBuilder.TYPE_CODE, "type");
		
		builder.restriction("characteristic", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "characteristic.code");
		builder.restriction("characteristic-value", "characteristic.code", "characteristic.value", QueryBuilder.TYPE_CODEABLE_CONCEPT, QueryBuilder.TYPE_QUANTITY_OR_RANGE);
		builder.restriction("value", true, QueryBuilder.TYPE_QUANTITY_OR_RANGE, "value");
		
		builder.restriction("exclude", false, QueryBuilder.TYPE_BOOLEAN, "characteristic.exclude");						
		builder.restriction("actual", false, QueryBuilder.TYPE_BOOLEAN, "actual");								
		
		return query.execute(info);
	}
 
	@Override
	public List<IBaseResource> search(SearchParameterMap params) {
		try {
					
			//ExecutionInfo info = info();
	
			Query query = new Query();		
			QueryBuilder builder = new QueryBuilder(params, query, null);
			builder.handleIdRestriction();		
			builder.restriction("actual", false, QueryBuilder.TYPE_BOOLEAN, "fhirGroup.actual");
			builder.restriction("characteristic", false, QueryBuilder.TYPE_CODEABLE_CONCEPT, "fhirGroup.characteristic.code");
			builder.restriction("code", false, QueryBuilder.TYPE_CODEABLE_CONCEPT, "fhirGroup.code");
			builder.restriction("exclude", false, QueryBuilder.TYPE_BOOLEAN, "fhirGroup.characteristic.exclude");
			builder.restriction("identifier", false, QueryBuilder.TYPE_IDENTIFIER, "fhirGroup.identifier");
			builder.restriction("type", false, QueryBuilder.TYPE_CODE, "fhirGroup.type");
			//builder.restriction("characteristic-value", "CodeableConcept", "valueDate", "CodeableConcept", "DateTime");
			//builder.restriction("value", "CodeableConcept", false, "fhirGroup.characteristic.value");																				
			
			if (params.containsKey("member")) {
				List<ReferenceParam> persons = builder.resolveReferences("member", null);
				Set<MidataId> ids = new HashSet<MidataId>();
				if (persons != null && FHIRTools.areAllOfType(persons, Sets.create("Patient", "Practitioner", "Person"))) {
					Set<String> pers = FHIRTools.referencesToIds(persons);
					for (String p : pers) {
						MidataId personId = FHIRTools.getUserIdFromReference(new Reference(p).getReferenceElement());
						Set<UserGroupMember> members = UserGroupMember.getAllActiveByMember(personId);
						for (UserGroupMember member : members) ids.add(member.userGroup);
					}
					
				}
				query.putAccount("_id", ids);
			}
			
			Map<String, Object> properties = query.retrieveAsNormalMongoQuery();
			builder.restriction("identifier", true, "string", "nameLC");
			
			/*Object keywords = query.retrieveIndexValues();
			if (keywords != null) properties.put("keywordsLC", keywords);
			properties.put("searchable", true);
			properties.put("status", User.NON_DELETED);
			*/
			boolean addMembers = params.hasElement("member") && params.getSummary().equals(SummaryEnum.FALSE);			
			
			if (Stats.enabled && addMembers && !params.containsKey("identifier") && !params.containsKey("_id")) Stats.addComment("Use _summary or _elements parameter if list of members is not needed.");
			
			Set<UserGroup> groups = UserGroup.getAllUserGroup(properties, UserGroup.FHIR);
			List<IBaseResource> result = new ArrayList<IBaseResource>();
			for (UserGroup group : groups) {
				if (group.fhirGroup != null) result.add(readGroupFromMidataUserGroup(group, addMembers));
			}
			
			List<IBaseResource> normal = super.search(params);
			result.addAll(normal);
			
			return result;
			
		 } catch (AppException e) {
		       ErrorReporter.report("FHIR (search)", null, e);	       
			   return null;
		 } catch (NullPointerException e2) {
		   	    ErrorReporter.report("FHIR (search)", null, e2);	 
				throw new InternalErrorException(e2);
		}
		
	}
	
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Group theGroup) {
		return super.createResource(theGroup);
	}
		
	@Override
	public String getRecordFormat() {	
		return "fhir/Group";
	}
	

	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Group theGroup) {
		return super.updateResource(theId, theGroup);
	}		

	public void prepare(Record record, Group theGroup) throws AppException {
		
		setRecordCodeByCodeableConcept(record, null, "Group");				
		record.name = theGroup.getName();		
		
		clean(theGroup);
 
	}	
 	
}