angular.module('portal')
.controller('UserGroupsCtrl', ['$scope', '$state', 'views', 'status', 'usergroups', function($scope, $state, views, status, usergroups) {

	$scope.status = new status(true);
		
	$scope.init = function() {	
		$scope.status.doBusy(usergroups.search({ "member" : true }, usergroups.ALLPUBLIC ))
    	.then(function(results) {
		  $scope.usergroups = results.data;
    	});
	};
	
	$scope.deleteGroup = function(grp) {
		$scope.status.doAction("delete", usergroups.deleteUserGroup(grp._id))
		.then(function() {
			$scope.init();
		});
	};
					
	$scope.init();
	
}]);