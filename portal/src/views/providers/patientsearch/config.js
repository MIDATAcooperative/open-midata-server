angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('provider.patientsearch', {
	      url: '/patientsearch',
	      templateUrl: 'views/providers/patientsearch/patientsearch.html'
	    });
});