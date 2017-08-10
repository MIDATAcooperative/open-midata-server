angular.module('portal')
.controller('ManageTermsCtrl', ['$scope', '$state', 'server', 'terms', 'status', 'languages', function($scope, $state, server, terms, status, languages) {
	
	// init
	$scope.error = null;
	$scope.newTerms = {  };
	$scope.status = new status(true);
	
	$scope.languages = languages.all;
	
	// register app
	$scope.addTerms = function() {														
		$scope.status.doAction('submit', terms.add($scope.newTerms))
		.then(function(data) { $state.go("^.viewterms"); });		
	};
		
	$scope.status.isBusy = false;
}]);