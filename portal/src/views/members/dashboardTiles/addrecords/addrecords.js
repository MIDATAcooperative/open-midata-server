angular.module('views')
.controller('AddRecordsCtrl', ['$scope', '$state', 'server', '$attrs', 'views', 'records', 'status', function($scope, $state, server, $attrs, views, records, status) {
	
	$scope.foundRecords = [];
	$scope.criteria = { query : "" };
	$scope.title = $attrs.title;
	$scope.viewid = $attrs.viewid || $scope.def.id;
	$scope.view = views.getView($scope.viewid);
	$scope.records = [];
	$scope.status = new status(true);
	$scope.newest = true;
	
	$scope.reload = function() {		
		if (!$scope.view.active) return;	
		$scope.foundRecords = [];
		$scope.criteria.query = "";
								
		$scope.searchRecords();
		
		if ($scope.view.setup.sharedRecords != null) $scope.records = _.chain($scope.view.setup.sharedRecords).pluck('_id').value();
	};
			
	$scope.shareRecords = function() {
		var selection = _.filter($scope.foundRecords, function(rec) { return rec.checked; });
		selection = _.chain(selection).pluck('_id').pluck('$oid').value();	
		$scope.status.doSilent(records.share($scope.view.setup.targetAps, selection, $scope.view.setup.type))
		.then(function () {
		   views.changed($scope.viewid);
		   views.disableView($scope.viewid);
		});
	};
	
	// check whether record is not already in active space
	$scope.isntInSpace = function(record) {		
		return !$scope.containsRecord($scope.records, record._id);
	};
	
	// helper method for contains
	$scope.containsRecord = function(recordIdList, recordId) {
		var ids = _.map(recordIdList, function(element) { return element.$oid; });
		return _.contains(ids, recordId.$oid);
	};
	
	$scope.showDetails = function(record) {
		if (!views.updateLinked($scope.view, "record", { id : record.id })) {
		  $state.go("^.recorddetail", { recordId : record.id });
		}
	};
	
	// search for records
	$scope.searchRecords = function() {		
		var query = $scope.criteria.query;
		$scope.foundRecords = [];
		
		if (query) {			
			$scope.newest = false;
			$scope.status.doBusy(records.search(query)).
				then(function(results) {
					$scope.error = null;
					$scope.foundRecords = results.data;					
				});
		} else {
			$scope.newest = true;
			
		    $scope.status.doBusy(records.getRecords(null, { "max-age" : 86400 * 31, "limit" : 100, "owner" : "self" }, [ "ownerName", "created", "id", "name" ])).
			then(function (result) { 
				$scope.foundRecords = result.data;
				$scope.searching = false; 
			});
		}
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);