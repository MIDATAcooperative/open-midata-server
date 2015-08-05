angular.module('portal')
.controller('MemberDetailsCtrl', ['$scope', 'server', 'views', function($scope, server, views) {
	
	$scope.memberid = window.location.pathname.split("/")[3];
	$scope.member = {};	
	$scope.loading = true;
		
	views.link("1", "record", "record");
	$scope.reload = function() {
			
		server.get(jsRoutes.controllers.providers.Providers.getMember($scope.memberid).url).
			success(function(data) { 												
				$scope.member = data.member;
				$scope.memberkey = data.memberkey;
				if (data.memberkey) {
				  views.setView("1", { aps : $scope.memberkey.aps.$oid, properties : { } , fields : [ "ownerName", "created", "id", "name" ]});
				} else {
				  views.disableView("1");
				}
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.error = err;				
			});
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
		server.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data)).
			success(function(apps) {
				$scope.apps = apps;
				$scope.loadingApps = false;
			}).
			error(function(err) { $scope.error = "Failed to load apps: " + err; });
	};
	
	// get name and type for app ids
	$scope.getVisualizationDetails = function(visualizationIds) {
		var properties = {"_id": visualizationIds };
		var fields = ["name", "type"];
		var data = {"properties": properties, "fields": fields};
		server.post(jsRoutes.controllers.Visualizations.get().url, JSON.stringify(data)).
			success(function(visualizations) {
				$scope.visualizations = visualizations;
				$scope.loadingVisualizations = false;
			}).
			error(function(err) { $scope.error = "Failed to load apps: " + err; });
	};
	
	// go to record creation/import dialog
	$scope.createOrImport = function(app) {
		if (app.type === "create") {
			window.location.href = portalRoutes.controllers.ProviderFrontend.createRecord(app._id.$oid, $scope.member._id.$oid).url;
		} else {
			window.location.href = portalRoutes.controllers.Records.importRecords(app._id.$oid).url;
		}
	};
	
	// Visualizations
	$scope.loadingVisualizations = true;
	$scope.visualizations = [];
	
	$scope.useVisualization = function(visualization) {		
		window.location.href = portalRoutes.controllers.ProviderFrontend.useVisualization($scope.member._id.$oid, visualization._id.$oid ).url;		
	};
	
}]);