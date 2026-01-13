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

import java.util.List;
import java.util.Set;

import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ValueSet;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.CompositeAndListParam;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.QuantityAndListParam;
import ca.uhn.fhir.rest.param.QuantityParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import models.CodeSystemEntry;
import models.MidataId;
import models.Record;
import models.enums.UserRole;
import utils.ErrorReporter;
import utils.Errors;
import utils.access.RecordManager;
import utils.collections.CMaps;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.IgnorableResourceNotFoundExcxeption;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;

public class CodeSystemResourceProvider extends RecordBasedResourceProvider<CodeSystem> implements IResourceProvider {

	// Provide one default constructor
	public CodeSystemResourceProvider() {						
		
		// Use name of @Search function as last parameter
		registerSearches("CodeSystem", getClass(), "getCodeSystem");
	}
	
	// Return corresponding FHIR class
	@Override
	public Class<CodeSystem> getResourceType() {
		return CodeSystem.class;
	}

	@Search()
	public Bundle getCodeSystem(
			@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,

			@Description(shortDefinition="A code defined in the code system")
  			@OptionalParam(name="code")
  			TokenAndListParam theCode, 
  			
  			@Description(shortDefinition="not-present | example | fragment | complete | supplement")
  			@OptionalParam(name="content-mode")
  			TokenAndListParam theContentMode, 
  			
  			@Description(shortDefinition="A use context assigned to the code system")
  			@OptionalParam(name="context")
  			TokenAndListParam theContext, 
  			
  			@Description(shortDefinition="A quantity- or range-valued use context assigned to the code system")
  			@OptionalParam(name="context-quantity")
  			QuantityAndListParam theContextQuantity, 
  
			@Description(shortDefinition="A type of use context assigned to the code system")
  			@OptionalParam(name="context-type")
  			TokenAndListParam theContextType, 
  			
  		    @Description(shortDefinition="A use context type and quantity- or range-based value assigned to the code system")
  			@OptionalParam(name="context-type-quantity", compositeTypes= { TokenParam.class, QuantityParam.class })
  			CompositeAndListParam<TokenParam,QuantityParam> theContextTypeQuantity, 
         
  			@Description(shortDefinition="A use context type and value assigned to the code system")
  			@OptionalParam(name="context-type-value", compositeTypes= { TokenParam.class, TokenParam.class })
  			CompositeAndListParam<TokenParam,TokenParam> theContextTypeValue, 
  
  	        @Description(shortDefinition="The code system publication date")
  	  	    @OptionalParam(name="date")
  	  		DateAndListParam theDate, 
		
            @Description(shortDefinition="The description of the code system")
  			@OptionalParam(name="description")
  			StringAndListParam theDescription, 
  			
  			@Description(shortDefinition="External identifier for the code system")
  			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier, 
              
            @Description(shortDefinition="Intended jurisdiction for the code system")
  			@OptionalParam(name="jurisdiction")
  			TokenAndListParam theJurisdiction, 
  			
  			@Description(shortDefinition="A language in which a designation is provided")
  			@OptionalParam(name="language")
  			TokenAndListParam theLanguage, 
			
            @Description(shortDefinition="Computationally friendly name of the code system")
  			@OptionalParam(name="name")
  			StringAndListParam theName, 
			
            @Description(shortDefinition="Name of the publisher of the code system")
  			@OptionalParam(name="publisher")
  			StringAndListParam thePublisher, 		
			
            @Description(shortDefinition="The current status of the code system")
  			@OptionalParam(name="status")
  			TokenAndListParam theStatus, 
  			
			@Description(shortDefinition="Find code system supplements for the referenced code system")
  			@OptionalParam(name="supplements", targetTypes={ CodeSystem.class } )
  			ReferenceAndListParam theSupplements, 

  		    @Description(shortDefinition="The system for any codes defined by this code system (same as 'url')")
  			@OptionalParam(name="system")
  			UriAndListParam theSystem, 
  		      
            @Description(shortDefinition="The human-friendly name of the code system")
  			@OptionalParam(name="title")
  			StringAndListParam theTitle, 
            
            @Description(shortDefinition="The uri that identifies the code system")
  			@OptionalParam(name="url")
  			UriAndListParam theUrl, 
  			
            
            @Description(shortDefinition="The business version of the code system")
  			@OptionalParam(name="version")
  			TokenAndListParam theVersion, 


 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= { 					
 					"*"
 			}) 
 			Set<Include> theIncludes,						
								
