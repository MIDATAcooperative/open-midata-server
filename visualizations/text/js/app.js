var textRecords = angular.module('textRecords', [ 'midata' ]);
textRecords.controller('CreateCtrl', ['$scope', '$http', '$location', 'midataServer', 'midataPortal',
	function($scope, $http, $location, midataServer, midataPortal) {
		
	    midataPortal.autoresize();
		// init
		$scope.errors = {};

		// get authorization token
		var authToken = $location.path().split("/")[1];
		
		// controller functions
		$scope.validate = function() {
			
			$scope.hasError = false;
			$scope.validateTitle();
			$scope.validateContent();
			if(!$scope.errors.title && !$scope.errors.content) {
				$scope.submit()
			}
			
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
			
			$scope.loading = true;
			
			var rec = {
				resourceType : "Observation",
				status : "final",
				code : {
					coding : [ { system : "http://midata.coop" , code : "diary", display : "Diary" } ]
				},
				effectiveDateTime : $scope.date,
				valueString : $scope.content
				
			};
			// submit to server
			midataServer.createRecord(authToken, $scope.title, $scope.content, "diary", "fhir/Observation", {"title": $scope.title, "content": $scope.content})
			.then(function() {
					$scope.success = "Record created successfully.";
					$scope.error = null;
					$scope.title = null;
					$scope.content = null;
					$scope.loading = false;
			}, function(err) {
					$scope.success = null;
					$scope.error = err;
					$scope.loading = false;
			});
		};
		
	}
]);
