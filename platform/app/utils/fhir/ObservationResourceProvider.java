package utils.fhir;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

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
import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.QuantityDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.dstu2.resource.OperationOutcome;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class ObservationResourceProvider extends ResourceProvider implements IResourceProvider {

	/**
	 * The getResourceType method comes from IResourceProvider, and must be
	 * overridden to indicate what type of resource this provider supplies.
	 */
	@Override
	public Class<Observation> getResourceType() {				
		return Observation.class;
	}

	/**
	 * The "@Read" annotation indicates that this method supports the read
	 * operation. Read operations should return a single resource instance.
	 * 
	 * @param theId
	 *            The read operation takes one parameter, which must be of type
	 *            IdDt and must be annotated with the "@Read.IdParam"
	 *            annotation.
	 * @return Returns a resource matching this identifier, or null if none
	 *         exists.
	 */
	@Read()
	public Observation getResourceById(@IdParam IdDt theId) throws AppException {
		Record record = RecordManager.instance.fetch(info().executorId, info().targetAPS, new ObjectId(theId.getIdPart()));
		IParser parser = ctx().newJsonParser();
		Observation p = parser.parseResource(Observation.class, record.data.toString());
		processResource(record, p);		
		return p;
	}

	@Search()
	public List<Observation> getObservation(@OptionalParam(name = Observation.SP_PATIENT) ReferenceOrListParam thePatient, @OptionalParam(name = Observation.SP_CODE) TokenOrListParam theCode)
			throws AppException {
		try {
			ExecutionInfo info = info();

			Map<String, Object> criteria = new HashMap<String, Object>();
			Map<String, Object> accountCriteria = CMaps.map("format", "fhir/Observation");

			if (thePatient != null) {
				accountCriteria.put("owner", refsToObjectIds(thePatient));
			}

			if (theCode != null) {
				accountCriteria.put("code", tokensToStrings(theCode));
			}

			AccessLog.logQuery(criteria, Sets.create("data"));
			List<Record> result = RecordManager.instance.list(info.executorId, info.targetAPS, accountCriteria, Sets.create("owner", "ownerName", "version", "created", "lastUpdated", "data"));
			ReferenceTool.resolveOwners(result, true, false);
			List<Observation> patients = new ArrayList<Observation>();
			IParser parser = ctx().newJsonParser();
			for (Record rec : result) {
				try {
					Observation p = parser.parseResource(Observation.class, rec.data.toString());
					processResource(rec, p);
					
					patients.add(p);
				} catch (DataFormatException e) {
				}
			}

			return patients;

		} catch (AppException e) {
			AccessLog.log("ERROR");
			return null;
		}
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
		for (CodingDt coding : theObservation.getCode().getCoding()) {
			if (coding.getCode() != null && coding.getSystem() != null) {
				record.code.add(coding.getSystem() + " " + coding.getCode());
			}
		}

		ContentInfo.setRecordCodeAndContent(record, record.code, null);

		IDatatype valType = theObservation.getValue();
		if (valType instanceof StringDt)
			record.subformat = "String";
		else if (valType instanceof QuantityDt)
			record.subformat = "Quantity";
		else if (valType instanceof BooleanDt)
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
		retVal.setId(new IdDt("Observation", record._id.toString(), "0"));

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
			p.getSubject().setReference(new IdDt("Patient", record.owner.toString()));
			p.getSubject().setDisplay(record.ownerName);
		}		
	}
	
}