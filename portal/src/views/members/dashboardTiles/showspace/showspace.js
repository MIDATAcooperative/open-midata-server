angular.module('views')
.controller('ShowSpaceCtrl', ['$scope', 'server', '$attrs', '$sce', 'views', 'status', 'spaces', 'currentUser', function($scope, server, $attrs, $sce, views, status, spaces, currentUser) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    $scope.spaces = null;
    $scope.showspace = false;    

    currentUser.then(function(userId) { 
    	$scope.userId = userId;
    	$scope.reload(); 
    });
    
    $scope.reload = function() {
    	if (!$scope.view.active || !$scope.userId) return;	
    	
    	var spaceId = $scope.view.setup.spaceId;    	
    
    	if (spaceId) {
    		$scope.selectSpace(spaceId);
    	} else {
    		$scope.showSpaceList();
    	}
    	    	
    };
    
    $scope.selectSpace = function(spaceId, title) {
        $scope.showspace = true;
        // if (title != null) $scope.view.title = title;
        $scope.status.doBusy(spaces.getUrl(spaceId)).
		then(function(results) {			
			$scope.url = $sce.trustAsResourceUrl(results.data);
		});
    };
        
    $scope.showSpaceList = function() {
    	$scope.showspace = false;
    	$scope.loadSpaceList();
    	// if ($scope.spaces == null) { $scope.loadSpaceList(); }
    };
    
    $scope.loadSpaceList = function() {
    	$scope.status.doBusy(spaces.getSpacesOfUser($scope.userId))
    	.then(function(results) {
    	  $scope.spaces = results.data;    	  
    	});
    };
	 
	
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);