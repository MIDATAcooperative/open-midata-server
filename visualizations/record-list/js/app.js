var recordList = angular.module('recordList', [ 'midata' ]);
recordList.controller('RecordListCtrl', ['$scope', '$filter', '$location', 'midataServer', 'midataPortal',
	function($scope, $filter, $location, midataServer, midataPortal) {
		
	    midataPortal.autoresize();
	    
		// init
		$scope.mode = 'loading';
		$scope.error = null;
		$scope.errors = {};
		$scope.records = [];

		// parse the authorization token from the url
		var authToken = $location.path().split("/")[1];

				
		// get the data for the records in this space
		$scope.getRecords = function() {
	
			midataServer.getRecords(authToken, { "format" : "fhir/Observation/String", content : "diary" }, ["name", "data"])
			.then(function(results) {
				    var records = results.data;
					for (var i = 0; i < records.length; i++) {
						try {
							$scope.records.push(records[i]);
							$scope.records[$scope.records.length - 1].id = $scope.records.length - 1;
						} catch(parsingError) {
							// skip this record
						}
					}
					$scope.mode = "view";
				}, function(err) {
					$scope.error = "Failed to load records: " + err.data;
					$scope.mode = "view";
				});
		};
		
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
			
			var record = {
					resourceType : "Observation",
					status : "final",
					code : {
						coding : [ { system : "http://midata.coop" , code : "diary", display : "Diary" } ]
					},
					effectiveDateTime : new Date($scope.date).toJSON(),
					valueString : $scope.content
					
			};
						
			// submit to server
			midataServer.createRecord(authToken, $scope.title, $scope.content, "diary", "fhir/Observation/String", record)
			.then(function() {
					$scope.success = "Record created successfully.";
					$scope.records.push({ name : $scope.title, data : record });
					
					$scope.title = null;
					$scope.error = null;					
					$scope.content = null;
					$scope.loading = false;
					$scope.mode = "view";
					
			}, function(err) {
					$scope.success = null;
					$scope.error = err.data;
					$scope.loading = false;
			});
		};
		
		$scope.newEntry = function() {
			$scope.mode = "create";
			$scope.date = $filter('date')(new Date(), "yyyy-MM-dd");
		};
		
		$scope.cancel = function() {
			$scope.mode = "view";
			$scope.title = null;
			$scope.content = null;
		};
		
		$scope.getRecords();
	}
]);
