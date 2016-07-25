angular.module('portal')
.controller('ManageAppCtrl', ['$scope', '$state', 'server', 'apps', 'status', function($scope, $state, server, apps, status) {
	
	// init
	$scope.error = null;
	$scope.app = { version:0, tags:[], i18n : {} };
	$scope.status = new status(true);
	$scope.allowDelete = $state.current.allowDelete;
	$scope.languages = ['en', 'de', 'fr', 'it'];
	$scope.sel = { lang : 'de' };
	$scope.targetUserRoles = [
        { value : "ANY", label : "Any Role" },
	    { value : "MEMBER", label : "MIDATA Members" },
	    { value : "PROVIDER", label : "Healthcare Providers" },
	    { value : "RESEARCH", label : "Research" },
	    { value : "DEVELOPER", label : "Developers" }
    ];
	$scope.types = [
	    { value : "visualization", label : "Visualization" },
	    { value : "create", label : "Input Form" },
	    { value : "oauth1", label : "OAuth 1 Import" },
	    { value : "oauth2", label : "OAuth 2 Import" },
	    { value : "mobile", label : "Mobile App" }
	];
	$scope.tags = [
	    "Analysis", "Import", "Planning", "Tools", "Fitbit", "Jawbone"
    ];
			
	$scope.loadApp = function(appId) {
		$scope.status.doBusy(apps.getApps({ "_id" : { "$oid" :  appId }}, ["creator", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type","accessTokenUrl", "authorizationUrl", "consumerKey", "consumerSecret", "defaultQuery", "defaultSpaceContext", "defaultSpaceName", "previewUrl", "recommendedPlugins", "requestTokenUrl", "scopeParameters","secret","redirectUri", "url","developmentServer","version","i18n"]))
		.then(function(data) { 
			$scope.app = data.data[0];
			if (!$scope.app.i18n) { $scope.app.i18n = {}; }
			$scope.app.defaultQueryStr = JSON.stringify($scope.app.defaultQuery);
		});
	};
	
	// register app
	$scope.updateApp = function() {
		if ($scope.app.defaultQueryStr != null && $scope.app.defaultQueryStr !== "") {
		  $scope.app.defaultQuery = JSON.parse($scope.app.defaultQueryStr);
		} else {
		  $scope.app.defaultQuery = null;	
		}
						
		angular.forEach($scope.languages, function(lang) {
			if ($scope.app.i18n[lang] && $scope.app.i18n[lang].name === "") {
			   delete $scope.app.i18n[lang];
			} 
		});
		
		// check whether url contains ":authToken"
		if ($scope.app.type !== "mobile" && $scope.app.url.indexOf(":authToken") < 0) {
			$scope.error = "Url must contain ':authToken' to receive the authorization token required to create records.";
			return;
		}
		
		if ($scope.app._id == null) {
			$scope.status.doAction('submit', apps.registerPlugin($scope.app))
			.then(function(data) { $state.go("^.yourapps"); });
		} else {			
		    $scope.status.doAction('submit', apps.updatePlugin($scope.app))
		    .then(function() { $state.go("^.yourapps"); });
		}
	};
	
	$scope.toggle = function(array,itm) {
		var pos = array.indexOf(itm);
		if (pos < 0) array.push(itm); else array.splice(pos, 1);
	};
	
	$scope.doInstall = function() {
		$state.go("^.visualization", { visualizationId : $scope.app._id.$oid, context : "sandbox" });
	};
	
	$scope.doDelete = function() {
		$scope.status.doAction('delete', apps.deletePlugin($scope.app))
		.then(function(data) { $state.go("^.plugins"); });
	};
	
	if ($state.params.appId != null) { $scope.loadApp($state.params.appId); }
	else { $scope.status.isBusy = false; }
}]);