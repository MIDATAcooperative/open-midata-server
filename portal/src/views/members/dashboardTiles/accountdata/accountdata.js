angular.module('views')
.controller('AccountDataCtrl', ['$scope', 'server', '$attrs', 'users', 'views', 'status', 'currentUser', function($scope, server, $attrs, users, views, status, currentUser) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    
    currentUser.then(function(userId) { 
    	$scope.userId = userId;
    	$scope.reload(); 
    });
    
    $scope.reload = function() { 
    	if (!$scope.view.active || !$scope.userId) return;	
    	
    	$scope.status.doBusy(users.getMembers({ "_id" : $scope.userId }, ["midataID", "firstname", "sirname", "birthday", "address1", "address2", "zip", "city", "country"]))
    	.then(function(results) { $scope.member = results.data[0]; });
    };
    
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);