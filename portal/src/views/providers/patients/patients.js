angular.module('portal')
.controller('PatientsCtrl', ['$scope', '$state', 'server', function($scope, $state, server) {
	
	$scope.criteria = {};
	$scope.member = null;
	$scope.error = null;
	$scope.loading = false;
	
	$scope.dosearch = function() {
		$scope.loading = true;
		
		server.post(jsRoutes.controllers.providers.Providers.list().url, $scope.criteria).
		then(function(result) { 				
		    $scope.patients = result.data;
		    $scope.error = null;
		    $scope.loading = false;
		    		    
		},function(err) {
			$scope.error = err;	
			$scope.results = null;
			$scope.loading = false;
		});
	};
	
	$scope.selectPatient = function(patient) {
		$state.go('^.memberdetails', { memberId : patient._id.$oid });		
	};
	
    $scope.dosearch();	
	
}]);