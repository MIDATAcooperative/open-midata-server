angular.module('portal')
.controller('StudyRulesCtrl', ['$scope', '$state', 'server', 'status', 'terms', 'apps', function($scope, $state, server, status, terms, apps) {
   
   $scope.studyid = $state.params.studyId;
   $scope.status = new status(false, $scope);
   $scope.error = null;
   $scope.requirements = apps.userfeatures;
   
   $scope.reload = function() {
	   	  
	   $scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url))
	    .then(function(data) { 				
			$scope.study = data.data;	
			if (!$scope.study.requirements) $scope.study.requirements = [];
			$scope.myform = $scope.study.recordQueryStr = JSON.stringify($scope.study.recordQuery);
		});
   };
   
   $scope.submit = function() {
	   $scope.error = null;
   	   
   	   try{
	     $scope.study.recordQuery = JSON.parse($scope.study.recordQueryStr);
   	   } catch (e) { console.log(e); $scope.error = e.message;return; }
	  
	   var data = { recordQuery : $scope.study.recordQuery, termsOfUse : $scope.study.termsOfUse, requirements: $scope.study.requirements };
	   $scope.status.doAction("update", server.put(jsRoutes.controllers.research.Studies.update($scope.studyid).url, JSON.stringify(data)))
	  .then(function(data) { 				
		    $scope.reload();
	   }); 
   };
   
   $scope.studyLocked = function() {
	 return (!$scope.study) || ($scope.study.validationStatus !== "DRAFT" && $scope.study.validationStatus !== "REJECTED");    
   };
   
   $scope.toggle = function(array,itm) {
		console.log(array);
		var pos = array.indexOf(itm);
		if (pos < 0) array.push(itm); else array.splice(pos, 1);
   };
         
   terms.search({}, ["name", "version", "language", "title"])
	.then(function(result) {
		$scope.terms = result.data;
	});
   
   $scope.reload();
}]);
