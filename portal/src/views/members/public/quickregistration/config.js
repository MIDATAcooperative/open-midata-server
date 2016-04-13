angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('public.quickregistration', {
	      url: '/join',
	      templateUrl: 'views/members/public/quickregistration/quickregistration.html' 
	    });
});