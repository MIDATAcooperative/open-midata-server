package utils.fhir;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.CompositeAndListParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.QuantityAndListParam;
import ca.uhn.fhir.rest.param.QuantityParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.dstu3.model.Type;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

import models.ContentInfo;
import models.Record;
import utils.AccessLog;
import utils.access.RecordManager;
import utils.auth.ExecutionInfo;
import utils.collections.CMaps;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.exceptions.AppException;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.StringType;


public class ObservationResourceProvider extends ResourceProvider<Observation> implements IResourceProvider {

	
	@Override
	public Class<Observation> getResourceType() {				
		return Observation.class;
	}
	

	@Search()
	public List<Observation> getObservation(
			@Description(shortDefinition="The resource identity")
			@OptionalParam(name="_id")
			StringAndListParam theId, 
			 
			@Description(shortDefinition="The resource language")
			@OptionalParam(name="_language")
			StringAndListParam theResourceLanguage, 
			  
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
			/* 
			@Description(shortDefinition="Return resources linked to by the given target")
			@OptionalParam(name="_has")
			HasAndListParam theHas, 
			*/  
			    
			@Description(shortDefinition="The code of the observation type")
			@OptionalParam(name="code")
			TokenAndListParam theCode, 
			  
			@Description(shortDefinition="The component code of the observation type")
			@OptionalParam(name="component-code")
			TokenAndListParam theComponent_code, 
			    
			@Description(shortDefinition="The value of the observation, if the value is a Quantity, or a SampledData (just search on the bounds of the values in sampled data)")
			@OptionalParam(name="value-quantity")
			QuantityAndListParam theValue_quantity, 
			    
			@Description(shortDefinition="The value of the component observation, if the value is a Quantity, or a SampledData (just search on the bounds of the values in sampled data)")
			@OptionalParam(name="component-value-quantity")
			QuantityAndListParam theComponent_value_quantity, 
			    
			@Description(shortDefinition="The value of the observation, if the value is a CodeableConcept")
			@OptionalParam(name="value-concept")
			TokenAndListParam theValue_concept, 
			   
			@Description(shortDefinition="The value of the component observation, if the value is a CodeableConcept")
			@OptionalParam(name="component-value-concept")
			TokenAndListParam theComponent_value_concept, 
			   
			@Description(shortDefinition="The value of the observation, if the value is a date or period of time")
			@OptionalParam(name="value-date")
			DateRangeParam theValue_date, 
			   
			@Description(shortDefinition="The value of the observation, if the value is a string, and also searches in CodeableConcept.text")
			@OptionalParam(name="value-string")
			StringAndListParam theValue_string, 
			    
			@Description(shortDefinition="The value of the component observation, if the value is a string, and also searches in CodeableConcept.text")
			@OptionalParam(name="component-value-string")
			StringAndListParam theComponent_value_string, 
			   
			@Description(shortDefinition="Obtained date/time. If the obtained element is a period, a date that falls in the period")
			@OptionalParam(name="date")
			DateRangeParam theDate, 
			   
			@Description(shortDefinition="The status of the observation")
			@OptionalParam(name="status")
			TokenAndListParam theStatus, 
			  
			@Description(shortDefinition="The subject that the observation is about")
			@OptionalParam(name="subject", targetTypes={  } )
			ReferenceAndListParam theSubject, 
			   
			@Description(shortDefinition="Who performed the observation")
			@OptionalParam(name="performer", targetTypes={  } )
			ReferenceAndListParam thePerformer, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="specimen", targetTypes={  } )
			ReferenceAndListParam theSpecimen, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="related-type")
			TokenAndListParam theRelated_type, 
			   
			@Description(shortDefinition="")
			@OptionalParam(name="related-target", targetTypes={  } )
			ReferenceAndListParam theRelated_target, 
			   
			@Description(shortDefinition="Healthcare event related to the observation")
			@OptionalParam(name="encounter", targetTypes={  } )
			ReferenceAndListParam theEncounter, 
			   
			@Description(shortDefinition="The reason why the expected value in the element Observation.value[x] is missing.")
			@OptionalParam(name="data-absent-reason")
			TokenAndListParam theData_absent_reason, 
			   
			@Description(shortDefinition="The reason why the expected value in the element Observation.component.value[x] is missing.")
			@OptionalParam(name="component-data-absent-reason")
			TokenAndListParam theComponent_data_absent_reason, 
			  
			@Description(shortDefinition="The subject that the observation is about (if patient)")
			@OptionalParam(name="patient", targetTypes={  Patient.class   } )
			ReferenceAndListParam thePatient, 
			   
			@Description(shortDefinition="The unique id for a particular observation")
			@OptionalParam(name="identifier")
			TokenAndListParam theIdentifier, 
			   
			@Description(shortDefinition="The Device that generated the observation data.")
			@OptionalParam(name="device", targetTypes={  } )
			ReferenceAndListParam theDevice, 
			   
			@Description(shortDefinition="The classification of the type of observation")
			@OptionalParam(name="category")
			TokenAndListParam theCategory, 
			   
			@Description(shortDefinition="Both code and one of the value parameters")
			@OptionalParam(name="code-value-quantity", compositeTypes= { TokenParam.class, QuantityParam.class })
			CompositeAndListParam<TokenParam, QuantityParam> theCode_value_quantity,
			  
			@Description(shortDefinition="Both code and one of the value parameters")
			@OptionalParam(name="code-value-concept", compositeTypes= { TokenParam.class, TokenParam.class })
			CompositeAndListParam<TokenParam, TokenParam> theCode_value_concept,
			  
			@Description(shortDefinition="Both code and one of the value parameters")
			@OptionalParam(name="code-value-date", compositeTypes= { TokenParam.class, DateParam.class })
			CompositeAndListParam<TokenParam, DateParam> theCode_value_date,
			   
			@Description(shortDefinition="Both code and one of the value parameters")
			@OptionalParam(name="code-value-string", compositeTypes= { TokenParam.class, StringParam.class })
			CompositeAndListParam<TokenParam, StringParam> theCode_value_string,
			  
			@Description(shortDefinition="Both component code and one of the component value parameters")
			@OptionalParam(name="component-code-component-value-quantity", compositeTypes= { TokenParam.class, QuantityParam.class })
			CompositeAndListParam<TokenParam, QuantityParam> theComponent_code_component_value_quantity,
			   
			@Description(shortDefinition="Both component code and one of the component value parameters")
			@OptionalParam(name="component-code-component-value-concept", compositeTypes= { TokenParam.class, TokenParam.class })
			CompositeAndListParam<TokenParam, TokenParam> theComponent_code_component_value_concept,
			   
			@Description(shortDefinition="Both component code and one of the component value parameters")
			@OptionalParam(name="component-code-component-value-string", compositeTypes= { TokenParam.class, StringParam.class })
			CompositeAndListParam<TokenParam, StringParam> theComponent_code_component_value_string,
			   
			@Description(shortDefinition="Related Observations - search on related-type and related-target together")
			@OptionalParam(name="related-target-related-type", compositeTypes= { ReferenceParam.class, TokenParam.class })
			CompositeAndListParam<ReferenceParam, TokenParam> theRelated_target_related_type,
			 
			@IncludeParam(reverse=true)
			Set<Include> theRevIncludes,
			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
			@OptionalParam(name="_lastUpdated")
			DateRangeParam theLastUpdated, 
			 
			@IncludeParam(allow= {
				"Observation:device" , 					"Observation:encounter" , 					"Observation:patient" , 					"Observation:performer" , 					"Observation:related-target" , 					"Observation:specimen" , 					"Observation:subject" , 						"Observation:device" , 					"Observation:encounter" , 					"Observation:patient" , 					"Observation:performer" , 					"Observation:related-target" , 					"Observation:specimen" , 					"Observation:subject" , 						"Observation:device" , 					"Observation:encounter" , 					"Observation:patient" , 					"Observation:performer" , 					"Observation:related-target" , 					"Observation:specimen" , 					"Observation:subject" , 						"Observation:device" , 					"Observation:encounter" , 					"Observation:patient" , 					"Observation:performer" , 					"Observation:related-target" , 					"Observation:specimen" , 					"Observation:subject" , 						"Observation:device" , 					"Observation:encounter" , 					"Observation:patient" , 					"Observation:performer" , 					"Observation:related-target" , 					"Observation:specimen" , 					"Observation:subject" , 						"Observation:device" , 					"Observation:encounter" , 					"Observation:patient" , 					"Observation:performer" , 					"Observation:related-target" , 					"Observation:specimen" , 					"Observation:subject" , 						"Observation:device" , 					"Observation:encounter" , 					"Observation:patient" , 					"Observation:performer" , 					"Observation:related-target" , 					"Observation:specimen" , 					"Observation:subject" 					, "*"
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
			paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT, theFtContent);
			paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TEXT, theFtText);
			paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TAG, theSearchForTag);
			paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY, theSearchForSecurity);
			paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE, theSearchForProfile);
			//paramMap.add("_has", theHas);
			paramMap.add("code", theCode);
			paramMap.add("component-code", theComponent_code);
			paramMap.add("value-quantity", theValue_quantity);
			paramMap.add("component-value-quantity", theComponent_value_quantity);
			paramMap.add("value-concept", theValue_concept);
			paramMap.add("component-value-concept", theComponent_value_concept);
			paramMap.add("value-date", theValue_date);
			paramMap.add("value-string", theValue_string);
			paramMap.add("component-value-string", theComponent_value_string);
			paramMap.add("date", theDate);
			paramMap.add("status", theStatus);
			paramMap.add("subject", theSubject);
			paramMap.add("performer", thePerformer);
			paramMap.add("specimen", theSpecimen);
			paramMap.add("related-type", theRelated_type);
			paramMap.add("related-target", theRelated_target);
			paramMap.add("encounter", theEncounter);
			paramMap.add("data-absent-reason", theData_absent_reason);
			paramMap.add("component-data-absent-reason", theComponent_data_absent_reason);
			paramMap.add("patient", thePatient);
			paramMap.add("identifier", theIdentifier);
			paramMap.add("device", theDevice);
			paramMap.add("category", theCategory);
			paramMap.add("code-value-quantity", theCode_value_quantity);
			paramMap.add("code-value-concept", theCode_value_concept);
			paramMap.add("code-value-date", theCode_value_date);
			paramMap.add("code-value-string", theCode_value_string);
			paramMap.add("component-code-component-value-quantity", theComponent_code_component_value_quantity);
			paramMap.add("component-code-component-value-concept", theComponent_code_component_value_concept);
			paramMap.add("component-code-component-value-string", theComponent_code_component_value_string);
			paramMap.add("related-target-related-type", theRelated_target_related_type);
			paramMap.setRevIncludes(theRevIncludes);
			paramMap.setLastUpdated(theLastUpdated);
			paramMap.setIncludes(theIncludes);
			paramMap.setSort(theSort);
			paramMap.setCount(theCount);
			
			return search(paramMap);
		}
		
	    public List<Record> searchRaw(SearchParameterMap params) throws AppException {
	    	ExecutionInfo info = info();
	    	
	    	Query query = new Query();
			QueryBuilder builder = new QueryBuilder(params, query);
			
            List<ReferenceParam> patients = builder.resolveReferences("patient", "Patient");
            if (patients != null) {
            	query.putAccount("owner", referencesToIds(patients));
            }
						
			Set<String> codes = builder.tokensToCodeSystemStrings("code");
			if (codes != null) {
				query.putAccount("code", codes);
				builder.restriction("code", "code", "CodeableConcept", false);
			} else {
				builder.restriction("code", "code", "CodeableConcept", true);					
			}
			
			builder.restriction("category", "category", "CodeableConcept", true);							
			
			return query.execute(info);		
	    }
			

	@Create
	public MethodOutcome createObservation(@ResourceParam Observation theObservation) throws AppException {

		// Save this patient to the database...
		Record record = new Record();
		record._id = new ObjectId();
		record.creator = info().executorId;
		record.format = "fhir/Observation";
		record.app = info().pluginId;
		record.created = new Date(System.currentTimeMillis());
		record.code = new HashSet<String>();
		for (Coding coding : theObservation.getCode().getCoding()) {
			if (coding.getCode() != null && coding.getSystem() != null) {
				record.code.add(coding.getSystem() + " " + coding.getCode());
			}
		}

		ContentInfo.setRecordCodeAndContent(record, record.code, null);

		Type valType = theObservation.getValue();
		if (valType instanceof StringType)
			record.subformat = "String";
		else if (valType instanceof Quantity)
			record.subformat = "Quantity";
		else if (valType instanceof BooleanType)
			record.subformat = "Boolean";
		else
			throw new UnprocessableEntityException("Value Type not Implemented");

		// ResourceReferenceDt subject = theObservation.getSubject();
		// if (subject.getReference().getIdPart()) {
		// }
		record.owner = info().ownerId;
		
		theObservation.setSubject(null);

		String encoded = ctx.newJsonParser().encodeResourceToString(theObservation);
		record.data = (DBObject) JSON.parse(encoded);

		RecordManager.instance.addRecord(info().executorId, record);

		// This method returns a MethodOutcome object which contains
		// the ID (composed of the type Patient, the logical ID 3746, and the
		// version ID 1)
		MethodOutcome retVal = new MethodOutcome();
		retVal.setId(new IdType("Observation", record._id.toString(), "0"));

		// You can also add an OperationOutcome resource to return
		// This part is optional though:
		// OperationOutcome outcome = new OperationOutcome();
		// outcome.addIssue().setDiagnostics("One minor issue detected");
		// retVal.setOperationOutcome(outcome);

		return retVal;
	}
	
	public static void processResource(Record record, Observation p) {
		ResourceProvider.processResource(record, p);
		if (p.getSubject().isEmpty()) {
			p.getSubject().setReferenceElement(new IdType("Patient", record.owner.toString()));
			p.getSubject().setDisplay(record.ownerName);
		}		
	}
	
}