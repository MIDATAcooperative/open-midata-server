angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('public_provider.login', {
	      url: '/login',
	      templateUrl: 'views/providers/public/login/login.html' 
	    });
});