angular.module('portal')
.controller('ServiceCtrl', ['$scope', 'server', '$state', 'status', 'session', 'ENV', '$document', 'circles', function($scope, server, $state, status, session, ENV, $document, circles) {
	
	// init		
	$scope.login = {};	
	$scope.error = null;
	$scope.status = new status(false);
	
	$scope.offline = (window.jsRoutes === undefined) || (window.jsRoutes.controllers === undefined);	
	$scope.notPublic = ENV.instanceType == "prod";
	
	$scope.init = function() {
		console.log("iii");
		if ($state.params.action) {
			console.log("iii2");
			$scope.action();
		} else
		if ($state.params.login) {
			$scope.login.email = $state.params.login;
			/*var elem = $document[0].getElementById('pw');
			console.log(elem);
			if (elem && elem.focus) elem.focus();*/
		}
	};
	
	// login
	$scope.login = function() {
		// check user input
		if (!$scope.login.email || !$scope.login.password) {
			$scope.error = { code : "error.missing.credentials" };
			return;
		}
		
		// send the request
		var data = {"email": $scope.login.email, "password": $scope.login.password};
		$scope.status.doAction("login", server.post(jsRoutes.controllers.Application.authenticate().url, JSON.stringify(data))).
		then(function(result) {
			$state.params.action="service";
			session.postLogin(result, $state);
																	
			
		}).
		catch(function(err) { $scope.error = err.data; });
	};
	
	$scope.action = function() {
		 var props = { "status" : "UNCONFIRMED" };
		 if ($state.params.consent) props._id = $state.params.consent;
		 var found = false;
		 console.log("actions");
		 circles.listConsents(props  , ["_id"]).then(function(result) {
			 if (result.data.length > 0) {
				 found = true;
				 $state.go("member.service_consent", { consentId : result.data[0]._id, callback : $state.params.callback, action : "service"  });
			 }
			 if (!found) {
				 console.log("leave");
			     $state.go("^.serviceleave", { callback : $state.params.callback });
			 }
		 });
		 
	};
	
	$scope.init();
}]);
