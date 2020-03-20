angular.module('portal')
.controller('YourAppsCtrl', ['$scope', '$state', 'views', 'session', 'apps', 'status', function($scope, $state, views, session, apps, status) {

	$scope.status = new status(true);
	
	$scope.init = function(userId) {		
		  $scope.status.doBusy(apps.getApps({ developerTeam : userId }, [ "creator", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type"]))
		  .then(function(data) { $scope.apps = data.data; });		  		  
	};
	
	session.currentUser.then(function(userId) { $scope.init(userId); });
}]);