angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('public_research.registration', {
	      url: '/registration',
	      templateUrl: 'views/research/public/registration/registration.html' 
	    });
});