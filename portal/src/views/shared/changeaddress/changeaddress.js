angular.module('portal')
.controller('ChangeAddressCtrl', ['$scope', '$state', 'status', 'users', 'session', function($scope, $state, status, users, session) {
	// init
	$scope.error = null;
	$scope.status = new status(false, $scope);
		
	session.currentUser.then(function(myUserId) { 
		$scope.status.doBusy(users.getMembers({"_id": myUserId }, ["name", "email", "gender", "address1", "address2", "zip", "city", "country", "firstname", "lastname", "mobile", "phone"]))
		.then(function(results) {
			$scope.registration = results.data[0];
		});
		
	});
	
	
    $scope.changeAddress = function() {		
		        
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
												
		$scope.status.doAction("changeAddress", users.updateAddress($scope.registration)).
		then(function(data) { 
			$state.go("^.user", { userId : $scope.registration._id });
		});
	};
			
}]);