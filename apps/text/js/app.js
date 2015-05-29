var textRecords = angular.module('textRecords', []);
textRecords.controller('CreateCtrl', ['$scope', '$http', '$location',
	function($scope, $http, $location) {
		
		// init
		$scope.errors = {};

		// get authorization token
		var authToken = $location.path().split("/")[1];
		
		// controller functions
		$scope.validate = function() {
			$scope.loading = true;
			$scope.hasError = false;
			$scope.validateTitle();
			$scope.validateContent();
			if(!$scope.errors.title && !$scope.errors.content) {
				$scope.submit()
			}
			$scope.loading = false;
		};
		
		$scope.validateTitle = function() {
			$scope.errors.title = null;
			if (!$scope.title) {
				$scope.errors.title = "No title provided";
			} else if ($scope.title.length > 50) {
				$scope.errors.title = "Title too long.";
			}
		};
		
		$scope.validateContent = function() {
			$scope.errors.content = null;
			if (!$scope.content) {
				$scope.errors.content = "No content provided.";
			}
		};
		
		$scope.submit = function() {
			// construct json
			var data = {
				"authToken": authToken,
				"data": JSON.stringify({"title": $scope.title, "content": $scope.content}),
				"name": $scope.title,
				"description": $scope.content,
				"format" : "text"
			};
			
			// submit to server
			$http.post("https://" + window.location.hostname + ":9000/api/apps/create", data).
				success(function() {
					$scope.success = "Record created successfully.";
					$scope.error = null;
					$scope.title = null;
					$scope.content = null;
				}).
				error(function(err) {
					$scope.success = null;
					$scope.error = err;
				});
		};
		
	}
]);
