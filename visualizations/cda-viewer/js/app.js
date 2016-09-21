var cda = angular.module('cda', [ 'midata' ]);
cda.controller('CDACtrl', ['$scope', '$http', '$location', 'midataPortal',
	function($scope, $http, $location, midataPortal) {
		
	    midataPortal.autoresize();
		// init
		$scope.loading = 1;
		$scope.error = null;
		$scope.records = [];

		// parse the authorization token from the url
		var authToken = $location.path().split("/")[1];

		// get the ids of the records assigned to this space
		var data = {"authToken": authToken};
				
		// get the data for the records in this space
		$scope.getRecords = function() {
			var data = {"authToken": authToken};
			data.properties = { "format" : "cda" };
			data.fields = ["_id", "data"];
			$http.post("https://" + window.location.hostname + ":9000/v1/plugin_api/records/search", JSON.stringify(data)).
				success(function(records) {
					$scope.records = records;					
					angular.forEach(records, function(record) { $scope.displayCDA(record._id); });
					$scope.loading--;
				}).
				error(function(err) {
					$scope.error = "Failed to load records: " + err;
					$scope.loading = false;
				});
		};
		
		$scope.loadXSL = function() {
			$http.get("CDA.xsl").then(function(result) {
				console.log(result.data);
				var parser = new DOMParser();
				$scope.xsl = parser.parseFromString(result.data, "text/xml");				
				$scope.getRecords();
			});
		};
		
		$scope.displayCDA = function(id) {
			$scope.loading++;
			var data = {"authToken": authToken, "_id" : id };
			$http.post("https://" + window.location.hostname + ":9000/v1/plugin_api/records/file", JSON.stringify(data)).
			then(function(result) {				
				var parser = new DOMParser();				
				var xml = parser.parseFromString(result.data, "text/xml");				
				var xsltProcessor = new XSLTProcessor();
				xsltProcessor.importStylesheet($scope.xsl);
				var  resultDocument = xsltProcessor.transformToFragment(xml, document);
				document.getElementById("myxml").appendChild(resultDocument);
				$scope.loading--;								
			}, function() { $scope.loading--; });
		}
		
		$scope.loadXSL();
	}
]);
