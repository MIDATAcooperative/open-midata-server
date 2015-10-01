angular.module('views')
.controller('TasksCtrl', ['$scope', '$state', 'server', '$attrs', 'views', 'tasking', 'status', function($scope, $state, server, $attrs, views, tasking, status) {
	
	$scope.tasks = [];	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);
	$scope.limit = 4;
	
	$scope.reload = function() {		
		if (!$scope.view.active) return;
		$scope.limit = $scope.view.position == "small" ? 4 : 20;
		$scope.status.doBusy(tasking.list()).
		then(function (result) {			
			$scope.tasks = result.data;
			if ($scope.tasks.length === 0) views.disableView($scope.view.id);
			
			angular.forEach($scope.tasks, function(task) {
				var teaser = {
						id : "task"+task._id.$oid,
						template : "/views/shared/dashboardTiles/taskdetails/taskdetails.html",
						title : task.title,
						position : "small",
						active : true,
						setup : {
							task : task				        	
						}
				};	
				views.layout.small.push(views.def(teaser));
			});
			
		});
	};
	
	$scope.showDetails = function(task) {
		tasking.execute(task._id.$oid)
		.then(function(data) {
		   $state.go('^.spaces' , { spaceId : data.data._id.$oid });
		});
					
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);