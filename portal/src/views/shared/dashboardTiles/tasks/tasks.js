angular.module('views')
.controller('TasksCtrl', ['$scope', '$state', 'server', '$attrs', 'views', 'tasking', 'status', function($scope, $state, server, $attrs, views, tasking, status) {
	
	$scope.tasks = [];	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);
	$scope.limit = 4;
	
	$scope.reload = function() {		
		if (!$scope.view.active) return;
		$scope.limit = $scope.view.position == "small" ? 4 : 20;
				
		$scope.status.doBusy(tasking.list())
		.then(function (result) {			
			$scope.tasks = result.data;
			if ($scope.tasks.length === 0) views.disableView($scope.view.id);
			console.log($scope.tasks);
			if (($scope.view.setup && $scope.view.setup.autoopen) || $scope.tasks.length < 3) {		
			  angular.forEach($scope.tasks, function(task) {
				$scope.showTaskDetail(task, "full");				
			  });
			}
		
			if (($scope.view.setup && $scope.view.setup.hidden) || $scope.tasks.length < 3) {
				views.disableView($scope.view.id);
			}
		});
						
	};
	
	$scope.showTaskDetail = function(task, position) {
		var teaser = {
				id : "task"+task._id,
				template : "/views/shared/dashboardTiles/taskdetails/taskdetails.html",
				title : task.title,
				position : position,
				active : true,
				setup : {
					task : task				        	
				}
		};	
		views.layout[position].push(views.def(teaser));
	};
	
	$scope.showDetails = function(task) {
		$scope.showTaskDetail(task, "modal");
		/*tasking.execute(task._id)
		.then(function(data) {
		   $state.go('^.spaces' , { spaceId : data.data._id });
		});*/
					
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);