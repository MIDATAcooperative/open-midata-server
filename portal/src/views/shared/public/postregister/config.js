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
	      url: '/upgrade?role&feature&pluginId',
	      templateUrl: 'views/shared/public/postregister/postregister.html' 
	   })
	   .state('research.upgrade', {
	      url: '/upgrade?role&feature&pluginId',
	      templateUrl: 'views/shared/public/postregister/postregister.html' 
	   })
	   .state('developer.upgrade', {
	      url: '/upgrade?role&feature&pluginId',
	      templateUrl: 'views/shared/public/postregister/postregister.html' 
	   }).state('provider.upgrade', {
		      url: '/upgrade?role&feature&pluginId',
		      templateUrl: 'views/shared/public/postregister/postregister.html' 
	   }).state('public.failure', {
	      url: '/failure?reason&action&language&email&login&client_id&redirect_uri&state&code_challenge&code_challenge_method&device_id&role&given&family&gender&country&birthdate&joincode&isnew',
	      templateUrl: 'views/shared/public/postregister/failure.html',
	      data : { role : "member", keep : true }
	    })
	    .state('public_provider.failure', {
	      url: '/failure?reason&action&language&email&login&client_id&redirect_uri&state&code_challenge&code_challenge_method&device_id&role&given&family&gender&country&birthdate&joincode&isnew',
	      templateUrl: 'views/shared/public/postregister/failure.html',
	      data : { role : "hpuser", keep : true }
	    })
	    .state('public_research.failure', {
	      url: '/failure?reason&action&language&email&login&client_id&redirect_uri&state&code_challenge&code_challenge_method&device_id&role&given&family&gender&country&birthdate&joincode&isnew',
	      templateUrl: 'views/shared/public/postregister/failure.html',
		  data : { role : "research", keep : true }
	    })
	    .state('public_developer.failure', {
	      url: '/failure?reason&action&language&email&login&client_id&redirect_uri&state&code_challenge&code_challenge_method&device_id&role&given&family&gender&country&birthdate&joincode&isnew',
	      templateUrl: 'views/shared/public/postregister/failure.html',
		  data : { role : "developer", keep : true }
	    })
	   
	   ;
	   
}]);