angular.module('portal')
.controller('StudyRulesCtrl', ['$scope', '$state', 'server', 'status', 'terms', 'apps', 'labels', '$translate', 'formats', '$document', function($scope, $state, server, status, terms, apps, labels, $translate, formats, $document) {
   
   $scope.studyid = $state.params.studyId;
   $scope.status = new status(true, $scope);
   $scope.error = null;
   $scope.requirements = apps.userfeatures;
   $scope.datePickers = {  };
   $scope.dateOptions = {
	  	 formatYear: 'yy',
	  	 startingDay: 1,
	  	  
   };
   $scope.query = {};
   $scope.codesystems = formats.codesystems;
   
   $scope.reload = function() {
	   	  
	   $scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url))
	    .then(function(data) { 				
			$scope.study = data.data;	
			if (!$scope.study.requirements) $scope.study.requirements = [];
			$scope.study.recordQueryStr = JSON.stringify($scope.study.recordQuery);
			//if ($scope.study.recordQueryStr === "{}") $scope.study.recordQueryStr = "{ \"content\" : [] }";
			//$scope.updateQuery();
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
   	   	   
	   var data = { recordQuery : $scope.study.recordQuery, termsOfUse : $scope.study.termsOfUse, requirements: $scope.study.requirements, startDate : $scope.study.startDate, endDate : $scope.study.endDate, dataCreatedBefore : $scope.study.dataCreatedBefore };
	   $scope.status.doAction("update", server.put(jsRoutes.controllers.research.Studies.update($scope.studyid).url, JSON.stringify(data)))
	  .then(function(data) { 				
		    $scope.reload();
		    $scope.saveOk = true;
	   }); 
   };
   
   $scope.studyLocked = function() {
	 return (!$scope.study) || ($scope.study.validationStatus !== "DRAFT" && $scope.study.validationStatus !== "REJECTED") || !$scope.study.myRole.setup;    
   };
   
   $scope.formChange = function() {
	 $scope.saveOk = false;  
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
