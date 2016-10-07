angular.module('portal')
.controller('AutoImportCtrl', ['$scope', '$state', 'session', 'server', 'spaces', 'status', 'ENV', function($scope, $state, session, server, spaces, status, ENV) {
	
	$scope.baseurl = ENV.apiurl;

	$scope.calls = [];
			
	$scope.init = function(userId, appId) {
		
		var properties = {"owner": userId, "visualization" : appId };
	    var fields = ["name", "type", "order", "autoImport", "context", "visualization"];
	    var data = {"properties": properties, "fields": fields};
	    spaces.get(properties, fields)
	    .then(function(results) {	    	
	    	angular.forEach(results.data, function(space) {
	    		spaces.getUrl(space._id)
	    		.then(function(spaceurl) {	    			
	    			var call = { name : space.name, autoImport : space.autoImport, authToken : spaceurl.data.token, loggedIn : !(spaceurl.data.authorizationUrl) };
	    			$scope.calls.push(call);	    			
	    		});
	    	});
	    });
				
	};
	
		
	$scope.doInstall = function() {
		$state.go("^.visualization", { visualizationId : $scope.app._id, context : "sandbox" });
	};
	
	session.currentUser.then(function(userId) { $scope.init(userId, $state.params.appId); });	
}]);