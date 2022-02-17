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

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Reference;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
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
import models.Consent;
import models.ContentInfo;
import models.MidataId;
import models.Model;
import models.Record;
import utils.AccessLog;
import utils.access.pseudo.FhirPseudonymizer;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;

public class ProvenanceResourceProvider extends RecordBasedResourceProvider<Provenance> implements IResourceProvider {

	// Provide one default constructor
	public ProvenanceResourceProvider() {
		
		// For each existing search parameter that has a "reference" type add one line:
		// searchParamNameToPathMap.put("Resource:search-name", "path from search specification");
		searchParamNameToPathMap.put("Provenance:agent", "agent.who");
		searchParamNameToPathMap.put("Provenance:entity", "entity.what");
		
		searchParamNameToPathMap.put("Provenance:location", "location");
		searchParamNameToTypeMap.put("Provenance:location", Sets.create("Location"));
		
		searchParamNameToPathMap.put("Provenance:patient", "target.where");
		searchParamNameToTypeMap.put("Provenance:patient", Sets.create("Patient"));
		
		searchParamNameToPathMap.put("Provenance:target", "target");
			
		// Use name of @Search function as last parameter
		registerSearches("Provenance", getClass(), "getProvenance");
		
		addPathWithVersion("Provenance.target");
		
		FhirPseudonymizer.forR4()
		  .reset("Provenance")		
		  .pseudonymizeReference("Provenance", "target")
		  .pseudonymizeReference("Provenance", "entry", "item");
	}
	
	@Read(version=false)
	public Provenance getResourceById(@IdParam IIdType theId) throws AppException {
		return super.getResourceById(theId);
	}
	
	// Return corresponding FHIR class
	@Override
	public Class<Provenance> getResourceType() {
		return Provenance.class;
	}

