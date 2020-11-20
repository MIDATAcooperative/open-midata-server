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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CarePlan;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RawParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
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
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import models.Record;
import utils.access.pseudo.FhirPseudonymizer;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;


public class CarePlanResourceProvider extends RecordBasedResourceProvider<CarePlan> implements IResourceProvider {

	public CarePlanResourceProvider() {
		searchParamNameToPathMap.put("CarePlan:activity-reference", "activity.reference");
		searchParamNameToTypeMap.put("CarePlan:activity-reference", Sets.create("Appointment", "ReferralRequest", "MedicationRequest", "Task", "NutritionOrder", "RequestGroup", "VisionPrescription", "ProcedureRequest", "DeviceRequest", "CommunicationRequest"));
		
		searchParamNameToPathMap.put("CarePlan:based-on", "basedOn");
		searchParamNameToTypeMap.put("CarePlan:based-on", Sets.create("CarePlan"));
		
		searchParamNameToPathMap.put("CarePlan:care-team", "careTeam");
		searchParamNameToTypeMap.put("CarePlan:care-team", Sets.create("CareTeam"));
		
		searchParamNameToPathMap.put("CarePlan:condition", "addresses");
		searchParamNameToTypeMap.put("CarePlan:condition", Sets.create("Condition"));
		
		searchParamNameToPathMap.put("CarePlan:context", "context");
		searchParamNameToTypeMap.put("CarePlan:context", Sets.create("EpisodeOfCare", "Encounter"));
		
		searchParamNameToPathMap.put("CarePlan:definition", "definition");
		searchParamNameToTypeMap.put("CarePlan:definition", Sets.create("Questionnaire", "PlanDefinition"));
		
		searchParamNameToPathMap.put("CarePlan:encounter", "context");
		searchParamNameToTypeMap.put("CarePlan:encounter", Sets.create("Encounter"));
		
		searchParamNameToPathMap.put("CarePlan:goal", "goal");
		searchParamNameToTypeMap.put("CarePlan:goal", Sets.create("Goal"));
		
		searchParamNameToPathMap.put("CarePlan:part-of", "partOf");
		searchParamNameToTypeMap.put("CarePlan:part-of", Sets.create("CarePlan"));
		
		searchParamNameToPathMap.put("CarePlan:patient", "subject");
		searchParamNameToTypeMap.put("CarePlan:patient", Sets.create("Patient"));
		
		searchParamNameToPathMap.put("CarePlan:performer", "activity.detail.performer");
		searchParamNameToTypeMap.put("CarePlan:performer", Sets.create("Practitioner", "Organization", "CareTeam", "Patient", "RelatedPerson"));
		
		searchParamNameToPathMap.put("CarePlan:replaces", "replaces");
		searchParamNameToTypeMap.put("CarePlan:replaces", Sets.create("CarePlan"));
		
		searchParamNameToPathMap.put("CarePlan:subject", "subject");
		searchParamNameToTypeMap.put("CarePlan:subject", Sets.create("Group", "Patient"));

		registerSearches("CarePlan", getClass(), "getCarePlan");
		
		FhirPseudonymizer.forSTU3()
		  .reset("CarePlan")
		  .pseudonymizeReference("CarePlan", "author")		
		  .pseudonymizeReference("CarePlan", "note", "authorReference")
		  .pseudonymizeReference("CarePlan", "activity", "detail", "performer");	
	}

	@Override
	public Class<CarePlan> getResourceType() {
		return CarePlan.class;
	}

