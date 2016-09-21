angular.module('portal')
.controller('MemberSearchCtrl', ['$scope', '$state', 'status', 'provideraccess', 'circles', function($scope, $state, status, provideraccess, circles) {
	
	$scope.criteria = {};
	$scope.newconsent = {};
	$scope.member = null;
	$scope.error = null;
	$scope.status = new status(true);
	$scope.loading = false;
	$scope.status.isBusy = false;
	
	$scope.dosearch = function() {
		$scope.loading = true;
		
		$scope.status.doBusy(provideraccess.search($scope.criteria))
		.then(function(data) { 				
		    $scope.member = data.data.member;
		    $scope.consents = data.data.consents;
		    $scope.error = null;
		    $scope.loading = false;
		    
		    //$state.go('^.memberdetails', { memberId : $scope.member._id });		    
		});
	};
	
	$scope.selectPatient = function() {
		$state.go('^.memberdetails', { memberId : $scope.member._id });		
	};
	
	$scope.addConsent = function() {
		
      $scope.newconsent.type = "HEALTHCARE";
      $scope.newconsent.owner = $scope.member._id;
      $scope.status.doAction("createconsent", circles.createNew($scope.newconsent))
      .then(function(data) {
    	 $scope.dosearch(); 
      });
	};
	
	$scope.usePasscode = function() {
		$scope.status.doAction("usepasscode", circles.joinByPasscode($scope.member._id, $scope.criteria.passcode))
	    .then(function(data) {
	    	 $scope.dosearch(); 
	    });
	};
	
}]);