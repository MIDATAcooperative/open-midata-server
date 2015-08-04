angular.module('views')
.controller('FlexibleRecordListCtrl', ['$scope', '$http', '$attrs', 'views', 'records', 'status', function($scope, $http, $attrs, views, records, status) {
			
	$scope.records = [];	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);
	$scope.limit = 4;
	
	
	$scope.reload = function() {		
		if (!$scope.view.active) return;
		$scope.limit = $scope.view.position == "small" ? 4 : 20;
		$scope.status.doBusy(records.getRecords($scope.view.setup.aps, $scope.view.setup.properties, $scope.view.setup.fields)).
		then(function (result) { $scope.records = result.data; });
	};
	
	$scope.showDetails = function(record) {
		if (!views.updateLinked($scope.view, "record", { id : record.id })) {
		  window.location.href = portalRoutes.controllers.Records.details(record.id).url;
		}
	};
	
	$scope.removeRecord = function(record) {
		$scope.status.doSilent(records.unshare($scope.view.setup.aps, record._id.$oid, $scope.view.setup.type));
		$scope.records.splice($scope.records.indexOf(record), 1);
	};
	
	$scope.shareRecords = function() {
		var selection = _.filter($scope.records, function(rec) { return rec.marked; });
		selection = _.chain(selection).pluck('_id').pluck('$oid').value();
		$scope.status.doSilent(records.share($scope.view.setup.targetAps, selection, $scope.view.setup.type))
		.then(function () {
		   views.changed($attrs.viewid);
		   views.disableView($attrs.viewid);
		});
	};
	
	$scope.addRecords = function() {
		views.updateLinked($scope.view, "shareFrom", 
				 { aps : null, 
			       properties:{}, 
			       fields : $scope.view.setup.fields, 
			       targetAps : $scope.view.setup.aps, 
			       allowShare : true,
			       type : $scope.view.setup.type,
			       sharedRecords : $scope.records
			      });
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);