angular.module('portal')
.controller('AppLinkCtrl', ['$scope', '$state', 'server', 'apps', 'status', 'circles', 'spaces', 'studies', '$q', '$translatePartialLoader', function($scope, $state, server, apps, status, circles, spaces, studies, $q, $translatePartialLoader) {
	
	$scope.appId = $state.params.appId;
	$scope.crit = { group : "" };
	$scope.status = new status(true, $scope);
	
	$scope.types = studies.linktypes;
	$scope.periods = studies.executionStati;
	$scope.selection = undefined;
	
	
	$scope.error = null;
	$scope.submitted = false;
	
	$translatePartialLoader.addPart("researchers");
	
	$scope.reload = function() {
	
		$scope.selection = null;
		
		$scope.status.doBusy(apps.getApps({ "_id" : $state.params.appId }, ["creator", "filename", "name", "description", "icons", "type", "targetUserRole" ]))
		.then(function(data) { 
			$scope.app = data.data[0];			
		});	
		
		$scope.status.doBusy(server.get(jsRoutes.controllers.Market.getStudyAppLinks("app", $scope.appId).url))
	    .then(function(data) { 				
			$scope.links = data.data;												
		});	
		
		$scope.status.doBusy(studies.search({ validationStatus : "VALIDATED" }, ["_id", "code", "name" ]))
		.then(function(data) {
			$scope.studies = data.data;
		});
	};
	
	$scope.addNew = function() {
		$scope.selection = { app : {}, study:{}, type:[], usePeriod:[] };
	};
	
	$scope.formChange = function() {
		$scope.saveOk = false;
	};
	
	$scope.select = function(link) {
		$scope.selection = link;
	};
	
	$scope.cancel = function() {
		$scope.selection = null;
	};
	
	$scope.toggle = function(array,itm) {
		console.log(array);
		var pos = array.indexOf(itm);
		if (pos < 0) array.push(itm); else array.splice(pos, 1);
   };
   
   $scope.checkType = function(app, linktype) {
	   if (!app || !app.type) return true;
	   if (app.type === "mobile") {
		   if (linktype === "AUTOADD_A" ) return true;
	   }
	   // "OFFER_P", "REQUIRE_P", "RECOMMEND_A", "AUTOADD_A", "DATALINK"
	   return false;
   };
   
   $scope.studyselection = function() {
	   $scope.status.doBusy(studies.search({ code : $scope.selection.study.code }, ["_id", "code", "name" ]))
		.then(function(data) {
			if (data.data && data.data.length == 1) {
			  $scope.selection.studyId = data.data[0]._id;
			  $scope.selection.study = data.data[0];			  
			}
		});
   };
   
   $scope.remove = function(link) {
	  $scope.status.doAction("delete", server.delete(jsRoutes.controllers.Market.deleteStudyAppLink(link._id).url))
	  .then(function() {
		  $scope.reload();
	  });
   };
   
   $scope.validate = function(link) {
		  $scope.status.doAction("validate", server.post(jsRoutes.controllers.Market.validateStudyAppLink(link._id).url))
		  .then(function() {
			  $scope.reload();
		  });
   };
   
   $scope.submit = function() {
	  	$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		
		if (! $scope.myform.$valid) {
			var elem = $document[0].querySelector('input.ng-invalid');
			if (elem && elem.focus) elem.focus();
			return;
		}
		
		$scope.selection.appId = $scope.appId;

		var first;
		if ($scope.selection._id) {
			first = $scope.status.doAction("delete", server.delete(jsRoutes.controllers.Market.deleteStudyAppLink($scope.selection._id).url));
		} else first = $q.when();
		first.then(function() { $scope.status.doAction("submit", server.post(jsRoutes.controllers.Market.insertStudyAppLink().url, $scope.selection))
		.then(function() {
			  $scope.reload();
		}); });
   };
      
		
   $scope.reload();
	
}]);