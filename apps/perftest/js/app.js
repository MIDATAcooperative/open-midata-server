var jsonRecords = angular.module('jsonRecords', []);
jsonRecords.factory('server', [ '$http', function($http) {
	
	var service = {};
	
	service.createRecord = function(authToken, name, description, content, format, data) {
		// construct json
		var data = {
			"authToken": authToken,
			"data": angular.toJson(data),
			"name": name,
			"format" : format,
			"content" : content,
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
		
		// init
		$scope.errors = {};
		$scope.data = {};
				
		// get authorization token
		var authToken = $location.path().split("/")[1];
		$scope.authToken = authToken;
		$scope.setup = { numCreate : 1, format : "Json" };
		
		$scope.data = { a : "This is a test record that has an average length. ",
				        b : "This is a test record that has an average length. ",
				        c : "This is a test record that has an average length. ",
				        d : "This is a test record that has an average length. ",
				        e : "This is a test record that has an average length. ",
				        f : "This is a test record that has an average length. "				        
				       };	
		$scope.success = false;
		
		$scope.init = function() {
			
		};
		
		$scope.execute = function() {
			var f = function(i) { return function() { return server.createRecord(authToken, "Record "+i, null, $scope.setup.content, $scope.setup.format, $scope.data); } };
			var q = null;
			for (var i=0;i < $scope.setup.numCreate;i++) {
				q = (q != null) ? q.then(f(i)) : f(i)();
			}
			q.then(function() { $scope.success = true; });			
		};
					
		$scope.init();
								
	}
]);
