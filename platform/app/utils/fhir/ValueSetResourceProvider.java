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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.CompositeAndListParam;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateOrListParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.QuantityAndListParam;
import ca.uhn.fhir.rest.param.QuantityParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import models.Record;
import models.RecordsInfo;
import models.enums.AggregationType;
import utils.access.RecordManager;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;


public class ValueSetResourceProvider extends RecordBasedResourceProvider<ValueSet> implements IResourceProvider {

	// Provide one default constructor
	public ValueSetResourceProvider() {						
		
		// Use name of @Search function as last parameter
		registerSearches("ValueSet", getClass(), "getValueSet");
	}
	
	// Return corresponding FHIR class
	@Override
	public Class<ValueSet> getResourceType() {
		return ValueSet.class;
	}

	@Search()
	public Bundle getValueSet(
			@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,

			@Description(shortDefinition = "The resource language") @OptionalParam(name = "_language") StringAndListParam theResourceLanguage,

			@Description(shortDefinition="This special parameter searches for codes in the value set.")
  			@OptionalParam(name="code")
  			TokenAndListParam theCode, 
    
            @Description(shortDefinition="A use context assigned to the value set.")
  			@OptionalParam(name="context")
  			TokenAndListParam theContext, 
    
            @Description(shortDefinition="A quantity- or range-valued use context assigned to the value set")
  			@OptionalParam(name="context-quantity")
  			QuantityAndListParam theContextQuantity, 
      
            @Description(shortDefinition="A type of use context assigned to the value set	ValueSet.useContext.code")
  			@OptionalParam(name="context-type")
  			TokenAndListParam theContextType, 
              
            @Description(shortDefinition="A use context type and quantity- or range-based value assigned to the value set")
  			@OptionalParam(name="context-type-quantity", compositeTypes= { TokenParam.class, QuantityParam.class })
  			CompositeAndListParam<TokenParam,QuantityParam> theContextTypeQuantity, 
            
            @Description(shortDefinition="A use context type and value assigned to the value set")
  			@OptionalParam(name="context-type-value", compositeTypes= { TokenParam.class, TokenParam.class })
  			CompositeAndListParam<TokenParam,TokenParam> theContextTypeValue, 
              
            @Description(shortDefinition="The value set publication date")
  			@OptionalParam(name="date")
  			DateAndListParam theDate, 
              
            @Description(shortDefinition="The description of the value set")
  			@OptionalParam(name="description")
  			StringAndListParam theDescription, 
              
            @Description(shortDefinition="Identifies the value set expansion (business identifier)")
  			@OptionalParam(name="expansion")
  			UriAndListParam theExpansion, 
              
            @Description(shortDefinition="External identifier for the value set")
  			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier, 
              
            @Description(shortDefinition="Intended jurisdiction for the value set")
  			@OptionalParam(name="jurisdiction")
  			TokenAndListParam theJurisdiction, 
              
            @Description(shortDefinition="Computationally friendly name of the value set")
  			@OptionalParam(name="name")
  			StringAndListParam theName, 
              
            @Description(shortDefinition="Name of the publisher of the value set")
  			@OptionalParam(name="publisher")
  			StringAndListParam thePublisher, 
              
            @Description(shortDefinition="A code system included or excluded in the value set or an imported value set")
  			@OptionalParam(name="reference")
  			UriAndListParam theReference, 
              
            @Description(shortDefinition="The current status of the value set")
  			@OptionalParam(name="status")
  			TokenAndListParam theStatus, 
              
            @Description(shortDefinition="The human-friendly name of the value set")
  			@OptionalParam(name="title")
  			StringAndListParam theTitle, 
            
            @Description(shortDefinition="The uri that identifies the value set")
  			@OptionalParam(name="url")
  			UriAndListParam theUrl, 
            
            @Description(shortDefinition="The business version of the value set")
  			@OptionalParam(name="version")
  			UriAndListParam theVersion,                               			  			

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
		paramMap.add("_language", theResourceLanguage);
    
        paramMap.add("code", theCode);
        paramMap.add("context", theContext);
        paramMap.add("context-quantity",theContextQuantity);
        paramMap.add("context-type",theContextType);
        paramMap.add("context-type-quantity", theContextTypeQuantity);
        paramMap.add("context-type-value",theContextTypeValue);
        paramMap.add("date",theDate);
        paramMap.add("description",theDescription);
        paramMap.add("expansion",theExpansion);
        paramMap.add("identifier",theIdentifier);
        paramMap.add("jurisdiction",theJurisdiction);
        paramMap.add("name",theName);
        paramMap.add("publisher",thePublisher);
        paramMap.add("reference",theReference);
        paramMap.add("status",theStatus);
        paramMap.add("title",theTitle);
        paramMap.add("url",theUrl);
        paramMap.add("version",theVersion);
	
		paramMap.setRevIncludes(theRevIncludes);
		paramMap.setLastUpdated(theLastUpdated);
		paramMap.setIncludes(theIncludes);
		paramMap.setSort(theSort);
		paramMap.setCount(theCount);
		
		// The last lines are different than the happy fhir version
		paramMap.setFrom(_page != null ? _page.getValue() : null);
		return searchBundle(paramMap, theDetails);
		
	}

