angular.module('views')
.controller('SpaceSummaryCtrl', ['$scope', 'server', '$attrs', '$sce', 'records', 'views', 'status', 'spaces', function($scope, server, $attrs, $sce, records, views, status, spaces) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    $scope.count = 0;
    $scope.last = null;
    
    $scope.reload = function() { 
        if (!$scope.view.active) return;	
    	
    	var spaceId = $scope.view.setup.spaceId;
    	var recordId = $scope.view.setup.recordId;
    	
    	$scope.status.doBusy(recordId ? records.getUrl(recordId) : ($scope.view.setup.nopreview ? spaces.getUrl(spaceId) : spaces.getPreviewUrl(spaceId))).
		then(function(results) {
			if (results.data) {
			  $scope.url = $sce.trustAsResourceUrl(results.data);
			} else {
			  $scope.status.doBusy(records.getRecords(spaceId, {}, ["created"])).
			  then(function(results) {
				 $scope.count = results.data.length;		
				 $scope.last = $scope.count > 0 ? _.chain(results.data).pluck('created').max().value() : null;
			  });						
			}
		});
    	    	    				
    };
        
    $scope.showSpace = function() {    	
    	window.location.href = "/members/spaces/"+$scope.view.setup.spaceId;
    };
    
    $scope.showCreate = function() {    	
    	window.location.href = "/members/records/create/" + $scope.view.setup.appId;    		
    };
    
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);