	@Search()
	public Bundle getCarePlan(
			@Description(shortDefinition = "The ID of the resource") @OptionalParam(name = "_id") TokenAndListParam the_id,

			@Description(shortDefinition = "The language of the resource") @OptionalParam(name = "_language") StringAndListParam the_language,

			@Description(shortDefinition = "Detail type of activity") @OptionalParam(name = "activity-code") TokenAndListParam theActivity_code,

			@Description(shortDefinition = "Specified date occurs within period specified by CarePlan.activity.timingSchedule") @OptionalParam(name = "activity-date") DateAndListParam theActivity_date,

			@Description(shortDefinition = "Activity details defined in specific resource") @OptionalParam(name = "activity-reference", targetTypes = {}) ReferenceAndListParam theActivity_reference,

			@Description(shortDefinition = "Fulfills care plan") @OptionalParam(name = "based-on", targetTypes = {}) ReferenceAndListParam theBased_on,

			@Description(shortDefinition = "Who's involved in plan?") @OptionalParam(name = "care-team", targetTypes = {}) ReferenceAndListParam theCare_team,

			@Description(shortDefinition = "Type of plan") @OptionalParam(name = "category") TokenAndListParam theCategory,

			@Description(shortDefinition = "Health issues this plan addresses") @OptionalParam(name = "condition", targetTypes = {}) ReferenceAndListParam theCondition,

			@Description(shortDefinition = "Created in context of") @OptionalParam(name = "context", targetTypes = {}) ReferenceAndListParam theContext,

			@Description(shortDefinition = "Time period plan covers") @OptionalParam(name = "date") DateAndListParam theDate,

			@Description(shortDefinition = "Protocol or definition") @OptionalParam(name = "definition", targetTypes = {}) ReferenceAndListParam theDefinition,

			@Description(shortDefinition = "Created in context of") @OptionalParam(name = "encounter", targetTypes = {}) ReferenceAndListParam theEncounter,

			@Description(shortDefinition = "Desired outcome of plan") @OptionalParam(name = "goal", targetTypes = {}) ReferenceAndListParam theGoal,

			@Description(shortDefinition = "External Ids for this plan") @OptionalParam(name = "identifier") TokenAndListParam theIdentifier,

			@Description(shortDefinition = "proposal | plan | order | option") @OptionalParam(name = "intent") TokenAndListParam theIntent,

			@Description(shortDefinition = "Part of referenced CarePlan") @OptionalParam(name = "part-of", targetTypes = {}) ReferenceAndListParam thePart_of,

			@Description(shortDefinition = "Who care plan is for") @OptionalParam(name = "patient", targetTypes = {}) ReferenceAndListParam thePatient,

			@Description(shortDefinition = "Matches if the practitioner is listed as a performer in any of the \"simple\" activities.  (For performers of the detailed activities, chain through the activitydetail search parameter.)") @OptionalParam(name = "performer", targetTypes = {}) ReferenceAndListParam thePerformer,

			@Description(shortDefinition = "CarePlan replaced by this CarePlan") @OptionalParam(name = "replaces", targetTypes = {}) ReferenceAndListParam theReplaces,

			@Description(shortDefinition = "draft | active | suspended | completed | entered-in-error | cancelled | unknown") @OptionalParam(name = "status") TokenAndListParam theStatus,

			@Description(shortDefinition = "Who care plan is for") @OptionalParam(name = "subject", targetTypes = {}) ReferenceAndListParam theSubject,

			@RawParam Map<String, List<String>> theAdditionalRawParams,

			@IncludeParam(reverse = true) Set<Include> theRevIncludes,
			@Description(shortDefinition = "Only return resources which were last updated as specified by the given range") @OptionalParam(name = "_lastUpdated") DateRangeParam theLastUpdated,

			@IncludeParam(allow = { "CarePlan:activity-reference", "CarePlan:based-on", "CarePlan:care-team",
					"CarePlan:condition", "CarePlan:context", "CarePlan:definition", "CarePlan:encounter",
					"CarePlan:goal", "CarePlan:part-of", "CarePlan:patient", "CarePlan:performer", "CarePlan:replaces",
					"CarePlan:subject", "*" }) Set<Include> theIncludes,
			@Sort SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount,

			@OptionalParam(name = "_page") StringParam _page,

			RequestDetails theDetails

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", the_id);
		paramMap.add("_language", the_language);
		paramMap.add("activity-code", theActivity_code);
		paramMap.add("activity-date", theActivity_date);
		paramMap.add("activity-reference", theActivity_reference);
		paramMap.add("based-on", theBased_on);
		paramMap.add("care-team", theCare_team);
		paramMap.add("category", theCategory);
		paramMap.add("condition", theCondition);
		paramMap.add("context", theContext);
		paramMap.add("date", theDate);
		paramMap.add("definition", theDefinition);
		paramMap.add("encounter", theEncounter);
		paramMap.add("goal", theGoal);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("intent", theIntent);
		paramMap.add("part-of", thePart_of);
		paramMap.add("patient", thePatient);
		paramMap.add("performer", thePerformer);
		paramMap.add("replaces", theReplaces);
		paramMap.add("status", theStatus);
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/CarePlan");

		builder.handleIdRestriction();

		// Add handling for search on the "owner" of the record.
		builder.recordOwnerReference("patient", "Patient", "subject");

		if (!builder.recordOwnerReference("subject", null, "subject"))
			builder.restriction("subject", true, null, "subject");

		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		
		builder.restriction("activity-code", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "activity.detail.code");
		builder.restriction("activity-date", true, QueryBuilder.TYPE_DATETIME_OR_PERIOD, "activity.detail.scheduled");
		builder.restriction("activity-reference", true, null, "activity.reference");
		builder.restriction("based-on", true, "CarePlan", "basedOn");
		builder.restriction("care-team", true, "CareTeam", "careTeam");
		builder.restriction("category", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "category");
		builder.restriction("condition", true, "Condition", "addresses");
		builder.restriction("context", true, null, "context");
		builder.restriction("date", true, QueryBuilder.TYPE_PERIOD, "period");
		builder.restriction("definition", true, null, "definition");
		builder.restriction("encounter", true, "Encounter", "context");
		builder.restriction("goal", true, "Goal", "goal");		
		builder.restriction("intent", true, QueryBuilder.TYPE_CODE, "intent");
		builder.restriction("part-of", true, "CarePlan", "partOf");
		builder.restriction("performer", true, null, "activity.detail.performer");
		builder.restriction("replaces", true, "CarePlan", "replaces");
		builder.restriction("status", false, QueryBuilder.TYPE_CODE, "status");

		return query.execute(info);
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam CarePlan theCarePlan) {
		return super.createResource(theCarePlan);
	}

