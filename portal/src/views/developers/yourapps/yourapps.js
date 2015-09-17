angular.module('portal')
.controller('YourAppsCtrl', ['$scope', '$state', 'views', 'session', 'apps', function($scope, $state, views, session, apps) {
	
	$scope.init = function(userId) {		
		  apps.getApps({ creator : userId }, [ "creator", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type"])
		  .then(function(data) { $scope.apps = data.data; });		  		  
	};
	
	session.currentUser.then(function(userId) { $scope.init(userId); });
}]);