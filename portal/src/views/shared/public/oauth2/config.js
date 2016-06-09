angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('public.oauth2', {
	      url: '/oauth2',
	      templateUrl: 'views/shared/public/oauth2/oauth2.html' 
	    });
});