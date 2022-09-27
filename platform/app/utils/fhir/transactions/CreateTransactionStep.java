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

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import models.Model;
import utils.exceptions.AppException;
import utils.fhir.FHIRServlet;
import utils.fhir.ReadWriteResourceProvider;
import utils.fhir.ResourceProvider;

/**
 * Create a new FHIR resource as part of a FHIR tranction or batch
 *
 */
public class CreateTransactionStep extends TransactionStep {

	/**
	 * Creates a new resource create step using the given ResourceProvider and DomainResource
	 * @param provider the ResourceProvider to use
	 * @param resource the DomainResource from the user request
	 */
	public CreateTransactionStep(ResourceProvider<DomainResource, Model> provider, DomainResource resource) {
		this.provider = provider;
		this.resource = resource;
	}
	
	@Override
    public void init() {
    	record = ((ReadWriteResourceProvider) provider).init(resource);
    }
	
	@Override
	public void prepare() throws AppException { 		
		((ReadWriteResourceProvider) provider).createPrepare(record, resource);		
	}
	
	@Override
	public void execute() throws AppException {
		
			if (result == null) {
				try {
					((ReadWriteResourceProvider) provider).createExecute(record, resource);
				} catch (AppException e) {
				  throw new InternalErrorException(e.getMessage());
				}
				result = new BundleEntryComponent();
				BundleEntryResponseComponent response = new BundleEntryResponseComponent();
				response.setLastModified(((ReadWriteResourceProvider) provider).getLastUpdated(record));
				response.setStatus("201 Created");
				response.setLocation(FHIRServlet.getBaseUrl()+"/"+provider.getResourceType().getSimpleName()+"/"+record._id.toString()+"/_history/0");
				result.setResponse(response);
			}
		 
	}

	
}
