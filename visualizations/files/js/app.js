var files = angular.module('files', ['angularFileUpload', 'midata']);
files.controller('FilesCtrl', ['$scope', '$http', '$location', 'FileUploader', 'midataPortal',
	function($scope, $http, $location, FileUploader, midataPortal) {
		
	    midataPortal.autoresize();
	    
		// init
		$scope.errors = {};
		$scope.uploading = false;
		$scope.uploadComplete = false;

		// get authorization token
		var authToken = $location.path().split("/")[1];

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
			$scope.success = "File upload complete.";
			$scope.title = null;
			$scope.description = null;
			$("#file").val("");
			$scope.uploading = false;
			$scope.loading = false;
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
		}
		
		// controller functions
		$scope.validate = function() {
			$scope.loading = true;
			$scope.errors = {};
			validateTitle();
			validateDescription();
			validateFile();
			if(!$scope.errors.title && !$scope.errors.description && !$scope.errors.file) {
				submit();
			} else {
				$scope.loading = false;
			}
		};
		
		var validateTitle = function() {
			$scope.errors.title = null;
			if (!$scope.title) {
				$scope.errors.title = "Please provide a title for your record.";
			} else if ($scope.title.length > 50) {
				$scope.errors.title = "Please provide a title with fewer than 50 characters.";
			}
		};

		var validateDescription = function() {
			$scope.errors.description = null;
			if (!$scope.description) {
				$scope.errors.description = "Please provide a brief description of the record.";
			}
		}
		
		var validateFile = function() {
			$scope.errors.file = null;
			if (uploader.queue.length === 0) {
				$scope.errors.file = "Please choose a file to upload.";
			} else if (uploader.queue.length > 1) {
				$scope.errors.file = "An unexpected error occurred. Please reload the page and try again.";
			}
		};

		var submit = function() {
			$scope.uploadComplete = false;
			$scope.uploading = true;

			// additional form data (specific to the current file)
			uploader.queue[0].formData = [{
				"authToken": authToken,
				"name": $scope.title,
				"description": $scope.description,
				"format" : "application/octet-stream",
				"content" : "unknown"
			}];

			// upload the current queue (1 file)
			uploader.uploadAll();
		}

		$scope.cancel = function() {
			uploader.cancelAll();
		}
		
	}
]);
