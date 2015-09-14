angular.module('portal')
.controller('NewConsentCtrl', ['$scope', '$state', 'server', 'status', 'circles', 'hc', function($scope, $state, server, status, circles, hc) {
	
	$scope.types = [
	                { value : "CIRCLE", label : "Other MIDATA Members"},
	                { value : "HEALTHCARE", label : "Healthcare Providers" },
	                { value : "STUDYPARTICIPATION", label : "Research" }
	               ];
			
	
	$scope.stati = [
	                { value : "ACTIVE", label : "Active" },
	                { value : "UNCONFIRMED", label : "Unconfirmed, needs confirmation" },
	                { value : "EXPIRED", label : "Expired" }
	                 ];
	
	$scope.consent = $scope.myform = { type : "CIRCLE", status : "ACTIVE", authorized : [] };
	$scope.status = new status(true);
	$scope.authpersons = [];
	
	$scope.init = function() {
		if ($state.params.authorize != null) {
			$scope.consent.type = "HEALTHCARE";			
			$scope.consent.authorized = [ { "$oid" : $state.params.authorize } ];
			
			hc.search({ "_id" : { "$oid" : $state.params.authorize } }, [ "firstname", "lastname", "city", "address1", "address2", "country"])
			.then(function(data) {
				$scope.authpersons = data.data;
			});
		}
	};
	
	$scope.create = function() {
		console.log("SUBMIT!");
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
		
		$scope.status.doAction("create", circles.createNew($scope.consent)).
		then(function(data) {
			if ($scope.authpersons.length > 0) {
				circles.addUsers(data.data._id.$oid, $scope.consent.authorized)
				.then(function() {
					$state.go("^.circles");
				});
			} else {
				$state.go("^.circles");
			}
			 
		 },
			 function(err) { 
				$scope.error = err;
				if (err.field && err.type) $scope.myform[err.field].$setValidity(err.type, false);				
		});
				
	};
	
	$scope.init();
}]);
	
