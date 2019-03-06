angular.module('portal')
.controller('ReverseConsentsCtrl', ['$scope', '$state', 'circles', 'session', 'views', 'status', function($scope, $state, circles, session, views, status) {

	$scope.status = new status(true);
	$scope.sortby="-dateOfCreation";  
		
	loadConsents = function(userId) {	
		$scope.status.doBusy(circles.listConsents({ member : true }, [ "name", "authorized", "type", "status", "records", "owner", "ownerName", "externalOwner" ]))
		.then(function(data) {
			$scope.consents = data.data;						
		});
	};
			
	$scope.addConsent = function() {
		$state.go("^.newconsent", { request : true });
	};
	
	$scope.editConsent = function(consent) {
		$state.go("^.showconsent", { consentId : consent._id });
	};
	
	$scope.changeView = function() {
		$state.go("^.circles");
	};
	
	$scope.setSort = function(key) {		
		if ($scope.sortby==key) $scope.sortby = "-"+key;
		else { $scope.sortby = key; }
	};
	
	session.currentUser.then(function(userId) { loadConsents(userId); });
	

}]);