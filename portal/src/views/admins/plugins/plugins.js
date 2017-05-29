angular.module('portal')
.controller('PluginsCtrl', ['$scope', '$state', 'views', 'session', 'apps', 'users', 'status', function($scope, $state, views, session, apps, users, status) {

	$scope.status = new status(true);
	$scope.pluginStati = ["DEVELOPMENT", "BETA", "ACTIVE", "DEPRECATED"];
	$scope.search = { criteria : {} };
	$scope.page = { nr : 1 };
	
	$scope.init = function(userId) {		
		$scope.reload();
		$scope.status.doBusy(users.getMembers({ role : "DEVELOPER" }, [ "firstname", "lastname", "email" ]))
		.then(function(data) {
			$scope.developers = data.data;
			$scope.developers.push({});
		});
	};
	
	$scope.reload = function() {
	   $scope.status.doBusy(apps.getApps( $scope.search.criteria, [ "creator", "creatorLogin", "filename", "version", "name", "description", "tags", "targetUserRole", "spotlighted", "type", "status"]))
	   .then(function(data) { $scope.apps = data.data; });
	};
	
	$scope.changePlugin = function(plugin) {				
		apps.updatePluginStatus(plugin)
	    .then(function() { $scope.init(); });				
	};	
	
	session.load("PluginsCtrl", $scope, ["search", "page"]);
	session.currentUser.then(function(userId) { $scope.init(userId); });
}]);