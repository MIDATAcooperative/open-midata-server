angular.module('portal')
.controller('RecordCtrl', ['$scope', '$state', 'server', '$sce', 'records', 'status', function($scope, $state, server, $sce, records, status) {
	// init
	$scope.error = null;
	$scope.record = {};
	$scope.status = new status(true);
	
	var recordId = $state.params.recordId;
	var data = {"_id": recordId };
	
	$scope.status.doBusy(records.getUrl(recordId)).
	then(function(results) {
		if (results.data) {
		  $scope.url = $sce.trustAsResourceUrl(results.data);
		}
	});
		
	server.post(jsRoutes.controllers.Records.get().url, JSON.stringify(data)).
		success(function(records) {
			$scope.record = records;
			$scope.record.json = JSON.stringify($scope.record.data, null, "\t");
			if (_.has($scope.record.data, "type") && $scope.record.data.type === "file") {
				$scope.downloadLink = jsRoutes.controllers.Records.getFile(recordId).url;
			}
			loadUserNames();
			loadAppName();										    	    	
									
		}).
		error(function(err) { $scope.error = "Failed to load record details: " + err; });
	
	var loadUserNames = function() {
		var data = {"properties": {"_id": [$scope.record.owner, $scope.record.creator]}, "fields": ["firstname", "sirname"]};
		server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) {
				_.each(users, function(user) {
					if ($scope.record.owner.$oid === user._id.$oid) { $scope.record.owner = (user.firstname+" "+user.sirname).trim(); }
					if ($scope.record.creator.$oid === user._id.$oid) { $scope.record.creator = (user.firstname+" "+user.sirname).trim(); }
				});
			}).
			error(function(err) { $scope.error = "Failed to load names: " + err; });
	};
	
	var loadAppName = function() {
		var data = {"properties": {"_id": $scope.record.app}, "fields": ["name"]};
		server.post(jsRoutes.controllers.Plugins.get().url, JSON.stringify(data)).
			success(function(apps) { $scope.record.app = apps[0].name; }).
			error(function(err) { $scope.error = "Failed to load app name: " + err; });
	};
	
	/*var rewriteCreated = function() {
		var split = $scope.record.created.split(" ");
		$scope.record.created = split[0] + " at " + split[1];
	}*/
	
}]);




