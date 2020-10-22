/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.fhir.transactions;

import org.hl7.fhir.r4.model.DomainResource;

import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import models.Model;
import utils.fhir.ResourceProvider;

/**
 * Deleted a FHIR resource as part of a FHIR tranction or batch
 * NOT IMPLEMENTED YET
 */
public class DeleteTransactionStep extends TransactionStep {

	/**
	 * Creates a new delete transaction step for the given ResourceProvider and DomainResource from the user request
	 * @param provider the ResourceProvider to use
	 * @param resource the DomainResource from the user request
	 */
	public DeleteTransactionStep(ResourceProvider<DomainResource, Model> provider, DomainResource resource) {
		this.provider = provider;
		this.resource = resource;
	}
	
	@Override
    public void init() {
    	throw new NotImplementedOperationException("delete not implemented");    	
    }
	
	@Override
	public void prepare() { 
		throw new NotImplementedOperationException("delete not implemented");
	}
	
	@Override
	public void execute() {
		throw new NotImplementedOperationException("delete not implemented");
	}
	
}