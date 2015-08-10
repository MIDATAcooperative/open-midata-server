angular.module('views')
.controller('ListStudiesCtrl', ['$scope', 'server', '$attrs', 'views', 'status', function($scope, server, $attrs, views, status) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);
	$scope.results =[];
	
	$scope.reload = function() {
		if (!$scope.view.active) return;
			
		$scope.status.doBusy(server.get(jsRoutes.controllers.members.Studies.list().url)).
		then(function(results) { 				
		   $scope.results = results.data;			
		});
	};
	
	$scope.reload();
	
}]);
