angular.module('portal')
.controller('NewConsentCtrl', ['$scope', '$state', 'server', 'status', 'circles', 'hc', 'views', 'session', 'users', function($scope, $state, server, status, circles, hc, views, session, users) {
	
	$scope.types = [
	                { value : "CIRCLE", label : "Other MIDATA Members"},
	                { value : "HEALTHCARE", label : "Healthcare Providers" },
	                { value : "STUDYPARTICIPATION", label : "Research" },
	                { value : "EXTERNALSERVICE", label : "Mobile App / External Service" }
	               ];
			
	
	$scope.stati = [
	                { value : "ACTIVE", label : "Active" },
	                { value : "UNCONFIRMED", label : "Unconfirmed, needs confirmation" },
	                { value : "EXPIRED", label : "Expired" }
	                 ];
	
	
	$scope.status = new status(true, $scope);
	$scope.authpersons = [];
	views.reset();
	
	$scope.init = function() {
		
		if ($state.params.consentId) {
			$scope.consentId = $state.params.consentId;
			
			$scope.status.doBusy(circles.listConsents({ "_id" : { "$oid" : $state.params.consentId } }, ["name", "type", "status", "authorized" ]))
			.then(function(data) {
				
				$scope.consent = $scope.myform = data.data[0];				
				views.setView("1", { aps : $state.params.consentId, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowRemove : false, allowAdd : false, type : "circles" });

                var role = ($scope.consent.type === "HEALTHCARE") ? "PROVIDER" : null;				
				angular.forEach($scope.consent.authorized, function(p) {					
					$scope.authpersons.push(session.resolve(p.$oid, function() {
						var res = { "_id" : p };
						if (role) res.role = role;
						return users.getMembers(res, (role == "PROVIDER" ? users.ALLPUBLIC : users.MINIMAL )); 
					}));
				});				
				
			});
			$scope.status.doBusy(server.get(jsRoutes.controllers.Records.getSharingDetails($state.params.consentId).url)).
			then(function(results) {				
			    $scope.sharing = results.data;
			    
			    if ($scope.sharing.query) {
			    	if ($scope.sharing.query["group-exclude"] && !angular.isArray($scope.sharing.query["group-exclude"])) { $scope.sharing.query["group-exclude"] = [ $scope.sharing.query["group-exclude"] ]; }
			    	if ($scope.sharing.query.group && !angular.isArray($scope.sharing.query.group)) { $scope.sharing.query.group = [ $scope.sharing.query.group ]; }
			    }
			});
		} else {
			$scope.consent = $scope.myform = { type : "CIRCLE", status : "ACTIVE", authorized : [] };
			views.disableView("1");
		}
		
		if ($state.params.authorize != null) {
			$scope.consent.type = "HEALTHCARE";			
			$scope.consent.authorized = [ { "$oid" : $state.params.authorize } ];
			
			hc.search({ "_id" : { "$oid" : $state.params.authorize } }, [ "firstname", "lastname", "city", "address1", "address2", "country"])
			.then(function(data) {
				$scope.authpersons = data.data;
				$scope.consent.name = "Health Professional: "+$scope.authpersons[0].firstname+" "+$scope.authpersons[0].lastname;
			});
		}
	};
	
	$scope.create = function() {	
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;		
		if (! $scope.myform.$valid) return;
		
		$scope.status.doAction("create", circles.createNew($scope.consent)).
		then(function(data) {
			if ($scope.authpersons.length > 0) {
				$scope.consent.authorized = [];
				angular.forEach($scope.authpersons, function(p) { $scope.consent.authorized.push({ "$oid" : p._id.$oid }); });
				
				circles.addUsers(data.data._id.$oid, $scope.consent.authorized )
				.then(function() {
					$state.go("^.recordsharing", { selectedType : "circles", selected : data.data._id.$oid });
				});
			} else {
				$state.go("^.recordsharing", { selectedType : "circles", selected : data.data._id.$oid });
			}
			 
		 });
				
	};
	
	$scope.removePerson = function(person) {
		if ($scope.consentId) {
		server.delete(jsRoutes.controllers.Circles.removeMember($scope.consent._id.$oid, person._id.$oid).url).
			success(function() {				
				$scope.authpersons.splice($scope.authpersons.indexOf(person), 1);
			}).
			error(function(err) { $scope.error = "Failed to remove the selected member from circle '" + circle.name + "': " + err; });
		} else {
			$scope.authpersons.splice($scope.authpersons.indexOf(person), 1);
			//$scope.consent.authorized.splice($scope.consent.authorized.indexOf(person._id.$oid), 1);
		}
	};
	
	var addPerson = function(person) {
		console.log("ADD:");
		console.log(person);
		if (person.length) {
			angular.forEach(person, function(p) { 
				$scope.authpersons.push(p); 
				$scope.consent.authorized.push(p._id);
			});
		} else {
		    $scope.authpersons.push(person);
		    $scope.consent.authorized.push(person._id);
		}
		
		if ($scope.consentId) {
			circles.addUsers($scope.consentId, $scope.consent.authorized );
		}
				
	};
	
	$scope.confirmPeopleChange = function() {
		$scope.confirmNeeded = false;
	};
	
	$scope.addPeople = function() {
		if ($scope.consent.type != "CIRCLE") {
		  views.setView("providersearch", { callback : addPerson });	
		} else {
		  views.setView("addusers", { consent : $scope.consent, callback : addPerson });
		}		
	};
	
	$scope.deleteConsent = function() {
		server.delete(jsRoutes.controllers.Circles["delete"]($scope.consent._id.$oid).url).
		then(function() {
			$state.go("^.circles");
		});
	};
	
	$scope.rejectConsent = function() {
		hc.reject($scope.consent._id.$oid).then(function() { $scope.init(); });
	};
	
	$scope.confirmConsent = function() {
		hc.confirm($scope.consent._id.$oid).then(function() { $scope.init(); });	
	};
	
	$scope.showStudyDetails = function() {
		$state.go('^.studydetails', { studyId : $scope.consent._id.$oid });		
	};
	
	$scope.showPasscode = function() {
		$scope.status.doAction('passcode', circles.listConsents({ "_id" : { "$oid" : $state.params.consentId } }, ["type", "passcode" ]))
		.then(function(data) {
			$scope.consent.passcode = data.data[0].passcode;
		});
	};
	
	$scope.init();
}]);
	
