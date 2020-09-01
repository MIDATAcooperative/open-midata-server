angular.module('portal')
.controller('DefineStudyCtrl', ['$scope', '$state', 'status', 'server', function($scope, $state, status, server) {

	$scope.status = new status(false, $scope);
	$scope.error = null;

	$scope.studydef = { value : "", researcher : "" };
	
	$scope.submit = function() {
		
	   $scope.submitted = true;	
	   if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
	   $scope.error = null;
	   $scope.status.doAction("import", server.post(jsRoutes.controllers.research.Studies.importStudy().url, JSON.stringify({ "base64" : $scope.studydef.value, "researcher" : $scope.studydef.researcher })))
	   .then(function(result) {
		  $state.go("^.study.overview", { studyId : result.data._id }); 
	   });	
	};
	
}]);