	// Main search function for resource. May have another name.
	// Copy method signature from happy fhir implementation.
	// Look at http://hapifhir.io/xref-jpaserver/ca/uhn/fhir/jpa/rp/dstu3/ObservationResourceProvider.html
	// Throw out unsupported search parameters (like "_has" and all starting with "PARAM_" )
	// Replace DateRangeParam with DateAndListParam everywhere except for _lastUpdated	
	// Add non FHIR _page parameter used for the pagination mechanism
	@Search()
	public Bundle getProvenance(
			@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,

			@Description(shortDefinition = "The resource language") @OptionalParam(name = "_language") StringAndListParam theResourceLanguage,

			@Description(shortDefinition="Who participated")
  			@OptionalParam(name="agent", targetTypes={  } )
  			ReferenceAndListParam theAgent, 

  			@Description(shortDefinition="What the agents role was")
  			@OptionalParam(name="agent-role")
  			TokenAndListParam theAgentRole,
			
  			@Description(shortDefinition="How the agent participated")
  			@OptionalParam(name="agent-type")
  			TokenAndListParam theAgentType,
			
  			@Description(shortDefinition="Identity of entity")
  			@OptionalParam(name="entity", targetTypes={  } )
  			ReferenceAndListParam theEntity,
			
  			@Description(shortDefinition="Where the activity occurred")
  			@OptionalParam(name="location", targetTypes={  } )
  			ReferenceAndListParam theLocation, 

  			@Description(shortDefinition="Target Reference(s) (usually version specific)")
  			@OptionalParam(name="patient", targetTypes={  } )
  			ReferenceAndListParam thePatient,
					
  			@Description(shortDefinition="When the activity was recorded / updated")
 			@OptionalParam(name="recorded")
			DateAndListParam theRecorded,
				
			@Description(shortDefinition="Indication of the reason the entity signed the object(s)")
  			@OptionalParam(name="signature-type")
  			TokenAndListParam theSignatureType,
			
  			@Description(shortDefinition="Target Reference(s) (usually version specific)")
  			@OptionalParam(name="target", targetTypes={  } )
  			ReferenceAndListParam theTarget, 

  			@Description(shortDefinition="When the activity occurred")
 			@OptionalParam(name="when")
			DateAndListParam theWhen,
			

 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= {
 					"Provenance:agent" ,
 					"Provenance:entity" ,
 					"Provenance:location" ,
 					"Provenance:patient" ,
 					"Provenance:target" , 					
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
	
		paramMap.add("agent", theAgent);
		paramMap.add("agent-role", theAgentRole);
		paramMap.add("agent-type", theAgentType);
		paramMap.add("entity", theEntity);
		paramMap.add("location", theLocation);
		paramMap.add("patient", thePatient);
		paramMap.add("recorded", theRecorded);
		paramMap.add("signature-type", theSignatureType);
		paramMap.add("target", theTarget);
		paramMap.add("when", theWhen);				
		
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
		AccessContext info = info();

		// construct empty query and a builder for that query
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Provenance");

	
		// Add default handling for the _id search parameter
		builder.handleIdRestriction();
		
		// Add handling for search on the "owner" of the record. 
		//builder.recordOwnerReference("patient", "Patient", "subject");
				
		// Add handling for search on the field that determines the MIDATA content type used.
        //builder.recordCodeRestriction("code", "code");
		
		builder.restriction("target", true, QueryBuilder.TYPE_VERSIONED_REFERENCE, "target");
		builder.restriction("patient", true, QueryBuilder.TYPE_VERSIONED_REFERENCE, "target");
		builder.restriction("recorded", true, QueryBuilder.TYPE_INSTANT, "recorded");
		builder.restriction("when", true, QueryBuilder.TYPE_DATETIME, "occurredDateTime");
		builder.restriction("entity", true, null, "entity.what");
		
		builder.restriction("agent", true, null, "agent.who");
		builder.restriction("agent-role", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "agent.role");
		builder.restriction("agent-type", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "agent.type");		
		builder.restriction("location", true, "Location", "location");						
		builder.restriction("signature-type", true, QueryBuilder.TYPE_CODING, "signature.type");
												
		// At last execute the constructed query
		return query.execute(info);
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Provenance theProvenance) {
		return super.createResource(theProvenance);
	}
		
	@Override
	public String getRecordFormat() {	
		return "fhir/Provenance";
	}
		
		
	public void prepare(Record record, Provenance theProvenance) throws AppException {
		ContentInfo.setRecordCodeAndContent(info().getUsedPlugin(), record, null, "Provenance");			
		record.name = "Provenance";
		
		theProvenance.setRecorded(new Date());
		
		MidataId owner = null;
		List<Reference> refs = theProvenance.getTarget();
		for (Reference ref : refs) {
			if (ref.hasReference()) {
				IBaseResource res = ref.getResource();
				if (res != null) {
					throw new UnsupportedOperationException("Not implemented");
				} else {
					IIdType iid = ref.getReferenceElement();
					MidataId recOwner = getOwner(iid);
					AccessLog.log("got owner = "+recOwner);
					if (owner == null) owner = recOwner;
					else if (recOwner == null) { }
					else if (!owner.equals(recOwner)) {
						throw new BadRequestException("error.internal", "Cannot determine data owner");
					}
				}
			}
		}
		if (owner == null) throw new BadRequestException("error.internal", "Cannot determine data owner");
		record.owner = owner;
			
		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta" section
		clean(theProvenance);
 
	}
	
	private MidataId getOwner(IIdType iid) throws AppException {
		String type = iid.getResourceType();
		String id = iid.getIdPart();		
		Model model = FHIRServlet.myProviders.get(type).fetchCurrent(iid);
		if (model==null) throw new BadRequestException("error.internal", "Referenced Record not found");
		if (model instanceof Consent) return ((Consent) model).owner;
		if (model instanceof Record) return ((Record) model).owner;
		return null;
	}
 
	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, Provenance p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);
		
		
	}
	


	@Override
	protected void convertToR4(Object in) {
		
	}
	

}