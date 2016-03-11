angular.module('portal')
.controller('PluginsCtrl', ['$scope', '$state', 'views', 'session', 'apps', 'status', function($scope, $state, views, session, apps, status) {

	$scope.status = new status(true);
	$scope.pluginStati = ["DEVELOPMENT", "BETA", "ACTIVE", "DEPRECATED"];
	
	$scope.init = function(userId) {		
		  $scope.status.doBusy(apps.getApps({  }, [ "creator", "filename", "version", "name", "description", "tags", "targetUserRole", "spotlighted", "type", "status"]))
		  .then(function(data) { $scope.apps = data.data; });		  		  
	};
	
	$scope.changePlugin = function(plugin) {				
		apps.updatePluginStatus(plugin)
	    .then(function() { $scope.init(); });				
	};	
	
	session.currentUser.then(function(userId) { $scope.init(userId); });
}]);