var recordList = angular.module('recordList', []);
recordList.controller('RecordListCtrl', ['$scope', '$http', '$location',
	function($scope, $http, $location) {
		
		// init
		$scope.loading = true;
		$scope.error = null;
		$scope.records = [];

		// parse the authorization token from the url
		var authToken = $location.path().split("/")[1];

		// get the ids of the records assigned to this space
		var data = {"authToken": authToken};
				
		// get the data for the records in this space
		$scope.getRecords = function() {
			data.properties = { "format" : "Text" };
			data.fields = ["data"];
			$http.post("https://" + window.location.hostname + ":9000/api/visualizations/records", JSON.stringify(data)).
				success(function(records) {
					for (var i = 0; i < records.length; i++) {
						try {
							$scope.records.push(records[i].data);
							$scope.records[$scope.records.length - 1].id = $scope.records.length - 1;
						} catch(parsingError) {
							// skip this record
						}
					}
					$scope.loading = false;
				}).
				error(function(err) {
					$scope.error = "Failed to load records: " + err;
					$scope.loading = false;
				});
		}
		
		$scope.getRecords();
	}
]);
