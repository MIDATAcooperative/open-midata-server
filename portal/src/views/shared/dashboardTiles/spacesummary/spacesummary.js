angular.module('views')
.controller('SpaceSummaryCtrl', ['$scope', '$state', '$translate', 'server', '$attrs', '$sce', 'records', 'views', 'status', 'spaces', function($scope, $state, $translate, server, $attrs, $sce, records, views, status, spaces) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    $scope.count = 0;
    $scope.last = null;
    
    $scope.reload = function() { 
        if (!$scope.view.active) return;	
    	
    	var spaceId = $scope.view.setup.spaceId;
    	var recordId = $scope.view.setup.recordId;
    	
    	if ($scope.view.setup.url) {
    	  $scope.url = $sce.trustAsResourceUrl($scope.view.setup.url);
    	  $scope.status.isBusy = false;
    	} else {
    	
    	$scope.status.doBusy(recordId ? records.getUrl(recordId) : ($scope.view.setup.nopreview ? spaces.getUrl(spaceId) : spaces.getPreviewUrl(spaceId))).
		then(function(results) {
			$scope.previewType = results.data.type || "visualization";
			if (results.data.url) {	
			  var url = results.data.url;
			  if (url.indexOf("?")>0) url+="&lang="+encodeURIComponent($translate.use()); else url+="?lang="+encodeURIComponent($translate.use());
			  $scope.url = $sce.trustAsResourceUrl(url);
			} else {			  
			  $scope.status.doBusy(records.getInfos(spaceId, {}, "ALL")).
			  then(function(results) {				 
				 if (results.data.length > 0) {
					 $scope.count = results.data[0].count;				 
					 $scope.last = results.data[0].newest;
				 }
			  });						
			}
		});
    	
    	}
    	    	    				
    };
        
    $scope.showSpace = function() {
    	if ($scope.view.setup.spaceType == "oauth1" || $scope.view.setup.spaceType == "oauth2") {
    	  $state.go('^.importrecords', { spaceId : $scope.view.setup.spaceId });
    	//} else if ($scope.view.setup.spaceType == "create") {
        //  $state.go('^.createrecord', { spaceId : $scope.view.setup.spaceId });
    	} else {
    	  $state.go('^.spaces', { spaceId : $scope.view.setup.spaceId });
    	}
    };
    
    $scope.view.makeBig = $scope.showSpace;
    
    $scope.showCreate = function() {    	
    	$state.go('^.createrecord', { appId : $scope.view.setup.appId });    		
    };
    
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);