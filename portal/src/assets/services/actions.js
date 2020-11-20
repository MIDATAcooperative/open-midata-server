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

angular.module('services')
.factory('actions', ['server', 'circles', 'spaces', function(server, circles, spaces) {
	var service = {};
	var current = null;
	var acarray = null;
	
	var getActions = function($state) {
        var ac = $state.params.action;
        if (!ac) return undefined;
		var acarray = JSON.parse(ac);
		if (!angular.isArray(acarray)) acarray = [ acarray ];
		return acarray;
	};
	
	var hasActions = function(acarray) {
		if (!acarray || acarray.length==0 || !acarray[0].ac) return false;
		return true;
	};
	
	service.hasMore = function() {
	   return hasActions(acarray);	
	};
	
	service.getAppName = function($state) {
		var ac = getActions($state);
		if (hasActions(ac) && ac[0].ac == "use") return ac[0].c;
		return null;
	};
	
	service.showAction = function($state, role, override) {
				
		role = role || $state.current.data.role.toLowerCase();
		acarray = override || getActions($state) || acarray;
		if (!hasActions(acarray)) return false;
			
		
		current = acarray[0];
				
		var action = current.ac;
		if (!action) return false;
		
		if (action == "consent") {			
		    var what = current.s.split(",");
			$state.go(role+".newconsent", { share : JSON.stringify({"group":what}), authorize : current.a, action : JSON.stringify(acarray.slice(1)) });
			return true;
		} else if (action == "confirm") {
			$state.go(role+".service_consent", { consentId : current.c, action : JSON.stringify(acarray.slice(1)) })
			return true;
		} else if (action == "study") {
			$state.go(role+".studydetails", { studyId : current.s, action : JSON.stringify(acarray.slice(1)) });
			return true;
		} else if (action == "use") {
			$state.go(role+".spaces", { app : current.c, action : JSON.stringify(acarray.slice(1)) });			
		    return true;
		} else if (action == "leave") {
			$state.go(role+".serviceleave", { callback : current.c });
			return true;
		} else if (action == "unconfirmed") {
			
		   var props = { "status" : "UNCONFIRMED" };
		   if (current.consent) props._id = current.consent;
		   		
		   circles.listConsents(props  , ["_id"]).then(function(result) {
				 if (result.data.length > 0) {				
					 $state.go(role+".service_consent", { consentId : result.data[0]._id, action : JSON.stringify(acarray)  });					 
				 } else {
					 acarray.splice(0,1);
				     service.showAction($state, role, acarray);	 
				 }				 
		   });
		   return true;
			
		}				
	};
	
	service.done = function($state) {
		var acarray = getActions($state);
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
	
	return service;
}]);
