angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('public.login', {
	      url: '/welcome',
	      templateUrl: 'views/members/public/login/login.html' 
	    });
});