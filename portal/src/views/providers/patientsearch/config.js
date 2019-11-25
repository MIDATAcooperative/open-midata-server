angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('provider.patientsearch', {
	      url: '/patientsearch?email',
	      templateUrl: 'views/providers/patientsearch/patientsearch.html'
	    });
}]);