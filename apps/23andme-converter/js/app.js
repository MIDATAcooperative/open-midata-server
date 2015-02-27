var converter = angular.module('converter', []);
converter.controller('ConverterCtrl', ['$scope', '$http', '$location',
	function($scope, $http, $location) {
		
		// init
		$scope.loading = true;
		$scope.converting = false;
		$scope.errors = {};
		$scope.files = {};

		// get authorization token
		var authToken = $location.path().split("/")[1];

		// get the list of files
		var data = {"authToken": authToken};
		$http.post("https://" + window.location.hostname + ":9000/apps/gdconverter/files", JSON.stringify(data)).
			success(function(files) {
				$scope.files = files;
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.errors.server = "Failed to load files: " + err;
				$scope.loading = false;
			});

		// controller functions
		$scope.validate = function() {
			$scope.converting = true;
			$scope.errors = {};
			validateSelection();
			if(!$scope.errors.file) {
				submit();
			} else {
				$scope.converting = false;
			}
		};
		
		var validateSelection = function() {
			$scope.errors.file = null;
			if (!$scope.file) {
				$scope.errors.file = "Please select a file to convert.";
			}
		};

		var submit = function() {
			data = {
				"authToken": authToken,
				"id": $scope.file._id.$oid,
				"name": "23andMe Genome Data (HDC Format)",
				"description": "23andMe genome data converted to the HDC format",
				"format" : "Genome Data/23andMe"
			};
			$http.post("https://" + window.location.hostname + ":9000/apps/gdconverter/convert", JSON.stringify(data)).
				success(function() {
					$scope.success = "File converted successfully.";
					$scope.converting = false;
				}).
				error(function(err) {
					$scope.errors.server = "Failed to convert file: " + err;
					$scope.converting = false;
				});
		}
		
	}
]);
