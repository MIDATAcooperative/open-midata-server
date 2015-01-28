var studies = angular.module('studies', []);
studies.controller('CreateStudyCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.study = {};
	$scope.submitted = false;
	
	// register new user
	$scope.createstudy = function() {
	
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
		
		$scope.study.error = null;
		
		// send the request
		var data = $scope.study;		
		
		$http.post(jsRoutes.controllers.research.Studies.create().url, JSON.stringify(data)).
			success(function(url) { window.location.replace(url); }).
			error(function(err) {
				$scope.error = err;
				if (err.field && err.type) $scope.myform[err.field].$setValidity(err.type, false);
				else $scope.study.error = err; 
			});
	}
	
}]);
studies.controller('ListStudiesCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.results =[];
	$scope.loading = true;
	
	$scope.reload = function() {
			
		$http.get(jsRoutes.controllers.research.Studies.list().url).
			success(function(data) { 				
				$scope.results = data;
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.error = err;				
			});
	};
	
	$scope.reload();
	
}]);
studies.controller('OverviewCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.studyid = window.location.pathname.split("/")[2];
	$scope.study = {};
	$scope.loading = true;
		
	$scope.reload = function() {
			
		$http.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url).
			success(function(data) { 				
				$scope.study = data;
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.error = err;				
			});
	};
	
	$scope.readyForParticipantSearch = function() {
		return $scope.study.validationStatus == "VALIDATED" && $scope.study.participantSearchStatus == "PRE";
	};
	
	$scope.startParticipantSearch = function() {
		$scope.error = null;
		
		$http.post(jsRoutes.controllers.research.Studies.startParticipantSearch($scope.studyid).url).
		success(function(data) { 				
		    $scope.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.reload();
	
}]);
studies.controller('CodesCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.studyid = window.location.pathname.split("/")[2];
	$scope.codes = null;
	$scope.newcodes = { count:1, reuseable:true, group:"" };
	$scope.loading = true;
	$scope.blocked = false;
	$scope.submitted = false;
		
	$scope.reload = function() {
			
		$http.get(jsRoutes.controllers.research.Studies.listCodes($scope.studyid).url).
			success(function(data) { 				
				$scope.codes = data;
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.error = err;
				$scope.blocked = true;
			});
	};
	
	$scope.generate = function() {
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
		
		$scope.newcodes.error = null;
		
		// send the request
		var data = $scope.newcodes;		
		
		$http.post(jsRoutes.controllers.research.Studies.generateCodes($scope.studyid).url, JSON.stringify(data)).
			success(function(url) { $scope.reload() }).
			error(function(err) {
				$scope.error = err;
				if (err.field && err.type) $scope.myform[err.field].$setValidity(err.type, false);
				else $scope.newcodes.error = err; 
			});
	};
	
	$scope.reload();
	
}]);
studies.controller('ListParticipantsCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.studyid = window.location.pathname.split("/")[2];
	$scope.results =[];
	$scope.loading = true;
	
	$scope.reload = function() {
			
		$http.get(jsRoutes.controllers.research.Studies.listParticipants($scope.studyid).url).
			success(function(data) { 				
				$scope.results = data;
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.error = err;				
			});
	};
	
	$scope.reload();
	
}]);