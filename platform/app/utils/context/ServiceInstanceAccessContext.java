package utils.context;

import models.MidataId;
import models.Record;
import models.ServiceInstance;
import utils.RuntimeConstants;
import utils.access.APSCache;
import utils.access.DBRecord;
import utils.collections.RequestCache;
import utils.exceptions.AppException;

public class ServiceInstanceAccessContext extends AccessContext {

	private ServiceInstance serviceInstance;
	private RequestCache requestCache;
	
	public ServiceInstanceAccessContext(APSCache cache, RequestCache requestCache, ServiceInstance serviceInstance) {
		super(cache, null);	 
		this.serviceInstance = serviceInstance;
		this.requestCache = requestCache;
	}	

	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		return true;
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return true;
	}

	@Override
	public boolean mayUpdateRecord(DBRecord stored, Record newVersion) {
		return true;
	}
	
		
	@Override
	public boolean mustPseudonymize() {
		return false;
	}
	
	@Override
	public boolean mustRename() {
		return false;
	}

	@Override
	public MidataId getTargetAps() {
		return serviceInstance.executorAccount;
	}
	
	
	
	@Override
	public MidataId getAccessor() {		
		return serviceInstance.executorAccount;
	}

	@Override
	public String getOwnerName() throws AppException {		
		return null;
	}
	@Override
	public MidataId getOwner() {
		return cache.getAccountOwner();
	}
	
	@Override
	public MidataId getOwnerPseudonymized() throws AppException {
		return cache.getAccountOwner();
	}
	
		
	@Override
	public MidataId getSelf() {
		return serviceInstance.executorAccount;
	}
	
	@Override
	public boolean mayAccess(String content, String format) throws AppException {
		return true;
	}
	
	@Override
	public boolean mayContainRecordsFromMultipleOwners() {		
		return false;
	}
	
	@Override
	public String toString() {
		return "si("+serviceInstance+")";
	}
	@Override
	public Object getAccessRestriction(String content, String format, String field) throws AppException {		
		return null;
	}
			
	@Override
	public RequestCache getRequestCache() {
		return requestCache;
	}

	@Override
	public String getContextName() {
		return "Service Instance '"+serviceInstance.name+"'";
	}
	
	@Override
	public MidataId getUsedPlugin() {
		return serviceInstance.appId; 
	}

}
