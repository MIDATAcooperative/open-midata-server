angular.module('portal')
.controller('RequiredInformationCtrl', ['$scope', '$state', 'server', function($scope, $state, server) {
   $scope.information = {};
   $scope.studyid = $state.params.studyId;	
   $scope.error = null;
   $scope.loading = true;
   
   $scope.reload = function() {
	   
	   server.get(jsRoutes.controllers.research.Studies.getRequiredInformationSetup($scope.studyid).url).
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
	   
	   server.post(jsRoutes.controllers.research.Studies.setRequiredInformationSetup($scope.studyid).url, params).
		success(function(data) { 				
		    $scope.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});  
   };
   
   $scope.reload();
}]);