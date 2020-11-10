/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.fhir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Flag;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.exceptions.FHIRException;
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
import ca.uhn.fhir.rest.api.server.IBundleProvider;
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
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import models.Record;
import models.RecordsInfo;
import models.enums.AggregationType;
import utils.AccessLog;
import utils.access.RecordManager;
import utils.access.pseudo.FhirPseudonymizer;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.fhir.RecordBasedResourceProvider;


public class FlagResourceProvider extends RecordBasedResourceProvider<Flag> implements IResourceProvider {

	public FlagResourceProvider() {
		searchParamNameToPathMap.put("Flag:author", "author");
		searchParamNameToTypeMap.put("Flag:author", Sets.create("Practitioner", "Organization", "Device", "Patient"));
		
		searchParamNameToPathMap.put("Flag:encounter", "encounter");
		searchParamNameToTypeMap.put("Flag:encounter", Sets.create("Encounter"));
		
		searchParamNameToPathMap.put("Flag:patient", "subject");
		searchParamNameToTypeMap.put("Flag:patient", Sets.create("Patient"));
		
		searchParamNameToPathMap.put("Flag:subject", "subject");
		searchParamNameToTypeMap.put("Flag:subject", Sets.create("Practitioner", "Group", "Organization", "Medication", "Patient", "PlanDefinition", "Procedure", "Location"));

		registerSearches("Flag", getClass(), "getFlag");
		
		FhirPseudonymizer.forR4()
		  .reset("Flag")		
		  .pseudonymizeReference("Flag", "author");
	}

	@Override
	public Class<Flag> getResourceType() {
		return Flag.class;
	}

	@Search()
	public Bundle getFlag(

			@Description(shortDefinition = "The ID of the resource") @OptionalParam(name = "_id") TokenAndListParam the_id,

			@Description(shortDefinition = "The language of the resource") @OptionalParam(name = "_language") StringAndListParam the_language,

			@Description(shortDefinition = "Flag creator") @OptionalParam(name = "author", targetTypes = {}) ReferenceAndListParam theAuthor,

			@Description(shortDefinition = "Time period when flag is active") @OptionalParam(name = "date") DateAndListParam theDate,

			@Description(shortDefinition = "Alert relevant during encounter") @OptionalParam(name = "encounter", targetTypes = {}) ReferenceAndListParam theEncounter,

			@Description(shortDefinition = "Business identifier") @OptionalParam(name = "identifier") TokenAndListParam theIdentifier,

			@Description(shortDefinition = "The identity of a subject to list flags for") @OptionalParam(name = "patient", targetTypes = {}) ReferenceAndListParam thePatient,

			@Description(shortDefinition = "The identity of a subject to list flags for") @OptionalParam(name = "subject", targetTypes = {}) ReferenceAndListParam theSubject,

			@IncludeParam(reverse = true) Set<Include> theRevIncludes,
			@Description(shortDefinition = "Only return resources which were last updated as specified by the given range") @OptionalParam(name = "_lastUpdated") DateRangeParam theLastUpdated,

			@IncludeParam(allow = { "Flag:author", "Flag:encounter", "Flag:patient", "Flag:subject",
					"*" }) Set<Include> theIncludes,

			@Sort SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount,

			@OptionalParam(name = "_page") StringParam _page,

			RequestDetails theDetails

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", the_id);
		paramMap.add("_language", the_language);
		paramMap.add("author", theAuthor);
		paramMap.add("date", theDate);
		paramMap.add("encounter", theEncounter);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("patient", thePatient);
		paramMap.add("subject", theSubject);
		paramMap.setRevIncludes(theRevIncludes);
		paramMap.setLastUpdated(theLastUpdated);
		paramMap.setIncludes(theIncludes);
		paramMap.setSort(theSort);
		paramMap.setCount(theCount);
		paramMap.setFrom(_page != null ? _page.getValue() : null);

		return searchBundle(paramMap, theDetails);

	}

	public List<Record> searchRaw(SearchParameterMap params) throws AppException {
		ExecutionInfo info = info();

		Query query = new Query();
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Flag");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", "subject");

		builder.restriction("date", true, QueryBuilder.TYPE_PERIOD, "period");
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");

		if (!builder.recordOwnerReference("subject", null, "subject"))
			builder.restriction("subject", true, null, "subject");

		builder.restriction("encounter", true, "Encounter", "encounter");
		builder.restriction("author", true, null, "author");

		return query.execute(info);
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Flag theFlag) {
		return super.createResource(theFlag);
	}

	@Override
	public String getRecordFormat() {	
		return "fhir/Flag";
	}
	

	// This method is required if it is allowed to update the resource.
	// Just change the resource type
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Flag theFlag) {
		return super.updateResource(theId, theFlag);
	}

	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical type
	// "content" set.
	// b) Each record must have a "name" that will be shown to the user in the
	// record tree.
	// The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the FHIR
	// representation
	public void prepare(Record record, Flag theFlag) throws AppException {
		// Task a : Set Record "content" field by using a code from the resource (or a
		// fixed value or something else useful)
		String display = setRecordCodeByCodeableConcept(record, theFlag.getCode(), "Flag"); 

		// Task b : Create record name
		String date = "No time";
		if (theFlag.hasPeriod()) {
			try {
				date = FHIRTools.stringFromDateTime(theFlag.getPeriod());
			} catch (Exception e) {
				throw new UnprocessableEntityException("Cannot process effectiveDateTime");
			}
		}
		record.name = display != null ? (display + " / " + date) : date;

		// Task c : Set record owner based on subject and clean subject if this was
		// possible
		Reference subjectRef = theFlag.getSubject();
		if (cleanAndSetRecordOwner(record, subjectRef))
			theFlag.setSubject(null);

		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta"
		// section
		clean(theFlag);

	}

	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, Flag p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);

		// Add subject field from record owner field if it is not already there
		if (p.getSubject().isEmpty()) {
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}

	@Override
	protected void convertToR4(Object in) {
		// Nothing to do
		
	}
}
