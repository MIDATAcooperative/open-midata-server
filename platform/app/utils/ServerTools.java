package utils;

import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.PortalSessionToken;
import utils.fhir.ResourceProvider;

public class ServerTools {

	public static void endRequest() {
		try {
		   RecordManager.instance.clear();		   
		} finally {
		   AuditManager.instance.clear();
		   AccessLog.newRequest();	
		   ResourceProvider.setExecutionInfo(null);
		   PortalSessionToken.clear();
		}
	}
}
