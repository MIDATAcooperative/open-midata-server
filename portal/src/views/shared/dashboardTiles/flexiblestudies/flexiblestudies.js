angular.module('views')
.controller('FlexibleStudiesCtrl', ['$scope', '$state', 'server', '$attrs', 'views', 'studies', 'status', function($scope, $state, server, $attrs, views, studies, status) {
	
	$scope.studies = [];	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);
	$scope.limit = 4;
	
	$scope.reload = function() {		
		if (!$scope.view.active) return;
		$scope.limit = $scope.view.position == "small" ? 4 : 20;
		$scope.status.doBusy(studies.search($scope.view.setup.properties, $scope.view.setup.fields)).
		then(function (result) { $scope.studies = result.data; });
	};
	
	$scope.showDetails = function(study) {
		$state.go('^.studydetails', { studyId : study._id.$oid });		
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);