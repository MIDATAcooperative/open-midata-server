angular.module('views')
.controller('ListHealthProviderCtrl', ['$scope', 'server', '$attrs', 'views', 'hc', 'status', function($scope, server, $attrs, views, hc, status) {
	
	$scope.results =[];
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
	
	$scope.reload = function() {
			
		$scope.status.doBusy(hc.list()).
		then(function(results) { 				
			$scope.results = results.data;				
			$scope.showNewHCRecords();
		});
	};
	
	$scope.confirm = function(memberKey) {
		hc.confirm(memberKey.provider.$oid).then(function() { $scope.reload(); });		
	};
	
	$scope.reject = function(memberKey) {
		hc.reject(memberKey.provider.$oid).then(function() { $scope.reload(); });
	};
	
	$scope.mayReject = $scope.mayConfirm = function(memberKey) {
		return memberKey.status == "UNCONFIRMED";
	};
	
	
	$scope.showNewHCRecords = function() {
		var creators = [];
		var aps = null;
		_.each($scope.results, function(hc) {
			console.log(hc);
			if (hc.provider) {
				creators.push(hc.provider.$oid);
				aps = hc.member.$oid;
			}
		});
		
		if (aps != null) {
		  views.setView("hcrecords", { aps : aps, properties: { "max-age" : 60*60*24*31, "creator" : creators }, fields : [ "creatorName", "created", "id", "name" ]});
		} else {
		  views.disableView("hcrecords");
		}
	};
	
	$scope.showRecords = function(mk) {
		views.setView("records", { aps : mk.aps.$oid, properties: {}, fields : [ "ownerName", "created", "id", "name" ], allowAdd : true, type:"memberkeys"}, mk.name);
	};
	
	$scope.reload();
	
}]);