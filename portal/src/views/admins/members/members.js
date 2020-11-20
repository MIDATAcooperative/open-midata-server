/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('portal')
.controller('MembersListCtrl', ['$scope', '$state', 'views', 'status', 'users', 'administration', 'paginationService', 'session', function($scope, $state, views, status, users, administration, paginationService, session) {

	$scope.status = new status(true);    
	$scope.roles = [ "MEMBER", "PROVIDER", "RESEARCH", "DEVELOPER", "ADMIN"];
	$scope.stati = [ "NEW", "ACTIVE", "BLOCKED", "DELETED" ];
	$scope.searches = [ 
	  { 
		name : "admin_members.contract_required",
		criteria : { status : "NEW", agbStatus : "REQUESTED" }
	  },
	  {
		name : "admin_members.contract_confirm_required",
		criteria : { status : "NEW", agbStatus : "PRINTED" }
	  },	 
	  {
		name : "admin_members.no_email_confirm",
		criteria : { emailStatus : "UNVALIDATED" }
	  },
	  {
		name : "admin_members.email_rejected",
		criteria : { emailStatus : "REJECTED" }
	  },
	  {
		name : "admin_members.not_admin_confirmed",
		criteria : { status : "NEW" }
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
	$scope.page = { nr : 1 };
	$scope.search = $scope.searches[0];
    
	$scope.reload = function(searchName, comeback) {		
		if (searchName) $scope.search = session.map($scope.searches, "name")[searchName];
		
		if ($scope.search.searchable && !$scope.search.criteria.lastname && !$scope.search.criteria.email) return;
		if (!$scope.search.criteria.lastname) { delete $scope.search.criteria.lastname; }
		if (!$scope.search.criteria.email) { delete $scope.search.criteria.email; }
		$scope.status.doBusy(users.getMembers($scope.search.criteria, [ "midataID", "firstname", "lastname", "email", "role", "subroles", "status", "emailStatus", "developer", "login", "security" ]))
		.then(function(data) {
			if (!comeback) paginationService.setCurrentPage("membertable", 1); // Reset view to first page
			$scope.members = data.data;						
		});
		
		$scope.dateLimit = new Date();
		$scope.dateLimit.setMonth($scope.dateLimit.getMonth() - 1);
	};
	
	$scope.changeUser = function(user) {	
		administration.changeStatus(user._id, user.status);
	};	
	
	session.load("MembersListCtrl", $scope, ["search", "page"]);
	$scope.searchName = $scope.search.name;
	$scope.reload(undefined, true);

}]);