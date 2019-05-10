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
	   if ($scope.search.criteria.creatorLogin === "") $scope.search.criteria.creatorLogin = undefined;
	   $scope.status.doBusy(apps.getApps( $scope.search.criteria, [ "creator", "creatorLogin", "filename", "version", "name", "description", "tags", "targetUserRole", "spotlighted", "type", "status", "orgName"]))
	   .then(function(data) { $scope.apps = data.data; });
	};
	
	$scope.changePlugin = function(plugin) {				
		apps.updatePluginStatus(plugin)
	    .then(function() { $scope.init(); });				
	};	
	
	$scope.emptyIsUndefined = function(x) {
	  return x === "" ? undefined : x;
	};
	
	session.load("PluginsCtrl", $scope, ["search", "page"]);
	if ($state.params.creator) $scope.search.criteria.creatorLogin = $state.params.creator;
	session.currentUser.then(function(userId) { $scope.init(userId); });
}]);