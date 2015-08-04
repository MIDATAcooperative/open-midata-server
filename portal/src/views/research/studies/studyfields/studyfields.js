angular.module('portal')
.controller('RequiredInformationCtrl', ['$scope', '$http', function($scope, $http) {
   $scope.information = {};
   $scope.studyid = window.location.pathname.split("/")[3];	
   $scope.error = null;
   $scope.loading = true;
   
   $scope.reload = function() {
	   
	   $http.get(jsRoutes.controllers.research.Studies.getRequiredInformationSetup($scope.studyid).url).
		success(function(data) { 								
			$scope.information = data;			
			$scope.loading = false;
			$scope.error = null;
		}).
		error(function(err) {
			$scope.error = err;				
		});
   };
   
   $scope.setRequiredInformation = function() {
	   var params = $scope.information;
	   
	   $http.post(jsRoutes.controllers.research.Studies.setRequiredInformationSetup($scope.studyid).url, params).
		success(function(data) { 				
		    $scope.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});  
   };
   
   $scope.reload();
}]);
