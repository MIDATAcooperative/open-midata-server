angular.module('portal')
.controller('StudyRulesCtrl', ['$scope', '$state', 'server', 'status', 'terms', 'apps', 'labels', '$translate', 'formats', '$document', 'studies', function($scope, $state, server, status, terms, apps, labels, $translate, formats, $document, studies) {
   
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
   $scope.joinmethods = studies.joinmethods;
   
   $scope.reload = function() {
	   	  
	   $scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url))
	    .then(function(data) { 				
			$scope.study = data.data;	
			if (!$scope.study.requirements) $scope.study.requirements = [];
			if (!$scope.study.joinMethods) $scope.study.joinMethods = [];
			if ($scope.study.consentObserverNames && $scope.study.consentObserverNames.length) {
				$scope.study.consentObserverStr = $scope.study.consentObserverNames.join(",");
			}
			$scope.study.recordQueryStr = JSON.stringify($scope.study.recordQuery);
			//if ($scope.study.recordQueryStr === "{}") $scope.study.recordQueryStr = "{ \"content\" : [] }";
			//$scope.updateQuery();
		});
	   
	   $scope.status.doBusy(apps.getApps({ type : "external", consentObserving : true }, ["_id", "filename", "name", "orgName", "publisher", "type", "targetUserRole"]))
		.then(function(data) {
			$scope.observers = data.data;
			console.log($scope.observers);
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
		
		var observersStr = $scope.study.consentObserverStr;
		var observers = [];
		if (observersStr) {
			var plugins = observersStr.split(",");			
			$scope.study.consentObserverNames = plugins;
		} else $scope.study.consentObserverNames = [];
		
		if (! $scope.myform.$valid) {
			var elem = $document[0].querySelector('input.ng-invalid');
			if (elem && elem.focus) elem.focus();
			return;
		}
   	   	   
	   var data = { joinMethods : $scope.study.joinMethods, termsOfUse : $scope.study.termsOfUse, requirements: $scope.study.requirements, startDate : $scope.study.startDate, endDate : $scope.study.endDate, dataCreatedBefore : $scope.study.dataCreatedBefore, consentObserverNames : $scope.study.consentObserverNames };
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
