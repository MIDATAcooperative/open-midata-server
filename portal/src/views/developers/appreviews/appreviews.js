angular.module('portal')
.controller('AppReviewsCtrl', ['$scope', '$state', 'server', 'apps', 'status', 'ENV', '$http', '$window', '$translatePartialLoader',function($scope, $state, server, apps, status, ENV, $http, $window, $translatePartialLoader) {
	
	// init
	$scope.error = null;
	$scope.newreview = { pluginId : $state.params.appId };
	$scope.status = new status(false, $scope);
	$scope.checks = [ "CONCEPT", "DATA_MODEL", "ACCESS_FILTER", "QUERIES", "DESCRIPTION", "ICONS", "MAILS", "PROJECTS", "CODE_REVIEW", "TEST_CONCEPT", "TEST_PROTOKOLL", "CONTRACT" ];
	$scope.stati = ["ACCEPTED","NEEDS_FIXING"];
	$scope.allowReview = $state.current.allowReview;
	
	if ($state.params.check) {
		$scope.newreview.check = $state.params.check;
		$scope.newreview.status = "ACCEPTED";
	}
	
	$scope.loadApp = function(appId) {
		$scope.appId=appId;
		$scope.status.doBusy(apps.getApps({ "_id" : appId }, ["creator", "filename", "name", "description", "icons" ]))
		.then(function(data) { 
			$scope.app = data.data[0];			
		});
		
		$scope.status.doBusy(server.get(jsRoutes.controllers.Market.getReviews(appId).url))
		.then(function(reviews) {
			$scope.reviews = reviews.data;
		});
		
		$scope.submitted = false;
	};
	
	$scope.submit = function() {
		
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
				
        $scope.status.doAction("upload", server.post(jsRoutes.controllers.Market.addReview().url, $scope.newreview))
        .then(function() {
        	$scope.newreview = { pluginId : $state.params.appId };
        	$scope.loadApp($state.params.appId);
        });
	};
			
	$translatePartialLoader.addPart("developers");	
	$scope.loadApp($state.params.appId);	
}]);