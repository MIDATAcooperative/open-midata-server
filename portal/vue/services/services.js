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
    
    service.listByStudy = function(id) {
        return server.get(jsRoutes.controllers.Services.listServiceInstancesStudy(id).url);
    };

    service.list = function() {
        return server.get(jsRoutes.controllers.Services.listServiceInstances().url);
    };
    
    service.removeService = function(instanceId) {
        return server.delete(jsRoutes.controllers.Services.removeServiceInstance(instanceId).url);
    };
    
    service.listKeys = function(instanceId) {
        return server.get(jsRoutes.controllers.Services.listApiKeys(instanceId).url);
    };

    service.addApiKey = function(instanceId) {
        return server.post(jsRoutes.controllers.Services.addApiKey(instanceId).url);
    };
    
    service.removeApiKey = function(instanceId, keyId) {
        return server.delete(jsRoutes.controllers.Services.removeApiKey(instanceId, keyId).url);
    };
    
	export default service;