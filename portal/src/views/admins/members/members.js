angular.module('portal')
.controller('MembersListCtrl', ['$scope', '$state', 'views', 'status', 'users', 'administration', function($scope, $state, views, status, users, administration) {

	$scope.status = new status(true);    
	$scope.roles = [ "MEMBER", "PROVIDER", "RESEARCH", "DEVELOPER", "ADMIN"];
	$scope.stati = [ "NEW", "ACTIVE", "BLOCKED", "DELETED" ];
	$scope.searches = [ 
	  { 
		name : "admin_members.contract_required",
		criteria : { status : "NEW", contractStatus : "REQUESTED", emailStatus : "VALIDATED" }
	  },
	  {
		name : "admin_members.contract_confirm_required",
		criteria : { status : "NEW", contractStatus : "PRINTED" }
	  },
	  { 
		name : "admin_members.trialusers",
		criteria : { status : "ACTIVE", subroles : "TRIALUSER" }
	  },
	  {
		name : "admin_members.overview",
		criteria : { role : "MEMBER", status : "NEW" },
	    changeable : true
	  },
	  {
		name : "admin_members.specific_user",
		criteria : { },
		searchable : "lastname"
	  }
	];
	$scope.search = $scope.searches[0];
    
	$scope.reload = function(search) {		
		if (search) $scope.search = search;
		console.log($scope.search);
		if ($scope.search.searchable && !$scope.search.criteria.lastname && !$scope.search.criteria.email) return;
		if (!$scope.search.criteria.lastname) { delete $scope.search.criteria.lastname; }
		if (!$scope.search.criteria.email) { delete $scope.search.criteria.email; }
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