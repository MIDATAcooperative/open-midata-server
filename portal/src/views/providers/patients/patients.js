angular.module('portal')
.controller('PatientsCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.criteria = {};
	$scope.member = null;
	$scope.error = null;
	$scope.loading = false;
	
	$scope.dosearch = function() {
		$scope.loading = true;
		
		$http.post(jsRoutes.controllers.providers.Providers.list().url, $scope.criteria).
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
		document.location.href = portalRoutes.controllers.ProviderFrontend.member(patient._id.$oid).url;
	};
	
    $scope.dosearch();	
	
}]);