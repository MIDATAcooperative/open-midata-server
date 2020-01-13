angular.module('portal')
.controller('StudyActionsCtrl', ['$scope', '$state', 'server', 'views', 'apps', 'status', 'circles', 'spaces', 'studies', '$q', function($scope, $state, server, views, apps, status, circles, spaces, studies, $q) {
	
	$scope.studyId = $state.params.studyId;
	$scope.crit = { group : "" };
	$scope.status = new status(true, $scope);
	
	$scope.types = studies.linktypes;
	$scope.periods = studies.executionStati;
	$scope.selection = undefined;
	
	views.reset();
	
	$scope.error = null;
	$scope.submitted = false;
	
	$scope.reload = function() {
	
		$scope.selection = null;
		
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyId).url))
	    .then(function(data) { 				
			$scope.study = data.data;												
		});			
		
		$scope.status.doBusy(server.get(jsRoutes.controllers.Market.getStudyAppLinks("study", $scope.studyId).url))
	    .then(function(data) { 				
			$scope.links = data.data;												
		});	
		
		$scope.status.doBusy(apps.getApps({ }, ["_id", "filename", "name", "orgName", "publisher", "type", "targetUserRole"]))
		.then(function(data) {
			$scope.apps = data.data;
		});
	};
	
	$scope.addNew = function() {
		$scope.selection = { app : {}, type:[], usePeriod:[], linkTargetType : "STUDY" };
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
   
   $scope.appselection = function() {
	   $scope.status.doBusy(apps.getApps({ filename : $scope.selection.app.filename }, ["_id", "filename", "name", "orgName", "publisher", "type", "targetUserRole"]))
		.then(function(data) {
			if (data.data && data.data.length == 1) {
			  $scope.selection.appId = data.data[0]._id;
			  $scope.selection.app = data.data[0];
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
		
		$scope.selection.studyId = $scope.studyId;

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

angular.module('portal')
.controller('StudyActionsCtrl2', ['$scope', '$state', 'server', 'views', 'apps', 'status', 'circles', 'spaces', function($scope, $state, server, views, apps, status, circles, spaces) {
	
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
			
			$scope.updateConsents();
						
		});
		
		apps.getApps({ "targetUserRole" : "RESEARCH", type : ["analyzer","visualization","mobile"] }, ["filename", "name","type"])
		.then(function(data) {
			$scope.plugins = data.data;
		});
		
	};
	
	$scope.setGroup = function() {
		$scope.group = $scope.crit.group;
		$scope.status.doBusy(server.post(jsRoutes.controllers.research.Studies.shareWithGroup($scope.studyId, $scope.group).url))
		.then(function(result) {
			if (result.data.length) {
			   $scope.aps = result.data[0]._id;
			   views.setView("group_records", { aps : $scope.aps, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowAdd : true, type : "studyrelated" });
			}
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
		
		spaces.getSpacesOfUserContext($scope.userId, $scope.study.code + ":" + ($scope.crit.group ? $scope.crit.group : ""))
    	.then(function(results) {
    		$scope.me_menu = results.data;
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
		
      var c = $scope.crit;		
	  $scope.status.doAction("addapplication", server.post(jsRoutes.controllers.research.Studies.addApplication($scope.studyId, $scope.group).url, { plugin : c.plugin._id, restrictread : c.restrictread, shareback : c.shareback, device : c.device }))
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
	
	$scope.showSpace = function(space) {
		$state.go('^.spaces', { spaceId : space._id, study : $scope.study._id });
	};
	
	$scope.deleteSpace = function(space) {
		$scope.status.doAction("deleteConsent", spaces.deleteSpace(space._id)).then(function() { $scope.updateConsents(); });
	};
	
	$scope.reload();
	
}]);