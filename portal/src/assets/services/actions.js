angular.module('services')
.factory('actions', ['server', 'circles', function(server, circles) {
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
	
	service.showAction = function($state, override) {
		acarray = override || getActions($state) || acarray;
		if (!hasActions(acarray)) return false;
			
		
		current = acarray[0];
		var action = current.ac;
		if (!action) return false;
		
		if (action == "consent") {			
		    var what = current.s.split(",");
			$state.go("member.newconsent", { share : JSON.stringify({"group":what}), authorize : current.a, action : JSON.stringify(acarray.slice(1)) });
			return true;
		} else if (action == "confirm") {
			$state.go("member.service_consent", { consentId : current.c, action : JSON.stringify(acarray.slice(1)) })
			return true;
		} else if (action == "study") {
			$state.go("member.studydetails", { studyId : current.s, action : JSON.stringify(acarray.slice(1)) });
			return true;
		} else if (action == "leave") {
			$state.go("member.serviceleave", { callback : current.c });
			return true;
		} else if (action == "unconfirmed") {
			
		   var props = { "status" : "UNCONFIRMED" };
		   if (current.consent) props._id = current.consent;
		   		
		   circles.listConsents(props  , ["_id"]).then(function(result) {
				 if (result.data.length > 0) {				
					 $state.go("member.service_consent", { consentId : result.data[0]._id, action : JSON.stringify(acarray)  });					 
				 } else {
					 acarray.splice(0,1);
				     service.showAction($state, acarray);	 
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
