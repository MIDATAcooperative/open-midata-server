package utils.plugins;

import java.io.Serializable;

enum DeployPhase implements Serializable {
	SCEDULED,
	
	COORDINATE,
	
	STARTED,
	
	CHECKOUT,
	
	REPORT_CHECKOUT,
	
	INSTALL,
	
	REPORT_INSTALL,
	
	AUDIT,
	
	REPORT_AUDIT,
	
	COMPILE,
	
	REPORT_COMPILE,
			
	PUBLISH,				
	
	FINISHED,
	
	FAILED,
	
	COORDINATE_DELETE,
	
	DELETE
}