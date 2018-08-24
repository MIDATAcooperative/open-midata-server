angular.module('portal')
.controller('StudyInfoCtrl', ['$scope', '$state', 'server', 'status', 'languages', function($scope, $state, server, status, languages) {
  
   $scope.studyid = $state.params.studyId;
   $scope.status = new status(true);
   $scope.languages = languages.all;
   $scope.sections = ["SUMMARY", "DESCRIPTION", "HOMEPAGE", "CONTACT", "INSTRUCTIONS", "PURPOSE", "AUDIENCE", "LOCATION", "PHASE", "SPONSOR", "SITE", "DEVICES", "COMMENT"];
   

   $scope.reload = function() {
	   	
	   $scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url))
	    .then(function(data) { 				
			$scope.study = data.data;
			$scope.study.recordQuery = undefined;
			
			$scope.infos = $scope.study.infos;
		});
   };
   
       
  $scope.submit = function() {
	 
   	   
   	   /*try{
	     $scope.study.recordQuery = JSON.parse($scope.study.recordQueryStr);
   	   } catch (e) { console.log(e); $scope.error = e.message;return; }
   	   */
	   	$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		
		if (! $scope.myform.$valid) {
			var elem = $document[0].querySelector('input.ng-invalid');
			if (elem && elem.focus) elem.focus();
			return;
		}
   	   	   
	   var data = { infos : $scope.study.infos };
	   $scope.status.doAction("update", server.put(jsRoutes.controllers.research.Studies.update($scope.studyid).url, JSON.stringify(data)))
	  .then(function(data) { 				
		    $scope.reload();
		    $scope.saveOk = true;
	   }); 
   };
   
   $scope.studyLocked = function() {
		 return (!$scope.study) || ($scope.study.validationStatus !== "DRAFT" && $scope.study.validationStatus !== "REJECTED") || !$scope.study.myRole.setup;
   };
   
   $scope.reload();
}]);