			@Sort SortSpec theSort,		
			
			@ca.uhn.fhir.rest.annotation.Count Integer theCount,
			
			// Non FHIR parameter used for pagination
			@OptionalParam(name="_page")
			StringParam _page,
			
			RequestDetails theDetails

	) throws AppException {

		// The implementation of this method may also be copied from happy fhir except for the last lines
		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", theId);		
    
        paramMap.add("code", theCode);
        paramMap.add("content-mode", theContentMode);
        paramMap.add("context",theContext);
        paramMap.add("context-quantity", theContextQuantity);	
        paramMap.add("context-type", theContextType);
        paramMap.add("context-type-quantity", theContextTypeQuantity);
        paramMap.add("context-type-value", theContextTypeValue);	
        paramMap.add("date", theDate);	
        paramMap.add("description", theDescription);	
        paramMap.add("identifier", theIdentifier);	
        paramMap.add("jurisdiction", theJurisdiction); 
        paramMap.add("language", theLanguage);	
        paramMap.add("name", theName);	
        paramMap.add("publisher", thePublisher);	
        paramMap.add("status", theStatus);	
        paramMap.add("supplements", theSupplements);	
        paramMap.add("system", theSystem);	
        paramMap.add("title", theTitle);	
        paramMap.add("url", theUrl);	
        paramMap.add("version", theVersion);	
	
		paramMap.setRevIncludes(theRevIncludes);
		paramMap.setLastUpdated(theLastUpdated);
		paramMap.setIncludes(theIncludes);
		paramMap.setSort(theSort);
		paramMap.setCount(theCount);
		
		// The last lines are different than the happy fhir version
		paramMap.setFrom(_page != null ? _page.getValue() : null);
		return searchBundle(paramMap, theDetails);
		
	}

	
	public Query buildQuery(SearchParameterMap params) throws AppException {
		
		info();

		// construct empty query and a builder for that query
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/CodeSystem");

		// Now all possible searches need to be handeled. For performance reasons it makes sense
		// to put searches that are very restrictive and frequently used first in order
	
		// Add default handling for the _id search parameter
		builder.handleIdRestriction();
				
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("url", true, QueryBuilder.TYPE_URI, "url");
		builder.restriction("system", true, QueryBuilder.TYPE_URI, "url");
        builder.restriction("version", true, QueryBuilder.TYPE_STRING, "version");
		builder.restriction("code", true, QueryBuilder.TYPE_CODE, "concept.code");
		builder.restriction("language", true, QueryBuilder.TYPE_CODE, "concept.designation.language");
		builder.restriction("title", true, QueryBuilder.TYPE_STRING, "title");
		builder.restriction("supplements", true, null, "supplements");
		builder.restriction("content-mode", true, QueryBuilder.TYPE_CODE, "content");
		builder.restriction("context", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "useContext.valueCodeableConcept");
        builder.restriction("context-quantity", true, QueryBuilder.TYPE_QUANTITY_OR_RANGE, "useContext.value");
        builder.restriction("context-type", true, QueryBuilder.TYPE_CODING, "useContext.code");
        builder.restriction("context-type-quantity", "useContext.code", "useContext.value", QueryBuilder.TYPE_CODING, QueryBuilder.TYPE_QUANTITY_OR_RANGE);
        builder.restriction("context-type-value", "useContext.code", "useContext.valueCodeableConcept", QueryBuilder.TYPE_CODING, QueryBuilder.TYPE_CODEABLE_CONCEPT);
        builder.restriction("date", true, QueryBuilder.TYPE_DATETIME, "date");
        builder.restriction("description", true, QueryBuilder.TYPE_STRING, "description");
        
        builder.restriction("jurisdiction", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "jurisdiction");
        builder.restriction("name", true, QueryBuilder.TYPE_STRING, "name");
        builder.restriction("publisher", true, QueryBuilder.TYPE_STRING, "publisher");
        builder.restriction("status", false, QueryBuilder.TYPE_CODE, "status");
        query.putAccount("public", "only");        		
		
		return query;
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam CodeSystem theCodeSystem) {
		return super.createResource(theCodeSystem);
	}
		
	@Override
	public String getRecordFormat() {	
		return "fhir/CodeSystem";
	}
	
	private boolean processConcepts(CodeSystem theCodeSystem) {
		if (theCodeSystem.getContent() == CodeSystemContentMode.NOTPRESENT) return false;
		if (theCodeSystem.getContent() == CodeSystemContentMode.SUPPLEMENT) return false;
		return true;
	}
		

	// This method is required if it is allowed to update the resource.
	// Just change the resource type
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam CodeSystem theCodeSystem) {
		return super.updateResource(theId, theCodeSystem);
	}		

	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical type "content" set. 
	// b) Each record must have a "name" that will be shown to the user in the record tree.
	//    The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the FHIR representation
	public void prepare(Record record, CodeSystem theCodeSystem) throws AppException {
		if (info().getAccessorRole() != UserRole.ADMIN && info().getAccessorRole() != UserRole.DEVELOPER) {
			throw new AuthenticationException();
		}
		
		setRecordCodeByCodings(record, null, "CodeSystem");
		
		String display = theCodeSystem.getName();		
		record.name = display;
		theCodeSystem.getMeta().getSecurity().add(new Coding("http://midata.coop/codesystems/security","public","Public"));
		
		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta" section
		clean(theCodeSystem);
 
	}	
 
	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, CodeSystem p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);		
	}
	
	private void processConceptDefinition(CodeSystem theResource, ConceptDefinitionComponent cdc, boolean delete) throws InternalServerException {
	    CodeSystemEntry cse = new CodeSystemEntry();
	    cse.system = theResource.getUrl();
	    cse.systemDisplay = theResource.getTitle();
	    cse.code = cdc.getCode();
	    if (theResource.getVersionNeeded()) {
	      cse.version = theResource.getVersion();
	    }
	    cse.display = cdc.getDisplay();
	    cse.language = theResource.getLanguage();
	    if (cse.language == null) cse.language = "en";
	    cse.fhirResource = MidataId.from(theResource.getId());
	    if (theResource.hasDate()) {
	      cse.lastUpdated = theResource.getDate().getTime();
	    } else {
	      cse.lastUpdated = System.currentTimeMillis();
	    }
	    
	    CodeSystemEntry existing;
	    
	    if (theResource.getVersionNeeded()) {
	      existing = CodeSystemEntry.getBySystemCodeVersionLanguage(cse.system, cse.code, cse.version, cse.language);	
	    } else {
	      existing = CodeSystemEntry.getBySystemCodeLanguage(cse.system, cse.code, cse.language);	 	
	    }
	    
	    if (delete) { 
	       if (existing != null) CodeSystemEntry.delete(existing._id);
	    } else {
	    	if (existing != null) {
	    	  cse._id = existing._id;
	        } else {
	    	  cse._id = new MidataId();
	        }

	        CodeSystemEntry.upsert(cse);
	    }
	    
	    if (cdc.getConcept() != null) {
	    	for (ConceptDefinitionComponent scdc : cdc.getConcept()) {
	    		processConceptDefinition(theResource, scdc, delete);
	    	}
	    }
	}
	
	@Override
	public CodeSystem createExecute(Record record, CodeSystem theResource) throws AppException {
		CodeSystem result = theResource;
		if (!theResource.hasExtension("http://midata.coop/extensions/bulk-upload")) {
		  result = super.createExecute(record, theResource);
		}
		
		if (processConcepts(theResource) && theResource.hasConcept()) {
		  for (ConceptDefinitionComponent cdc : result.getConcept()) {
			processConceptDefinition(theResource, cdc, false);
		  }
		}
		
		return result;
	}

	@Override
	public void updateExecute(Record record, CodeSystem theResource) throws AppException {
		super.updateExecute(record, theResource);
		
		if (processConcepts(theResource) && theResource.hasConcept()) {
		  for (ConceptDefinitionComponent cdc : theResource.getConcept()) {
			processConceptDefinition(theResource, cdc, false);
		  }
		}
		
	}

	@Override
	protected void convertToR4(Object in) {			
	}
	
	@Delete()
	public void deleteCodeSystem(@IdParam IdType theId) {
		if (info().getAccessorRole() != UserRole.ADMIN && info().getAccessorRole() != UserRole.DEVELOPER) {
			throw new AuthenticationException();
		}
		
		try {
	    	CodeSystem theResource = getResourceById(theId);
	    	String system = theResource.getUrl();
	    	
	    	if (processConcepts(theResource) && theResource.hasConcept()) {
	    		for (ConceptDefinitionComponent cdc : theResource.getConcept()) {
	    			processConceptDefinition(theResource, cdc, true);
	    		}
	    	} else if (system != null && system.length() > 0 && theResource.getContent() == CodeSystemContentMode.NOTPRESENT) {
	    		Set<CodeSystemEntry> entries = CodeSystemEntry.lookup(system, null, theResource.getVersion(), theResource.getLanguage());
	    		for (CodeSystemEntry entry : entries) CodeSystemEntry.delete(entry._id);
	    	}
    	
	    	RecordManager.instance.deleteFromPublic(info(), CMaps.map("format", "fhir/CodeSystem").map("content", "CodeSystem").map("_id", theResource.getIdPart()));
		} catch (Exception e) {
			throw Errors.handle("FHIR (delete resource)", info(), e);
		} 
	}
	
	/**
	 * $lookup operation
	 */
	@SuppressWarnings("unchecked")
	@Operation(
			name = "$lookup",
			idempotent = true,
			returnParameters = {
				@OperationParam(name = "name", typeName = "string", min = 1),
				@OperationParam(name = "version", typeName = "string", min = 0),
				@OperationParam(name = "display", typeName = "string", min = 1)
			})
	public IBaseParameters lookup(
			@OperationParam(name = "code", min = 0, max = 1, typeName = "code") IPrimitiveType<String> theCode,
			@OperationParam(name = "system", min = 0, max = 1, typeName = "uri") IPrimitiveType<String> theSystem,
			@OperationParam(name = "coding", min = 0, max = 1, typeName = "Coding") IBaseCoding theCoding,
			@OperationParam(name = "version", min = 0, max = 1, typeName = "string") IPrimitiveType<String> theVersion,
			@OperationParam(name = "displayLanguage", min = 0, max = 1, typeName = "code")
					IPrimitiveType<String> theDisplayLanguage,
			@OperationParam(name = "property", min = 0, max = OperationParam.MAX_UNLIMITED, typeName = "code")
					List<IPrimitiveType<String>> thePropertyNames,
			RequestDetails theRequestDetails) throws AppException {

		    if (!checkAccessible()) throw new AuthenticationException();
		
			Set<CodeSystemEntry> results = CodeSystemEntry.lookup((theSystem!=null?theSystem.getValue():null), (theCode!=null?theCode.getValue():null), (theVersion!=null?theVersion.getValue():null), (theDisplayLanguage!=null?theDisplayLanguage.getValue():null));
			if (results.isEmpty()) throw new IgnorableResourceNotFoundExcxeption("Unable to find code in provided system");
			CodeSystemEntry cse = results.iterator().next();
			Parameters result = new Parameters();
			
			result.addParameter("name", cse.systemDisplay);
			if (cse.version != null) result.addParameter("version", cse.version);
            result.addParameter("display", cse.display);		
	      
			return result;
		
	}
	

}
