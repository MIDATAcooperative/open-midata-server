angular.module('views')
.controller('RecordDetailCtrl', ['$scope', 'server', '$attrs', 'views', 'records', 'apps', 'status', '$state', '$timeout', function($scope, server, $attrs, views, records, apps, status, $state, $timeout) {
		
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.record = {};
	$scope.status = new status(true);
	
	$scope.reload = function() {
	   if (!$scope.view.active) return;	
       $scope.status.doBusy(records.getRecord($scope.view.setup.id)).
	   then(function(result) {
			$scope.record = result.data;
			$scope.record.json = JSON.stringify($scope.record.data, null, "\t");
						
			loadUserNames();
			
			apps.getApps({"_id": $scope.record.app}, ["name"]).
			then(function(result) { $scope.record.app = result.data[0].name; });
						
			//var split = $scope.record.created.split(" ");
			//$scope.record.created = split[0] + " at " + split[1];
		});
	};
    
    
	var loadUserNames = function() {		
		var data = {"properties": {"_id": [$scope.record.owner, $scope.record.creator]}, "fields": ["firstname", "lastname"]};
		$scope.status.doSilent(server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data))).
			then(function(result) {				
				_.each(result.data, function(user) {
					if ($scope.record.owner && $scope.record.owner === user._id) { $scope.record.owner = (user.firstname+" "+user.lastname).trim(); }
					if ($scope.record.creator && $scope.record.creator === user._id) { $scope.record.creator = (user.firstname+" "+user.lastname).trim(); }
				});
				if (!$scope.record.owner) $scope.record.owner = "?";
				if (!$scope.record.creator) $scope.record.creator = "Same as owner";
			});
	};
	
	$scope.showDetail = function() {		
		var recordId = $scope.view.setup.id;
		views.disableView($scope.view.id);
		var sname = $state.current.name.split('.')[0]+".recorddetail";		
		$timeout(function() { $state.go(sname, { recordId : recordId }); }, 500);		
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);