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

import utils.fhir.ResourceProvider;

public class GetTransactionStep extends TransactionStep {

	
	public GetTransactionStep(ResourceProvider provider, DomainResource resource) {
		this.provider = provider;
		this.resource = resource;
	}
	
    public void init() {
    	//record = provider.init();
    } 
	
	public void prepare() { 
		/*try {
		  provider.prepare(record, resource);
		} catch (Exception e) {
		  setResultBasedOnException(e);
		}*/
	}
	
	public void execute() {
		//provider.insertRecord(record, resource);
	}

}
