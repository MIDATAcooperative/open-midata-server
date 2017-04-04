angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('provider.patientsearch', {
	      url: '/patientsearch',
	      templateUrl: 'views/providers/patientsearch/patientsearch.html'
	    });
}]);