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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Basic;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.History;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import models.MidataId;
import models.Record;
import models.enums.AuditEventType;
import utils.access.RecordManager;
import utils.audit.AuditHeaderTool;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.json.JsonOutput;

/**
 * Base class for FHIR resource providers. There is one provider subclass for each FHIR resource type.
 *
 */
public class BasicResourceProvider extends RecordBasedResourceProvider<Basic> implements IResourceProvider {
 
    public BasicResourceProvider() {
    	searchParamNameToPathMap.put("Basic:author", "author");
    	searchParamNameToPathMap.put("Basic:patient", "subject");
    	searchParamNameToTypeMap.put("Basic:patient", Sets.create("Patient"));
    	searchParamNameToPathMap.put("Basic:subject", "subject");	
    	
    	registerSearches("Basic", getClass(), "getBasic");
    }
	
    /**
     * 
     */
    @Override
    public Class<Basic> getResourceType() {
        return Basic.class;
    }
     
   
    @Read()
    public Basic getResourceById(@IdParam IIdType theId) throws AppException {    	
    	Record record;
		if (theId.hasVersionIdPart()) {
			List<Record> result = RecordManager.instance.list(info().getAccessorRole(), info(), CMaps.map("_id", new MidataId(theId.getIdPart())).map("version", theId.getVersionIdPart()), RecordManager.COMPLETE_DATA);
			record = result.isEmpty() ? null : result.get(0);
		} else {
		    record = RecordManager.instance.fetch(info().getAccessorRole(), info(), new MidataId(theId.getIdPart()), null);
		}
		if (record == null) throw new ResourceNotFoundException(theId);		
    	    	
		Basic p = parse(Collections.singletonList(record), Basic.class).get(0);
		//AuditHeaderTool.createAuditEntryFromHeaders(info(), AuditEventType.REST_READ, record.context.getOwner());
		return p;    	
    }
    
