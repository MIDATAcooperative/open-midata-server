angular.module('portal')
.controller('AdminTermsCtrl', ['$scope', '$state', 'views', 'status', 'terms', function($scope, $state, views, status, terms) {

	$scope.status = new status(true);
		
	$scope.init = function(userId) {	
		$scope.status.doBusy(terms.search({ }, ["name", "version", "language", "title", "createdAt"]))
    	.then(function(results) {
		  $scope.terms = results.data;
    	});
	};
		
	$scope.name = function(term) {
		return term.name+"--"+term.version;
	};
		
	$scope.init();
	
}]);