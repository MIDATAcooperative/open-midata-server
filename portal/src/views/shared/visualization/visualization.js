angular.module('portal')
.controller('VisualizationCtrl', ['$scope', 'apps', '$state', 'status', 'users', 'session', '$window', 'labels', '$translate', '$q', 'views', function($scope, apps, $state, status, users, session, $window, labels, $translate, $q, views) {
	// init
	$scope.error = null;
	$scope.success = false;
	$scope.visualization = {};
	$scope.options = { study : $state.params.options };
	$scope.params = $state.params;
	$scope.status = new status(true);
	$scope.labels = [];
	if ($state.params.context == "workspace") {
		$scope.contexts = [
		         		  { value : "workspace", label : "visualization.location.workspace" },
		         		  { value : "mobile", label : "visualization.location.mobile" }
		                  ];
	} else if ($state.params.context == "research" || $state.params.context == "sandbox") {
		$scope.contexts = [		         	
		         		  { value : "research", label : "visualization.location.research" },
		         		  { value : "sandbox", label : "visualization.location.sandbox" },		         	
		         		  { value : "mobile", label : "visualization.location.mobile" }
	                    	];
	} else {
		$scope.contexts = [
		  { value : "me", label : "visualization.location.me" },
		  { value : "mydata", label : "visualization.location.mydata" },
		  { value : "social", label : "visualization.location.social" },
		  { value : "providers", label : "visualization.location.providers" },
		  { value : "studies", label : "visualization.location.studies" },		
		  { value : "mobile", label : "visualization.location.mobile" },
		  { value : "config", label : "visualization.location.config" },
	    ];
	}
	$scope.view = views.getView("terms");
	
	
	// parse visualization id (format: /visualizations/:id) and load the visualization
	var visualizationId = $state.params.visualizationId;	
	$scope.status.doBusy(apps.getApps({"_id":  visualizationId}, ["name", "creator", "creatorLogin", "developerTeam", "description", "defaultSpaceContext", "defaultSpaceName", "defaultQuery", "type", "orgName", "publisher", "requirements", "termsOfUse"]))
	.then(function(results) {
		   var visualizations = results.data;
			$scope.error = null;
			$scope.app = $scope.visualization = visualizations[0];
			if (!$scope.visualization.termsOfUse) $scope.options.appAgb = true;
			if ($scope.visualization.type !== "mobile" && $scope.visualization.type !== "service") {
			  $scope.options.createSpace = true;
			  $scope.options.spaceName = $scope.params.name ||  $scope.visualization.defaultSpaceName;
			  if ($scope.visualization.defaultQuery != null) {
				  $scope.options.query = $scope.visualization.defaultQuery;
				  $scope.options.applyRules = true;
				
			  } else {				 
				  console.log($scope.visualization);
			  }
			}
			if ($scope.params && $scope.params.context) {
				$scope.options.context = $scope.params.context;
			} else { 
				$scope.options.context = $scope.visualization.defaultSpaceContext; 
			}
			if ($scope.visualization.defaultSpaceContext == "config" && $state.params.context !== "sandbox") $scope.options.context = "config"; 
			if ($scope.params && $scope.params.name) {
				$scope.options.spaceName = $scope.params.name;
			}
			if ($scope.params && $scope.params.query) {
				$scope.options.query = JSON.parse(decodeURIComponent($scope.params.query));
				$scope.options.applyRules = true;
			}
			
			//getCreatorName();

			return $scope.prepareQuery($scope.options.query, $scope.visualization.filename, $scope.labels);
		});
	
	
	
	/*getCreatorName = function() {
		users.getMembers({"_id": $scope.visualization.creator}, ["name"])
		.then(function(users) { if (users.data && users.data[0]) { $scope.visualization.creator = users.data[0].name; } });			
	};*/

	$scope.prepareQuery = function(defaultQuery, appName, genLabels, reqInf) {
		var sq = labels.simplifyQuery(defaultQuery, appName, true);
		var result = [];		
		if (sq) {
			if (sq.content) {
				angular.forEach(sq.content, function(r) {
				  if (r === "Patient" || r === "Group" || r === "Person" || r === "Practitioner" || r === "ValueSet") return;
				  result.push(labels.getContentLabel($translate.use(), r).then(function(lab) {
					if (genLabels.indexOf(lab)<0) genLabels.push(lab); 
				  }));
				});
			}
			if (sq.group) {
				angular.forEach(sq.group, function(r) {
					  result.push(labels.getGroupLabel($translate.use(), sq["group-system"], r).then(function(lab) {
						  if (genLabels.indexOf(lab)<0) genLabels.push(lab); 
					  }));
				});
			}			
		}
		if (reqInf == 'RESTRICTED') result.push($translate("studydetails.information_restricted").then(function(l) { genLabels.push(l); }));
		if (reqInf == 'DEMOGRAPHIC') result.push($translate("studydetails.information_demographic").then(function(l) { genLabels.push(l); }));
		if (reqInf == 'NONE') result.push($translate("studydetails.information_none").then(function(l) { genLabels.push(l); }));
		return $q.all(result);
				
	};
	
	$scope.install = function() {
		$scope.status.doAction("install", apps.installPlugin($scope.visualization._id, $scope.options))
		.then(function(result) {
				$scope.visualization.installed = true;
				$scope.success = true;
				session.login();
				if (result.data && result.data._id) {
					if ($scope.visualization.type === "oauth1" || $scope.visualization.type === "oauth2") {
					  $state.go('^.importrecords', { spaceId : result.data._id, params : $scope.params.params });	
					} else {
				      $state.go('^.spaces', { spaceId : result.data._id, params : $scope.params.params });
					}
				} else {
					if ($scope.visualization.type == "external") {
						$state.go('^.apps');
					} else $state.go('^.dashboard', { dashId : $scope.options.context });
				}
		});			
	};
		
	
	$scope.goBack = function() {
		$window.history.back();
	};

	$scope.terms = function(def) {
		console.log("TERMS");
		views.setView("terms", def, "Terms");
	};
	
}]);