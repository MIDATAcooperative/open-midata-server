var records = angular.module('records', ['filters', 'date']);
records.controller('RecordsCtrl', ['$scope', '$http', 'filterService', 'dateService', function($scope, $http, filterService, dateService) {
	
	// init
	$scope.error = null;
	$scope.loadingApps = true;
	$scope.loadingRecords = true;
	$scope.userId = null;
	$scope.apps = [];
	$scope.records = [];
	$scope.loadingSpaces = false;
	$scope.spaces = [];
	$scope.loadingCircles = false;
	$scope.circles = [];
	$scope.loadingStudies = false;
	$scope.participations = [];
	
	// get current user
	$http(jsRoutes.controllers.Users.getCurrentUser()).
		success(function(userId) {
			$scope.userId = userId;
			getApps(userId);
			getRecords(userId);
		});
	
	// get apps
	getApps = function(userId) {
		var properties = {"_id": userId};
		var fields = ["apps"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) {
				getAppDetails(users[0].apps);
			}).
			error(function(err) { $scope.error = "Failed to load apps: " + err; });
	}
	
	// get name and type for app ids
	getAppDetails = function(appIds) {
		var properties = {"_id": appIds};
		var fields = ["name", "type"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data)).
			success(function(apps) {
				$scope.apps = apps;
				$scope.loadingApps = false;
			}).
			error(function(err) { $scope.error = "Failed to load apps: " + err; });
	}
	
	// get records
	getRecords = function(userId) {
		$http(jsRoutes.controllers.Records.getVisibleRecords()).
			success(function(data) {
				$scope.records = data;
				prepareRecords();
				initFilterService($scope.records, userId);
				$scope.loadingRecords = false;
			}).
			error(function(err) { $scope.error = "Failed to load records: " + err; });
	}
	
	// prepare records: clip time from created and add JS date
	prepareRecords = function() {
		_.each($scope.records, function(record) {
			var date = record.created.split(" ")[0];
			record.created = {"name": date, "value": dateService.toDate(date)};
		});
	}
	
	// initialize filter service
	initFilterService = function(records, userId) {
		$scope.filterChanged = function(serviceId) { /* do nothing, automatically updated by filters */ }
		var context = "records";
		var serviceId = 0;
		filterService.init(context, serviceId, records, userId, $scope.filterChanged);
		$scope.filters = filterService.filters;
		$scope.filterError = filterService.error;
		$scope.addFilter = filterService.addFilter;
		$scope.removeFilter = filterService.removeFilter;
		
		// set filters if any are defined in the url
		if (window.location.pathname.indexOf("filters") !== -1) {
			var split = window.location.pathname.split("/");
			for (var i = 4; i < split.length - 2; i += 3) {
				var name = split[i];
				var arg1 = split[i+1];
				var arg2 = split[i+2];

				// wrap in function so that references are kept through promises/timeouts
				(function(name, arg1, arg2) {
					$scope.addFilter(serviceId);
					var filter = _.last($scope.filters[serviceId].current);
					filter.property = _.findWhere($scope.filters[serviceId].properties, {"name": name});
					if (filter.property.type === "point") {
						filter.operator = $scope.filters[serviceId].operators[arg1];
						$scope.filters[serviceId].promises[filter.property.name].then(function(values) {
							filter.value = _.find(values, function(value) { return value._id.$oid === arg2; });
						});
					} else if (filter.property.type === "range") {
						filter.from = {"name": arg1, "value": dateService.toDate(arg1)};
						filter.to = {"name": arg2, "value": dateService.toDate(arg2)};
						filterService.setSlider(filter, filter.from, filter.to);
					}
				})(name, arg1, arg2);
			}
		}
	}
	
	// go to record creation/import dialog
	$scope.createOrImport = function(app) {
		if (app.type === "create") {
			window.location.href = jsRoutes.controllers.Records.create(app._id.$oid).url;
		} else {
			window.location.href = jsRoutes.controllers.Records.importRecords(app._id.$oid).url;
		}
	}
	
	// show record details
	$scope.showDetails = function(record) {
		window.location.href = jsRoutes.controllers.Records.details(record._id.$oid).url;
	}
	
	// check whether the user is the owner of the record
	$scope.isOwnRecord = function(record) {
		return $scope.userId.$oid === record.owner.$oid;
	}
	
	// activate record (spaces or circles of this record are being looked at)
	$scope.activate = function(record) {
		_.each($scope.records, function(record) { record.active = false; });
		if (record) { record.active = true; }
	}
	
	// get active record
	getActiveRecord = function() {
		return _.find($scope.records, function(record) { return record.active; });
	}
	
	// load spaces
	$scope.loadSpaces = function() {
		if ($scope.spaces.length === 0) {
			$scope.loadingSpaces = true;
			var properties = {"owner": $scope.userId};
			var fields = ["name", "records", "order"];
			var data = {"properties": properties, "fields": fields};
			$http.post(jsRoutes.controllers.Spaces.get().url, JSON.stringify(data)).
				success(function(spaces) {
					$scope.error = null;
					$scope.spaces = spaces;
					$scope.loadingSpaces = false;
					prepareSpaces();
				}).
				error(function(err) {
					$scope.error = "Failed to load spaces: " + err;
					$scope.loadingSpaces = false;
				});
		} else {
			prepareSpaces();
		}
	}
	
	// set checkbox variable 'checked' for spaces that contain the currently
	// active record
	prepareSpaces = function() {
		_.each($scope.spaces, function(space) { space.checked = false; });
		var activeRecord = getActiveRecord();
		if (activeRecord) {
			var spacesWithRecord = _.filter($scope.spaces, function(space) { return containsRecord(space.records, activeRecord._id); });
			_.each(spacesWithRecord, function(space) { space.checked = true; });
		}
	}
	
	// load circles
	$scope.loadCircles = function() {
		if ($scope.circles.length === 0) {
			$scope.loadingCircles = true;
			var properties = {"owner": $scope.userId};
			var fields = ["name", "shared", "order"];
			var data = {"properties": properties, "fields": fields};
			$http.post(jsRoutes.controllers.Circles.get().url, JSON.stringify(data)).
				success(function(circles) {
					$scope.error = null;
					$scope.circles = circles;
					$scope.loadingCircles = false;
					prepareCircles();
				}).
				error(function(err) {
					$scope.error = "Failed to load circles: " + err;
					$scope.loadingCircles = false;
				});
		} else {
			prepareCircles();
		}
	}
	
	// set checkbox variable 'checked' for circles that the currently active
	// record is shared with
	prepareCircles = function() {
		_.each($scope.circles, function(circle) { circle.checked = false; });
		var activeRecord = getActiveRecord();
		if (activeRecord) {
			var circlesWithRecord = _.filter($scope.circles, function(circle) { return containsRecord(circle.shared, activeRecord._id); });
			_.each(circlesWithRecord, function(circle) { circle.checked = true; });
		}
	}
	
	// load study participation
	$scope.loadStudies = function() {
		if ($scope.participations.length === 0) {
			$scope.loadingStudies = true;
			//var properties = {"owner": $scope.userId};
			//var fields = ["name", "shared", "order"];
			//var data = {"properties": properties, "fields": fields};
			$http.get(jsRoutes.controllers.members.Studies.list().url).
				success(function(participations) {
					$scope.error = null;
					$scope.participations = participations;
					$scope.loadingStudies = false;
					prepareStudies();
				}).
				error(function(err) {
					$scope.error = "Failed to load studies: " + err;
					$scope.loadingStudies = false;
				});
		} else {
			prepareStudies();
		}
	}
	
	// set checkbox variable 'checked' for circles that the currently active
	// record is shared with
	prepareStudies = function() {
		_.each($scope.participations, function(participation) { participation.checked = false; });
		var activeRecord = getActiveRecord();
		if (activeRecord) {
			var circlesWithRecord = _.filter($scope.circles, function(circle) { return containsRecord(circle.shared, activeRecord._id); });
			_.each(circlesWithRecord, function(circle) { circle.checked = true; });
		}
	}
	
	// helper method for contains
	containsRecord = function(recordIdList, recordId) {
		var ids = _.map(recordIdList, function(element) { return element.$oid; });
		return _.contains(ids, recordId.$oid);
	}
	
	// update spaces for active record
	$scope.updateSpaces = function() {
		var activeRecord = getActiveRecord();
		var checkedSpaces = _.filter($scope.spaces, function(space) { return space.checked; });
		var spaceIds = _.map(checkedSpaces, function(space) { return space._id; });
		var data = {"spaces": spaceIds};
		if (activeRecord) {
			$http.post(jsRoutes.controllers.Records.updateSpaces(activeRecord._id.$oid).url, JSON.stringify(data)).
				success(function() {
					$scope.error = null;
					_.each($scope.spaces, function(space) {
						removeRecordIfPresent(space.records, activeRecord._id);
					});
					_.each(checkedSpaces, function(space) { space.records.push(activeRecord._id); });
				}).
				error(function(err) { $scope.error = "Failed to update spaces: " + err; });
		} else {
			data.records = _.pluck($scope.filteredRecords, '_id');
			$http.post(jsRoutes.controllers.Records.showInSpaces().url, JSON.stringify(data)).
				success(function() {
					$scope.error = null;
					_.each(checkedSpaces, function(space) { space.records = _.union(space.records, data.records); });
				}).
				error(function(err) { $scope.error = "Failed to add records to space(s): " + err; });
		}
	}
	
	// helper method for remove (in cases where object equality doesn't work)
	removeRecordIfPresent = function(recordIdList, recordId) {
		_.each(recordIdList, function(element) {
			if (element.$oid === recordId.$oid) {
				recordIdList.splice(recordIdList.indexOf(element));
			}
		});
	}
	
	// update circles for active record
	$scope.updateCircles = function() {
		var activeRecord = getActiveRecord();
		var circlesChecked = _.filter($scope.circles, function(circle) { return circle.checked; });
		if (activeRecord) {
			var circlesWithRecord = _.filter($scope.circles, function(circle) { return containsRecord(circle.shared, activeRecord._id); });
			var circleIdsChecked = _.map(circlesChecked, function(circle) { return circle._id.$oid; });
			var circleIdsWithRecord = _.map(circlesWithRecord, function(circle) { return circle._id.$oid; });
			var idsStarted = _.difference(circleIdsChecked, circleIdsWithRecord);
			var idsStopped = _.difference(circleIdsWithRecord, circleIdsChecked);
			// construct objectId objects again...
			var circleIdsStarted = _.map(idsStarted, function(id) { return {"$oid": id}; });
			var circleIdsStopped = _.map(idsStopped, function(id) { return {"$oid": id}; });
			var data = {"started": circleIdsStarted, "stopped": circleIdsStopped};
			$http.post(jsRoutes.controllers.Records.updateSharing(activeRecord._id.$oid).url, JSON.stringify(data)).
				success(function() {
					$scope.error = null;
					_.each($scope.circles, function(circle) {
						if (containsRecord(circleIdsStarted, circle._id)) {
							circle.shared.push(activeRecord._id);
						} else if (containsRecord(circleIdsStopped, circle._id)) {
							removeRecordIfPresent(circle.shared, activeRecord._id);
						}
					});
				}).
				error(function(err) { $scope.error = "Failed to update circles: " + err; });
		} else {
			var data = {};
			data.circles = _.pluck(circlesChecked, '_id');
			data.records = _.pluck($scope.filteredRecords, '_id');
			$http.post(jsRoutes.controllers.Records.shareWithCircles().url, JSON.stringify(data)).
				success(function() {
					$scope.error = null;
					_.each(circlesChecked, function(circle) { circle.shared = _.union(circle.shared, data.records)});
				}).
				error(function(err) { $scope.error = "Failed to share records with circle(s): " + err; });
		}
	}
	
}]);

