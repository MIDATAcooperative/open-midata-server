package utils;

import utils.access.RecordManager;
import utils.auth.PortalSessionToken;
import utils.fhir.ResourceProvider;

public class ServerTools {

	public static void endRequest() {
		try {
		   RecordManager.instance.clear();		   
		} finally {
		   AccessLog.newRequest();	
		   ResourceProvider.setExecutionInfo(null);
		   PortalSessionToken.clear();
		}
	}
}
