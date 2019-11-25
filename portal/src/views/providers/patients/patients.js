angular.module('portal')
.controller('PatientsCtrl', ['$scope', '$state', 'fhir', 'status', 'server', function($scope, $state, fhir, status, server) {
	
	$scope.criteria = {};
	$scope.member = null;
	$scope.error = null;
	$scope.status = new status(true);
	
	$scope.dosearch = function() {
		
		/*$scope.status.doBusy(server.post(jsRoutes.controllers.providers.Providers.list().url, $scope.criteria)).
		then(function(result) { 			
			$scope.patients = result.data;
		});*/
								
		$scope.status.doBusy(fhir.search("Patient", {})).
		then(function(result) { 			
		   $scope.patients = result; 			 		    		  
		});
	};
	
	$scope.selectPatient = function(patient) {
		$state.go('^.memberdetails', { user : patient.id });		
	};
	
    $scope.dosearch();	
	
}]);