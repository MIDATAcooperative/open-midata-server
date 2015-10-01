angular.module('views')
.controller('TaskDetailsCtrl', ['$scope', '$state', 'server', '$attrs', 'views', 'tasking', 'status', 'session', 'users', 'apps', function($scope, $state, server, $attrs, views, tasking, status, session, users, apps) {
	
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);
	$scope.status.isBusy = false;
	
	
	$scope.reload = function() {		
		if (!$scope.view.active) return;
		$scope.task = $scope.view.setup.task;	
		$scope.creator = session.resolve($scope.task.createdBy.$oid, function() { return users.getMembers({ "_id" : $scope.task.createdBy },users.ALLPUBLIC ); });
		//apps.getApps({"_id": $scope.task.plugin}, ["name"])
		//.then(function(res) { console.log(res); $scope.plugin = res.data[0]; });
		
		$scope.plugin = session.resolve($scope.task.plugin.$oid, function() { return apps.getApps({"_id": $scope.task.plugin}, ["name"]); });
		
	};
	
	$scope.showDetails = function(task) {
		tasking.execute(task._id.$oid)
		.then(function(data) {
		   $state.go('^.spaces' , { spaceId : data.data._id.$oid });
		});
					
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);