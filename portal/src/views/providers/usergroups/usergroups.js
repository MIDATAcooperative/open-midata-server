angular.module('portal')
.controller('UserGroupsCtrl', ['$scope', '$state', 'views', 'status', 'usergroups', function($scope, $state, views, status, usergroups) {

	$scope.status = new status(true);
		
	$scope.init = function(userId) {	
		$scope.status.doBusy(usergroups.search({ "member" : true }, usergroups.ALLPUBLIC ))
    	.then(function(results) {
		  $scope.usergroups = results.data;
    	});
	};
					
	$scope.init();
	
}]);