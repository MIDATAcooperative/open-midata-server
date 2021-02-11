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
const querystring = require('querystring');

	var service = {};
	
	service.unwrap = function(result) {
		var res = [];
		if (result && result.data && result.data.entry && result.data.entry.length)
		for (let entry of result.data.entry) {
			res.push(entry.resource);
		}
		return res;
	};
	
	service.search = function(resource, params) {			
		var p = querystring.stringify(params);

		return server.getR4("/fhir/"+resource+(p?("?"+p):"")).then(function(result) {
			return service.unwrap(result);
		});
	};	
	
	service.postR4 = function(resource, data) {
		return server.post("/fhir/"+resource, JSON.stringify(data));
	};
		
	export default service;