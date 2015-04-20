var views = angular.module('views', ['services']);
views.controller('FlexibleRecordListCtrl', ['$scope', '$http', '$attrs', 'views', 'records', function($scope, $http, $attrs, views, records) {
	
	$scope.records = [];
	$scope.title = $attrs.title;
	$scope.view = views.getView($attrs.viewid);
	
	$scope.reload = function() {		
		if (!$scope.view.active) return;
		records.getRecords($scope.view.setup.aps, $scope.view.setup.properties, $scope.view.setup.fields).
		then(function (result) { $scope.records = result.data; });
	};
	
	$scope.showDetails = function(record) {
		if (!views.updateLinked($scope.view, "record", { id : record.id })) {
		  window.location.href = portalRoutes.controllers.Records.details(record.id).url;
		}
	};
	
	$scope.removeRecord = function(record) {
		records.unshare($scope.view.setup.aps, record._id.$oid, $scope.view.setup.type);
		$scope.records.splice($scope.records.indexOf(record), 1);
	};
	
	$scope.shareRecords = function() {
		var selection = _.filter($scope.records, function(rec) { return rec.marked; });
		selection = _.chain(selection).pluck('_id').pluck('$oid').value();
		records.share($scope.view.setup.targetAps, selection, $scope.view.setup.type)
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
views.controller('FlexibleStudiesCtrl', ['$scope', '$http', '$attrs', 'views', 'studies', function($scope, $http, $attrs, views, studies) {
	
	$scope.studies = [];
	$scope.title = $attrs.title;
	$scope.view = views.getView($attrs.viewid);
	
	$scope.reload = function() {		
		if (!$scope.view.active) return;
		studies.search($scope.view.setup.properties, $scope.view.setup.fields).
		then(function (result) { $scope.studies = result.data; });
	};
	
	$scope.showDetails = function(study) {
		window.location.href = portalRoutes.controllers.MemberFrontend.studydetails(study._id.$oid).url;
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);
views.controller('RecordDetailCtrl', ['$scope', '$http', '$attrs', 'views', 'records', 'apps', function($scope, $http, $attrs, views, records, apps) {
	
	$scope.title = $attrs.title;
	$scope.view = views.getView($attrs.viewid);
	$scope.record = {};
	
	$scope.reload = function() {
	   if (!$scope.view.active) return;	
       records.getRecord($scope.view.setup.id).
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
		$http.post(jsRoutes.controllers.Users.getUsers().url, JSON.stringify(data)).
			success(function(users) {				
				_.each(users, function(user) {
					if ($scope.record.owner && $scope.record.owner.$oid === user._id.$oid) { $scope.record.owner = (user.firstname+" "+user.sirname).trim(); }
					if ($scope.record.creator && $scope.record.creator.$oid === user._id.$oid) { $scope.record.creator = (user.firstname+" "+user.sirname).trim(); }
				});
				if (!$scope.record.owner) $scope.record.owner = "?";
				if (!$scope.record.creator) $scope.record.creator = "Same as owner";
			}).
			error(function(err) { $scope.error = "Failed to load names: " + err; });
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);
views.controller('AddRecordsCtrl', ['$scope', '$http', '$attrs', 'views', 'records', function($scope, $http, $attrs, views, records) {
	
	$scope.foundRecords = [];
	$scope.criteria = { query : "" };
	$scope.title = $attrs.title;
	$scope.view = views.getView($attrs.viewid);
	$scope.records = [];
	$scope.searching = false;
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
		records.share($scope.view.setup.targetAps, selection, $scope.view.setup.type)
		.then(function () {
		   views.changed($attrs.viewid);
		   views.disableView($attrs.viewid);
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
		  window.location.href = portalRoutes.controllers.Records.details(record.id).url;
		}
	};
	
	// search for records
	$scope.searchRecords = function() {		
		var query = $scope.criteria.query;
		$scope.foundRecords = [];
		
		if (query) {
			$scope.searching = true;
			$scope.newest = false;
			records.search(query).
				success(function(records) {
					$scope.error = null;
					$scope.foundRecords = records;
					$scope.searching = false;
				}).
				error(function(err) {
					$scope.error = "Record search failed: " + err;
					$scope.searching = false;
				});
		} else {
			$scope.newest = true;
			$scope.searching = true;
		    records.getRecords(null, { "max-age" : 86400 * 31 }, [ "ownerName", "created", "id", "name" ]).
			then(function (result) { 
				$scope.foundRecords = result.data;
				$scope.searching = false; 
			});
		}
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);
