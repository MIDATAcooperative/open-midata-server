var jsonRecords = angular.module('jsonRecords', []);
jsonRecords.factory('server', [ '$http', function($http) {
	
	var service = {};
	
	service.createRecord = function(authToken, name, description, format, data) {
		// construct json
		var data = {
			"authToken": authToken,
			"data": angular.toJson(data),
			"name": name,
			"format" : format,
			"description": (description || "")
		};
		
		// submit to server
		return $http.post("https://" + window.location.hostname + ":9000/api/apps/create", data);
	};
	
	service.createConversion = function(authToken, name, description, format, data, appendToId) {
		// construct json
		var data = {
			"authToken": authToken,
			"data": angular.toJson(data),
			"name": name,
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
jsonRecords.controller('CreateCtrl', ['$scope', '$http', '$location', '$filter', 'server',
	function($scope, $http, $location, $filter, server) {
		
		// init
		$scope.errors = {};
				
		$scope.formats = [
		   {
				label : "Weight",
				unit : "kg",
				format : "Body Weight/Fitbit",
				objkey : "weight"
		   },
		   {
			   label : "Height",
			   unit : "cm",
			   format : "height",
			   objkey : "height"
		   },
		   {
			   label : "Steps",
			   unit : "steps",
			   format : "Steps/Fitbit",
			   objkey : "activities-steps"
		   },
		   {
			   label : "Heartrate",
			   unit : "bpm",
			   format : "heartrate",
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
			
			server.createRecord(authToken, $scope.newentry.format.label, "Manually entered "+$scope.newentry.format.label, $scope.newentry.format.format, envelope)
			.then(function() { $scope.success = true; $scope.reset(); });			
		};
					
		$scope.reset();
								
	}
]);
