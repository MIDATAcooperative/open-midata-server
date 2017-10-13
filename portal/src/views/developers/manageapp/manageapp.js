angular.module('portal')
.controller('ManageAppCtrl', ['$scope', '$state', '$translatePartialLoader', 'server', 'apps', 'status', 'studies', 'languages', 'terms', function($scope, $state, $translatePartialLoader, server, apps, status, studies, languages, terms) {
	
	// init
	$scope.error = null;
	$scope.app = { version:0, tags:[], i18n : {}, requirements:[] };
	$scope.status = new status(false, $scope);
	$scope.allowDelete = $state.current.allowDelete;
	$scope.allowStudyConfig = $state.current.allowStudyConfig;
	$scope.languages = languages.array;
	$scope.requirements = apps.userfeatures;
	$scope.writemodes = apps.writemodes;
	
	$scope.sel = { lang : 'de' };
	$scope.targetUserRoles = [
        { value : "ANY", label : "Any Role" },
	    { value : "MEMBER", label : "MIDATA Members" },
	    { value : "PROVIDER", label : "Healthcare Providers" },
	    { value : "RESEARCH", label : "Research" },
	    { value : "DEVELOPER", label : "Developers" }
    ];
	$scope.types = [
	    { value : "visualization", label : "Plugin" },
	    { value : "create", label : "Input Form (Deprecated)" },
	    { value : "oauth1", label : "OAuth 1 Import" },
	    { value : "oauth2", label : "OAuth 2 Import" },
	    { value : "mobile", label : "Mobile App" }
	];
	$scope.tags = [
	    "Analysis", "Import", "Planning", "Protocol"
    ];
			
	$scope.loadApp = function(appId) {
		$scope.status.doBusy(apps.getApps({ "_id" : appId }, ["creator", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type","accessTokenUrl", "authorizationUrl", "consumerKey", "consumerSecret", "defaultQuery", "defaultSpaceContext", "defaultSpaceName", "previewUrl", "recommendedPlugins", "requestTokenUrl", "scopeParameters","secret","redirectUri", "url","developmentServer","version","i18n","status", "resharesData", "allowsUserSearch", "linkedStudy", "mustParticipateInStudy", "pluginVersion", "requirements", "termsOfUse", "orgName", "unlockCode", "writes"]))
		.then(function(data) { 
			$scope.app = data.data[0];			
			if ($scope.app.status == "DEVELOPMENT" || $scope.app.status == "BETA") {
				$scope.allowDelete = true;
			} else {
				$scope.allowDelete = $state.current.allowDelete;
			}
			if (!$scope.app.i18n) { $scope.app.i18n = {}; }
			if (!$scope.app.requirements) { $scope.app.requirements = []; }
			$scope.app.defaultQueryStr = JSON.stringify($scope.app.defaultQuery);
			
			if ($scope.app.linkedStudy) {
				
				$scope.status.doBusy(studies.search({ _id : $scope.app.linkedStudy }, ["code", "name"]))
				.then(function(studyresult) {
					if (studyresult.data && studyresult.data.length) {
					  $scope.app.linkedStudyCode = studyresult.data[0].code;
				 	  $scope.app.linkedStudyName = studyresult.data[0].name;
					}
				});
				
			}
		});
	};
	
	// register app
	$scope.updateApp = function() {
		
		$scope.submitted = true;	
		
		if ($scope.app.defaultQueryStr != null && $scope.app.defaultQueryStr !== "") {
		  try {
			  
		    $scope.app.defaultQuery = JSON.parse($scope.app.defaultQueryStr);
		      $scope.myform.defaultQuery.$setValidity('json', true);
		    
		  } catch (e) {
			  $scope.myform.defaultQuery.$setValidity('json', false);
			  //$scope.error = "Invalid JSON in Access Query!";
			  return;
		  }
		} else {
		  $scope.app.defaultQuery = null;
		  $scope.myform.defaultQuery.$setValidity('json', true);
		}
						
		angular.forEach($scope.languages, function(lang) {
			if ($scope.app.i18n[lang] && $scope.app.i18n[lang].name === "") {
			   delete $scope.app.i18n[lang];
			} 
		});
		
		// check whether url contains ":authToken"
		if ($scope.app.type && $scope.app.type !== "mobile" && $scope.app.url.indexOf(":authToken") < 0) {
			$scope.myform.url.$setValidity('authToken', false);
			//$scope.error = "Url must contain ':authToken' to receive the authorization token required to create records.";
			return;
		} else {
			$scope.myform.url.$setValidity('authToken', true);
		}
		
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		
		if (! $scope.myform.$valid) return;
		
		if ($scope.app._id == null) {
			$scope.status.doAction('submit', apps.registerPlugin($scope.app))
			.then(function(data) { $state.go("^.yourapps"); });
		} else {			
		    $scope.status.doAction('submit', apps.updatePlugin($scope.app))
		    .then(function() { $state.go("^.yourapps"); });
		}
	};
	
	$scope.toggle = function(array,itm) {
		console.log(array);
		var pos = array.indexOf(itm);
		if (pos < 0) array.push(itm); else array.splice(pos, 1);
	};
	
	$scope.doInstall = function() {
		$state.go("^.visualization", { visualizationId : $scope.app._id, context : "sandbox" });
	};
	
	$scope.doDelete = function() {
		if ($state.current.allowDelete) {
		    $scope.status.doAction('delete', apps.deletePlugin($scope.app))
		    .then(function(data) { $state.go("^.plugins"); });
		} else {
			$scope.status.doAction('delete', apps.deletePluginDeveloper($scope.app))
		    .then(function(data) { $state.go("^.yourapps"); });
		}
	};
	
	$translatePartialLoader.addPart("developers");
	
	terms.search({}, ["name", "version", "language", "title"])
	.then(function(result) {
		$scope.terms = result.data;
		if ($state.params.appId != null) { $scope.loadApp($state.params.appId); }
		else { $scope.status.isBusy = false; }
	});
	
}]);