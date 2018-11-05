angular.module('portal')
.controller('TimelineCtrl', ['$scope', '$state', '$translate','server', '$sce', 'status', 'spaces', 'views', 'session', '$window', 'apps', 'ENV', function($scope, $state, $translate, server, $sce, status, spaces, views, session, $window, apps, ENV) {
	
	// init
	$scope.error = null;
	$scope.userId = null;
	$scope.loading = true;
	$scope.spaceId = $state.params.spaceId;
	$scope.space = { "_id" : $scope.spaceId };
	$scope.params = $state.params.params ? JSON.parse($state.params.params) : null;
	
    $scope.status = new status(true);
	
	$scope.def = views.def( {                        
		  id : "share",
		  template : "/views/members/dashboardTiles/share/share.html",
		  title : "Share With...",
		  active : false,
		  position : "modal"			 
	} );
	// get current user
	session.currentUser
	.then(function(userId) {
			$scope.userId = userId;
			
			spaces.autoadd().then(function() {
			spaces.getSpacesOfUserContext(userId, "me").then(function(result) {
				$scope.spaces = [];
				angular.forEach(result.data, function(s) {
					if (s.name != "Fitness" && s.name != "Timeline") {
					  $scope.spaces.push(s);
					}
				});
			});
			});
			
			server.post(jsRoutes.controllers.Plugins.get().url, JSON.stringify({ "properties" : { "filename" : "timeline" }, "fields": ["_id", "type"] }))
			.then(function(result) {				
				  if (result.data.length == 1) {
					  var app = result.data[0];
					  spaces.get({ "owner": userId, "visualization" : result.data[0]._id }, ["_id"])
					  .then(function(spaceresult) {
						 if (spaceresult.data.length > 0) {
							 var target = spaceresult.data[0];
							 getAuthToken(target, $state.params.user);							 
						 } else {
							 
							 
							 apps.installPlugin(app._id, { applyRules : true, context : "me" /*, study : data.study */ })
							 .then(function(result) {				
									//
									if (result.data && result.data._id) {
										getAuthToken(result.data, $state.params.user);
									} 
								});
						 }
					  });
				  }
			});				
			
	});
			
	// get the authorization token for the current space
	getAuthToken = function(space) {
		console.log(space);
		$scope.space = space;
		$scope.status.doBusy(spaces.getUrl(space._id, $state.params.user))
		.then(function(result) {   
			console.log("YESW!!!");
			$scope.title = result.data.name;
			var url = spaces.mainUrl(result.data, $translate.use(), $scope.params);			
			space.trustedUrl = $sce.trustAsResourceUrl(url);
		});
	};
	
	// reload the iframe displaying the visualization
	reloadIframe = function(space) {
		space.trustedUrl = $sce.trustAsResourceUrl(space.completedUrl);

		// have to detach and append again to force reload; just setting src didn't do the trick
		var iframe = $("#iframe").detach();
		// set src attribute of iframe to avoid creating an entry in the browser history
		iframe.attr("src", space.trustedUrl);
		$("#iframe-placeholder").append(iframe);
	};
	
	
	
					
	
	// start side-by-side display of current visualization
	$scope.startCompare = function(space) {
		// copy relevant properties
		space.copy = {};
		space.copy._id =  "copy-" + space._id;
		space.copy.name = space.name;
		space.copy.trustedUrl = space.trustedUrl;
		
		// detach/attach iframe to force loading
		//reloadIframe(space.copy);
		
		// start side-by-side display
		space.compare = true;
	};

	// end side-by-side display of current visualization
	$scope.endCompare = function(space) {
		space.compare = false;
		space.copy = {};
	};
	
	$scope.startShare = function(space) {
		views.setView("share", { space : space._id });
	};
	
	
	// delete a space
	$scope.deleteSpace = function(space) {
		server.delete(jsRoutes.controllers.Spaces["delete"](space._id).url).
			then(function() {
				$scope.spaces.splice($scope.spaces.indexOf(space), 1);
			},function(err) { $scope.error = "Failed to delete space '" + space.name + "': " + err; });
	};
	
	$scope.goBack = function() {
	   $window.history.back();
	   //spaces.get({ "_id" :  $scope.spaceId }, ["context"]).
	   //then(function(result) { $state.go('^.dashboard', { dashId : result.data[0].context }); });
	};
	
	$scope.openAppLink = function(data) {
		spaces.openAppLink($state, $scope.userId, data);	 
	};
	
	$scope.use = function(view) {
		spaces.openAppLink($state, $scope.userId, { app : view});
	};
	
	$scope.getIconUrl = function(space) {
		if (!space.visualization) return null;
		return ENV.apiurl + "/api/shared/icon/APPICON/" + space.visualization;
	};
	
}]);
