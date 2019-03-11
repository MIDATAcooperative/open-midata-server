angular.module('portal')
.controller('PatientsCtrl', ['$scope', '$state', 'server', 'status', function($scope, $state, server, status) {
	
	$scope.criteria = {};
	$scope.member = null;
	$scope.error = null;
	$scope.status = new status(true);
	
	$scope.dosearch = function() {
		
		
		$scope.status.doBusy(server.post(jsRoutes.controllers.providers.Providers.list().url, $scope.criteria)).
		then(function(result) { 				
		    $scope.patients = result.data;			 		    		  
		});
	};
	
	$scope.selectPatient = function(patient) {
		$state.go('^.memberdetails', { user : patient._id });		
	};
	
    $scope.dosearch();	
	
}]);