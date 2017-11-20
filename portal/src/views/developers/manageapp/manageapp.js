angular.module('portal')
.controller('ManageAppCtrl', ['$scope', '$state', '$translatePartialLoader', 'server', 'apps', 'status', 'studies', 'languages', 'terms', 'labels', '$translate', 'formats', function($scope, $state, $translatePartialLoader, server, apps, status, studies, languages, terms, labels, $translate, formats) {
	
	// init
	$scope.error = null;
	$scope.app = { version:0, tags:[], i18n : {}, requirements:[] };
	$scope.status = new status(false, $scope);
	$scope.allowDelete = $state.current.allowDelete;
	$scope.allowStudyConfig = $state.current.allowStudyConfig;
	$scope.languages = languages.array;
	$scope.requirements = apps.userfeatures;
	$scope.writemodes = apps.writemodes;
	$scope.query = {};
	$scope.codesystems = formats.codesystems;
	
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
			$scope.updateQuery();
			
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
	
	$scope.updateQuery = function() {
		$scope.codeerror = null;
		$scope.myform.queryadd.$invalid = false;
		try {
		  $scope.app.defaultQuery = JSON.parse($scope.app.defaultQueryStr);
		var q = $scope.app.defaultQuery;
		var is = function(f, v) {
			return f && (f == v ||( f.length > 0 && f.indexOf(v) >= 0));
		};
		$scope.query.self = is(q.owner, "self");
		$scope.query.ownapp = is(q.app, $scope.app.filename);
		$scope.query.learn = is(q.learn, true);
		$scope.query.all = is(q.group, "all");
		
		$scope.labels = [];
		
		if ($scope.app.defaultQuery.content) {
			var patientFound = false;
			angular.forEach($scope.app.defaultQuery.content, function(r) {
			  if (r == "Patient") patientFound = true;
			  labels.getContentLabel($translate.use(), r).then(function(lab) {
				 $scope.labels.push({ type : "content", field : r, label : lab, selected : true }); 
			  });
			});
			if (!patientFound) {
				labels.getContentLabel($translate.use(), "Patient").then(function(lab) {
					$scope.labels.push({ type : "content", field : "Patient", label : lab, selected : false }); 
				});
			}
			
		}
		if ($scope.app.defaultQuery.group) {
			angular.forEach($scope.app.defaultQuery.group, function(r) {
				  labels.getGroupLabel($translate.use(), r).then(function(lab) {
					 $scope.labels.push({ type : "group", field : r, label : lab, selected : true }); 
				  });
			});
		}
		} catch (e) {}	
	};
	
	$scope.patchQuery = function(field, val) {
		if (field == "owner") {
			if ($scope.query.self) $scope.app.defaultQuery.owner = "self"; 
			else $scope.app.defaultQuery.owner = undefined;
		}
		if (field == "app") {
			if ($scope.query.ownapp) $scope.app.defaultQuery.app = $scope.app.filename; 
			else $scope.app.defaultQuery.app = undefined;
		}
		if (field == "group") {
			if ($scope.query.all) {
				$scope.app.defaultQuery.group = ["all"]; 
				$scope.app.defaultQuery["group-system"] = "v1";
			}
			else {
				$scope.app.defaultQuery.group = undefined;
				$scope.app.defaultQuery["group-system"] = undefined;
			}
		}
		if (field == "learn") {
			if ($scope.query.learn) {
				$scope.app.defaultQuery.learn = true;
				if (!$scope.app.defaultQuery.content) $scope.app.defaultQuery.content = [];
			}
			else $scope.app.defaultQuery.learn = undefined;
		}
		if (field == "content") {
			if (!$scope.app.defaultQuery.content) $scope.app.defaultQuery.content = [];
			if (val.selected) {
				$scope.app.defaultQuery.content.push(val.field);
			} else {
				$scope.app.defaultQuery.content.splice($scope.app.defaultQuery.content.indexOf(val.field), 1);
			}						
		}
		
		$scope.app.defaultQueryStr = JSON.stringify($scope.app.defaultQuery);
	};
	
	$scope.addCode = function() {	
		
		formats.searchCodes({ system : $scope.query.system.system, code : $scope.query.code }, ["content"])
		.then(function(r) {
			if (r.data && r.data.length == 1) {
				$scope.patchQuery("content", { field : r.data[0].content, selected : true });
				$scope.updateQuery();
			} else {
				$scope.codeerror = "error.unknown.code";
				$scope.myform.queryadd.$invalid = true;
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