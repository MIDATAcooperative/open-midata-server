angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('public.lostpw', {
	      url: '/lostpw',
	      templateUrl: 'views/shared/public/lostpw/lostpw.html' 
	    })
	    .state('public_hc.lostpw', {
	      url: '/lostpw',
	      templateUrl: 'views/shared/public/lostpw/lostpw.html' 
	    })
	    .state('public_research.lostpw', {
	      url: '/lostpw',
	      templateUrl: 'views/shared/public/lostpw/lostpw.html' 
	    });
});