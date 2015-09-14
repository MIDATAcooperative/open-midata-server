angular.module('views')
.controller('AccountDataCtrl', ['$scope', 'server', '$attrs', 'users', 'views', 'status', 'session', function($scope, server, $attrs, users, views, status, session) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    
    session.currentUser.then(function(userId) { 
    	$scope.userId = userId;
    	$scope.reload(); 
    });
    
    $scope.reload = function() { 
    	if (!$scope.view.active || !$scope.userId) return;	
    	
    	$scope.status.doBusy(users.getMembers({ "_id" : $scope.userId }, ["midataID", "firstname", "lastname", "birthday", "address1", "address2", "zip", "city", "country"]))
    	.then(function(results) { $scope.member = results.data[0]; });
    };
    
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);