angular.module('portal')
.controller('NewConsentCtrl', ['$scope', '$state', 'server', 'status', 'circles', 'hc', 'views', 'session', 'users', 'usergroups', function($scope, $state, server, status, circles, hc, views, session, users, usergroups) {
	
	$scope.types = [
	                { value : "CIRCLE", label : "enum.consenttype.CIRCLE"},
	                { value : "HEALTHCARE", label : "enum.consenttype.HEALTHCARE" },
	                { value : "STUDYPARTICIPATION", label : "enum.consenttype.STUDYPARTICIPATION" },
	                { value : "EXTERNALSERVICE", label : "enum.consenttype.EXTERNALSERVICE" }
	               ];
			
	
	$scope.stati = [
	                { value : "ACTIVE", label : "enum.consent.ACTIVE" },
	                { value : "UNCONFIRMED", label : "enum.consent.UNCONFIRMED" },
	                { value : "EXPIRED", label : "enum.consent.EXPIRED" }
	                 ];
	
	
	$scope.status = new status(false, $scope);
	$scope.authpersons = [];
	$scope.authteams = [];
	$scope.datePickers = {  };
	$scope.dateOptions = {
	  	 formatYear: 'yy',
	  	 startingDay: 1
	};
	views.reset();
	
	$scope.init = function() {
		
		if ($state.params.consentId) {
			$scope.consentId = $state.params.consentId;
			
			$scope.status.doBusy(circles.listConsents({ "_id" : $state.params.consentId }, ["name", "type", "status", "authorized", "entityType", "createdBefore", "validUntil" ]))
			.then(function(data) {
				
				$scope.consent = $scope.myform = data.data[0];				
				views.setView("records_shared", { aps : $state.params.consentId, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowRemove : false, allowAdd : false, type : "circles" });

				if ($scope.consent.entityType == "USERGROUP") {
					$scope.status.doBusy(usergroups.search({ "_id" : $scope.consent.authorized }, ["name"]))
					.then(function(data2) {
						angular.forEach(data2.data, function(userGroup) {
							$scope.authteams.push(userGroup);
						});
					});
				} else {
	                var role = ($scope.consent.type === "HEALTHCARE") ? "PROVIDER" : null;				
					angular.forEach($scope.consent.authorized, function(p) {					
						$scope.authpersons.push(session.resolve(p, function() {
							var res = { "_id" : p };
							if (role) res.role = role;
							return users.getMembers(res, (role == "PROVIDER" ? users.ALLPUBLIC : users.MINIMAL )); 
						}));
					});		
				}
				
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
			$scope.consent = { type : "CIRCLE", status : "ACTIVE", authorized : [] };
			views.disableView("records_shared");
		}
		
		if ($state.params.authorize != null) {
			$scope.consent.type = "HEALTHCARE";			
			$scope.consent.authorized = [ $state.params.authorize ];
			
			hc.search({ "_id" :  $state.params.authorize }, [ "firstname", "lastname", "city", "address1", "address2", "country"])
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
				angular.forEach($scope.authpersons, function(p) { $scope.consent.authorized.push( p._id); });
				
				circles.addUsers(data.data._id, $scope.consent.authorized )
				.then(function() {
					$state.go("^.recordsharing", { selectedType : "circles", selected : data.data._id });
				});
			} else {
				$state.go("^.recordsharing", { selectedType : "circles", selected : data.data._id });
			}
			 
		 });
				
	};
	
	$scope.removePerson = function(person) {
		if ($scope.consentId) {
		server.delete(jsRoutes.controllers.Circles.removeMember($scope.consent._id, person._id).url).
			success(function() {
				if ($scope.consent.entityType == "USERGROUP") {
				  $scope.authteams.splice($scope.authteams.indexOf(person), 1);
				} else {
				  $scope.authpersons.splice($scope.authpersons.indexOf(person), 1);
				}
			}).
			error(function(err) { $scope.error = { code : "error.internal" }; });
		} else {
			if ($scope.consent.entityType == "USERGROUP") {
				  $scope.authteams.splice($scope.authteams.indexOf(person), 1);
			} else {
				  $scope.authpersons.splice($scope.authpersons.indexOf(person), 1);
			}			
		}
	};
	
	var addPerson = function(person, isTeam) {	
		console.log(person);
		if (isTeam) {
			$scope.authteams.push(person);
			$scope.consent.authorized.push(person._id);
			$scope.consent.entityType = "USERGROUP";
		} else {
		if (person.length) {
			angular.forEach(person, function(p) { 
				$scope.authpersons.push(p); 
				$scope.consent.authorized.push(p._id);
			});
		} else {
		    $scope.authpersons.push(person);
		    $scope.consent.authorized.push(person._id);
		}
		}
		if ($scope.consentId) {
			circles.addUsers($scope.consentId, $scope.consent.authorized, isTeam ? "USERGROUP" : "USER" );
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
	
	$scope.addUserGroup = function() {		
		views.setView("usergroupsearch", { callback : addPerson });					
	};
	
	$scope.deleteConsent = function() {
		server.delete(jsRoutes.controllers.Circles["delete"]($scope.consent._id).url).
		then(function() {
			$state.go("^.circles");
		});
	};
	
	$scope.rejectConsent = function() {
		hc.reject($scope.consent._id).then(function() { $scope.init(); });
	};
	
	$scope.confirmConsent = function() {
		hc.confirm($scope.consent._id).then(function() { $scope.init(); });	
	};
	
	$scope.showStudyDetails = function() {
		$state.go('^.studydetails', { studyId : $scope.consent._id });		
	};
	
	$scope.showPasscode = function() {
		$scope.status.doAction('passcode', circles.listConsents({ "_id" : $state.params.consentId }, ["type", "passcode" ]))
		.then(function(data) {
			$scope.consent.passcode = data.data[0].passcode;
		});
	};
	
	session.currentUser.then(function(userId) {
	  /*if (session.user.subroles.indexOf("TRIALUSER") >= 0) {
		  $scope.locked = true;
	  } else $scope.locked = false;*/
	  $scope.init();
	});
}]);
	
