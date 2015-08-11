angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('public_developer.login', {
	      url: '/login',
	      templateUrl: 'views/developers/public/login/login.html' 
	    });
});