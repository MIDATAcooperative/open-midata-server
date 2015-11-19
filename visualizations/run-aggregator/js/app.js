var runAggregator = angular.module('runAggregator', []);
runAggregator.controller('RunAggregatorCtrl', ['$scope', '$http', '$location',
	function($scope, $http, $location) {
		
		// init
		$scope.loading = true;
		$scope.error = null;
		$scope.records = [];
		$scope.distance = 0;
		$scope.time = 0;
		$scope.speed = 0;

		// parse authorization token
		var authToken = $location.path().split("/")[1];

		// get the ids of the records assigned to this space
		var data = {"authToken": authToken};
		$http.post("https://" + window.location.hostname + ":9000/v1/plugin_api/records/ids", JSON.stringify(data)).
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
			data.fields = ["data"];
			$http.post("https://" + window.location.hostname + ":9000/v1/plugin_api/records/search", JSON.stringify(data)).
				success(function(records) {
					// parse JSON records
					for (var i = 0; i < records.length; i++) {
						try {
							$scope.records.push(records[i].data);
						} catch(parsingError) {
							// skip this record
						}
					}

					// compute statistics
					computeStatistics();
				}).
				error(function(err) {
					$scope.error = "Failed to load records: " + err;
					$scope.loading = false;
				});
		}

		computeStatistics = function() {
			// init
			var distance = time = speed = 0;

			// parse numbers out of text records
			for (var i = 0; i < $scope.records.length; i++) {
				var data = $scope.records[i].content;
				var distanceEnd = data.lastIndexOf("km");
				var timeEnd = data.lastIndexOf("h");
				if (distanceEnd !== -1 && timeEnd !== -1) {
					var distanceString = data.substring(0, distanceEnd).trim();
					distanceString = distanceString.substring(distanceString.lastIndexOf(" ") + 1);
					var timeString = data.substring(0, timeEnd).trim();
					timeString = timeString.substring(timeString.lastIndexOf(" ") + 1);
					var curDistance = parseFloat(distanceString);
					var curTime = parseFloat(timeString);
					if (!isNaN(curDistance) && !isNaN(curTime)) {
						distance += curDistance;
						time += curTime;
					}
				}
			}
			if (time > 0) {
				speed = distance / time;
			}

			// format output
			$scope.distance = distance.toFixed(2);
			$scope.time = time.toFixed(2);
			$scope.speed = speed.toFixed(2);
			$scope.loading = false;
		}

}]);
