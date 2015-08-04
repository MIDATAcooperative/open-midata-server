angular.module('portal')
.controller('Spaces2Ctrl', ['$scope', '$http', '$sce', 'status', 'spaces', 'views', function($scope, $http, $sce, status, spaces, views) {
	
	// init
	$scope.error = null;
	$scope.userId = null;
	$scope.loading = true;
	$scope.spaceId = window.location.pathname.split("/")[3];
	$scope.space = { "_id" : { "$oid" : $scope.spaceId } };
	
	
	// get current user
	$http(jsRoutes.controllers.Users.getCurrentUser()).
		success(function(userId) {
			$scope.userId = userId;
			getAuthToken($scope.space);
		});
		
	
	
	// get the authorization token for the current space
	getAuthToken = function(space) {
		
		spaces.getUrl(space._id.$oid)
		.then(function(result) {		
			space.trustedUrl = $sce.trustAsResourceUrl(result.data);
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
		  template : "/assets/views/members/share.html",
		  title : "Share With...",
		  active : false,
		  position : "modal"			 
	} );
					
	
	// start side-by-side display of current visualization
	$scope.startCompare = function(space) {
		// copy relevant properties
		space.copy = {};
		space.copy._id = {"$oid": "copy-" + space._id.$oid};
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
		views.setView("share", { space : space._id.$oid });
	};
	
	
	// delete a space
	$scope.deleteSpace = function(space) {
		$http(jsRoutes.controllers.Spaces["delete"](space._id.$oid)).
			success(function() {
				$scope.error = null;
				$scope.spaces.splice($scope.spaces.indexOf(space), 1);
				if ($scope.spaces.length > 0) {
					$scope.spaces[0].active = true;
				}
			}).
			error(function(err) { $scope.error = "Failed to delete space '" + space.name + "': " + err; });
	};
	
	$scope.goBack = function() {
	   spaces.get({ "_id" :  { $oid : $scope.spaceId } }, ["context"]).
	   then(function(result) { document.location.href = "/members/dashboard/" + encodeURIComponent(result.data[0].context); });
	};
	
}]);
