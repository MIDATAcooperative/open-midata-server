angular.module('portal')
.controller('CodesCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.studyid = window.location.pathname.split("/")[3];
	$scope.codes = null;
	$scope.newcodes = { count:1, reuseable:true, group:"" };
	$scope.loading = true;
	$scope.createnew = false;
	$scope.blocked = false;
	$scope.submitted = false;
	$scope.error = null;
		
	$scope.reload = function() {
			
		$http.get(jsRoutes.controllers.research.Studies.listCodes($scope.studyid).url).
			success(function(data) { 				
				$scope.codes = data;
				$scope.loading = false;
				$scope.createnew = false;
				$scope.error = null;
			}).
			error(function(err) {
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
		
		$http.post(jsRoutes.controllers.research.Studies.generateCodes($scope.studyid).url, JSON.stringify(data)).
			success(function(url) { $scope.reload(); }).
			error(function(err) {
				$scope.newcodes.error = err;
				if (err.field && err.type) $scope.myform[err.field].$setValidity(err.type, false);			
			});
	};
	
	$scope.reload();
	
}]);