	@Override
	public String getRecordFormat() {	
		return "fhir/CarePlan";
	}
	

	// This method is required if it is allowed to update the resource.
	// Just change the resource type
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam CarePlan theCarePlan) {
		return super.updateResource(theId, theCarePlan);
	}

	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical type
	// "content" set.
	// b) Each record must have a "name" that will be shown to the user in the
	// record tree.
	// The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the FHIR
	// representation
	public void prepare(Record record, CarePlan theCarePlan) throws AppException {
		// Task a : Set Record "content" field by using a code from the resource (or a
		// fixed value or something else useful)
		String display = setRecordCodeByCodeableConcept(record, null, "CarePlan");  

		// Task b : Create record name
		String date = "No time";
		if (theCarePlan.hasPeriod()) {
			try {
				date = FHIRTools.stringFromDateTime(theCarePlan.getPeriod());
			} catch (Exception e) {
				throw new UnprocessableEntityException("Cannot process effectiveDateTime");
			}
		}
		record.name = display != null ? (display + " / " + date) : date;

		// Task c : Set record owner based on subject and clean subject if this was
		// possible
		Reference subjectRef = theCarePlan.getSubject();
		if (cleanAndSetRecordOwner(record, subjectRef))
			theCarePlan.setSubject(null);

		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta"
		// section
		clean(theCarePlan);

	}

	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, CarePlan p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);

		// Add subject field from record owner field if it is not already there
		if (p.getSubject().isEmpty()) {
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}
}
