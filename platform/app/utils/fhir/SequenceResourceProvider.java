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
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MolecularSequence;
import org.hl7.fhir.r4.model.Reference;

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
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;

public class SequenceResourceProvider extends RecordBasedResourceProvider<MolecularSequence> implements IResourceProvider {

	// Provide one default constructor
	public SequenceResourceProvider() {

		searchParamNameToPathMap.put("MolecularSequence:patient", "patient");

		searchParamNameToTypeMap.put("MolecularSequence:patient", Sets.create("Patient"));

		registerSearches("MolecularSequence", getClass(), "getSequence");
	}

	// Return corresponding FHIR class
	@Override
	public Class<MolecularSequence> getResourceType() {
		return MolecularSequence.class;
	}

	@Search()
	public Bundle getSequence(
			@Description(shortDefinition = "The ID of the resource") @OptionalParam(name = "_id") TokenAndListParam the_id,

			@Description(shortDefinition = "The language of the resource") @OptionalParam(name = "_language") StringAndListParam the_language,

  			@Description(shortDefinition="Chromosome number of the reference sequence")
  			@OptionalParam(name="chromosome")
  			TokenAndListParam theChromosome,
    
  			@Description(shortDefinition="Search parameter by chromosome and variant coordinate. This will refer to part of a locus or part of a gene where search region will be represented in 1-based system. Since the coordinateSystem can either be 0-based or 1-based, this search query will include the result of both coordinateSystem that contains the equivalent segment of the gene or whole genome sequence. For example, a search for sequence can be represented as `chromosome-variant-coordinate=1$lt345$gt123`, this means it will search for the MolecularSequence resource with variants on chromosome 1 and with position >123 and <345, where in 1-based system resource, all strings within region 1:124-344 will be revealed, while in 0-based system resource, all strings within region 1:123-344 will be revealed. You may want to check detail about 0-based v.s. 1-based above.")
  			@OptionalParam(name="chromosome-variant-coordinate", compositeTypes= { TokenParam.class, NumberParam.class })
  			CompositeAndListParam<TokenParam, NumberParam> theChromosome_variant_coordinate,
    
  			@Description(shortDefinition="Search parameter by chromosome and window. This will refer to part of a locus or part of a gene where search region will be represented in 1-based system. Since the coordinateSystem can either be 0-based or 1-based, this search query will include the result of both coordinateSystem that contains the equivalent segment of the gene or whole genome sequence. For example, a search for sequence can be represented as `chromosome-window-coordinate=1$lt345$gt123`, this means it will search for the MolecularSequence resource with a window on chromosome 1 and with position >123 and <345, where in 1-based system resource, all strings within region 1:124-344 will be revealed, while in 0-based system resource, all strings within region 1:123-344 will be revealed. You may want to check detail about 0-based v.s. 1-based above.")
  			@OptionalParam(name="chromosome-window-coordinate", compositeTypes= { TokenParam.class, NumberParam.class })
  			CompositeAndListParam<TokenParam, NumberParam> theChromosome_window_coordinate,
    
  			@Description(shortDefinition="The unique identity for a particular sequence")
  			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier,
    
  			@Description(shortDefinition="The subject that the observation is about")
  			@OptionalParam(name="patient", targetTypes={  } )
  			ReferenceAndListParam thePatient, 
    
  			@Description(shortDefinition="Reference Sequence of the sequence")
  			@OptionalParam(name="referenceseqid")
  			TokenAndListParam theReferenceseqid,
    
  			@Description(shortDefinition="Search parameter by reference sequence and variant coordinate. This will refer to part of a locus or part of a gene where search region will be represented in 1-based system. Since the coordinateSystem can either be 0-based or 1-based, this search query will include the result of both coordinateSystem that contains the equivalent segment of the gene or whole genome sequence. For example, a search for sequence can be represented as `referenceSeqId-variant-coordinate=NC_000001.11$lt345$gt123`, this means it will search for the MolecularSequence resource with variants on NC_000001.11 and with position >123 and <345, where in 1-based system resource, all strings within region NC_000001.11:124-344 will be revealed, while in 0-based system resource, all strings within region NC_000001.11:123-344 will be revealed. You may want to check detail about 0-based v.s. 1-based above.")
  			@OptionalParam(name="referenceseqid-variant-coordinate", compositeTypes= { TokenParam.class, NumberParam.class })
  			CompositeAndListParam<TokenParam, NumberParam> theReferenceseqid_variant_coordinate,
    
  			@Description(shortDefinition="Search parameter by reference sequence and window. This will refer to part of a locus or part of a gene where search region will be represented in 1-based system. Since the coordinateSystem can either be 0-based or 1-based, this search query will include the result of both coordinateSystem that contains the equivalent segment of the gene or whole genome sequence. For example, a search for sequence can be represented as `referenceSeqId-window-coordinate=NC_000001.11$lt345$gt123`, this means it will search for the MolecularSequence resource with a window on NC_000001.11 and with position >123 and <345, where in 1-based system resource, all strings within region NC_000001.11:124-344 will be revealed, while in 0-based system resource, all strings within region NC_000001.11:123-344 will be revealed. You may want to check detail about 0-based v.s. 1-based above.")
  			@OptionalParam(name="referenceseqid-window-coordinate", compositeTypes= { TokenParam.class, NumberParam.class })
  			CompositeAndListParam<TokenParam, NumberParam> theReferenceseqid_window_coordinate,
    
 			@Description(shortDefinition="Amino Acid Sequence/ DNA Sequence / RNA Sequence")
 			@OptionalParam(name="type")
 			TokenAndListParam theType,
   
 			@Description(shortDefinition="End position (0-based exclusive, which menas the acid at this position will not be included, 1-based inclusive, which means the acid at this position will be included) of the variant.")
 			@OptionalParam(name="variant-end")
 			NumberAndListParam theVariant_end, 
   
 			@Description(shortDefinition="Start position (0-based inclusive, 1-based inclusive, that means the nucleic acid or amino acid at this position will be included) of the variant.")
 			@OptionalParam(name="variant-start")
 			NumberAndListParam theVariant_start, 
   
 			@Description(shortDefinition="End position (0-based exclusive, which menas the acid at this position will not be included, 1-based inclusive, which means the acid at this position will be included) of the reference sequence.")
 			@OptionalParam(name="window-end")
 			NumberAndListParam theWindow_end, 
   
 			@Description(shortDefinition="Start position (0-based inclusive, 1-based inclusive, that means the nucleic acid or amino acid at this position will be included) of the reference sequence.")
 			@OptionalParam(name="window-start")
 			NumberAndListParam theWindow_start, 
 
 			@RawParam
 			Map<String, List<String>> theAdditionalRawParams,
 
 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= {
 					"MolecularSequence:patient",
 					"*"
 			}) 
 			Set<Include> theIncludes,

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
		paramMap.add("chromosome-variant-coordinate", theChromosome_variant_coordinate);
		paramMap.add("chromosome-window-coordinate", theChromosome_window_coordinate);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("patient", thePatient);
		paramMap.add("referenceseqid", theReferenceseqid);
		paramMap.add("referenceseqid-variant-coordinate", theReferenceseqid_variant_coordinate);
		paramMap.add("referenceseqid-window-coordinate", theReferenceseqid_window_coordinate);
		paramMap.add("type", theType);
		paramMap.add("variant-end", theVariant_end);
		paramMap.add("variant-start", theVariant_start);
		paramMap.add("window-end", theWindow_end);
		paramMap.add("window-start", theWindow_start);
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Sequence");

