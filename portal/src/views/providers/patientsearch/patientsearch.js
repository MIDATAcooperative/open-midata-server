angular.module('portal')
.controller('MemberSearchCtrl', ['$scope', '$state', 'status', 'provideraccess', 'circles', 'usergroups', function($scope, $state, status, provideraccess, circles, usergroups) {
	
	$scope.criteria = {};
	$scope.newconsent = {};
	$scope.member = null;
	$scope.error = null;
	$scope.status = new status(false, $scope);
	$scope.loading = false;
	$scope.status.isBusy = false;
	$scope.searched = false;
	
	$scope.dosearch = function() {
		
		$scope.member = null;
		$scope.consents = null;
		
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;		
		if (! $scope.myform.$valid) return;
		
		$scope.loading = true;
		
		$scope.init();
		
		
		$scope.status.doBusy(provideraccess.search($scope.criteria))
		.then(function(data) { 				
			    $scope.member = data.data.member;
			    $scope.consents = data.data.consents;
			    $scope.error = null;
			    $scope.loading = false;
			    $scope.submitted = false;	
			    $scope.searched = true;
			    
			    //$state.go('^.memberdetails', { memberId : $scope.member._id });		    
		});
		
		
	};
	
	$scope.selectPatient = function() {
		$state.go('^.memberdetails', { user : $scope.member._id });		
	};
	
	$scope.addConsent = function() {	
		if ($scope.criteria.passcode) {
		  $scope.usePasscode();
		} else {		
		  if ($scope.member) {
		    $state.go("^.newconsent", { "authorize" : $scope.newconsent.usergroup, "owner" : $scope.member._id, "request" : true });
		  } else {
			$state.go("^.newconsent", { "authorize" : $scope.newconsent.usergroup, "extowner" : $scope.criteria.email, "request" : true });
		  }
		}
	};
	
	$scope.usePasscode = function() {
		$scope.status.doAction("usepasscode", circles.joinByPasscode($scope.member._id, $scope.criteria.passcode, $scope.newconsent.usergroup ))
	    .then(function(data) {
	    	 $state.go("^.editconsent", { consentId : data.data._id });
	    });
	};
	
	$scope.init = function() {
		$scope.status.doBusy(usergroups.search({ "member" : true, "active" : true }, usergroups.ALLPUBLIC ))
    	.then(function(results) {
		  $scope.usergroups = results.data;
    	});
	};
	
	
	
}]);