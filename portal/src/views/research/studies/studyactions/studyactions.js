angular.module('portal')
.controller('StudyActionsCtrl', ['$scope', '$state', 'server', 'views', 'status', function($scope, $state, server, views, status) {
	
	$scope.studyId = $state.params.studyId;
	$scope.crit = { group : "" };
	$scope.status = new status(true);
	views.reset();
	
	$scope.reload = function() {
	
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyId).url))
	    .then(function(data) { 				
			$scope.study = data.data;	
		});
		
	};
	
	$scope.setGroup = function() {
		$scope.group = $scope.crit.group;
		server.post(jsRoutes.controllers.research.Studies.shareWithGroup($scope.studyId, $scope.group).url)
		.then(function(result) {
			
		   $scope.aps = result.data._id;
		   views.setView("group_records", { aps : $scope.aps, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowAdd : true, type : "studyrelated" });		   
		});
		
		
	};
	
	
	$scope.addTask = function() {	 
	  views.setView("addtask", { "studyId" : $scope.studyId, "group" : $scope.group });	 
	};		
	
	$scope.reload();
	
}]);