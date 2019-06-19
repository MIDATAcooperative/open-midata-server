package utils.fhir;

import java.util.Date;

import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.IdType;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import models.Model;
import utils.ErrorReporter;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;

public abstract class ReadWriteResourceProvider<T extends DomainResource, M extends Model> extends ResourceProvider<T,M> {

	/**
	 * Default wrapper for FHIR create
	 * @param theResource
	 * @return
	 */
	protected MethodOutcome createResource(@ResourceParam T theResource) {

		try {
			return create(theResource);
		} catch (BaseServerResponseException e) {
			throw e;
		} catch (BadRequestException e2) {
			throw new InvalidRequestException(e2.getMessage());
		} catch (InternalServerException e3) {
			ErrorReporter.report("FHIR (create resource)", null, e3);
			throw new InternalErrorException(e3);
		} catch (Exception e4) {
			ErrorReporter.report("FHIR (create resource)", null, e4);
			throw new InternalErrorException(e4);
		}		

	}
	
	/**
	 * Default wrapper for FHIR update
	 * @param theId
	 * @param theResource
	 * @return
	 */
	protected MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam T theResource) {

		try {
			if (theId.getVersionIdPart() == null && (theResource.getMeta() == null || theResource.getMeta().getVersionId() == null)) throw new PreconditionFailedException("Resource version missing!");
			return update(theId, theResource);
		} catch (BaseServerResponseException e) {
			throw e;
		} catch (BadRequestException e2) {
			throw new InvalidRequestException(e2.getMessage());
		} catch (InternalServerException e3) {
			ErrorReporter.report("FHIR (update resource)", null, e3);
			throw new InternalErrorException(e3);
		} catch (Exception e4) {
			ErrorReporter.report("FHIR (update resource)", null, e4);
			throw new InternalErrorException(e4);
		}		

	}
	
	/**
	 * Default implementation for create
	 * @param theResource
	 * @return
	 * @throws AppException
	 */
	final
	protected MethodOutcome create(T theResource) throws AppException {
		M record = init();			
		createPrepare(record, theResource);
		createExecute(record, theResource);		
		processResource(record, theResource);						
		return outcome(theResource.getResourceType().name(), theResource);				
	}
	
	public abstract void createPrepare(M record, T theResource) throws AppException;
	
	public abstract void createExecute(M record, T theResource) throws AppException;
	
	public abstract M init();
	
	/**
	 * Default implementation for update
	 * @param theId
	 * @param theResource
	 * @return
	 * @throws AppException
	 */
	final
	protected MethodOutcome update(IdType theId, T theResource) throws AppException {
		M record = fetchCurrent(theId);
		if (record == null) throw new ResourceNotFoundException(theId);
		updatePrepare(record, theResource);		
		updateExecute(record, theResource);	
		processResource(record, theResource);		
		return outcome(theResource.getResourceType().name(), theResource);				
	}
	
	public abstract void updatePrepare(M record, T theResource) throws AppException;
	
	public abstract void updateExecute(M record, T theResource) throws AppException;	
	
	public abstract String getVersion(M record);
	
	public abstract Date getLastUpdated(M record);
}
