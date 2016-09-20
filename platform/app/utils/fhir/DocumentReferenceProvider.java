package utils.fhir;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.ContentInfo;
import models.Record;

import models.MidataId;
import org.hl7.fhir.dstu3.exceptions.FHIRException;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Range;
import org.hl7.fhir.dstu3.model.Ratio;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.SampledData;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.TimeType;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.IIdType;

import play.Play;

import utils.auth.ExecutionInfo;
import utils.exceptions.AppException;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.CompositeAndListParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
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
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class DocumentReferenceProvider extends ResourceProvider<DocumentReference> implements IResourceProvider {

	@Override
	public Class<DocumentReference> getResourceType() {
		return DocumentReference.class;
	}

	@Search()
	public List<DocumentReference> getDocumentReference(
			

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		
		return search(paramMap);
	}

	public List<Record> searchRaw(SearchParameterMap params) throws AppException {
		ExecutionInfo info = info();

		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/DocumentReference");

		
		
		return query.execute(info);
	}

	@Create
	public MethodOutcome createDocumenentReference(@ResourceParam DocumentReference theObservation) {

		Record record = newRecord("fhir/DocumentReference");
		prepare(record, theObservation);
		// insert
		insertRecord(record, theObservation);

		processResource(record, theObservation);
		return outcome("DocumentReference", record, theObservation);

	}
	
	public Record init() { return newRecord("fhir/DocumentReference"); }

	@Update
	public MethodOutcome updateDocumentReference(@IdParam IdType theId, @ResourceParam DocumentReference theObservation) {
		Record record = fetchCurrent(theId);
		prepare(record, theObservation);
		updateRecord(record, theObservation);
		return outcome("DocumentReference", record, theObservation);
	}

	public void prepare(Record record, DocumentReference theObservation) {
		// Set Record code and content
		record.code = new HashSet<String>(); 
		for (Coding coding : theObservation.getType().getCoding()) {			
			if (coding.getCode() != null && coding.getSystem() != null) {
				record.code.add(coding.getSystem() + " " + coding.getCode());
			}
		}
		try {
			ContentInfo.setRecordCodeAndContent(record, record.code, null);
		} catch (AppException e) {
			throw new InternalErrorException(e);
		}
		record.name = theObservation.getDescription();
	
		// clean
		Reference subjectRef = theObservation.getSubject();
		boolean cleanSubject = true;
		if (subjectRef != null) {
			IIdType target = subjectRef.getReferenceElement();
			if (target != null) {
				String rt = target.getResourceType();
				if (rt != null && rt.equals("Patient")) {
					String tId = target.getIdPart();
					if (! MidataId.isValid(tId)) throw new UnprocessableEntityException("Subject Reference not valid");
					record.owner = new MidataId(tId);
				} else cleanSubject = false;
			}
		}
		
		if (cleanSubject) theObservation.setSubject(null);
		clean(theObservation);

	}

	/*
	 * @Delete() public void deleteObservation(@IdParam IdType theId) { Record
	 * record = fetchCurrent(theId);
	 * RecordManager.instance.deleteRecord(info().executorId, info().targetAPS,
	 * record); }
	 */
 
	@Override
	public void processResource(Record record, DocumentReference p) {
		super.processResource(record, p);
		if (p.getSubject().isEmpty()) {
			p.getSubject().setReferenceElement(new IdType("Patient", record.owner.toString()));
			p.getSubject().setDisplay(record.ownerName);
		}
		for (DocumentReferenceContentComponent component : p.getContent()) {
			Attachment attachment = component.getAttachment();
			if (attachment != null && attachment.getUrl() == null && attachment.getData() == null) {	
			  String url = "https://"+Play.application().configuration().getString("platform.server")+"/v1/records/file?_id="+record._id;
			  attachment.setUrl(url);
			}
		}
	}

	@Override
	public void clean(DocumentReference theObservation) {
		
		super.clean(theObservation);
	}

}