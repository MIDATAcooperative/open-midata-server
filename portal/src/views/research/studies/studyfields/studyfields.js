angular.module('portal')
.controller('RequiredInformationCtrl', ['$scope', '$state', 'server', 'status', function($scope, $state, server, status) {
   $scope.information = {};
   $scope.studyid = $state.params.studyId;
   $scope.status = new status(true);
   
   
   $scope.reload = function() {
	   
	   $scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.getRequiredInformationSetup($scope.studyid).url))
		.then(function(data) { 								
			$scope.information = data.data;						
		});
	   
	   $scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url))
	    .then(function(data) { 				
			$scope.study = data.data;
			$scope.study.recordQuery = undefined;
		});
   };
   
   $scope.setRequiredInformation = function() {
	   var params = $scope.information;
	   
	   server.post(jsRoutes.controllers.research.Studies.setRequiredInformationSetup($scope.studyid).url, params).
		success(function(data) { 				
		    $scope.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});  
   };
   
   $scope.addGroup = function() {
	   if (!$scope.study.groups) { $scope.study.groups = []; }
	   
	   $scope.study.groups.push({ name:"", description:"" });
   };
   
   $scope.deleteGroup = function(group) {
	   $scope.study.groups.splice($scope.study.groups.indexOf(group), 1);
	   $scope.groupschanged = true;
   };
   
   $scope.saveGroups = function() {
	 $scope.groupschanged = false;
	 $scope.status.doAction("groups", server.put(jsRoutes.controllers.research.Studies.update($scope.studyid).url, JSON.stringify($scope.study)));	 
   };
   
   $scope.studyLocked = function() {
		 return (!$scope.study) || ($scope.study.validationStatus !== "DRAFT" && $scope.study.validationStatus !== "REJECTED") || !$scope.study.myRole.setup;
   };
   
   $scope.reload();
}]);
