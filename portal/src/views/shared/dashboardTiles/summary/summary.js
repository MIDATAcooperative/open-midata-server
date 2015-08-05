angular.module('views')
.controller('SummaryCtrl', ['$scope', 'server', '$attrs', '$sce', 'views', 'status', function($scope, server, $attrs, $sce, views, status) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    
    $scope.reload = function() { };
    
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);