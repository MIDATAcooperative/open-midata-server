angular.module('views')
.controller('AddTaskCtrl', ['$scope', '$state', '$translate', 'server', '$attrs', 'views', 'tasking', 'status', 'session', function($scope, $state, $translate, server, $attrs, views, tasking, status, session) {
			

	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);
	$scope.task = { frequency:"ONCE"};
	
	$scope.tasks = [{
		id : "survey",
		name : "addtask.survey_name",
		
		plugin : "55af6055e4b044f0420c9bf2",
		context : "me",
		pluginQuery : {
			"format" : [
				"survey/questions"
			],
			"owner" : ""						
		},
		confirmQuery : {
			"format" : ["survey/answers"]
		}
	}];
	
		
	$scope.frequencies = tasking.frequencies;
	
	$scope.reload = function() {
		
	};
	
	$scope.prepare = function(tasktemplate) {
		$translate("addtask."+tasktemplate.id+"_title").then(function(t) { $scope.task.title = t; });
		$translate("addtask."+tasktemplate.id+"_description").then(function(t) { $scope.task.description = t; });						
		$scope.task.plugin = tasktemplate.plugin;
		$scope.task.context = tasktemplate.context;
		$scope.task.pluginQuery = tasktemplate.pluginQuery;
		$scope.task.pluginQuery.owner = session.user._id;
		$scope.task.confirmQuery = tasktemplate.confirmQuery;		
	};
	
	$scope.submit = function() {
		$scope.task.createdBy = session.user._id;
		
		if ($scope.view.setup.studyId) {
            $scope.task.study = $scope.view.setup.studyId;
            $scope.task.group = $scope.view.setup.group;
            console.log($scope.task);
            
            $scope.status.doBusy(server.post(jsRoutes.controllers.research.Studies.addTask($scope.task.study, $scope.task.group).url,  JSON.stringify($scope.task)))
			.then(function() {
				views.disableView($scope.view.id);
			});
		} else {
			$scope.task.owner = $scope.view.setup.owner;
			$scope.task.shareBackTo = $scope.view.setup.shareBackTo;
	        console.log($scope.task);
			
			$scope.status.doBusy(tasking.add($scope.task))
			.then(function() {
				views.disableView($scope.view.id);
			});
		}
		
		
		
	};
		
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);