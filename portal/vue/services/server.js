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

	import Axios from 'axios';
	import ENV from 'config';
	
	var service = {};	
	
	service.get = function(url) {	
		return Axios.get(ENV.apiurl + url, { headers : { "X-Session-Token" : sessionStorage.token } });
	};
	
	service.getR4 = function(url) {	
		return Axios.get(ENV.apiurl + url, { headers : { "X-Session-Token" : sessionStorage.token, "Accept" : "application/fhir+json; fhirVersion=4.0" } });
	};
	
	service.post = function(url, body) {
		return Axios.post(ENV.apiurl + url, body, { headers : { "X-Session-Token" : sessionStorage.token, "Prefer" : "return=representation" } });
	};
	
	service.postR4 = function(url, body) {
		return Axios.post(ENV.apiurl + url, body, { headers : { "X-Session-Token" : sessionStorage.token, "Prefer" : "return=representation", "Content-Type" : "application/fhir+json; fhirVersion=4.0" } });
	};
				
	service.put = function(url, body) {
		return Axios.put(ENV.apiurl + url, body, { headers : { "X-Session-Token" : sessionStorage.token } });
	};
	
	service.patch = function(url, body) {
		return Axios.patch(ENV.apiurl + url, body, { headers : { "X-Session-Token" : sessionStorage.token } });
	};
	
	service.delete = function(url, body) {
		return Axios.delete(ENV.apiurl + url, { headers : { "X-Session-Token" : sessionStorage.token } });
	};
	
	service.token = function() {
		return service.post(jsRoutes.controllers.Application.downloadToken().url);
	};
				 	
export default service;