	// The actual search method implementation.
	// Basically this "maps" the FHIR query to a MIDATA query and executes it
	public List<Record> searchRaw(SearchParameterMap params) throws AppException {
		
		// get execution context (which user, which app)
		ExecutionInfo info = info();

		// construct empty query and a builder for that query
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/ValueSet");

		// Now all possible searches need to be handeled. For performance reasons it makes sense
		// to put searches that are very restrictive and frequently used first in order
	
		// Add default handling for the _id search parameter
		builder.handleIdRestriction();
		
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("url", true, QueryBuilder.TYPE_URI, "url");
        builder.restriction("version", true, QueryBuilder.TYPE_STRING, "version");
		builder.restriction("code", true, QueryBuilder.TYPE_CODE, "expansion.contains.code", QueryBuilder.TYPE_CODE, "compose.include.concept.code");
		builder.restriction("title", true, QueryBuilder.TYPE_STRING, "title");
		
		builder.restriction("context", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "useContext.valueCodeableConcept");
        builder.restriction("context-quantity", true, QueryBuilder.TYPE_QUANTITY_OR_RANGE, "useContext.value");
        builder.restriction("context-type", true, QueryBuilder.TYPE_CODING, "useContext.code");
        builder.restriction("context-type-quantity", "useContext.code", "useContext.value", QueryBuilder.TYPE_CODING, QueryBuilder.TYPE_QUANTITY_OR_RANGE);
        builder.restriction("context-type-value", "useContext.code", "useContext.valueCodeableConcept", QueryBuilder.TYPE_CODING, QueryBuilder.TYPE_CODEABLE_CONCEPT);
        builder.restriction("date", true, QueryBuilder.TYPE_DATETIME, "date");
        builder.restriction("description", true, QueryBuilder.TYPE_STRING, "description");
        builder.restriction("expansion", true, QueryBuilder.TYPE_URI, "expansion.identifier");
        
        builder.restriction("jurisdiction", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "jurisdiction");
        builder.restriction("name", true, QueryBuilder.TYPE_STRING, "name");
        builder.restriction("publisher", true, QueryBuilder.TYPE_STRING, "publisher");
        builder.restriction("reference", true, QueryBuilder.TYPE_URI, "compose.include.system");
        builder.restriction("status", false, QueryBuilder.TYPE_CODE, "status");
        //query.putAccount("public", "only");        		
		// At last execute the constructed query
		return query.execute(info);
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam ValueSet theValueSet) {
		return super.createResource(theValueSet);
	}
		
	@Override
	public String getRecordFormat() {	
		return "fhir/ValueSet";
	}
		

	// This method is required if it is allowed to update the resource.
	// Just change the resource type
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam ValueSet theValueSet) {
		return super.updateResource(theId, theValueSet);
	}		

	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical type "content" set. 
	// b) Each record must have a "name" that will be shown to the user in the record tree.
	//    The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the FHIR representation
	public void prepare(Record record, ValueSet theValueSet) throws AppException {
		setRecordCodeByCodings(record, null, "ValueSet");
		
		String display = theValueSet.getName();		
		record.name = display;
		//theValueSet.getMeta().getSecurity().add(new Coding("http://midata.coop/codesystems/security","public","Public"));
		
		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta" section
		clean(theValueSet);
 
	}	
 
	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, ValueSet p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);		
	}
	
	@Override
	protected void convertToR4(Object in) {			
	}
	

}