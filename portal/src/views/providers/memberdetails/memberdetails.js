angular.module('portal')
.controller('MemberDetailsCtrl', ['$scope', '$state', 'server', 'views', 'circles', 'session', 'status', function($scope, $state, server, views, circles, session, status) {
	
	$scope.memberid = $state.params.memberId;
	$scope.member = {};	
	$scope.status = new status(true);
		
	views.reset();
	views.link("1", "record", "record");
	$scope.reload = function() {
			
		$scope.status.doBusy(server.get(jsRoutes.controllers.providers.Providers.getMember($scope.memberid).url))
		.then(function(results) {
			    var data = results.data;
				$scope.member = data.member;
				$scope.consents = data.consents;
				$scope.backwards = data.backwards;
				console.log(data);
				$scope.memberkey = data.memberkey;
				if (data.memberkey) {
				  views.setView("1", { aps : $scope.memberkey._id.$oid, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowAdd: false, type : "memberkeys"});
				} else {
				  views.disableView("1");
				}
			});
	};
	
	$scope.selectConsent = function(consent) {
		$scope.hideAdd = false;
		$scope.consent = consent;
		console.log($scope.consent);
		if ($scope.consent != null) {
			views.setView("1", { aps : $scope.consent._id.$oid, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowAdd : false, type : "memberkeys" });			
		} else {
			views.disableView("1");
		}
	};
	
	var addDataConsent = function(backConsent) {
		$scope.consent = null;
		$scope.hideAdd = true;
		views.setView("1", { aps : backConsent._id.$oid, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowAdd : true, type : "hcrelated" });
	};
	
	$scope.addData = function() {
		if ($scope.backwards.length > 0) {
			var consent = $scope.backwards[0];
			addDataConsent(consent);
		} else {
			circles.createNew({ type : "HCRELATED", name : $scope.member.firstname+" "+$scope.member.surname })
			.then(function(data) {
				circles.addUsers(data.data._id.$oid, [ { "$oid" : $scope.memberid } ])
				.then(function(xdata) {
					$scope.backwards.push(data.data);
					addDataConsent(data.data);
				});
			});
		}
	};
	
	$scope.addTask = function() {
	  console.log("AAAA");
	  console.log($scope);
	  console.log($scope.consent);
	  views.setView("addtask", { "owner" : $scope.memberid, "shareBackTo" : $scope.consent._id.$oid });
	  console.log("BBBB");
	};
		
	$scope.reload();
	
	// For adding new records
	$scope.error = null;
	
	$scope.loadingApps = true;	
	$scope.userId = null;
	$scope.apps = [];
	
	
	// get current user
	server.get(jsRoutes.controllers.Users.getCurrentUser().url).
		success(function(userId) {
			$scope.userId = userId;
			$scope.getApps(userId);			
		});
	
	// get apps
	$scope.getApps = function(userId) {
		var properties = {"_id": userId};
		var fields = ["apps", "visualizations"];
		var data = {"properties": properties, "fields": fields};
		server.post(jsRoutes.controllers.Users.getUsers().url, JSON.stringify(data)).
			success(function(users) {
				$scope.getAppDetails(users[0].apps);
				$scope.getVisualizationDetails(users[0].visualizations);
			}).
			error(function(err) { $scope.error = "Failed to load apps: " + err; });
	};
	
	// get name and type for app ids
	$scope.getAppDetails = function(appIds) {
		var properties = {"_id": appIds, "type" : ["create","oauth1","oauth2"] };
		var fields = ["name", "type"];
		var data = {"properties": properties, "fields": fields};
		server.post(jsRoutes.controllers.Plugins.get().url, JSON.stringify(data)).
			success(function(apps) {
				$scope.apps = apps;
				$scope.loadingApps = false;
			}).
			error(function(err) { $scope.error = "Failed to load apps: " + err; });
	};
	
	// get name and type for app ids
	$scope.getVisualizationDetails = function(visualizationIds) {
		var properties = {"_id": visualizationIds, type: ["visualization"] };
		var fields = ["name", "type"];
		var data = {"properties": properties, "fields": fields};
		server.post(jsRoutes.controllers.Plugins.get().url, JSON.stringify(data)).
			success(function(visualizations) {
				$scope.visualizations = visualizations;
				$scope.loadingVisualizations = false;
			}).
			error(function(err) { $scope.error = "Failed to load apps: " + err; });
	};
	
	// go to record creation/import dialog
	$scope.createOrImport = function(app) {
		if (app.type === "create") {
			$state.go("^.createpatientrecord", { memberId : $scope.member._id.$oid, appId : app._id.$oid, consentId : $scope.consent._id.$oid });			
		} else {
			$state.go("^.importrecords", { appId : app._id.$oid });			
		}
	};
	
	// Visualizations
	$scope.loadingVisualizations = true;
	$scope.visualizations = [];
	
	$scope.useVisualization = function(visualization) {		
		$state.go("^.usevisualization", { memberId : $scope.member._id.$oid , visualizationId : visualization._id.$oid, consentId : $scope.consent._id.$oid });				
	};
	
}]);