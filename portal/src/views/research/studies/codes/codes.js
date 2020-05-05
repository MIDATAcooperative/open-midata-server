angular.module('portal')
.controller('CodesCtrl', ['$scope', '$state', 'server', 'status', function($scope, $state, server, status) {
	
	$scope.studyid = $state.params.studyId;
	$scope.codes = null;
	$scope.newcodes = { count:1, reuseable:true, group:"" };
	$scope.status = new status(false, $scope);   
	$scope.createnew = false;
	$scope.blocked = false;
	$scope.submitted = false;
	$scope.error = null;
		
	$scope.reload = function() {
			
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url))
		.then(function(data) { 				
			$scope.study = data.data;			
		});
		
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.listCodes($scope.studyid).url)).
			then(function(data) { 				
				$scope.codes = data.data;
				$scope.loading = false;
				$scope.createnew = false;
				$scope.error = null;
			}, function(err) {
				$scope.error = err.data.type;
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
				$scope.newcodes.error = err.data;
				if (err.field && err.type) $scope.myform[err.field].$setValidity(err.type, false);			
			});
	};
	
	$scope.reload();
	
}]);