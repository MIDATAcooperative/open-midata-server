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
import utils.AccessLog;
import utils.ErrorReporter;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;

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
		} catch (PluginException e4) {
			ErrorReporter.reportPluginProblem("FHIR (create resource)", null, e4);
			throw new InternalErrorException(e4);
		} catch (InternalServerException e3) {
			ErrorReporter.report("FHIR (create resource)", null, e3);
			throw new InternalErrorException(e3.getMessage());
		} catch (Exception e4) {
			e4.printStackTrace();
			ErrorReporter.report("FHIR (create resource)", null, e4);
			throw new InternalErrorException("internal error during create resource");
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
		} catch (PluginException e4) {
			ErrorReporter.reportPluginProblem("FHIR (update resource)", null, e4);
			throw new InternalErrorException(e4);
		} catch (InternalServerException e3) {
			ErrorReporter.report("FHIR (update resource)", null, e3);
			throw new InternalErrorException(e3.getMessage());
		} catch (Exception e4) {
			ErrorReporter.report("FHIR (update resource)", null, e4);
			throw new InternalErrorException("internal error during update resource");
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
		M record = init(theResource);			
		createPrepare(record, theResource);
		AccessLog.logBegin("begin create for resource ", getResourceType().getSimpleName());
		try {
		  theResource = createExecute(record, theResource);
		} finally {
		  AccessLog.logEnd("end create resource");
		}
		processResource(record, theResource);						
		return outcome(theResource.getResourceType().name(), theResource);				
	}
	
	public abstract void createPrepare(M record, T theResource) throws AppException;
	
	public abstract T createExecute(M record, T theResource) throws AppException;
	
	public abstract M init(T theResource) throws AppException;
	
	/**
	 * Default implementation for update
	 * @param theId
	 * @param theResource
	 * @return
	 * @throws AppException
	 */
	final
	protected MethodOutcome update(IdType theId, T theResource) throws AppException {
		M record = fetchCurrent(theId, theResource);
		if (record == null) throw new ResourceNotFoundException(theId);
		updatePrepare(record, theResource);	
		AccessLog.logBegin("begin update for resource ", getResourceType().getSimpleName());
		try {
		   updateExecute(record, theResource);	
		} finally {
		   AccessLog.logEnd("end update resource");
		}
		processResource(record, theResource);		
		return outcome(theResource.getResourceType().name(), theResource);				
	}
	
	public abstract void updatePrepare(M record, T theResource) throws AppException;
	
	public abstract void updateExecute(M record, T theResource) throws AppException;	
	
	public abstract String getVersion(M record);
	
	public abstract Date getLastUpdated(M record);
}
