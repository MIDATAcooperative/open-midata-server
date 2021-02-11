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

import circles from './circles';


	var service = {};
	var current = null;
	var acarray = null;
	
	var getActions = function($route) {
        var ac = $route.query.action;
        if (!ac) return undefined;
		var acarray = JSON.parse(ac);
		if (!Array.isArray(acarray)) acarray = [ acarray ];
		return acarray;
	};
	
	var hasActions = function(acarray) {
		if (!acarray || acarray.length==0 || !acarray[0].ac) return false;
		return true;
	};
	
	service.hasMore = function() {
	   return hasActions(acarray);	
	};
	
	service.getAppName = function($route) {
		var ac = getActions($route);
		if (hasActions(ac) && ac[0].ac == "use") return ac[0].c;
		return null;
	};
	
	service.showAction = function($router, $route, role, override) {
				
		role = role || $route.meta.role.toLowerCase();
		acarray = override || getActions($route) || acarray;
		if (!hasActions(acarray)) return false;
			
		
		current = acarray[0];
				
		var action = current.ac;
		if (!action) return false;
		
		if (action == "consent") {			
		    var what = current.s.split(",");
			$router.push({ name : role+".newconsent", query : { share : JSON.stringify({"group":what}), authorize : current.a, action : JSON.stringify(acarray.slice(1)) } });
			return true;
		} else if (action == "confirm") {
			$router.push({ name : role+".service_consent", query : { consentId : current.c, action : JSON.stringify(acarray.slice(1)) } })
			return true;
		} else if (action == "study") {
			$router.push({ name : role+".studydetails", query : { studyId : current.s, action : JSON.stringify(acarray.slice(1)) } });
			return true;
		} else if (action == "use") {
			$router.push({ name : role+".spaces", query : { app : current.c, action : JSON.stringify(acarray.slice(1)) } });			
		    return true;
		} else if (action == "leave") {
			$router.push({ name : role+".serviceleave", query : { callback : current.c } });
			return true;
		} else if (action == "unconfirmed") {
			
		   var props = { "status" : "UNCONFIRMED" };
		   if (current.consent) props._id = current.consent;
		   		
		   circles.listConsents(props  , ["_id"]).then(function(result) {
				 if (result.data.length > 0) {				
					 $router.push({ name : role+".service_consent", query : { consentId : result.data[0]._id, action : JSON.stringify(acarray)  } });					 
				 } else {
					 acarray.splice(0,1);
				     service.showAction($router, $route, role, acarray);	 
				 }				 
		   });
		   return true;
			
		}				
	};
	
	service.done = function($route) {
		var acarray = getActions($route);
		if (!hasActions(acarray)) return "[]";
		acarray.splice(0,1);
		return JSON.stringify(acarray);
	};
	
	service.params = function() {
	  return current || {};
	};
	
	service.logout = function() {
	  acarray = current = null;
	};
	
export default service;