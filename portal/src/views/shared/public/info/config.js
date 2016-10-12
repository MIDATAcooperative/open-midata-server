angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('public.info', {
	      url: '/info',
	      templateUrl: 'views/shared/public/info/info.html' 
	    });
});