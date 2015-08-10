angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('provider.patients', {
	      url: '/patients',
	      templateUrl: 'views/providers/patients/patients.html',
	      dashId : 'memberdetails'
	    });
});