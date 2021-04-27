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
import ENV from "config";

	var service = {};
 
	var arr = function(obj) {
		if (Array.isArray(obj.length)) return obj;
		return obj.split(",");
	};
	
	service.all = [];
	   
	service.array = arr(ENV.languages);
	
	for (var i=0;i<service.array.length;i++) service.all.push({ value : service.array[i], name : "enum.language."+service.array[i].toUpperCase() });
	
	service.countries = arr(ENV.countries);
	
export default service;