angular.module('portal')
.controller('RecordCtrl', ['$scope', '$state', 'server', '$sce', 'records', 'status', 'ENV', '$window', function($scope, $state, server, $sce, records, status, ENV, $window) {
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
		
	$scope.status.doBusy(server.post(jsRoutes.controllers.Records.get().url, JSON.stringify(data)))
	.then(function(records) {
			$scope.record = records.data;
			$scope.record.json = JSON.stringify($scope.record.data, null, "\t");
			if (_.has($scope.record.data, "type") && $scope.record.data.type === "file") {
				$scope.downloadLink = true;
			}
			loadUserNames();
			loadAppName();										    	    	
									
		});
	
	var loadUserNames = function() {
		var data = {"properties": {"_id": [$scope.record.owner, $scope.record.creator]}, "fields": ["firstname", "lastname"]};
		server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) {
				_.each(users, function(user) {
					if ($scope.record.owner.$oid === user._id.$oid) { $scope.record.owner = (user.firstname+" "+user.lastname).trim(); }
					if ($scope.record.creator.$oid === user._id.$oid) { $scope.record.creator = (user.firstname+" "+user.lastname).trim(); }
				});
			}).
			error(function(err) { $scope.error = { code : "error.internal" }; } );
	};
	
	var loadAppName = function() {
		var data = {"properties": {"_id": $scope.record.app}, "fields": ["name"]};
		server.post(jsRoutes.controllers.Plugins.get().url, JSON.stringify(data)).
			success(function(apps) { $scope.record.app = apps[0].name; }).
			error(function(err) { $scope.error = { code : "error.internal" }; });
	};
		
	$scope.goBack = function() {
		$window.history.back();
	};
	
	$scope.download = function() {
		$scope.status.doAction("download", server.token())
		.then(function(response) {
		  document.location.href = ENV.apiurl + jsRoutes.controllers.Records.getFile(recordId).url + "?token=" + encodeURIComponent(response.data.token);
		});
	};
	
}]);




