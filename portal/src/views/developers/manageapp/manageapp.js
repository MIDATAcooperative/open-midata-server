/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('portal')
.controller('ManageAppCtrl', ['$scope', '$state', '$translatePartialLoader', 'server', 'apps', 'status', 'studies', 'languages', 'terms', 'labels', '$translate', 'formats', 'ENV', 'session', function($scope, $state, $translatePartialLoader, server, apps, status, studies, languages, terms, labels, $translate, formats, ENV, session) {
	
	// init
	$scope.error = null;
	$scope.app = { version:0, tags:[], i18n : {}, sendReports:true, redirectUri : "http://localhost", targetUserRole : "ANY", requirements:[], defaultQuery:{ content:[] }, tokenExchangeParams : "client_id=<client_id>&grant_type=<grant_type>&code=<code>&redirect_uri=<redirect_uri>"  };
	$scope.status = new status(false, $scope);
	$scope.allowDelete = $state.current.allowDelete;
	$scope.allowExport = $state.current.allowExport;
	$scope.allowStudyConfig = $state.current.allowStudyConfig;
	$scope.languages = languages.array;
	$scope.requirements = apps.userfeatures;
	$scope.writemodes = apps.writemodes;
	$scope.query = {};
	$scope.codesystems = formats.codesystems;
	$scope.user = { subroles:[] };
	$scope.checks = [ "CONCEPT", "DATA_MODEL", "CODE_REVIEW", "TEST_CONCEPT", "TEST_PROTOKOLL", "CONTRACT" ];
	
	$scope.sel = { lang : 'de' };
	$scope.targetUserRoles = [
        { value : "ANY", label : "Any Role" },
	    { value : "MEMBER", label : "Account Holders" },
	    { value : "PROVIDER", label : "Healthcare Providers" },
	    { value : "RESEARCH", label : "Researchers" },
	    { value : "DEVELOPER", label : "Developers" }
    ];
	$scope.types = [
	    { value : "visualization", label : "Plugin" },
	    { value : "service", label : "Service" },
	    { value : "oauth1", label : "OAuth 1 Import" },
	    { value : "oauth2", label : "OAuth 2 Import" },
		{ value : "mobile", label : "Mobile App" },
		{ value : "external", label : "External Service" },
		{ value : "analyzer", label : "Project analyzer" },
		{ value : "endpoint", label : "FHIR endpoint" }
	];
	$scope.tags = [
	    "Analysis", "Import", "Planning", "Protocol"
    ];
			
	$scope.loadApp = function(appId) {
		$scope.status.doBusy(apps.getApps({ "_id" : appId }, ["creator", "creatorLogin", "developerTeam", "developerTeamLogins", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type","accessTokenUrl", "authorizationUrl", "consumerKey", "consumerSecret", "tokenExchangeParams", "defaultQuery", "defaultSpaceContext", "defaultSpaceName", "previewUrl", "recommendedPlugins", "requestTokenUrl", "scopeParameters","secret","redirectUri", "url","developmentServer","version","i18n","status", "resharesData", "allowsUserSearch", "pluginVersion", "requirements", "termsOfUse", "orgName", "publisher", "unlockCode", "writes", "icons", "apiUrl", "noUpdateHistory", "pseudonymize", "predefinedMessages", "defaultSubscriptions", "sendReports", "consentObserving"]))
		.then(function(data) { 
			$scope.app = data.data[0];			
			if ($scope.app.status == "DEVELOPMENT" || $scope.app.status == "BETA") {
				$scope.allowDelete = true;
			} else {
				$scope.allowDelete = $state.current.allowDelete;
			}
			if (!$scope.app.i18n) { $scope.app.i18n = {}; }
			if (!$scope.app.requirements) { $scope.app.requirements = []; }
			if ($scope.app.developerTeamLogins) $scope.app.developerTeamLoginsStr = $scope.app.developerTeamLogins.join(", "); 
			if ($scope.app.type === "oauth2" && ! ($scope.app.tokenExchangeParams) ) $scope.app.tokenExchangeParams = "client_id=<client_id>&grant_type=<grant_type>&code=<code>&redirect_uri=<redirect_uri>";
			$scope.app.defaultQueryStr = JSON.stringify($scope.app.defaultQuery);
			//$scope.updateQuery();
			
			/*if ($scope.app.linkedStudy) {
				
				$scope.status.doBusy(studies.search({ _id : $scope.app.linkedStudy }, ["code", "name"]))
				.then(function(studyresult) {
					if (studyresult.data && studyresult.data.length) {
					  $scope.app.linkedStudyCode = studyresult.data[0].code;
				 	  $scope.app.linkedStudyName = studyresult.data[0].name;
					}
				});
				
			}*/
		});
		
		$scope.status.doBusy(server.get(jsRoutes.controllers.Market.getReviews(appId).url))
		.then(function(reviews) {
			$scope.reviews = {};
			for (var i=0;i<reviews.data.length;i++) {
				var status = reviews.data[i].status;
				$scope.reviews[reviews.data[i].check] = (status == "OBSOLETE" ? null : status);
			}			
		});
	};
	
	
	// register app
	$scope.updateApp = function() {
		
		$scope.submitted = true;	
		
		/*
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
		}*/
						
		angular.forEach($scope.languages, function(lang) {
			if ($scope.app.i18n[lang] && $scope.app.i18n[lang].name === "") {
			   delete $scope.app.i18n[lang];
			} 
		});
		
		// check whether url contains ":authToken"
		if ($scope.app.type && $scope.app.type !== "mobile" && $scope.app.type !== "service" && $scope.app.url && $scope.app.url.indexOf(":authToken") < 0) {
			$scope.myform.url.$setValidity('authToken', false);
			//$scope.error = "Url must contain ':authToken' to receive the authorization token required to create records.";
			return;
		} else {
			if ($scope.myform.url && $scope.myform.url.$setValidity) {
			  $scope.myform.url.$setValidity('authToken', true);
			}
		}
		
		if ($scope.app.targetUserRole!="RESEARCH") $scope.app.noUpdateHistory=false;
		if ($scope.app.type!="analyzer" && $scope.app.type!="endpoint") $scope.app.pseudonymize=false;
		
		if ($scope.app.developerTeamLoginsStr) {
		  $scope.app.developerTeamLogins = $scope.app.developerTeamLoginsStr.split(/[ ,]+/);
		} else $scope.app.developerTeamLogins = [];
		
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		
		if (! $scope.myform.$valid) return;
		
		if ($scope.app._id == null) {
			$scope.status.doAction('submit', apps.registerPlugin($scope.app))
			.then(function(data) { $state.go("^.manageapp", { appId : data.data._id }); });
		} else {			
		    $scope.status.doAction('submit', apps.updatePlugin($scope.app))
		    .then(function() { $state.go("^.manageapp", { appId : $state.params.appId }); });
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
		    .then(function(data) { $state.go("^.yourapps"); });
		} else {
			$scope.status.doAction('delete', apps.deletePluginDeveloper($scope.app))
		    .then(function(data) { $state.go("^.yourapps"); });
		}
	};
	
	/*$scope.checkStudyEmpty = function() {
		if ($scope.app && !$scope.app.linkedStudyCode) $scope.app.mustParticipateInStudy = false;
	};*/
	
	$translatePartialLoader.addPart("developers");
	
	session.currentUser.then(function(userId) {			
		$scope.user = session.user;	
	    return terms.search({}, ["name", "version", "language", "title"]);
	}).then(function(result) {
		$scope.terms = result.data;
		if ($state.params.appId != null) { $scope.loadApp($state.params.appId); }
		else {
			$scope.app.defaultQueryStr = JSON.stringify($scope.app.defaultQuery);
			//$scope.updateQuery();
			$scope.status.isBusy = false;
		}		
	});
	
	$scope.exportPlugin = function() {
		$scope.status.doAction("download", server.token())
		.then(function(response) {
		  document.location.href = ENV.apiurl + jsRoutes.controllers.Market.exportPlugin($scope.app._id).url + "?token=" + encodeURIComponent(response.data.token);
		});
	};
	
	$scope.go = function(where) {
		$state.go(where, { appId : $state.params.appId });
	};
	
	$scope.hasIcon = function() {
		if (!$scope.app || !$scope.app.icons) return false;
		return $scope.app.icons.indexOf("APPICON") >= 0;
	};
	
	$scope.getIconUrl = function() {
		if (!$scope.app) return null;
		return ENV.apiurl + "/api/shared/icon/APPICON/" + $scope.app.filename;
	};
	
	$scope.requireLogout = function() {
		$scope.logoutRequired = true;
	};
	
	$scope.getOAuthLogin = function() {
		if (!$scope.app || !$scope.app.redirectUri) return "";
		return "/oauth.html#/portal/oauth2?response_type=code&client_id="+encodeURIComponent($scope.app.filename)+"&redirect_uri="+encodeURIComponent($scope.app.redirectUri.split(" ")[0]);
	};
	
	$scope.keyCount = function(obj) {
		if (!obj) return 0;
		return Object.keys(obj).length;
	};
	
	$scope.hasCount = function(obj) {
		if (!obj) return false;
		if (obj.content && obj.content.length==0) return false;
		return Object.keys(obj).length > 0;
	};
	
	$scope.hasSubRole = function(subRole) {	
		return $scope.user.subroles.indexOf(subRole) >= 0;
	};
	
}]);