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

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
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
import models.enums.AuditEventType;
import utils.InstanceConfig;
import utils.audit.AuditHeaderTool;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;



public class DiagnosticReportResourceProvider extends RecordBasedResourceProvider<DiagnosticReport>
		implements IResourceProvider {

	public DiagnosticReportResourceProvider() {
		searchParamNameToPathMap.put("DiagnosticReport:based-on", "basedOn");
		searchParamNameToTypeMap.put("DiagnosticReport:based-on", Sets.create("ReferralRequest", "CarePlan", "MedicationRequest", "NutritionOrder", "ProcedureRequest", "ImmunizationRecommendation"));
		
		searchParamNameToPathMap.put("DiagnosticReport:context", "context");
		searchParamNameToTypeMap.put("DiagnosticReport:context", Sets.create("EpisodeOfCare", "Encounter"));
		
		searchParamNameToPathMap.put("DiagnosticReport:encounter", "context");
		searchParamNameToTypeMap.put("DiagnosticReport:encounter", Sets.create("Encounter"));
		
		searchParamNameToPathMap.put("DiagnosticReport:image", "image.link");
		searchParamNameToTypeMap.put("DiagnosticReport:image", Sets.create("Media"));
		
		searchParamNameToPathMap.put("DiagnosticReport:patient", "subject");
		searchParamNameToTypeMap.put("DiagnosticReport:patient", Sets.create("Patient"));
		
		searchParamNameToPathMap.put("DiagnosticReport:performer", "performer.actor");
		searchParamNameToTypeMap.put("DiagnosticReport:performer", Sets.create("Practitioner", "Organization"));
		
		searchParamNameToPathMap.put("DiagnosticReport:result", "result");
		searchParamNameToTypeMap.put("DiagnosticReport:result", Sets.create("Observation"));
		
		searchParamNameToPathMap.put("DiagnosticReport:specimen", "specimen");
		searchParamNameToTypeMap.put("DiagnosticReport:specimen", Sets.create("Specimen"));
		
		searchParamNameToPathMap.put("DiagnosticReport:subject", "subject");
		searchParamNameToTypeMap.put("DiagnosticReport:subject", Sets.create("Group", "Device", "Patient", "Location"));

		registerSearches("DiagnosticReport", getClass(), "getDiagnosticReport");
	}

	@Override
	public Class<DiagnosticReport> getResourceType() {
		return DiagnosticReport.class;
	}

	@Search()
	public Bundle getDiagnosticReport(
			@Description(shortDefinition = "The ID of the resource") @OptionalParam(name = "_id") TokenAndListParam the_id,

			@Description(shortDefinition = "The language of the resource") @OptionalParam(name = "_language") StringAndListParam the_language,

			@Description(shortDefinition = "Reference to the procedure request.") @OptionalParam(name = "based-on", targetTypes = {}) ReferenceAndListParam theBased_on,

			@Description(shortDefinition = "Which diagnostic discipline/department created the report") @OptionalParam(name = "category") TokenAndListParam theCategory,

			@Description(shortDefinition = "The code for the report as a whole, as opposed to codes for the atomic results, ch are the names on the observation resource referred to from the result") @OptionalParam(name = "code") TokenAndListParam theCode,

			@Description(shortDefinition = "Healthcare event (Episode of Care or Encounter) related to the report") @OptionalParam(name = "context", targetTypes = {}) ReferenceAndListParam theContext,

			@Description(shortDefinition = "The clinically relevant time of the report") @OptionalParam(name = "date") DateAndListParam theDate,

			@Description(shortDefinition = "A coded diagnosis on the report") @OptionalParam(name = "diagnosis") TokenAndListParam theDiagnosis,

			@Description(shortDefinition = "The Encounter when the order was made") @OptionalParam(name = "encounter", targetTypes = {}) ReferenceAndListParam theEncounter,

			@Description(shortDefinition = "An identifier for the report") @OptionalParam(name = "identifier") TokenAndListParam theIdentifier,

			@Description(shortDefinition = "A reference to the image source.") @OptionalParam(name = "image", targetTypes = {}) ReferenceAndListParam theImage,

			@Description(shortDefinition = "When the report was issued") @OptionalParam(name = "issued") DateAndListParam theIssued,

			@Description(shortDefinition = "The subject of the report if a patient") @OptionalParam(name = "patient", targetTypes = {}) ReferenceAndListParam thePatient,

			@Description(shortDefinition = "Who was the source of the report (organization)") @OptionalParam(name = "performer", targetTypes = {}) ReferenceAndListParam thePerformer,

			@Description(shortDefinition = "Link to an atomic result (observation resource)") @OptionalParam(name = "result", targetTypes = {}) ReferenceAndListParam theResult,

			@Description(shortDefinition = "The specimen details") @OptionalParam(name = "specimen", targetTypes = {}) ReferenceAndListParam theSpecimen,

			@Description(shortDefinition = "The status of the report") @OptionalParam(name = "status") TokenAndListParam theStatus,

			@Description(shortDefinition = "The subject of the report") @OptionalParam(name = "subject", targetTypes = {}) ReferenceAndListParam theSubject,

			@RawParam Map<String, List<String>> theAdditionalRawParams,

			@IncludeParam(reverse = true) Set<Include> theRevIncludes,
			@Description(shortDefinition = "Only return resources which were last updated as specified by the given range") @OptionalParam(name = "_lastUpdated") DateRangeParam theLastUpdated,

			@IncludeParam(allow = { "DiagnosticReport:based-on", "DiagnosticReport:context",
					"DiagnosticReport:encounter", "DiagnosticReport:image", "DiagnosticReport:patient",
					"DiagnosticReport:performer", "DiagnosticReport:result", "DiagnosticReport:specimen",
					"DiagnosticReport:subject", "*" }) Set<Include> theIncludes,

			@Sort SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount,

			@OptionalParam(name = "_page") StringParam _page,

			RequestDetails theDetails

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", the_id);
		paramMap.add("_language", the_language);
		paramMap.add("based-on", theBased_on);
		paramMap.add("category", theCategory);
		paramMap.add("code", theCode);
		paramMap.add("context", theContext);
		paramMap.add("date", theDate);
		paramMap.add("diagnosis", theDiagnosis);
		paramMap.add("encounter", theEncounter);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("image", theImage);
		paramMap.add("issued", theIssued);
		paramMap.add("patient", thePatient);
		paramMap.add("performer", thePerformer);
		paramMap.add("result", theResult);
		paramMap.add("specimen", theSpecimen);
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
		AccessContext info = info();

		Query query = new Query();
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/DiagnosticReport");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", "subject");

		// Add handling for search on the field that determines the MIDATA content type
		// used.
		builder.recordCodeRestriction("code", "code");

		if (!builder.recordOwnerReference("subject", null, "subject"))
			builder.restriction("subject", true, null, "subject"); // TODO not so sure what to do here with patient and
																	// subject

		builder.restriction("based-on", true, null, "basedOn");
		builder.restriction("category", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "category");
		
		builder.restriction("context", true, null, "context");
		builder.restriction("date", true, QueryBuilder.TYPE_DATETIME_OR_PERIOD, "effective");
		builder.restriction("diagnosis", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "codedDiagnosis");
		builder.restriction("encounter", true, "Encounter", "encounter");
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("image", true, "Media", "image.link");
		builder.restriction("issued", true, QueryBuilder.TYPE_DATETIME, "issued");
		builder.restriction("performer", true, "Performer", "performer");
		builder.restriction("result", true, "Observation", "result");
		builder.restriction("specimen", true, "Specimen", "specimen");
		builder.restriction("status", true, QueryBuilder.TYPE_CODE, "status");

		return query.execute(info);
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam DiagnosticReport theDiagnosticReport) {
		return super.createResource(theDiagnosticReport);
	}
	
	@Override
	public void createExecute(Record record, DiagnosticReport theDiagnosticReport) throws AppException {
        Attachment attachment = null;
		
        List<Attachment> att = theDiagnosticReport.getPresentedForm();		
		if (att != null && att.size() == 1) {
			attachment = att.get(0);		
		}
		
		insertRecord(record, theDiagnosticReport, attachment);
		AuditHeaderTool.createAuditEntryFromHeaders(info(), AuditEventType.REST_CREATE, record.owner);
	}	

	@Override
	public String getRecordFormat() {	
		return "fhir/DiagnosticReport";
	}	


	// This method is required if it is allowed to update the resource.
	// Just change the resource type
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam DiagnosticReport theDiagnosticReport) {
		return super.updateResource(theId, theDiagnosticReport);
	}

	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical type
	// "content" set.
	// b) Each record must have a "name" that will be shown to the user in the
	// record tree.
	// The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the FHIR
	// representation
	public void prepare(Record record, DiagnosticReport theDiagnosticReport) throws AppException {
		// Task a : Set Record "content" field by using a code from the resource (or a
		// fixed value or something else useful)
		String display = setRecordCodeByCodeableConcept(record, theDiagnosticReport.getCode(), "DiagnosticReport"); 

		// Task b : Create record name
		String date = "No time";
		if (theDiagnosticReport.hasEffective()) {
			try {
				date = FHIRTools.stringFromDateTime(theDiagnosticReport.getEffective());
			} catch (Exception e) {
				throw new UnprocessableEntityException("Cannot process effectiveDateTime");
			}
		}
		record.name = display != null ? (display + " / " + date) : date;

		// Task c : Set record owner based on subject and clean subject if this was
		// possible
		Reference subjectRef = theDiagnosticReport.getSubject();
		if (cleanAndSetRecordOwner(record, subjectRef))
			theDiagnosticReport.setSubject(null);

		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta"
		// section
		clean(theDiagnosticReport);

	}

	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, DiagnosticReport p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);

		// Add subject field from record owner field if it is not already there
		if (p.getSubject().isEmpty()) {
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
								
		for (Attachment attachment : p.getPresentedForm()) {			
			if (attachment != null && attachment.getUrl() == null && attachment.getData() == null) {	
			  String url = "https://"+InstanceConfig.getInstance().getPlatformServer()+"/v1/records/file?_id="+record._id;
			  attachment.setUrl(url);
			}
		}
	}
}
