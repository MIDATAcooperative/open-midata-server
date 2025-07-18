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

package utils.fhir.transactions;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.r4.model.DomainResource;

import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import models.Model;
import utils.AccessLog;
import utils.exceptions.AppException;
import utils.fhir.FHIRServlet;
import utils.fhir.ReadWriteResourceProvider;
import utils.fhir.ResourceProvider;

/**
 * Transaction step containing a resource UPDATE
 *
 */
public class UpdateTransactionStep extends TransactionStep {

	/**
	 * Creates update transaction step for a given ResourceProvider and DomainResource from request 
	 * @param provider the resource provider to use
	 * @param resource the domain resource provided in the request
	 */
	public UpdateTransactionStep(ResourceProvider<DomainResource, Model> provider, DomainResource resource) {
		this.provider = provider;
		this.resource = resource;
	}
	
	public UpdateTransactionStep(ResourceProvider<DomainResource, Model> provider, DomainResource resource, Model existing) {
		this.provider = provider;
		this.resource = resource;
		this.record = existing;
	}
	
	@Override
    public void init() throws AppException {
		if (record == null) record = provider.fetchCurrent(resource.getIdElement(), resource, false);
    }
	
	@Override
	public void prepare() throws AppException { 				  
		((ReadWriteResourceProvider) provider).updatePrepare(record, resource);
		  if (resource.getMeta() == null || resource.getMeta().getVersionId() == null) throw new PreconditionFailedException("Resource version missing!");
		  if (!((ReadWriteResourceProvider) provider).getVersion(record).equals(resource.getMeta().getVersionId())) throw new ResourceVersionConflictException("Wrong resource version supplied!") ;
		
	}
	
	@Override
	public void execute() throws AppException {
		
		
		if (result == null) {
			AccessLog.logBegin("begin update transaction for resource ",provider.getResourceType().getSimpleName());
			try {	
				((ReadWriteResourceProvider) provider).updateExecute(record, resource);
				result = new BundleEntryComponent();
				BundleEntryResponseComponent response = new BundleEntryResponseComponent();
				response.setLastModified(((ReadWriteResourceProvider) provider).getLastUpdated(record));
				response.setStatus("200 OK");
				response.setLocation(FHIRServlet.getBaseUrl()+"/"+provider.getResourceType().getSimpleName()+"/"+record._id.toString()+"/_history/"+((ReadWriteResourceProvider) provider).getVersion(record));
				result.setResponse(response);
			} finally {
				AccessLog.logEnd("end update transaction");
			}
		}	
		
	}
		
}