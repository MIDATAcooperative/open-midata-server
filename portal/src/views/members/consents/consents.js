angular.module('portal')
.controller('ConsentsCtrl', ['$scope', '$state', 'circles', 'session', 'views', 'status', function($scope, $state, circles, session, views, status) {

	$scope.status = new status(true);
	
	// get circles and make either given or first circle active
	loadConsents = function(userId) {
		
		$scope.status.doBusy(circles.listConsents({ }, [ "name", "authorized", "type", "status" ]))
		.then(function(data) {
			$scope.consents = data.data;						
		});
	};
	
	$scope.addConsent = function() {
		$state.go("^.newconsent");
	};
	
	$scope.editConsent = function(consent) {
		$state.go("^.editconsent", { consentId : consent._id.$oid });
	};
	
	session.currentUser.then(function(userId) { loadConsents(userId); });
	

}]);