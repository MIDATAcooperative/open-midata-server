angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('public_provider.registration', {
	      url: '/registration',
	      templateUrl: 'views/providers/public/registration/registration.html' 
	    });
});