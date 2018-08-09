package utils.fhir;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Sequence;

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
import ca.uhn.fhir.rest.param.CompositeAndListParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.NumberAndListParam;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import models.Record;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class SequenceResourceProvider extends RecordBasedResourceProvider<Sequence> implements IResourceProvider {

	// Provide one default constructor
	public SequenceResourceProvider() {

		searchParamNameToPathMap.put("Sequence:patient", "patient");

		searchParamNameToTypeMap.put("Sequence:patient", Sets.create("Patient"));

		registerSearches("Sequence", getClass(), "getSequence");
	}

	// Return corresponding FHIR class
	@Override
	public Class<Sequence> getResourceType() {
		return Sequence.class;
	}

	@Search()
	public Bundle getSequence(
			@Description(shortDefinition = "The ID of the resource") @OptionalParam(name = "_id") TokenAndListParam the_id,

			@Description(shortDefinition = "The language of the resource") @OptionalParam(name = "_language") StringAndListParam the_language,

			@Description(shortDefinition = "Chromosome number of the reference sequence") @OptionalParam(name = "chromosome") TokenAndListParam theChromosome,

			@Description(shortDefinition = "Search parameter for region of the reference DNA sequence string. This will refer to part of a locus or part of a gene where search region will be represented in 1-based system. Since the coordinateSystem can either be 0-based or 1-based, this search query will include the result of both coordinateSystem that contains the equivalent segment of the gene or whole genome sequence. For example, a search for sequence can be represented as `coordinate=1$lt345$gt123`, this means it will search for the Sequence resource on chromosome 1 and with position >123 and <345, where in 1-based system resource, all strings within region 1:124-344 will be revealed, while in 0-based system resource, all strings within region 1:123-344 will be revealed. You may want to check detail about 0-based v.s. 1-based above.") @OptionalParam(name = "coordinate", compositeTypes = {
					TokenParam.class, NumberParam.class }) CompositeAndListParam<TokenParam, NumberParam> theCoordinate,

			@Description(shortDefinition = "End position (0-based exclusive, which menas the acid at this position will not be included, 1-based inclusive, which means the acid at this position will be included) of the reference sequence.") @OptionalParam(name = "end") NumberAndListParam theEnd,

			@Description(shortDefinition = "The unique identity for a particular sequence") @OptionalParam(name = "identifier") TokenAndListParam theIdentifier,

			@Description(shortDefinition = "The subject that the observation is about") @OptionalParam(name = "patient", targetTypes = {}) ReferenceAndListParam thePatient,

			@Description(shortDefinition = "Start position (0-based inclusive, 1-based inclusive, that means the nucleic acid or amino acid at this position will be included) of the reference sequence.") @OptionalParam(name = "start") NumberAndListParam theStart,

			@Description(shortDefinition = "Amino Acid Sequence/ DNA Sequence / RNA Sequence") @OptionalParam(name = "type") TokenAndListParam theType,

			@RawParam Map<String, List<String>> theAdditionalRawParams,

			@IncludeParam(reverse = true) Set<Include> theRevIncludes,
			@Description(shortDefinition = "Only return resources which were last updated as specified by the given range") @OptionalParam(name = "_lastUpdated") DateRangeParam theLastUpdated,

			@IncludeParam(allow = { "Sequence:patient", "*" }) Set<Include> theIncludes,

			@Sort SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount,

			// Non FHIR parameter used for pagination
			@OptionalParam(name = "_page") StringParam _page,

			RequestDetails theDetails

	) throws AppException {

		// The implementation of this method may also be copied from happy fhir except
		// for the last lines
		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", the_id);
		paramMap.add("_language", the_language);
		paramMap.add("chromosome", theChromosome);
		paramMap.add("coordinate", theCoordinate);
		paramMap.add("end", theEnd);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("patient", thePatient);
		paramMap.add("start", theStart);
		paramMap.add("type", theType);

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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Sequence");

		// Now all possible searches need to be handeled. For performance reasons it
		// makes sense
		// to put searches that are very restrictive and frequently used first in order

		// Add default handling for the _id search parameter
		builder.handleIdRestriction();

		// Add handling for search on the "owner" of the record.
		builder.recordOwnerReference("patient", "Patient", "subject");

		// Add handling for search on the field that determines the MIDATA content type
		// used.
		builder.recordCodeRestriction("code", "code");


		builder.restriction("chromosome", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "referenceSeq.chromosome");
		


		// TODO implement the coordinate search.
		// builder.restriction("coordinate");

		// Example for a restriction on a Codeable Concept:
		builder.restriction("end", true, QueryBuilder.TYPE_STRING, "referenceSeq.windowEnd	"); // TODO implement type number
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("start", true, QueryBuilder.TYPE_STRING, "referenceSeq.windowStart"); // TODO implement type number
		builder.restriction("type", true, QueryBuilder.TYPE_STRING, "type"); // TODO implement type number
		
		
		// At last execute the constructed query
		return query.execute(info);
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Sequence theSequence) {
		return super.createResource(theSequence);
	}

	// Construct a new empty MIDATA record that is initialized with the correct
	// format.
	public Record init() {
		return newRecord("fhir/Sequence");
	}

	// This method is required if it is allowed to update the resource.
	// Just change the resource type
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Sequence theSequence) {
		return super.updateResource(theId, theSequence);
	}

	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical type
	// "content" set.
	// b) Each record must have a "name" that will be shown to the user in the
	// record tree.
	// The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the FHIR
	// representation
	public void prepare(Record record, Sequence theSequence) throws AppException {
		// Task a : Set Record "content" field by using a code from the resource (or a
		// fixed value or something else useful)
		String display = setRecordCodeByCodeableConcept(record, null, "Sequence"); // TODO check line

		// Task b : Create record name
		String date = FHIRTools.stringFromDateTime(DateTimeType.now()); // TODO Sequence has nothing that could be used here.

		record.name = display != null ? (display + " / " + date) : date;

		// Task c : Set record owner based on subject and clean subject if this was
		// possible
		Reference subjectRef = theSequence.getPatient();
		if (cleanAndSetRecordOwner(record, subjectRef))
			theSequence.setPatient(null);

		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta"
		// section
		clean(theSequence);

	}

	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, Sequence p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);

		// Add subject field from record owner field if it is not already there
		if (p.getPatient().isEmpty()) {
			p.setPatient(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}

}