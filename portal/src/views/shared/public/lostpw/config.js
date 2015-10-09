angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('public.lostpw', {
	      url: '/lostpw',
	      templateUrl: 'views/shared/public/lostpw/lostpw.html',
	      data : { role : "member" }
	    })
	    .state('public_provider.lostpw', {
	      url: '/lostpw',
	      templateUrl: 'views/shared/public/lostpw/lostpw.html',
	      data : { role : "hpuser" } 
	    })
	    .state('public_research.lostpw', {
	      url: '/lostpw',
	      templateUrl: 'views/shared/public/lostpw/lostpw.html',
		  data : { role : "research" }
	    })
	    .state('public_developer.lostpw', {
	      url: '/lostpw',
	      templateUrl: 'views/shared/public/lostpw/lostpw.html',
		  data : { role : "developer" }
	    });
});