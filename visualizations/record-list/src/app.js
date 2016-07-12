var recordList = angular.module('recordList', [ 'midata', 'ui.bootstrap', 'pascalprecht.translate' ]);
recordList.config(['$translateProvider', function($translateProvider) {	    
    
	$translateProvider
	.useSanitizeValueStrategy('escape')	   	    
	.registerAvailableLanguageKeys(['en', 'de', 'it', 'fr'], {
	  'en_*': 'en',
	  'de_*': 'de',
	  'fr_*': 'fr',
	  'it_*': 'it',
	})
	.translations('en', en)
	.translations('de', de)
	.translations('it', it)
	.translations('fr', fr)
	.fallbackLanguage('en');
}]);
recordList.run(['$translate', 'midataPortal', function($translate, midataPortal) {
 	console.log("Language: "+midataPortal.language);
	$translate.use(midataPortal.language);	   	  
}]);
recordList.controller('RecordListCtrl', ['$scope', '$filter', '$location', 'midataServer', 'midataPortal',
	function($scope, $filter, $location, midataServer, midataPortal) {
		
	    midataPortal.autoresize();
	    
		$scope.datePickers = {};
	    $scope.dateOptions = {
	       formatYear: 'yy',
	       startingDay: 1
	    };
	    
		// init
		$scope.mode = 'loading';
		$scope.error = null;
		$scope.errors = {};
		$scope.records = [];

		// parse the authorization token from the url
		var authToken = $location.path().split("/")[1];

				
		// get the data for the records in this space
		$scope.getRecords = function() {
	
			midataServer.getRecords(authToken, { "format" : "fhir/Observation", subformat : "String", content : "diary" }, ["name", "data"])
			.then(function(results) {
				    var records = results.data;
					for (var i = 0; i < records.length; i++) {
						
							$scope.records.push(records[i]);
							$scope.records[$scope.records.length - 1].id = $scope.records.length - 1;
						
					}
					$scope.mode = "view";
				}, function(err) {
					$scope.error = "failed";
					$scope.mode = "view";
				});
		};
		
        $scope.validate = function() {
			
			$scope.hasError = false;
			$scope.validateTitle();
			$scope.validateContent();
			if(!$scope.errors.title && !$scope.errors.content) {
				$scope.submit();
			}
			
		};
		
		$scope.validateTitle = function() {
			$scope.errors.title = null;
			if (!$scope.newentry.title) {
				$scope.errors.title = "no_title_error";
			} else if ($scope.newentry.title.length > 50) {
				$scope.errors.title = "title_too_long_error"; 
			}
		};
		
		$scope.validateContent = function() {
			$scope.errors.content = null;
			if (!$scope.newentry.content) {
				$scope.errors.content = "no_content_error"; 
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
					effectiveDateTime : new Date($scope.newentry.date).toJSON(),
					valueString : $scope.newentry.content
					
			};
						
			// submit to server
			midataServer.createRecord(authToken, { "name" : $scope.newentry.title, "content" : "diary", "subformat" : "String", "format" : "fhir/Observation" }, record)
			.then(function() {
					$scope.success = "success";
					$scope.records.push({ name : $scope.newentry.title, data : record });
					
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
			$scope.newentry = { content:"", title:"", date : $filter('date')(new Date(), "yyyy-MM-dd") };
		};
		
		$scope.cancel = function() {
			$scope.mode = "view";
			$scope.title = null;
			$scope.content = null;
		};
		
		$scope.getRecords();
	}
]);
recordList.controller('RecordListPreviewCtrl', ['$scope', '$filter', '$location', 'midataServer', 'midataPortal',
 	function($scope, $filter, $location, midataServer, midataPortal) {
 		
 	    
 		$scope.mode = 'loading';
 		$scope.error = null;
 		$scope.errors = {};
 		$scope.records = [];

 		// parse the authorization token from the url
 		var authToken = $location.path().split("/")[1];

 				
 		// get the data for the records in this space
 		$scope.getInfos = function() {
 	        midataServer.getSummary(authToken, "ALL", { "format" : "fhir/Observation", subformat : "String", content : "diary" })
 	        .then(function(result) {
 	        	if (result.data && result.data.length > 0) {
 	        	   $scope.summary = result.data[0]; 	
 	        	   console.log($scope.summary);
 	        	   var newestId = $scope.summary.newestRecord.$oid;
 	        	   midataServer.getRecords(authToken, { "_id" : newestId, "format" : "fhir/Observation", content : "diary" }, ["name", "data"])
 	        	   .then(function(result2) {
 	        		   $scope.record = result2.data[0];
 	        	   });
 	        	} else {
 	        		$scope.summary = { count : 0 };
 	        	}
 	        }); 			 			
 		};
 		
       		
 		$scope.getInfos();
 	}
 ]);
