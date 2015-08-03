angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('public.registration', {
	      url: '/registration',
	      templateUrl: 'views/members/public/registration/registration.html' 
	    });
});