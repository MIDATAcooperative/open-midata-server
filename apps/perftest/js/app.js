var jsonRecords = angular.module('jsonRecords', []);
jsonRecords.factory('server', [ '$http', function($http) {
	
	var service = {};
	
	var rand = function(min,max) {
	   return Math.floor((Math.random() * (max-min)) + min); 
	};
	
	var twodigit = function(d) {
		return d < 10 ? ("0" + d) : d;
	}
	
	service.createRecord = function(authToken, name, description, content, format, data) {
		// construct json
		var data = {
			"authToken": authToken,
			"data": angular.toJson(data),
			"name": name,
			"format" : format,
			"content" : content,
			"created-override" : (rand(2000,2015) +"-" + twodigit(rand(1,12)) + "-" + twodigit(rand(1,28))),
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
jsonRecords.controller('CreateCtrl', ['$scope', '$http', '$location', '$filter', 'server',
	function($scope, $http, $location, $filter, server) {
		
	    var rand = function(min,max) {
		   return Math.floor((Math.random() * (max-min)) + min); 
		};
		
		var twodigit = function(d) {
			return d < 10 ? ("0" + d) : d;
		}
	
		// init
		$scope.errors = {};
		$scope.data = {};
				
		// get authorization token
		var authToken = $location.path().split("/")[1];
		$scope.authToken = authToken;
		$scope.setup = { numCreate : 1, format : "Json" };
		
		$scope.data = { "weight": [ { "value": rand(50,80), "unit": "kg", "dateTime": (rand(2000,2015) +"-" + twodigit(rand(1,12)) + "-" + twodigit(rand(1,28))) } ] }
		$scope.success = false;
		
		$scope.init = function() {
			
		};
		
		$scope.execute = function() {
			var f = function(i) { return function() {
				$scope.data = { "weight": [ { "value": rand(50,80), "unit": "kg", "dateTime": (rand(2000,2015) +"-" + twodigit(rand(1,12)) + "-" + twodigit(rand(1,28))) } ] };
				return server.createRecord(authToken, "Record "+i, null, $scope.setup.content, $scope.setup.format, $scope.data); 
			} };
			var q = null;
			for (var i=0;i < $scope.setup.numCreate;i++) {
				q = (q != null) ? q.then(f(i)) : f(i)();
			}
			q.then(function() { $scope.success = true; });			
		};
					
		$scope.init();
								
	}
]);
