angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('public_developer.registration', {
	      url: '/registration',
	      templateUrl: 'views/developers/public/registration/registration.html' 
	    });
});