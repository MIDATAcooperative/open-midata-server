var records = angular.module('records', ['filters', 'date']);
records.controller('RecordsCtrl', ['$scope', '$http', 'filterService', 'dateService', function($scope, $http, filterService, dateService) {
	
	// init
	$scope.error = null;
	$scope.loadingSharing = true;
	$scope.shared = null;
	$scope.changed = {};
	$scope.loadingApps = true;
	$scope.loadingRecords = true;
	$scope.userId = null;
	$scope.apps = [];
	$scope.records = [];	
	$scope.spaces = [];
	$scope.circles = [];	
	$scope.participations = [];
	$scope.activeRecord = null;
		
	// get current user
	$http(jsRoutes.controllers.Users.getCurrentUser()).
		success(function(userId) {
			$scope.userId = userId;
			$scope.getApps(userId);
			$scope.getRecords(userId);
		});
	
	// get apps
	$scope.getApps = function(userId) {
		var properties = {"_id": userId};
		var fields = ["apps"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) {
				$scope.getAppDetails(users[0].apps);
			}).
			error(function(err) { $scope.error = "Failed to load apps: " + err; });
	};
	
	// get name and type for app ids
	$scope.getAppDetails = function(appIds) {
		var properties = {"_id": appIds};
		var fields = ["name", "type"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data)).
			success(function(apps) {
				$scope.apps = apps;
				$scope.loadingApps = false;
			}).
			error(function(err) { $scope.error = "Failed to load apps: " + err; });
	};
	
	// get records
	$scope.getRecords = function(userId) {
		$http(jsRoutes.controllers.Records.getVisibleRecords()).
			success(function(data) {
				$scope.records = data;
				$scope.prepareRecords();
				$scope.initFilterService($scope.records, userId);
				$scope.loadingRecords = false;
			}).
			error(function(err) { $scope.error = "Failed to load records: " + err; });
	};
	
	// prepare records: clip time from created and add JS date
	$scope.prepareRecords = function() {
		_.each($scope.records, function(record) {
			var date = record.created.split(" ")[0];
			record.created = {"name": date, "value": dateService.toDate(date)};
		});
	};
	
	// initialize filter service
	$scope.initFilterService = function(records, userId) {
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
	};
	
	// go to record creation/import dialog
	$scope.createOrImport = function(app) {
		if (app.type === "create") {
			window.location.href = jsRoutes.controllers.Records.create(app._id.$oid).url;
		} else {
			window.location.href = jsRoutes.controllers.Records.importRecords(app._id.$oid).url;
		}
	};
	
	// show record details
	$scope.showDetails = function(record) {
		window.location.href = jsRoutes.controllers.Records.details(record.id).url;
	};
	
	// check whether the user is the owner of the record
	$scope.isOwnRecord = function(record) {
		return $scope.userId.$oid === record.owner.$oid;
	};
	
	// activate record (spaces or circles of this record are being looked at)
	$scope.activate = function(record) {
		$scope.activeRecord = record;		
	}
	
	// get active record
	$scope.getActiveRecord = function() {
		return $scope.activeRecord;
	};
	
	$scope.loadShared = function(type) {
		if ($scope.shared == null) {
			$http.get(jsRoutes.controllers.Records.getSharingInfo().url).
			success(function(data) {			
				$scope.shared = data.shared;
				$scope.circles = data.circles;
				$scope.spaces = data.spaces;
				$scope.participations = data.participations;				
				$scope.loadingSharing = false;
				$scope.prepare(type);
			}).
			error(function(err) {
				$scope.error = "Failed to load Sharing: " + err;
				$scope.loadingSharing = false;
			});				
		} else $scope.prepare(type);
	};
	
	$scope.updateShared = function(type) {
		var recs;
		var data = { records:[], started:[], stopped:[], type:type };
		if ($scope.activeRecord != null) {
			data.records = [ $scope.activeRecord.id ];
			recs = [ $scope.activeRecord._id.$oid ];
		} else {
		    data.records = _.chain($scope.filteredRecords).pluck('id').value();
		    recs = _.chain($scope.filteredRecords).pluck('_id').pluck('$oid').value();
		}
		
		_.each($scope[type], function(obj) {
		    if (obj.checked && !obj.wasChecked) data.started.push(obj._id.$oid);
		    else if (!obj.checked && obj.wasChecked) data.stopped.push(obj._id.$oid);
		});
		
		$http.post(jsRoutes.controllers.Records.updateSharing().url, JSON.stringify(data)).
		success(function() {
			$scope.error = null;
			_.each(recs, function(record) {
			  _.each(data.started, function(started) {
				  var l = $scope.shared[type][started];
				  if (l.indexOf(record) < 0) { l.push(record); }
			  });
			  _.each(data.stopped, function(stopped) {
				  $scope.removeRecordIfPresent($scope.shared[type][stopped], record);
			  });

		    });
		}).
		error(function(err) { $scope.error = "Failed to update sharing: " + err; });
	}
		
	
	// set checkbox variable 'checked' for spaces that contain the currently
	// active record
	$scope.prepare = function(type) {
		_.each($scope[type], function(obj) { obj.checked = obj.wasChecked = false; });
		var activeRecord = $scope.getActiveRecord();		
		if (activeRecord) {
			var objsWithRecord = _.filter($scope[type], function(obj) { return $scope.containsRecord($scope.shared[type][obj._id.$oid], activeRecord._id.$oid); });
			_.each(objsWithRecord, function(obj) { obj.checked = obj.wasChecked = true; });
		}
	};
		
	
	// helper method for contains
	$scope.containsRecord = function(recordIdList, recordId) {		
		return _.contains(recordIdList, recordId);
	};
	
		
	// helper method for remove (in cases where object equality doesn't work)
	$scope.removeRecordIfPresent = function(recordIdList, recordId) {
	   var idx = recordIdList.indexOf(recordId);
	   if (idx >= 0) recordIdList.splice(idx, 1);		
	};
		
	
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
