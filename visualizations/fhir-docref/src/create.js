angular.module('fhirDocref')
.controller('CreateCtrl', ['$scope', '$http', '$location', '$timeout', 'FileUploader', 'midataPortal', 'midataServer',
   	function($scope, $http, $location, $timeout, FileUploader, midataPortal, midataServer) {
   		
   	  
   		// init
   		$scope.errors = {};
   		$scope.data = {};
   		$scope.uploading = false;
   		$scope.uploadComplete = false;
   		
   		$scope.types = [
          "11502-2", "60001", "60002", "60003", "60004", "60005", "60006"
        ];

   		// set up the uploader
   		var uploader = null;

   		var initUploader = function() {
   			uploader = $scope.uploader = new FileUploader({
   				"url": "https://" + window.location.hostname + ":9000/v1/plugin_api/records/upload",
   				"removeAfterUpload": true,
   				"queueLimit": 1 // restrict to one file per upload
   			});
   		};
   		initUploader();

   		// register callbacks
   		uploader.onSuccessItem = function() {
   			$scope.success = "success";
   			$scope.data.title = null;
   			$scope.data.type = null;
   			//angular.element("#file").val("");
   			$scope.uploading = false;
   			$scope.loading = false;
   			
   			$timeout(function() { 
				$scope.success = false;
				midataPortal.updateNotification();
				midataPortal.doneNotification();
				if ($scope.init) $scope.init();
		   }, 2000); 
   		};

   		uploader.onCancelItem = function() {
   			$scope.loading = false;
   			$scope.uploading = false;
   		};

   		uploader.onErrorItem = function(item, response, status, headers) {
   			$scope.success = null;
   			$scope.errors.server = response;
   			$scope.loading = false;
   			$scope.uploading = false;
   		};

   		uploader.onProgressItem = function(item, progress) {
   			if (progress === 100) {
   				$scope.uploadComplete = true;
   			}
   		};
   		
   		// controller functions
   		$scope.validate = function() {
   			$scope.loading = true;
   			$scope.errors = {};
   			validateTitle();   			
   			validateFile();
   			if(!$scope.errors.title && !$scope.errors.type && !$scope.errors.file) {
   				submit();
   			} else {
   				$scope.loading = false;
   			}
   		};
   		
   		var validateTitle = function() {
   			$scope.errors.title = null;
   			if (!$scope.data.title) {
   				$scope.errors.title = "missing_title";
   			} else if ($scope.data.title.length > 50) {
   				$scope.errors.title = "shorter_title"; 
   			}
   		};

   		/*var validateDescription = function() {
   			$scope.errors.description = null;
   			if (!$scope.data.description) {
   				$scope.errors.description = "missing_description";
   			}
   		};*/
   		
   		var validateFile = function() {
   			$scope.errors.file = null;
   			if (uploader.queue.length === 0) {
   				$scope.errors.file = "choose"; 
   			} else if (uploader.queue.length > 1) {
   				$scope.errors.file = "internal";
   			}
   		};

   		var submit = function() {
   			$scope.uploadComplete = false;
   			$scope.uploading = true;
   			
   			var fhirResource = {
   				"resourceType" : "DocumentReference",   			 
   		        "type" : { coding : [{ system : "http://loinc.org", code : $scope.data.type }] }, 
   		        "class" : { coding : [{ system : "http://loinc.org", code : $scope.data.type }] },   		     		 
	   		    "indexed" : new Date(),
	   		    "status" : "current",   		   		 
	   		    "description" : $scope.data.title   		  	   		     		  
   			};

   			// additional form data (specific to the current file)
   			uploader.queue[0].formData = [{
   				"authToken": midataServer.authToken,
   				"name": $scope.data.title,
   				"data": JSON.stringify(fhirResource),
   				"format" : "fhir/DocumentReference",
   				"code" : "http://loinc.org "+$scope.data.type
   			}];

   			// upload the current queue (1 file)
   			uploader.uploadAll();
   		};

   		$scope.cancel = function() {
   			uploader.cancelAll();
   		};
   		
   	}
   ]);
