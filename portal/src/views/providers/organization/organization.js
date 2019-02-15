angular.module('portal')
.controller('POrganizationCtrl', ['$scope', '$state', 'server', 'status', 'session', 'users', function($scope, $state, server, status, session, users) {
		
	$scope.error = null;
	$scope.submitted = false;
	$scope.success = false;
	$scope.status = new status(true, $scope);
	$scope.sortby="lastname";   
		
	$scope.reload = function() {
		
		
		$scope.status.doBusy(server.get(jsRoutes.controllers.providers.Providers.getOrganization(session.org).url))
		.then(function(data) { 				
		  $scope.org = data.data;												
		});
		
		$scope.status.doBusy(users.getMembers({ role : "PROVIDER", provider : session.org }, users.MINIMAL ))
		.then(function(data) {
			$scope.persons = data.data;
		});
		
		
	};
	
	$scope.editorg = function() {
	
		$scope.submitted = true;
		$scope.success = false;
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
						
		
	   $scope.status.doAction("update", server.post(jsRoutes.controllers.providers.Providers.updateOrganization(session.org).url, JSON.stringify($scope.org)))
	   .then(function(result) { $scope.saveOk = true; });
		
	
	};
	
	$scope.formChange = function() {
		$scope.saveOk = false;
	};
	
	$scope.setSort = function(key) {
		console.log(key);
		if ($scope.sortby==key) $scope.sortby = "-"+key;
		else { $scope.sortby = key; }
	};
	
	session.currentUser.then(function() { $scope.reload(); });
	
	
}]);