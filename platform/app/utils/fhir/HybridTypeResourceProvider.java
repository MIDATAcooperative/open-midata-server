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
import java.util.List;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.DomainResource;

import ca.uhn.fhir.rest.annotation.History;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import models.Model;
import scala.NotImplementedError;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;

public abstract class HybridTypeResourceProvider<T extends DomainResource, M1 extends Model, M2 extends Model> extends ReadWriteResourceProvider<T, Model> {

	private ReadWriteResourceProvider<T,M1> first;
	private ResourceProvider<T,M2> second;
	private ReadWriteResourceProvider<T,M2> secondRW;
	private Class<M1> firstClass;
	private Class<M2> secondClass;
	
	public ReadWriteResourceProvider<T,M1> getFirstProvider() { return first; }
	public ResourceProvider<T,M2> getSecondProvider() { return second; }
	
	public HybridTypeResourceProvider(Class<M1> firstClass, ReadWriteResourceProvider<T,M1> firstProvider,
			                          Class<M2> secondClass, ResourceProvider<T,M2> secondProvider) {
		this.firstClass = firstClass;
		this.first = firstProvider;
		this.secondClass = secondClass;
		this.second = secondProvider;
		if (secondProvider instanceof ReadWriteResourceProvider) {
            this.secondRW = (ReadWriteResourceProvider) secondProvider;			
		}
	}
	
	public abstract boolean handleWithFirstProvider(T resource);
	
	public boolean isFirstModel(Model model) {
		return firstClass.isInstance(model);
	}
	
	public boolean isSecondModel(Model model) {
		return secondClass.isInstance(model);
	}
	
	
	@Override
	public void createPrepare(Model record, T theResource) throws AppException {
		if (handleWithFirstProvider(theResource)) {
			first.createPrepare((M1) record, theResource);
		} else {
			if (secondRW!=null) secondRW.createPrepare((M2) record, theResource);
			else throw new BadRequestException("error.nowrite", "Cannot write this resource.");
		}		
	}

	@Override
	public T createExecute(Model record, T theResource) throws AppException {
		if (handleWithFirstProvider(theResource)) {
			return first.createExecute((M1) record, theResource);
		} else  {
			return secondRW.createExecute((M2) record, theResource);
		} 
		
	}

	@Override
	public Model init(T theResource) {
		if (handleWithFirstProvider(theResource)) return first.init(theResource);
		if (secondRW!=null) {
		  return secondRW.init(theResource);
		} else return null;
	}

	@Override
	public void updatePrepare(Model record, T theResource) throws AppException {
		if (handleWithFirstProvider(theResource)) {
			first.updatePrepare((M1) record, theResource);
		} else  {
			if (secondRW!=null) secondRW.updatePrepare((M2) record, theResource);
			else throw new BadRequestException("error.nowrite", "Cannot write this resource.");
		} 
		
	}

	@Override
	public void updateExecute(Model record, T theResource) throws AppException {
		if (handleWithFirstProvider(theResource)) {
			first.updateExecute((M1) record, theResource);
		} else  {
			secondRW.updateExecute((M2) record, theResource);
		} 		
	}

	@Override
	public String getVersion(Model record) {
		if (isFirstModel(record)) return first.getVersion((M1) record);
		else if (secondRW!=null) return secondRW.getVersion((M2) record);
		return "0";
	}

	@Override
	public Date getLastUpdated(Model record) {
		if (isFirstModel(record)) return first.getLastUpdated((M1) record);
		else if (secondRW!=null) return secondRW.getLastUpdated((M2) record);
		return null;
	}

	@Override
	public Model fetchCurrent(IIdType theId, T resource) throws AppException {
		if (resource == null) {
			try {
			  return first.fetchCurrent(theId, resource);
			} catch (ResourceNotFoundException e) {
			  return second.fetchCurrent(theId, resource);
			}			
		}
		if (handleWithFirstProvider(resource)) return first.fetchCurrent(theId, resource);
		return second.fetchCurrent(theId, resource);
	}

	@Override
	public void processResource(Model record, T resource) throws AppException {
		if (handleWithFirstProvider(resource)) {
			first.processResource((M1) record, resource);
		} else  {
			second.processResource((M2) record, resource);
		} 			
	}

	@Override
	public List<Model> searchRaw(SearchParameterMap params) throws AppException {
		throw new NotImplementedError();
	}

	@Read(version=true)
	public T getResourceById(@IdParam IIdType theId) throws AppException {	
		try {
		    T result = first.getResourceById(theId);
		    if (result == null) throw new ResourceNotFoundException(theId);
		    return result;
		} catch (ResourceNotFoundException e) {
			return second.getResourceById(theId);
		}		
	}
	
	@History()
	public List<T> getHistory(@IdParam IIdType theId) throws AppException {
		List<T> result = first.getHistory(theId);
		if (result != null) return result;
		return second.getHistory(theId);
	}

	@Override
	public List<T> parse(List<Model> result, Class<T> resultClass) throws AppException {
		throw new NotImplementedError();
	}
	
	public T parse(Model record, Class<T> resultClass) throws AppException {
		if (isFirstModel(record)) return first.parse((M1) record, resultClass);
		return second.parse((M2) record, resultClass);
	}
	
	public List<T> basicSearch(SearchParameterMap params) throws AppException {
		List<T> result1 = first.basicSearch(params);
		List<T> result2 = second.basicSearch(params);
		if (result1 == null || result1.isEmpty()) return result2;
		if (result2 == null || result2.isEmpty()) return result1;
		result1.addAll(result2);
		return result1;		
	}
	
	public int countResources(SearchParameterMap params) {
		int r1 = first.countResources(params);
		int r2 = second.countResources(params);
		return r1 + r2;
	}
	
}
