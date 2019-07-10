angular.module('portal')
.controller('Spaces2Ctrl', ['$scope', '$state', '$translate','server', '$sce', 'status', 'spaces', 'views', 'session', '$window', 'apps', function($scope, $state, $translate, server, $sce, status, spaces, views, session, $window, apps) {
	
	// init
	$scope.error = null;
	$scope.userId = null;
	$scope.loading = true;
	$scope.spaceId = $state.params.spaceId;
	$scope.space = { "_id" : $scope.spaceId };
	$scope.params = $state.params.params ? JSON.parse($state.params.params) : null;
			
	session.currentUser
	.then(function(userId) {
			$scope.userId = userId;
			
			if ($state.params.app && !$state.params.spaceId) {				
				$scope.openAppLink({ app : $state.params.app });
			} else getAuthToken($scope.space, $state.params.user);
	});
			
	// get the authorization token for the current space
	getAuthToken = function(space) {
		
		spaces.getUrl(space._id, $state.params.user)
		.then(function(result) {   
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
	
	
	$scope.status = new status(true);
	
	$scope.def = views.def( {                        
		  id : "share",
		  template : "/views/members/dashboardTiles/share/share.html",
		  title : "Share With...",
		  active : false,
		  position : "modal"			 
	} );
					
	
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
				$scope.error = null;
				$state.go('^.dashboard', { dashId : "me" });
			},function(err) { $scope.error = "Failed to delete space '" + space.name + "': " + err; });
	};
	
	$scope.goBack = function() {
		if ($state.params.user) {
			if ($state.current.data.role=="RESEARCH") {
			  $state.go("research.study.participant", { participantId : $state.params.user, studyId : $state.params.study });
			} else {
			  $state.go("provider.memberdetails", { user : $state.params.user });
			}
		} else {
		   spaces.get({ "_id" :  $scope.spaceId }, ["context"]).
		   then(function(result) { 
			   if (!result.data || result.data[0].context == "me" || result.data[0].context == "mydata") {
				 $state.go("^.overview");
			   } else {
			     $state.go('^.dashboard', { dashId : result.data[0].context }); 
			   }
		   });
		}
	};
	
	$scope.openAppLink = function(data) {
		data = data || {};
		data.user = $state.params.user;
		spaces.openAppLink($state, $scope.userId, data);	 
	};
	
	$scope.notLocked = function() {
		return !$state.params.app;
	};
	
}]);
