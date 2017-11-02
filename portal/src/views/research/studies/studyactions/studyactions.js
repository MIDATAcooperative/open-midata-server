angular.module('portal')
.controller('StudyActionsCtrl', ['$scope', '$state', 'server', 'views', 'apps', 'status', 'circles', function($scope, $state, server, views, apps, status, circles) {
	
	$scope.studyId = $state.params.studyId;
	$scope.crit = { group : "" };
	$scope.status = new status(true);
	views.reset();
	
	$scope.error = null;
	$scope.submitted = false;
	
	$scope.reload = function() {
	
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyId).url))
	    .then(function(data) { 				
			$scope.study = data.data;	
		});
		
		apps.getApps({ "targetUserRole" : "RESEARCH" }, ["filename", "name"])
		.then(function(data) {
			$scope.plugins = data.data;
		});
		
	};
	
	$scope.setGroup = function() {
		$scope.group = $scope.crit.group;
		$scope.status.doBusy(server.post(jsRoutes.controllers.research.Studies.shareWithGroup($scope.studyId, $scope.group).url))
		.then(function(result) {
			
		   $scope.aps = result.data[0]._id;
		   views.setView("group_records", { aps : $scope.aps, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowAdd : true, type : "studyrelated" });		   
		});
		
		$scope.updateConsents();
		
	};
	
	$scope.changedGroup = function() {
		$scope.consents = null;
		$scope.crit.device = "";
		$scope.group = $scope.crit.group;
		$scope.updateConsents();
	};
	
	
	$scope.addTask = function() {	 
	   views.setView("addtask", { "studyId" : $scope.studyId, "group" : $scope.group });
	};		
	
	$scope.updateConsents = function() {
		$scope.status.doBusy(circles.listConsents({ "sharingQuery.link-study" : $scope.studyId, "sharingQuery.link-study-group" : $scope.crit.group }, [ "name", "authorized", "type", "status", "records" ]))
		.then(function(data) {
			$scope.consents = data.data;						
		});
	};
	
	$scope.editConsent = function(consent) {
		$state.go("^.^.editconsent", { consentId : consent._id });
	};
	
	$scope.addApplication = function(myform) {	
	    console.log("ADDNOW");
	    $scope.myform = myform;
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type && myform[$scope.error.field]) myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! myform.$valid) return;
		
		
	  $scope.status.doAction("addapplication", server.post(jsRoutes.controllers.research.Studies.addApplication($scope.studyId, $scope.group).url, $scope.crit))
		.then(function(result) {
			$scope.submitted = false;
			$scope.updateConsents();
		});
	};
	
	$scope.deleteConsent = function(consent) {
		$scope.status.doAction("deleteConsent", server.delete(jsRoutes.controllers.Circles["delete"](consent._id).url)).
		then(function() {
			$scope.updateConsents();
		});
	};
	
	$scope.reload();
	
}]);