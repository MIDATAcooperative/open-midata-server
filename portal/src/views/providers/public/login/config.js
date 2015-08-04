angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('public_providers.login', {
	      url: '/login',
	      templateUrl: 'views/providers/public/login/login.html' 
	    });
});