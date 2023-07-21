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

package models;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFilter;

import models.enums.UserStatus;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

@JsonFilter("ServiceInstance")
public class ServiceInstance extends Model {

	private static final String collection = "serviceinstances";
	
	public @NotMaterialized final static Set<String> ALL = 
			 Sets.create("_id", "name", "endpoint", "appId", "executorAccount", "linkedStudy", "linkedStudyGroup", "managerAccount", "publicKey", "studyRelatedOnly", "restrictReadToGroup", "status");
   
	public @NotMaterialized final static Set<String> LIMITED = 
			 Sets.create("_id", "name", "endpoint", "appId", "linkedStudy", "managerAccount","status");
  
    /** 
     * name of service
    */
    public String name;
    
    /**
     * pubish fhir endpoint 
     */
    public String endpoint;

    /**
     * id of service application
     */
    public MidataId appId;
    
    public @NotMaterialized Plugin app;
    
    /**
     * id of executor. (optional)
     */
    public MidataId executorAccount;
    
    /**
     * id of linked study (optional)
     */
    public MidataId linkedStudy;

    /**
     * Name of linked study group
     */
    public String linkedStudyGroup;
    
    
    public boolean studyRelatedOnly;
    
    public boolean restrictReadToGroup;
        
    /**
     * if of user who manages this service instance
     */
    public MidataId managerAccount;

    /**
     * public key if no other executor exists
     */
    public byte[] publicKey;

    /**
     * status of this service instance
     */
    public UserStatus status;
                         
	public static ServiceInstance getById(MidataId id, Set<String> fields) throws InternalServerException {
		return Model.get(ServiceInstance.class, collection, CMaps.map("_id", id), fields);
    }
	
	public static ServiceInstance getByEndpoint(String endpoint, Set<String> fields) throws InternalServerException {
		return Model.get(ServiceInstance.class, collection, CMaps.map("endpoint", endpoint), fields);
    }
    
  public static Set<ServiceInstance> getByManager(MidataId managerId, Set<String> fields) throws InternalServerException {
		return Model.getAll(ServiceInstance.class, collection, CMaps.map("managerAccount", managerId), fields);
  }
  
  public static Set<ServiceInstance> getByManagerAndApp(MidataId managerId, MidataId appId, Set<String> fields) throws InternalServerException {
		return Model.getAll(ServiceInstance.class, collection, CMaps.map("managerAccount", managerId).map("appId", appId), fields);
}

  public static Set<ServiceInstance> getByManager(Set<MidataId> managerId, Set<String> fields) throws InternalServerException {
		return Model.getAll(ServiceInstance.class, collection, CMaps.map("managerAccount", managerId), fields);
  }
  
  public static Set<ServiceInstance> getByApp(MidataId appId, Set<String> fields) throws InternalServerException {
		return Model.getAll(ServiceInstance.class, collection, CMaps.map("appId", appId), fields);
	}
  
  public static Set<ServiceInstance> getWithEndpoint(Set<String> fields) throws InternalServerException {
		return Model.getAll(ServiceInstance.class, collection, CMaps.map("endpoint", CMaps.map("$exists", true)), fields);
   }
		
	public void add() throws InternalServerException {
		Model.insert(collection, this);	
	}

	public static void delete(MidataId instanceId) throws InternalServerException {				
		Model.delete(ServiceInstance.class, collection, CMaps.map("_id", instanceId));		
	}
		
}