// record creation
var createRecords = angular.module('createRecords', []);
createRecords.controller('CreateRecordsCtrl', ['$scope', '$http', '$sce', function($scope, $http, $sce) {
	
	// init
	$scope.error = null;
	
	// get app id (format: /records/create/:appId)
	var appId = window.location.pathname.split("/")[4];
	
	// get app url
	$http(jsRoutes.controllers.Apps.getUrl(appId)).
		success(function(url) {
			$scope.error = null;
			$scope.url = $sce.trustAsResourceUrl(url);
		}).
		error(function(err) { $scope.error = "Failed to load app: " + err; });
	
}]);

// importing records
var importRecords = angular.module('importRecords', []);
importRecords.controller('ImportRecordsCtrl', ['$scope', '$http', '$sce', function($scope, $http, $sce) {
	
	// init
	$scope.error = null;
	$scope.message = null;
	$scope.loading = true;
	$scope.authorizing = false;
	$scope.authorizingOAuth1 = false;
	$scope.authorized = false;
	$scope.finished = false;
	var app = {};
	var authorizationUrl = null;
	var authWindow = null;
	var userId = null;
	
	// get app id (format: /records/import/:appId)
	var appId = window.location.pathname.split("/")[4];
	
	// get current user
	$http(jsRoutes.controllers.Users.getCurrentUser()).
		success(function(uId) {
			userId = uId.$oid;
			checkAuthorized();
		});
	
	// check whether we have been authorized already
	checkAuthorized = function() {
		var properties = {"_id": {"$oid": userId}};
		var fields = ["tokens." + appId];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) {
				var tokens = users[0].tokens[appId];
				if(!_.isEmpty(tokens)) {
					$scope.authorized = true;
					$scope.message = "Loading app...";
					$scope.loading = false;
					loadApp();
				} else {
					loadAppDetails();
				}
			}).
			error(function(err) {
				$scope.error = "Failed to load user tokens: " + err;
				$scope.loading = false;
			});
	}
	
	// get the app information
	loadAppDetails = function() {
		var properties = {"_id": {"$oid": appId}};
		var fields = ["filename", "name", "type", "authorizationUrl", "consumerKey", "scopeParameters"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data)).
			success(function(apps) {
				app = apps[0];
				$scope.appName = app.name;
				$scope.message = "The app is not authorized to import data on your behalf yet.";
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.error = "Failed to load apps: " + err;
				$scope.loading = false;
			});
	}

	// start authorization procedure
	$scope.authorize = function() {
		$scope.authorizing = true;
		$scope.message = "Authorization in progress...";
		var redirectUri = "https://" + window.location.hostname + ":9000/records/redirect/" + app._id.$oid;
		if (app.type === "oauth2") {
			var parameters = "?response_type=code" + "&client_id=" + app.consumerKey + "&scope=" + app.scopeParameters +
				"&redirect_uri=" + redirectUri;
			authWindow = window.open(app.authorizationUrl + encodeURI(parameters));
			window.addEventListener("message", onAuthorized);
		} else if (app.type === "oauth1") {
			$http(jsRoutes.controllers.Apps.getRequestTokenOAuth1(appId)).
				success(function(authUrl) {
					authorizationUrl = authUrl;
					$scope.authorizingOAuth1 = true;
				}).
				error(function(err) {
					$scope.error = "Failed to get the request token.";
					$scope.authorizing = false;
				})
		} else {
			$scope.error = "App type not supported yet.";
			$scope.authorizing = false;
		}
	}
	
	// need to factor this out, so that the pop-up doesn't get blocked
	$scope.authorizeOAuth1 = function() {
		authWindow = window.open(authorizationUrl);
		window.addEventListener("message", onAuthorized);
	}
	
	// authorization granted
	onAuthorized = function(event) {
		$scope.authorizingOAuth1 = false;
		var message = null;
		var error = null;
		if (event.origin === "https://" + window.location.hostname + ":9000" && event.source === authWindow) {
			var arguments = event.data.split("&");
			var keys = _.map(arguments, function(argument) { return argument.split("=")[0]; });
			var values = _.map(arguments, function(argument) { return argument.split("=")[1]; });
			var params = _.object(keys, values);
			if (_.has(params, "error")) {
				error = "The following error occurred: " + params.error + ". Please try again.";
			} else if (_.has(params, "code")) {
				message = "User authorization granted. Requesting access token...";
				requestAccessToken(params.code);
			} else if (_.has(params, "oauth_verifier")) {
				message = "User authorization granted. Requesting access token...";
				requestAccessToken(params.oauth_verifier);
			} else {
				error = "An unknown error occured while requesting authorization. Please try again."
			}
		} else {
			error = "User authorization failed. Please try again.";
		}
		
		// update message with scope.apply because angular doesn't recognize the change 
		$scope.$apply(function() {
			$scope.message = message;
			$scope.error = error;
			if (error) {
				$scope.authorizing = false;
			}
		});
		authWindow.close();
	}
	
	// request access token
	requestAccessToken = function(code) {
		var data = {"code": code};
		var requestTokensUrl = null; 
		if (app.type === "oauth2") {
			requestTokensUrl = jsRoutes.controllers.Apps.requestAccessTokenOAuth2(appId).url;
		} else if (app.type === "oauth1") {
			requestTokensUrl = jsRoutes.controllers.Apps.requestAccessTokenOAuth1(appId).url;
		}
		$http.post(requestTokensUrl, JSON.stringify(data)).
			success(function() {
				$scope.authorized = true;
				$scope.authorizing = false;
				$scope.message = "Loading app...";
				loadApp();
			}).
			error(function(err) {
				$scope.error = "Requesting access token failed: " + err;
				$scope.authorizing = false;
			});
	}
	
	// load the app into the iframe
	loadApp = function() {
		// get app url
		$http(jsRoutes.controllers.Apps.getUrl(appId)).
			success(function(url) {
				$scope.error = null;
				$scope.message = null;
				$scope.url = $sce.trustAsResourceUrl(url);
				$scope.loaded = true;
			}).
			error(function(err) { $scope.error = "Failed to load app: " + err; });
	}
}]);
