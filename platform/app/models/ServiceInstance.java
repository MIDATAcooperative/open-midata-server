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
			 Sets.create("_id", "name", "endpoint", "appId", "executorAccount", "linkedStudy", "linkedStudyGroup", "managerAccount", "publicKey", "studyRelatedOnly", "status");
    
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
		
	public void add() throws InternalServerException {
		Model.insert(collection, this);	
	}

	public static void delete(MidataId instanceId) throws InternalServerException {				
		Model.delete(ServiceInstance.class, collection, CMaps.map("_id", instanceId));		
	}
		
}
