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

package utils.plugins;

import java.io.Serializable;

enum DeployPhase implements Serializable {
	SCEDULED,
	
	COORDINATE,
	
	COORDINATE_AUDIT,
	
	COORDINATE_AUDIT_FIX,
	
	COORDINATE_DEPLOY,
	
	COUNT,
	
	REPORT_COUNT,
	
	CHECKOUT,
	
	REPORT_CHECKOUT,
	
	INSTALL,
	
	REPORT_INSTALL,
	
	AUDIT,
	
	AUDITFIX,
	
	REPORT_AUDIT,
	
	COMPILE,
	
	REPORT_COMPILE,
			
	EXPORT_TO_CDN,	
	
	REPORT_EXPORT_TO_CDN,
	
	IMPORT_CDN,
	
	REPORT_IMPORT_CDN,
	
	EXPORT_SCRIPTS,
	
	REPORT_EXPORT_SCRIPTS,
	
	IMPORT_SCRIPTS,
	
	REPORT_IMPORT_SCRIPTS,
				
	FINISHED,
	
	FINISH_AUDIT,
	
	FAILED,
	
	COORDINATE_DELETE,
	
	DELETE,
	
	REPORT_DELETE,
	
	COORDINATE_WIPE,
	
	WIPE_CDN,
	
	REPORT_WIPE_CDN,
	
	WIPE_SCRIPT,
	
	REPORT_WIPE_SCRIPT;
	
	boolean isReport() {
		return this == REPORT_CHECKOUT 
				|| this == REPORT_INSTALL 
				|| this == REPORT_AUDIT 
				|| this == REPORT_COMPILE 
				|| this == REPORT_EXPORT_SCRIPTS				
				|| this == REPORT_EXPORT_TO_CDN
				|| this == REPORT_IMPORT_SCRIPTS				
				|| this == REPORT_IMPORT_CDN
				|| this == FINISHED
				|| this == FINISH_AUDIT
				|| this == FAILED
				|| this == REPORT_WIPE_CDN; 
	}
	
}