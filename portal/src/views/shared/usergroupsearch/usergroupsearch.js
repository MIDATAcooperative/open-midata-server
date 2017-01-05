angular.module('views')
.controller('UserGroupSearchResultsCtrl', ['$scope', 'server', 'usergroups', '$state', 'status', 'views', function($scope, server, usergroups, $state, status, views) {
		
    $scope.status = new status(true);
    $scope.criteria = { name : $state.params.name };
    $scope.view = $scope.def ? views.getView($scope.def.id) : views.getView("usergroupsearch");
    
    var dosearch = function(crit) {
    	$scope.status.doBusy(usergroups.search(crit, ["name"]))
    	.then(function(data) {
    		$scope.usergroups = data.data;
    		angular.forEach($scope.usergroups, function(usergroup) {
    			
    			usergroups.listUserGroupMembers(usergroup._id)
    			.then(function(result) {
    				usergroup.members = result.data;
    			});
    			
    		});
    	});
    };
    
    $scope.search = function() {
    	var crit = {};
    	if ($scope.criteria.name !== "") crit.name = $scope.criteria.name;    	
    	dosearch(crit);	    	    	    	
    };
    
    $scope.addIndividuals = function(prov) {
    	var toAdd = [];
    	angular.forEach(prov.members, function(member) { toAdd.push(member.user); });
    	$scope.view.setup.callback(toAdd);
    	views.disableView($scope.view.id);    	
    };
    
    $scope.addGroup = function(prov) {
    	
    	views.disableView($scope.view.id);    	
    };
    
    if ($scope.view.active) $scope.search(); else { $scope.status.isBusy = false; }
    
		
}]);