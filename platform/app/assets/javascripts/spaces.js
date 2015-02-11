var spaces = angular.module('spaces', []);
spaces.controller('SpacesCtrl', ['$scope', '$http', '$sce', function($scope, $http, $sce) {
	
	// init
	$scope.error = null;
	$scope.userId = null;
	$scope.loading = true;
	$scope.spaces = [];
	$scope.add = {};
	$scope.loadingVisualizations = false;
	$scope.visualizations = [];
	$scope.searching = false;
	
	// get current user
	$http(jsRoutes.controllers.Users.getCurrentUser()).
		success(function(userId) {
			$scope.userId = userId;
			getSpaces();
		});
	
	// get spaces and make given space active (if one is given; first otherwise)
	getSpaces = function(userId) {
		var properties = {"owner": $scope.userId};
		var fields = ["name", "records", "visualization", "order"]
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Spaces.get().url, JSON.stringify(data)).
			success(function(spaces) {
				$scope.spaces = spaces;
				if ($scope.spaces.length > 0) {
					var active = window.location.pathname.split("/")[3];
					if (active) {
						$scope.makeActive(_.find($scope.spaces, function(space) { return space._id.$oid === active; }));
					} else {
						$scope.makeActive($scope.spaces[0]);
					}
				}
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.error = "Failed to load spaces: " + err;
				$scope.loading = false;
			});
	}
	
	// make space tab active
	$scope.makeActive = function(space) {
		_.each($scope.spaces, function(space) { space.active = false; });
		space.active = true;
		
		// lazily load url, authToken and visualization
		if (!space.trustedUrl) {
			loadBaseUrl(space);
		}
	}
	
	// load visualization url for given space
	loadBaseUrl = function(space) {
		$http(jsRoutes.controllers.Visualizations.getUrl(space.visualization.$oid)).
			success(function(url) {
				space.baseUrl = url;
				getAuthToken(space);
			}).
			error(function(err) { $scope.error = "Failed to load space '" + space.name + "': " + err; });
	}
	
	// get the authorization token for the current space
	getAuthToken = function(space) {
		$http(jsRoutes.controllers.Spaces.getToken(space._id.$oid)).
			success(function(authToken) {
				space.completedUrl = space.baseUrl.replace(":authToken", authToken);
				reloadIframe(space);
			}).
			error(function(err) { $scope.error = "Failed to get the authorization token: " + err; });
	}
	
	// reload the iframe displaying the visualization
	reloadIframe = function(space) {
		space.trustedUrl = $sce.trustAsResourceUrl(space.completedUrl);

		// have to detach and append again to force reload; just setting src didn't do the trick
		var iframe = $("#iframe-" + space._id.$oid).detach();
		// set src attribute of iframe to avoid creating an entry in the browser history
		iframe.attr("src", space.trustedUrl);
		$("#iframe-placeholder-" + space._id.$oid).append(iframe);
	}
	
	// start side-by-side display of current visualization
	$scope.startCompare = function(space) {
		// copy relevant properties
		space.copy = {};
		space.copy._id = {"$oid": "copy-" + space._id.$oid};
		space.copy.name = space.name;
		space.copy.completedUrl = space.completedUrl;
		
		// detach/attach iframe to force loading
		reloadIframe(space.copy);
		
		// start side-by-side display
		space.compare = true;
	}

	// end side-by-side display of current visualization
	$scope.endCompare = function(space) {
		space.compare = false;
		space.copy = {};
	}
	
	// load all installed visualizations (for creating a new space)
	$scope.loadVisualizations = function() {
		if ($scope.visualizations.length === 0) {
			$scope.loadingVisualizations = true;
			var properties = {"_id": $scope.userId};
			var fields = ["visualizations"];
			var data = {"properties": properties, "fields": fields};
			$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
				success(function(users) { getVisualizations(users[0].visualizations); }).
				error(function(err) {
					$scope.error = "Failed to load visualizations: " + err;
					$scope.loadingVisualizations = false;
				});
		}
	}
	
	getVisualizations = function(ids) {
		var properties = {"_id": ids};
		var fields = ["name"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Visualizations.get().url, JSON.stringify(data)).
			success(function(visualizations) {
				$scope.error = null;
				$scope.visualizations = visualizations;
				$scope.loadingVisualizations = false;
			}).
			error(function(err) {
				$scope.error = "Failed to load visualizations: " + err;
				$scope.loadingVisualizations = false;
			});
	}
	
	// add a space
	$scope.addSpace = function() {
		// dismiss modal
		$("#spaceModal").modal("hide");
		
		// check user input
		if (!$scope.add.name) {
			$scope.error = "Please provide a name for your new space.";
			return;
		} else if (!$scope.add.visualization) {
			$scope.error = "Please select a visualization for your new space.";
			return;
		}
		
		// send the request
		var data = {"name": $scope.add.name, "visualization": $scope.add.visualization.$oid};
		$http.post(jsRoutes.controllers.Spaces.add().url, JSON.stringify(data)).
			success(function(space) {
				$scope.error = null;
				$scope.add = {};
				$scope.spaces.push(space);
				$scope.makeActive(space);
			}).
			error(function(err) { $scope.error = "Failed to add space '" + data.name + "': " + err; });
	}
	
	// delete a space
	$scope.deleteSpace = function(space) {
		$http(jsRoutes.controllers.Spaces["delete"](space._id.$oid)).
			success(function() {
				$scope.error = null;
				$scope.spaces.splice($scope.spaces.indexOf(space), 1);
				if ($scope.spaces.length > 0) {
					$scope.spaces[0].active = true;
				}
			}).
			error(function(err) { $scope.error = "Failed to delete space '" + space.name + "': " + err; });
	}
	
	// check whether record is not already in active space
	$scope.isntInSpace = function(record) {
		var activeSpace = _.find($scope.spaces, function(space) { return space.active; });
		return !containsRecord(activeSpace.records, record._id);
	}
	
	// helper method for contains
	containsRecord = function(recordIdList, recordId) {
		var ids = _.map(recordIdList, function(element) { return element.$oid; });
		return _.contains(ids, recordId.$oid);
	}
	
	// search for records
	$scope.searchRecords = function(space) {
		$scope.searching = true;
		var query = space.recordQuery;
		if (query) {
		$http(jsRoutes.controllers.Records.search(query)).
			success(function(records) {
				$scope.error = null;
				$scope.foundRecords = records;
				$scope.searching = false;
			}).
			error(function(err) {
				$scope.error = "Record search failed: " + err;
				$scope.searching = false;
			});
		}
	}
	
	// add a record
	$scope.addRecords = function(space) {
		// get the ids of the records that should be added to the space
		var recordsToAdd = _.filter($scope.foundRecords, function(record) { return record.checked; });
		var recordIds = _.map(recordsToAdd, function(record) { return record._id; });
		
		var data = {"records": recordIds};
		$http.post(jsRoutes.controllers.Spaces.addRecords(space._id.$oid).url, JSON.stringify(data)).
			success(function() {
				$scope.error = null;
				space.recordQuery = null;
				$scope.foundRecords = [];
				reloadIframe(space);
			}).
			error(function(err) { $scope.error = "Failed to add records: " + err; });
	}
	
}]);