angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('public_providers.registration', {
	      url: '/registration',
	      templateUrl: 'views/providers/public/registration/registration.html' 
	    });
});