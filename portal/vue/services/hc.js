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

import server from "./server";
	
	var service = {};
	
	service.list = function() {
	    return server.get(jsRoutes.controllers.members.HealthProvider.list().url);
	};
	
	service.search = function(props, fields) {
		var data = { properties : props, fields : fields };
	    return server.post(jsRoutes.controllers.members.HealthProvider.search().url, data);
	};
	
	service.searchOrganization = function(props) {		
	    return server.post(jsRoutes.controllers.providers.Providers.searchOrganization().url, props);
	};
	
	service.confirm = function(consentId) {
		var data = {"consent": consentId };

		return server.post(jsRoutes.controllers.members.HealthProvider.confirmConsent().url, data);
	};
	
	service.reject = function(consentId) {
		var data = {"consent": consentId };

		return server.post(jsRoutes.controllers.members.HealthProvider.rejectConsent().url, data);
	};
		
	export default service;