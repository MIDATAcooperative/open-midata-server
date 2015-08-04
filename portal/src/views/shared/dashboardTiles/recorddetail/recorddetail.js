angular.module('views')
.controller('RecordDetailCtrl', ['$scope', '$http', '$attrs', 'views', 'records', 'apps', 'status', function($scope, $http, $attrs, views, records, apps, status) {
		
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.record = {};
	$scope.status = new status(true);
	
	$scope.reload = function() {
	   if (!$scope.view.active) return;	
       $scope.status.doBusy(records.getRecord($scope.view.setup.id)).
	   then(function(result) {
			$scope.record = result.data;
			$scope.record.json = JSON.stringify($scope.record.data, null, "\t");
			if (_.has($scope.record.data, "type") && $scope.record.data.type === "file") {
				$scope.downloadLink = jsRoutes.controllers.Records.getFile(recordId).url;
			}
			
			loadUserNames();
			
			apps.getApps({"_id": $scope.record.app}, ["name"]).
			then(function(result) { $scope.record.app = result.data[0].name; });
			
			console.log($scope.record);
			//var split = $scope.record.created.split(" ");
			//$scope.record.created = split[0] + " at " + split[1];
		});
	};
    
    
	var loadUserNames = function() {		
		var data = {"properties": {"_id": [$scope.record.owner, $scope.record.creator]}, "fields": ["firstname", "sirname"]};
		$scope.status.doSilent($http.post(jsRoutes.controllers.Users.getUsers().url, JSON.stringify(data))).
			then(function(result) {				
				_.each(result.data, function(user) {
					if ($scope.record.owner && $scope.record.owner.$oid === user._id.$oid) { $scope.record.owner = (user.firstname+" "+user.sirname).trim(); }
					if ($scope.record.creator && $scope.record.creator.$oid === user._id.$oid) { $scope.record.creator = (user.firstname+" "+user.sirname).trim(); }
				});
				if (!$scope.record.owner) $scope.record.owner = "?";
				if (!$scope.record.creator) $scope.record.creator = "Same as owner";
			});
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);