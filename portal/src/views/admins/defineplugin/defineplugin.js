angular.module('portal')
.controller('DefinePluginCtrl', ['$scope', '$state', 'status', 'server', function($scope, $state, status, server) {

	$scope.status = new status(false, $scope);
	$scope.error = null;

	$scope.appdef = { value : "" };
	
	$scope.submit = function() {
		
	   $scope.submitted = true;	
	   if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
	   $scope.error = null;
	   $scope.status.doAction("import", server.post(jsRoutes.controllers.Market.importPlugin().url, JSON.stringify({ "base64" : $scope.appdef.value })))
	   .then(function(result) {
		  $state.go("^.manageapp", { appId : result.data._id }); 
	   });	
	};
	
}]);