		// Now all possible searches need to be handeled. For performance reasons it
		// makes sense
		// to put searches that are very restrictive and frequently used first in order

		// Add default handling for the _id search parameter
		builder.handleIdRestriction();

		// Add handling for search on the "owner" of the record.
		builder.recordOwnerReference("patient", "Patient", "patient");
		

		builder.restriction("chromosome", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "referenceSeq.chromosome");
		
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");

		// TODO implement the coordinate search.
		// builder.restriction("coordinate");

		// Example for a restriction on a Codeable Concept:
		builder.restriction("end", true, QueryBuilder.TYPE_INTEGER, "referenceSeq.windowEnd	"); 		
		builder.restriction("start", true, QueryBuilder.TYPE_INTEGER, "referenceSeq.windowStart"); 
		builder.restriction("type", true, QueryBuilder.TYPE_CODE, "type"); 
		
		
		// At last execute the constructed query
		return query.execute(info);
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam MolecularSequence theSequence) {
		return super.createResource(theSequence);
	}

	@Override
	public String getRecordFormat() {	
		return "fhir/Sequence";
	}	
	
	

	// This method is required if it is allowed to update the resource.
	// Just change the resource type
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam MolecularSequence theSequence) {
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
	public void prepare(Record record, MolecularSequence theSequence) throws AppException {
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
	public void processResource(Record record, MolecularSequence p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);

		// Add subject field from record owner field if it is not already there
		if (p.getPatient().isEmpty()) {
			p.setPatient(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}

	@Override
	protected void convertToR4(Object in) {
		// No action
		
	}

}