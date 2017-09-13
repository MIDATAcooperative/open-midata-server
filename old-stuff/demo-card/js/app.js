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
				
		
		
		// get the data for the records in this space
		getRecords = function() {
			data.properties = { format : [ "demo-card/part1", "demo-card/part2" ] };
			data.fields = ["data", "content", "format", "created", "owner", "ownerName"];
			$http.post("https://" + window.location.hostname + ":9000/v1/plugin_api/records/search", JSON.stringify(data)).
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
				var format = (rec.format == "demo-card/part1") ? "part1" : (rec.format == "demo-card/part2") ? "part2" : null;
				if (!format) { 
					$scope.skipped.wrongFormat++;
					continue;				
				}
				var ownerName = rec.ownerName;
				var ownerName1 = ownerName.toLowerCase().replace(/\s/g,'');
				if (owners[ownerName1] == null) owners[ownerName1] = { ownerName:ownerName };
				var owner = owners[ownerName1];
				if (owner[format] == null) {
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
		
		getRecords();
		
	}
]);
