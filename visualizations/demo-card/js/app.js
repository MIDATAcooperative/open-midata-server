var recordList = angular.module('recordList', []);
recordList.controller('RecordListCtrl', ['$scope', '$http', '$location',
	function($scope, $http, $location) {
		
		// init
		$scope.loading = true;
		$scope.error = null;
		$scope.records = [];
		$scope.skipped = { wrongFormat : 0, outdated : 0};

		// parse the authorization token from the url
		var authToken = $location.path().split("/")[1];

		// get the ids of the records assigned to this space
		var data = {"authToken": authToken};
		$http.post("https://" + window.location.hostname + ":9000/api/visualizations/ids", JSON.stringify(data)).
			success(function(recordIds) {
				getRecords(recordIds);
			}).
			error(function(err) {
				$scope.error = "Failed to load records: " + err;
				$scope.loading = false;
			});
		
		// get the data for the records in this space
		getRecords = function(recordIds) {
			data.properties = {"_id": recordIds};
			data.fields = ["data", "format", "created", "owner", "ownerName"];
			$http.post("https://" + window.location.hostname + ":9000/api/visualizations/records", JSON.stringify(data)).
				success(function(records) {
					$scope.buildView(records);
					$scope.loading = false;
				}).
				error(function(err) {
					$scope.error = "Failed to load records: " + err;
					$scope.loading = false;
				});
		};
		
		$scope.buildView = function(records) {
			var owners = {};
			$scope.records = [];
			$scope.skipped = { wrongFormat : 0, outdated : 0};
			
			for (var i = 0; i < records.length; i++) {
				var rec = records[i];
				var format = (rec.format == "cardio1/demo-card") ? "part1" : (rec.format == "cardio2/demo-card") ? "part2" : null;
				if (!format) { 
					$scope.skipped.wrongFormat++;
					continue;				
				}
				if (!owners[rec.owner]) owners[rec.owner] = { ownerName:rec.ownerName };
				var owner = owners[rec.owner];
				if (!owner[format]) {
					owner[format] = rec;
				} else {
					$scope.skipped.outdated++;
					if (owner[format].created < rec.created) owner[format] = rec;
				}
			}
			
			for (var k in owners) {
				$scope.records.push(owners[k]);
			}
		};
		
		$scope.isComplete = function(pair) {
		  return pair!=null && pair.part1!=null && pair.part2!=null;	
		};
		
		$scope.hasRisk = function(pair) {
			if (pair.part1 == null || pair.part2 == null) return false;
			return pair.part2.data.ldl > 160 || pair.part1.data.bloodpressureSys > 160;			 
		}; 
		
		
	}
]);
