angular.module('portal')
.controller('ConsentsCtrl', ['$scope', '$state', 'circles', 'session', 'views', 'status', function($scope, $state, circles, session, views, status) {

	$scope.status = new status(true);
	$scope.role = $state.current.data.role;
	$scope.sortby="-dateOfCreation";  
		
	loadConsents = function(userId) {	
		var prop = {};
		if ($state.current.types) prop = { type : $state.current.types };
		$scope.status.doBusy(circles.listConsents(prop, [ "name", "authorized", "type", "status", "records", "dateOfCreation" ]))
		.then(function(data) {
			$scope.consents = data.data;						
		});
	};
	
	$scope.addConsent = function() {
		$state.go("^.newconsent");
	};
	
	$scope.editConsent = function(consent) {
		$state.go("^.editconsent", { consentId : consent._id });
	};
	
	$scope.changeView = function() {
		$state.go("^.revconsents");
	};
	
	$scope.setSort = function(key) {		
		if ($scope.sortby==key) $scope.sortby = "-"+key;
		else { $scope.sortby = key; }
	};
	
	session.currentUser.then(function(userId) { loadConsents(userId); });
	

}]);