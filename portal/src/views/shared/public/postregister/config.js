angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('public.postregister', {
	      url: '/postregister',
	      templateUrl: 'views/shared/public/postregister/postregister.html',
	      data : { role : "member" },
	      params : { progress : null }
	    })
	    .state('public_provider.postregister', {
	      url: '/postregister',
	      templateUrl: 'views/shared/public/postregister/postregister.html',
	      data : { role : "hpuser" },
	      params : { progress : null }
	    })
	    .state('public_research.postregister', {
	      url: '/postregister',
	      templateUrl: 'views/shared/public/postregister/postregister.html',
		  data : { role : "research" },
	      params : { progress : null }
	    })
	    .state('public_developer.postregister', {
	      url: '/postregister',
	      templateUrl: 'views/shared/public/postregister/postregister.html',
		  data : { role : "developer" },
	      params : { progress : null }
	    })
	    .state('public.confirm', {
	      url: '/confirm/:token',
	      templateUrl: 'views/shared/public/postregister/postregister.html',
	      data : { mode : "VALIDATED" }
	    })
	   .state('public.reject', {
		      url: '/reject/:token',
		      templateUrl: 'views/shared/public/postregister/postregister.html',
		      data : { mode : "REJECTED" }
		});
	   
});