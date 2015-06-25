var details = angular.module('details', [ 'services' ]);
details.controller('RecordCtrl', ['$scope', '$http', '$sce', 'records', 'status', function($scope, $http, $sce, records, status) {
	// init
	$scope.error = null;
	$scope.record = {};
	$scope.status = new status(true);
	
	// parse record id (format: /record/:id) and load the record
	var recordId = window.location.pathname.split("/")[3];
	var data = {"_id": recordId };
	
	$scope.status.doBusy(records.getUrl(recordId)).
	then(function(results) {
		if (results.data) {
		  $scope.url = $sce.trustAsResourceUrl(results.data);
		}
	});
		
	$http.post(jsRoutes.controllers.Records.get().url, JSON.stringify(data)).
		success(function(records) {
			$scope.record = records;
			$scope.record.json = JSON.stringify($scope.record.data, null, "\t");
			if (_.has($scope.record.data, "type") && $scope.record.data.type === "file") {
				$scope.downloadLink = jsRoutes.controllers.Records.getFile(recordId).url;
			}
			loadUserNames();
			loadAppName();										    	    	
									
		}).
		error(function(err) { $scope.error = "Failed to load record details: " + err; });
	
	var loadUserNames = function() {
		var data = {"properties": {"_id": [$scope.record.owner, $scope.record.creator]}, "fields": ["firstname", "sirname"]};
		$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) {
				_.each(users, function(user) {
					if ($scope.record.owner.$oid === user._id.$oid) { $scope.record.owner = (user.firstname+" "+user.sirname).trim(); }
					if ($scope.record.creator.$oid === user._id.$oid) { $scope.record.creator = (user.firstname+" "+user.sirname).trim(); }
				});
			}).
			error(function(err) { $scope.error = "Failed to load names: " + err; });
	}
	
	var loadAppName = function() {
		var data = {"properties": {"_id": $scope.record.app}, "fields": ["name"]};
		$http.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data)).
			success(function(apps) { $scope.record.app = apps[0].name; }).
			error(function(err) { $scope.error = "Failed to load app name: " + err; });
	}
	
	/*var rewriteCreated = function() {
		var split = $scope.record.created.split(" ");
		$scope.record.created = split[0] + " at " + split[1];
	}*/
	
}]);
details.controller('UserCtrl', ['$scope', '$http', function($scope, $http) {
	// init
	$scope.error = null;
	$scope.user = {};
	
	// parse user id (format: /users/:id) and load the user details
	var userId = window.location.pathname.split("/")[3];
	var data = {"properties": {"_id": {"$oid": userId}}, "fields": ["name", "email"]};
	$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
		success(function(users) { $scope.user = users[0]; }).
		error(function(err) { $scope.error = "Failed to load user details: " + err; });
	
}]);
details.controller('MessageCtrl', ['$scope', '$http', function($scope, $http) {
	// init
	$scope.error = null;
	$scope.message = {};
	
	// parse message id (format: /messages/:id) and load the app
	var messageId = window.location.pathname.split("/")[3];
	var data = {"properties": {"_id": {"$oid": messageId}}, "fields": ["sender", "receivers", "created", "title", "content"]};
	$http.post(jsRoutes.controllers.Messages.get().url, JSON.stringify(data)).
		success(function(messages) {
			$scope.message = messages[0];
			getSenderName();
			getReceiverNames();
			//rewriteCreated();
		}).
		error(function(err) { $scope.error = "Failed to load message details: " + err; });
	
	getSenderName = function() {
		var data = {"properties": {"_id": $scope.message.sender}, "fields": ["name"]};
		$http.post(jsRoutes.controllers.Users.getUsers().url, JSON.stringify(data)).
			success(function(users) { $scope.message.sender.name = users[0].name; }).
			error(function(err) { $scope.error = "Failed to load sender name: " + err; });
	}
	
	getReceiverNames = function() {
		var data = {"properties": {"_id": $scope.message.receivers}, "fields": ["name"]};
		$http.post(jsRoutes.controllers.Users.getUsers().url, JSON.stringify(data)).
			success(function(users) {
				_.each(users, function(user) {
					var receiver = _.find($scope.message.receivers, function(rec) { return rec.$oid === user._id.$oid; });
					receiver.name = user.name;
				});
			}).
			error(function(err) { $scope.error = "Failed to load receiver names: " + err; });
	}
	
	/*rewriteCreated = function() {
		var split = $scope.message.created.split(" ");
		$scope.message.created = split[0] + " at " + split[1];
	}*/
	
}]);
details.controller('AppCtrl', ['$scope', '$http', function($scope, $http) {
	// init
	$scope.error = null;
	$scope.success = false;
	$scope.app = {};
	$scope.visualizations = [];
	
	// parse app id (format: /apps/:id) and load the app
	var appId = window.location.pathname.split("/")[3];
	var data = {"properties": {"_id": {"$oid": appId}}, "fields": ["name", "creator", "description", "recommendedVisualizations"]};
	$http.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data)).
		success(function(apps) {
			$scope.error = null;
			$scope.app = apps[0];
			isInstalled();
			getCreatorName();
			if ($scope.app.recommendedVisualizations && $scope.app.recommendedVisualizations.length > 0) {
				$scope.loadRecommendations($scope.app.recommendedVisualizations);
			}
		}).
		error(function(err) { $scope.error = "Failed to load app details: " + err; });
	
	$scope.loadRecommendations = function(ids) {
		var data = { "properties": {"_id": ids }, "fields": ["name", "creator", "description"]};
		$http.post(jsRoutes.controllers.Visualizations.get().url, JSON.stringify(data)).
			success(function(visualizations) {				
				$scope.visualizations = visualizations;				
			}).
			error(function(err) { $scope.error = "Failed to load visualization details: " + err; });
	};
	
	// show visualization details
	$scope.showVisualizationDetails = function(visualization) {
		window.location.href = jsRoutes.controllers.Visualizations.details(visualization._id.$oid).url;
	};
	
	isInstalled = function() {
		$http(jsRoutes.controllers.Apps.isInstalled($scope.app._id.$oid)).
			success(function(installed) { $scope.app.installed = installed; }).
			error(function(err) { $scope.error = "Failed to check whether this app is installed: " + err; });
	}
	
	getCreatorName = function() {
		var data = {"properties": {"_id": $scope.app.creator}, "fields": ["name"]};
		$http.post(jsRoutes.controllers.Users.getUsers().url, JSON.stringify(data)).
			success(function(users) { $scope.app.creator = users[0].name; }).
			error(function(err) { $scope.error = "Failed to load the name of the creator: " + err; })
	}
	
	$scope.install = function() {
		$http(jsRoutes.controllers.Apps.install($scope.app._id.$oid)).
			success(function() {
				$scope.app.installed = true;
				$scope.success = true;
			}).
			error(function(err) { $scope.error = "Failed to install the app: " + err; });
	}
	
	$scope.uninstall = function() {
		$http(jsRoutes.controllers.Apps.uninstall($scope.app._id.$oid)).
		success(function() {
			$scope.app.installed = false;
			$scope.success = false;
		}).
		error(function(err) { $scope.error = "Failed to uninstall the app: " + err; });
	}
	
}]);
details.controller('VisualizationCtrl', ['$scope', '$http', '$location', function($scope, $http, $location) {
	// init
	$scope.error = null;
	$scope.success = false;
	$scope.visualization = {};
	$scope.options = {};
	
	// parse visualization id (format: /visualizations/:id) and load the visualization
	var visualizationId = window.location.pathname.split("/")[3];
	var data = {"properties": {"_id": {"$oid": visualizationId}}, "fields": ["name", "creator", "description", "defaultSpaceName", "defaultRules"]};
	$http.post(jsRoutes.controllers.Visualizations.get().url, JSON.stringify(data)).
		success(function(visualizations) {
			$scope.error = null;
			$scope.visualization = visualizations[0];
			if ($scope.visualization.defaultSpaceName!=null) {
			  $scope.options.createSpace = true;
			  $scope.options.spaceName = $scope.visualization.defaultSpaceName;
			  if ($scope.visualization.defaultRules &&$scope.visualization.defaultRules.length >0 ) {
				  $scope.options.applyRules = true;
			  }
			}
			isInstalled();
			getCreatorName();
		}).
		error(function(err) { $scope.error = "Failed to load visualization details: " + err; });
	
	isInstalled = function() {
		$http(jsRoutes.controllers.Visualizations.isInstalled($scope.visualization._id.$oid)).
			success(function(installed) { $scope.visualization.installed = installed; }).
			error(function(err) { $scope.error = "Failed to check whether this visualization is installed: " + err; });
	}
	
	getCreatorName = function() {
		var data = {"properties": {"_id": $scope.visualization.creator}, "fields": ["name"]};
		$http.post(jsRoutes.controllers.Users.getUsers().url, JSON.stringify(data)).
			success(function(users) { $scope.visualization.creator = users[0].name; }).
			error(function(err) { $scope.error = "Failed to load the name of the creator: " + err; })
	}
	
	$scope.install = function() {
		$http.put(jsRoutes.controllers.Visualizations.install($scope.visualization._id.$oid).url, JSON.stringify($scope.options)).
			success(function() {
				$scope.visualization.installed = true;
				$scope.success = true;
				if ($location.search().next) document.location.href = $location.search().next; 
			}).
			error(function(err) { $scope.error = "Failed to install the visualization: " + err; });
	}
	
	$scope.uninstall = function() {
		$http(jsRoutes.controllers.Visualizations.uninstall($scope.visualization._id.$oid)).
		success(function() {
			$scope.visualization.installed = false;
			$scope.success = false;
		}).
		error(function(err) { $scope.error = "Failed to uninstall the visualization: " + err; });
	}
	
}]);
