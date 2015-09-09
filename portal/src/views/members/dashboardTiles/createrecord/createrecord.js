angular.module('views')
.controller('CreateRecordCtrl', ['$scope', '$state', 'server', '$attrs', '$sce', 'views', 'status', 'apps', 'session', function($scope, $state, server, $attrs, $sce, views, status, apps, session) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    $scope.apps = null;
    $scope.showapp = false;    

    session.currentUser.then(function(userId) { 
    	$scope.userId = userId;
    	$scope.reload(); 
    });
    
    $scope.reload = function() {
    	if (!$scope.view.active || !$scope.userId) return;	
    	
    	var appId = $scope.view.setup.appId;
    	var userId = $scope.view.setup.userId;
    
    	if (appId) {
    		$scope.selectApp(appId);
    	} else {
    		$scope.showAppList();
    	}
    	    	
    };
    
    $scope.selectApp = function(appId, title) {
    	if (!$scope.view.setup.inline) {
    		$state.go('^.createrecord', { appId : appId });
    		return;
    	}
    	
        $scope.showapp = true;
        // if (title != null) $scope.view.title = title;
        $scope.status.doBusy(server.get(jsRoutes.controllers.Apps.getPreviewUrl(appId).url)).
		then(function(results) {			
			$scope.url = $sce.trustAsResourceUrl(results.data);
		});
    };
    
    $scope.showAppList = function() {
    	$scope.showapp = false;
    	if ($scope.apps == null) $scope.loadAppList();
    };
    
    $scope.loadAppList = function() {
    	$scope.apps = [];
    	$scope.status.doBusy(apps.getAppsOfUser($scope.userId, ["create","oauth1","oauth2"], ["name", "type", "previewUrl"]))
    	.then(function(results) {
    	   $scope.apps = results.data;   
    	  _.each(results.data, function(app) {
    		  if (!app.previewUrl) return;    		 
     		  var appdef =
     		     {
     		    	   id : "app"+app._id.$oid,
     		    	   template : "/views/members/dashboardTiles/createrecord/createrecord.html",
     		    	   title : app.name,
     		    	   active : true,
     		    	   position : "small",
     		    	   actions : { big : "/members/records/create/" + app._id.$oid },
     		    	   setup : { appId : app._id.$oid, inline : true }
     		     };
     		 views.layout.small.push(views.def(appdef)); 
     	  });     
    	  
    	  
    	});
    };
	/* 
	$scope.memberUrl = portalRoutes.controllers.ProviderFrontend.member(userId).url;
	console.log($scope.memberUrl);
	
	
	server(jsRoutes.controllers.Apps.getUrlForMember(appId, userId)).
		success(function(url) {
			$scope.error = null;
			$scope.url = $sce.trustAsResourceUrl(url);
		}).
		error(function(err) { $scope.error = "Failed to load app: " + err; });
	*/
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);