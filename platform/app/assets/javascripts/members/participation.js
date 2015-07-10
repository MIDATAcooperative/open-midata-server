var participation = angular.module('participation', [ 'services', 'views', 'dashboards']);
participation.controller('EnterCodeCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.code = {};
	$scope.error = null;
	$scope.submitted = false;
	
	// register new user
	$scope.submitcode = function() {
	
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
		
		$scope.code.error = null;
		
		// send the request
		var data = $scope.code;		
		
		$http.post(jsRoutes.controllers.members.Studies.enterCode().url, JSON.stringify(data)).
			success(function(data) { 
				window.location.replace(portalRoutes.controllers.MemberFrontend.studydetails(data.study).url); 
			}).
			error(function(err) {
				$scope.error = err;
				if (err.field && err.type) $scope.myform[err.field].$setValidity(err.type, false);				
			});
	}
}]);

participation.controller('StudiesCtrl', ['$scope', '$http', 'views', function($scope, $http, views) {
  views.setView("newstudies", { properties : { }, fields : ["name"] });		
}]);

participation.controller('ListStudiesCtrl', ['$scope', '$http', '$attrs', 'views', 'status', function($scope, $http, $attrs, views, status) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);
	$scope.results =[];
	
	$scope.reload = function() {
		if (!$scope.view.active) return;
			
		$scope.status.doBusy($http.get(jsRoutes.controllers.members.Studies.list().url)).
		then(function(results) { 				
		   $scope.results = results.data;			
		});
	};
	
	$scope.reload();
	
}]);
participation.controller('StudyDetailCtrl', ['$scope', '$http', 'views', function($scope, $http, views) {
	
	$scope.studyid = window.location.pathname.split("/")[3];
	$scope.study = {};
	$scope.participation = {};
	$scope.loading = true;
	$scope.error = null;
		
	views.link("1", "record", "record");
	views.link("1", "shareFrom", "share");
	views.link("share", "record", "record");
	
	$scope.reload = function() {
			
		$http.get(jsRoutes.controllers.members.Studies.get($scope.studyid).url).
			success(function(data) { 				
				$scope.study = data.study;
				$scope.participation = data.participation;
				$scope.research = data.research;
				$scope.loading = false;
				$scope.error = null;
				
				if ($scope.participation && !($scope.participation.status == "CODE" || $scope.participation.status == "MATCH" )) {
				  views.setView("1", { aps : $scope.participation._id.$oid, properties : { } , type:"participations", allowAdd : true, allowRemove : false, fields : [ "ownerName", "created", "id", "name" ]});
				} else {
				  views.disableView("1");
				}
			}).
			error(function(err) {
				$scope.error = err;				
			});
	};
	
	$scope.needs = function(what) {
		return $scope.study.requiredInformation && $scope.study.requiredInformation == what;
	}
	
	$scope.mayRequestParticipation = function() {
		return ($scope.participation != null && ( $scope.participation.status == "MATCH" || $scope.participation.status == "CODE" ))
		       || ($scope.participation == null && $scope.study.participantSearchStatus == 'SEARCHING');
	};
	
	$scope.mayDeclineParticipation = function() {
		return $scope.participation != null && ( $scope.participation.status == "MATCH" || $scope.participation.status == "CODE" );
	};
	
	$scope.requestParticipation = function() {
		$scope.error = null;
		
		$http.post(jsRoutes.controllers.members.Studies.requestParticipation($scope.studyid).url).
		success(function(data) { 				
		    $scope.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.noParticipation = function() {
		$scope.error = null;
		
		$http.post(jsRoutes.controllers.members.Studies.noParticipation($scope.studyid).url).
		success(function(data) { 				
		    $scope.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.reload();
	
}]);