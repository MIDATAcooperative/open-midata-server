angular.module('portal')
.controller('StudyRulesCtrl', ['$scope', '$state', 'server', 'status', function($scope, $state, server, status) {
   
   $scope.studyid = $state.params.studyId;
   $scope.status = new status(true);
   
   
   $scope.reload = function() {
	   	  
	   $scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url))
	    .then(function(data) { 				
			$scope.study = data.data;	
			$scope.myform = $scope.study.recordQueryStr = JSON.stringify($scope.study.recordQuery);
		});
   };
   
   $scope.submit = function() {
   	   console.log("A");
	   $scope.study.recordQuery = JSON.parse($scope.study.recordQueryStr);
	   console.log($scope.study);
	   var data = { recordQuery : $scope.study.recordQuery };
	   $scope.status.doAction("update", server.put(jsRoutes.controllers.research.Studies.update($scope.studyid).url, JSON.stringify(data)))
	  .then(function(data) { 				
		    $scope.reload();
	   }); 
   };
         
   $scope.reload();
}]);
