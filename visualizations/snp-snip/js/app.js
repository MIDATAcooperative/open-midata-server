var modules = {}; // will hold the handlers for modules defined in modules/js/handlers.js

var hdcSnpSnip = angular.module('hdcSnpSnip', [ 'midata' ]);
hdcSnpSnip.controller('SnpSnipCtrl', ['$scope', '$http', '$sce', '$location', 'midataPortal',
	function($scope, $http, $sce, $location, midataPortal) {

	    midataPortal.autoresize();
	    
		// export sce to module handlers
		$scope.angular = {'sce': $sce};

		$scope.searches = [];
		$scope.data = {};
		$scope.recordIds = [];
		$scope.records = {};
		$scope.selectedRecord = null;
		$scope.input = null;

		// parse the authorization token from the url
		var authToken = $location.path().split("/")[1];

		// get record ids
		var data = {"authToken": authToken};
		/*$http.post("https://" + window.location.hostname + ":9000/api/visualizations/ids", JSON.stringify(data)).
			success(function(recordIds) {
				$scope.recordIds = recordIds;
				
			}).
			error(function(err) {
				$scope.importFailed = true;
			});
        */
		$scope.recordCount = function() {
			return $scope.recordIds.length;
		}

		$scope.makeActive = function(search) {
			$scope.rs = search.rs;
			for (i in $scope.searches) {
				$scope.searches[i].active = false;
			}
			search.active = true;
		}

		$scope.removeTab = function (search) {
			var index = $scope.searches.map(function(s){return s.rs}).indexOf(search.rs);
			if (index >= 0) {
				$scope.searches.splice(index, 1);
				if ($scope.searches.length && search.active) {
					var newActive = (index === 0) ? 0 : index - 1;
					$scope.makeActive($scope.searches[newActive]);
				}
			}
		};

		$scope.searchUpdate = function() {
			// validate input
			$scope.invalidInput = !isValidRs($scope.input);
			if (!$scope.invalidInput) {
				// check if searched already
				var index = $scope.searches.map(function(search){return search.rs}).indexOf($scope.input);
				if (index > -1) {
					$scope.makeActive($scope.searches[index]);
				} else {
					getData($scope.input);
					prepareSearchResults($scope.input);
				}
			}
		};

		$scope.fixOrientation = function(genotype, orientation) {
			if (orientation === "minus") {
				var swap = {'A' : 'T', 'T' : 'A', 'C' : 'G', 'G' : 'C'};
				var types = genotype.split('');
				return swap[types[0]]+swap[types[1]];
			} else {
				return genotype;
			}
		}

		isValidRs = function(rs) {
			return rs.match(/^rs\d+$/);
		}

		getMetaData = function() {
			var data = {"authToken": authToken};
			data.properties = { /* "_id": $scope.recordIds */ };
			data.fields = ["name", "data" , "data.date", "data.build", "data.buildUrl" ];
			$http.post("https://" + window.location.hostname + ":9000/api/visualizations/records", JSON.stringify(data)).
				success(function(records) {
					for (i in records) {
						var curId = records[i]._id.$oid;
						$scope.recordIds.push(curId);
						$scope.records[curId] = records[i];
					}
					if (records.length) {
						$scope.selectedRecord = $scope.records[records[0]._id.$oid];
					}
					$scope.imported = true;
				}).
				error(function(err) {
					$scope.importFailed = true;
				})
		};
		
		getMetaData();

		getData = function(rsNumber) {
			$scope.loadingRecordDataFailed = false;
			var data = {"authToken": authToken};
			data.properties = { "_id" : $scope.selectedRecord._id };
			data.fields = ["data", "data." + rsNumber];
			$http.post("https://" + window.location.hostname + ":9000/api/visualizations/records", JSON.stringify(data)).
				success(function(records) {
					for (i in records) {
						var curId = records[i]._id.$oid;
						for (rsNum in records[i].data) {
							$scope.records[curId][rsNum] = records[i].data[rsNum];
						}
					}
				}).
				error(function(err) {
					$scope.loadingRecordDataFailed = true;
				});
		}

		prepareSearchResults = function(rs) {
			$scope.loadingDataFailed = false;

			// add tab and make active
			$scope.searches.push({rs: rs, active: true});
			$scope.makeActive($scope.searches[$scope.searches.length - 1]);

			// get the required data if searching for this rs for the first time
			if (!$scope.data.hasOwnProperty(rs)) {
				$scope.data[rs] = {};

				var response;

				// get all data from the node server
				$http.get("https://" + window.location.hostname + ":5000/snp-snip/?rs="+rs).
					success(function(response) {

							$scope.data[rs].resources = Object.keys(response).sort(function(r1, r2){
								if (!(modules[r1] && modules[r2])) {
									return 0;
								} else {
									return (modules[r1].position < modules[r2].position) ? -1 : 1;
								}
							});
							var resources = Object.keys(response).sort(function(r1, r2){
								if (!(modules[r1] && modules[r2])) {
									return 0;
								} else {
									return (modules[r1].priority < modules[r2].priority) ? 1 : -1;
								}
							});

							// prepare the data received from the server
							for (i in resources) {
								if (modules.hasOwnProperty(resources[i])) {
									modules[resources[i]].handler($scope, response[resources[i]]);
								}
							}

							// set orientation
							$scope.data[rs].genotypes = {};
							if ($scope.data[rs].snpediaOrientation === "minus") {
								$scope.data[rs].orientation = "minus";
							} else {
								$scope.data[rs].orientation = "plus";
							}
					}).
					error(function(err) {
						$scope.loadingDataFailed = true;
					});
			}
		}
	}
]);
