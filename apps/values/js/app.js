var jsonRecords = angular.module('jsonRecords', []);
jsonRecords.factory('server', [ '$http', function($http) {
	
	var service = {};
	
	service.createRecord = function(authToken, name, description, content, format, data) {
		// construct json
		var data = {
			"authToken": authToken,
			"data": angular.toJson(data),
			"name": name,
			"content" : content,
			"format" : format,
			"description": (description || "")
		};
		
		// submit to server
		return $http.post("https://" + window.location.hostname + ":9000/api/apps/create", data);
	};
	
	service.createConversion = function(authToken, name, description, content, format, data, appendToId) {
		// construct json
		var data = {
			"authToken": authToken,
			"data": angular.toJson(data),
			"name": name,
			"content" : content,
			"format" : format,
			"description": (description || ""),
			"document" : appendToId,
			"part" : format
		};
		
		// submit to server
		return $http.post("https://" + window.location.hostname + ":9000/api/apps/create", data);
	};
		
		
	return service;	
}]);
jsonRecords.controller('CreateCtrl', ['$scope', '$http', '$location', '$filter', '$timeout', 'server',
	function($scope, $http, $location, $filter, $timeout, server) {
		
		// init
		$scope.errors = {};
				
		$scope.formats = [
		   {
				label : "Weight",
				unit : "kg",
				content : "body/weight",
				objkey : "weight"
		   },
		   {
			   label : "Height",
			   unit : "cm",
			   format : "body/height",
			   objkey : "height"
		   },
		   {
			   label : "Steps",
			   unit : "steps",
			   content : "activities/steps",
			   objkey : "activities-steps"
		   },
		   {
			   label : "Heartrate",
			   unit : "bpm",
			   content : "activities/heartrate",
			   objkey : "heartrate"
		   }
		];
		
				
		// get authorization token
		var authToken = $location.path().split("/")[1];
		var preselect = $location.path().split("/")[2];
		var preselectFormat = $filter('filter')($scope.formats,{ format : preselect });
		if (preselectFormat.length == 0) preselectFormat = $scope.formats[0]; else preselectFormat = preselectFormat[0]; 
		console.log(authToken);
		$scope.authToken = authToken;		
		$scope.isValid = true;
		$scope.isBusy = false;
		$scope.success = false;
		
		$scope.reset = function() {
			$scope.newentry = { 
					format : preselectFormat,
					value : 0,
					context : "",
					date : $filter('date')(new Date(), "yyyy-MM-dd")
			};
		};
		
		
		$scope.add = function() {
			$scope.success = false;
			console.log("Add");
			
			var data = { 
					value : $scope.newentry.value, 
					unit : $scope.newentry.format.unit,
					dateTime : $scope.newentry.date
			};
			
			if ($scope.newentry.context != null && $scope.newentry.context != "") data.context = $scope.newentry.context;
			
			var envelope = {};
			envelope[$scope.newentry.format.objkey] = [ data ];
			
			$scope.isBusy = true;
			server.createRecord(authToken, $scope.newentry.format.label, "Manually entered "+$scope.newentry.format.label, $scope.newentry.format.content, "measurements", envelope)
			.then(function() { $scope.success = true; $scope.isBusy = false; $scope.reset(); $timeout(function() { $scope.success = false; }, 2000); });			
		};
					
		$scope.reset();
								
	}
]);
