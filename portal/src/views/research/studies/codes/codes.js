angular.module('portal')
.controller('CodesCtrl', ['$scope', '$state', 'server', function($scope, $state, server) {
	
	$scope.studyid = $state.params.studyId;
	$scope.codes = null;
	$scope.newcodes = { count:1, reuseable:true, group:"" };
	$scope.loading = true;
	$scope.createnew = false;
	$scope.blocked = false;
	$scope.submitted = false;
	$scope.error = null;
		
	$scope.reload = function() {
			
		server.get(jsRoutes.controllers.research.Studies.listCodes($scope.studyid).url).
			then(function(data) { 				
				$scope.codes = data.data;
				$scope.loading = false;
				$scope.createnew = false;
				$scope.error = null;
			}, function(err) {
				$scope.error = err;
				$scope.blocked = true;
				$scope.createnew = false;
			});
	};
	
	$scope.generate = function() {
		$scope.submitted = true;	
		if ($scope.newcodes.error && $scope.newcodes.error.field && $scope.newcodes.error.type) $scope.myform[$scope.newcodes.error.field].$setValidity($scope.newcodes.error.type, true);
		$scope.newcodes.error = null;
		if (! $scope.myform.$valid) return;
		
		$scope.newcodes.error = null;
		
		// send the request
		var data = $scope.newcodes;		
		
		server.post(jsRoutes.controllers.research.Studies.generateCodes($scope.studyid).url, JSON.stringify(data)).
			then(function(url) { $scope.reload(); }, function(err) {
				$scope.newcodes.error = err;
				if (err.field && err.type) $scope.myform[err.field].$setValidity(err.type, false);			
			});
	};
	
	$scope.reload();
	
}]);