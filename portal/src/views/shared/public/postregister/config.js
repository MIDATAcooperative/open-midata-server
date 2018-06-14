angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public.postregister', {
	      url: '/postregister?callback&consent&action',
	      templateUrl: 'views/shared/public/postregister/postregister.html',
	      data : { role : "member", keep : true },
	      params : { progress : null }
	    })
	    .state('public_provider.postregister', {
	      url: '/postregister?callback&consent&action',
	      templateUrl: 'views/shared/public/postregister/postregister.html',
	      data : { role : "hpuser", keep : true },
	      params : { progress : null }
	    })
	    .state('public_research.postregister', {
	      url: '/postregister?callback&consent&action',
	      templateUrl: 'views/shared/public/postregister/postregister.html',
		  data : { role : "research", keep : true },
	      params : { progress : null }
	    })
	    .state('public_developer.postregister', {
	      url: '/postregister?callback&consent&action',
	      templateUrl: 'views/shared/public/postregister/postregister.html',
		  data : { role : "developer", keep : true },
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
		})
	   .state('member.upgrade', {
	      url: '/upgrade?role&feature',
	      templateUrl: 'views/shared/public/postregister/postregister.html' 
	   });
	   
}]);