var jsonRecords = angular.module('jsonRecords', []);
jsonRecords.controller('CreateCtrl', ['$scope', '$http', '$location',
	function($scope, $http, $location) {
		
		// init
		$scope.errors = {};

		// get authorization token
		var authToken = $location.path().split("/")[1];
		
		// controller functions
		$scope.validate = function() {
			$scope.loading = true;
			$scope.errors = {};
			$scope.validateTitle();
			$scope.validateDescription();
			$scope.validateData();
			if(!$scope.errors.title && !$scope.errors.description && !$scope.errors.data) {
				$scope.submit()
			} else {
				$scope.loading = false;
			}
		};
		
		$scope.validateTitle = function() {
			$scope.errors.title = null;
			if (!$scope.title) {
				$scope.errors.title = "Please provide a title for your record.";
			} else if ($scope.title.length > 50) {
				$scope.errors.title = "Please provide a title with fewer than 50 characters.";
			}
		};

		$scope.validateDescription = function() {
			$scope.errors.description = null;
			if (!$scope.description) {
				$scope.errors.description = "Please provide a brief description of the record.";
			}
		}
		
		$scope.validateData = function() {
			$scope.errors.data = null;
			if (!$scope.data) {
				$scope.errors.data = "Please provide data for your record.";
			} else {
				try {
					$scope.json = JSON.parse($scope.data);
				} catch (parsingException) {
					$scope.errors.data = "Please provide valid JSON as data for your record.\n" + parsingException;
				}
			}
		};
		
		$scope.submit = function() {
			// construct json
			var data = {
				"authToken": authToken,
				"data": JSON.stringify($scope.json),
				"name": $scope.title,
				"description": $scope.description,
				"format" : "Json"
			};
			
			// submit to server
			$http.post("https://" + window.location.hostname + ":9000/api/apps/create", data).
				success(function() {
					$scope.success = "Record created successfully.";
					$scope.title = null;
					$scope.description = null;
					$scope.data = null;
					$scope.loading = false;
				}).
				error(function(err) {
					$scope.success = null;
					$scope.errors.server = err;
					$scope.loading = false;
				});
		};
		
	}
]);
