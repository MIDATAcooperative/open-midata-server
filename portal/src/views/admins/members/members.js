angular.module('portal')
.controller('MembersListCtrl', ['$scope', '$state', 'views', 'status', 'users', 'administration', function($scope, $state, views, status, users, administration) {

	$scope.status = new status(true);    
	$scope.roles = [ "MEMBER", "PROVIDER", "RESEARCH", "DEVELOPER", "ADMIN"];
	$scope.stati = [ "NEW", "ACTIVE", "BLOCKED", "DELETED" ];
	$scope.searches = [ 
	  { 
		name : "Contract needs to be sent",
		criteria : { status : "NEW", contractStatus : "NEW", emailStatus : "VALIDATED" }
	  },
	  {
		name : "Signed contract needs to be confirmed",
		criteria : { status : "NEW", contractStatus : "PRINTED" }
	  },
	  {
		name : "Overview",
		criteria : { role : "MEMBER", status : "NEW" },
	    changeable : true
	  }
	];
	$scope.search = $scope.searches[0];
    
	$scope.reload = function(search) {
		console.log(search);
		console.log($scope.search);
		if (search) $scope.search = search;
		$scope.status.doBusy(users.getMembers($scope.search.criteria, [ "midataID", "firstname", "lastname", "email", "role", "status" ]))
		.then(function(data) {
			$scope.members = data.data;						
		});
	};
	
	$scope.changeUser = function(user) {
		console.log(user);
		administration.changeStatus(user._id.$oid, user.status);
	};	
	
	$scope.reload();

}]);