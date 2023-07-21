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

import server from './server';

	var service = {};
	
	service.changeStatus = function(userId, status, contractStatus, agbStatus, emailStatus, authType, subroles) {
		var data = { user : userId, status : status };
		if (contractStatus) { data.contractStatus = contractStatus; }
		if (agbStatus) { data.agbStatus = agbStatus; }
		if (emailStatus) { data.emailStatus = emailStatus; }
		if (authType) { data.authType = authType; }
		if (subroles) { data.subroles = subroles; }
		return server.post(jsRoutes.controllers.admin.Administration.changeStatus().url, data);
	};
	
	service.changeOrganizationStatus = function(orgId, status) {
		var data = { organization : orgId, status : status };		
		return server.post(jsRoutes.controllers.admin.Administration.changeOrganizationStatus().url, data);
	};
	
	service.addComment = function(userId, comment) {
		var data = { user : userId, comment : comment };		
		return server.post(jsRoutes.controllers.admin.Administration.addComment().url, data);
	};
	
	service.wipe = function(userId) {
		var data = { user : userId };		
		return server.post(jsRoutes.controllers.admin.Administration.adminWipeAccount().url, data);
	};
		
	export default service;