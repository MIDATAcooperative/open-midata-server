angular.module('views')
.controller('ProviderSearchCtrl', ['$scope', '$state', '$attrs', '$sce', 'views', 'status', function($scope, $state, $attrs, $sce, views, status) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    
    $scope.criteria = {};
    
    $scope.reload = function() { };
    
    $scope.search = function() {
    	$state.go("^.providersearch", { name : $scope.criteria.name, city : $scope.criteria.city });
    };
    
	/* $scope.$watch('view.setup', function() { $scope.reload(); }); */
	
}]);