    @History()
	public List<Basic> getHistory(@IdParam IIdType theId) throws AppException {
	   List<Record> records = RecordManager.instance.list(info().getAccessorRole(), info(), CMaps.map("_id", new MidataId(theId.getIdPart())).map("history", true).map("sort","lastUpdated desc").map("limit",2000), RecordManager.COMPLETE_DATA);
	   if (records.isEmpty()) throw new ResourceNotFoundException(theId); 
	   
	   //AuditHeaderTool.createAuditEntryFromHeaders(info(), AuditEventType.REST_HISTORY, records.get(0).context.getOwner());		 
	   
	   return parse(records, Basic.class);	   	  
	}
    
    
    public List<Basic> parse(List<Record> result, Class<Basic> resultClass) throws AppException {
		ArrayList<Basic> parsed = new ArrayList<Basic>();	
	    IParser parser = ctx().newJsonParser();
	    for (Record rec : result) {
	      if (rec.format.equals("fhir/Basic")) {
			  try {
				Basic p = parser.parseResource(resultClass, JsonOutput.toJsonString(rec.data));
		        processResource(rec, p);											
				parsed.add(p);
		  	  } catch (DataFormatException e) {
			  }
	      } else {
	    	  Basic basic = new Basic();
	    	  basic.setId(rec._id.toString());
	    	  basic.setCreated(rec.created);
	    	  
	    	  if (rec.code != null) {
		    	  Iterator<String> codeIter = rec.code.iterator();
		    	  if (codeIter.hasNext()) {
		    		  String code = codeIter.next();
		    		  int idx = code.indexOf(" ");
		    		  String system = code.substring(0, idx);
		    		  code = code.substring(idx+1);
		    		  Coding coding = new Coding(system, code, null);
		    		  basic.setCode(new CodeableConcept().addCoding(coding));
		    	  }
	    	  }
	    	  
	    	  basic.setAuthor(new Reference("Patient/"+rec.modifiedBy.toString()));
	    	  basic.setSubject(new Reference("Patient/"+rec.owner.toString()));
	    	  
	    	  basic.addExtension().setUrl("http://midata.coop/extensions/format-codes/"+rec.format).setValue(new StringType(rec.data.toString()));
	    	  parsed.add(basic);
	      }
	    }
	    return parsed;
	}
    
    
    @Search()
    public Bundle getBasic(
    		@Description(shortDefinition="The resource identity")
    		@OptionalParam(name="_id")
    		StringAndListParam theId, 
    		/* 
    		@Description(shortDefinition="The resource language")
    		@OptionalParam(name="_language")
    		StringAndListParam theResourceLanguage,
    		*/ 
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
    		   
    		@Description(shortDefinition="")
    		@OptionalParam(name="subject", targetTypes={  } )
    		ReferenceAndListParam theSubject, 
    		   
    		@Description(shortDefinition="")
    		@OptionalParam(name="created")
    		DateRangeParam theCreated, 
    		   
    		@Description(shortDefinition="")
    		@OptionalParam(name="code")
    		TokenAndListParam theCode, 
    		   
    		@Description(shortDefinition="")
    		@OptionalParam(name="patient", targetTypes={  Patient.class   } )
    		ReferenceAndListParam thePatient, 
    		   
    		@Description(shortDefinition="")
    		@OptionalParam(name="author", targetTypes={  } )
    		ReferenceAndListParam theAuthor, 
    		  
    		/*
    		@Description(shortDefinition="")
    		@OptionalParam(name="identifier")
    		TokenAndListParam theIdentifier, 
    		*/ 
    		 
    		@IncludeParam(reverse=true)
    		Set<Include> theRevIncludes,
    		@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
    		@OptionalParam(name="_lastUpdated")
    		DateRangeParam theLastUpdated, 
    		  
    		@IncludeParam(allow= {
    				"Basic:author" , 
    				"Basic:patient" , 			
    				"Basic:subject" ,
    				"*"
    		}) 
    		Set<Include> theIncludes,
    					
    		@Sort 
    		SortSpec theSort,
    				
    		@ca.uhn.fhir.rest.annotation.Count
    		Integer theCount,
    		
    		@OptionalParam(name="_page")
			StringParam _page,
			
			RequestDetails theDetails
    		) throws AppException {
    	
    	SearchParameterMap paramMap = new SearchParameterMap();
    	paramMap.add("_id", theId);
    	//paramMap.add("_language", theResourceLanguage);
    	
    	//paramMap.add("_has", theHas);    	
    	paramMap.add("subject", theSubject);
    	paramMap.add("created", theCreated);
    	paramMap.add("code", theCode);
    	paramMap.add("patient", thePatient);
    	paramMap.add("author", theAuthor);
    	
    	
    	paramMap.setRevIncludes(theRevIncludes);
    	paramMap.setLastUpdated(theLastUpdated);
    	paramMap.setIncludes(theIncludes);
    	paramMap.setSort(theSort);
    	paramMap.setCount(theCount);
    	
    	paramMap.setFrom(_page != null ? _page.getValue() : null);

		return searchBundle(paramMap, theDetails);	    	    	
    }
           
    @Override
    public List<Record> searchRaw(SearchParameterMap params) throws AppException {
		AccessContext info = info();

		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, null);

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", "subject");
		builder.recordOwnerReference("subject", null, "subject");
		builder.recordCreatorReference("author", "Patient");
		
		Set<String> codes = builder.tokensToCodeSystemStrings("code");
		if (codes != null) {
			query.putAccount("code", codes);			
		} 
			
		return query.execute(info);
	}
    
            
    public void prepare(Record record, Basic theBasic) throws AppException {
    	
    	record.code = new HashSet<String>(); 
		String display = setRecordCodeByCodeableConcept(record, theBasic.getCode(), "Basic");
		
		String date = theBasic.getCreatedElement().toHumanDisplay();
		
		record.name = display != null ? (display + " / " + date) : date;   
		
		Reference subjectRef = theBasic.getSubject();
		if (cleanAndSetRecordOwner(record, subjectRef)) theBasic.setSubject(null);
		
		clean(theBasic);
    }
    
    public void processResource(Record record, Basic resource) throws AppException {
    	super.processResource(record, resource);
    	if (resource.getSubject().isEmpty()) {
    		resource.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
 
		}		
	}

    public Record init() { return newRecord("fhir/Basic"); }

	@Override
	public String getRecordFormat() {		
		return null;
	}
   

}