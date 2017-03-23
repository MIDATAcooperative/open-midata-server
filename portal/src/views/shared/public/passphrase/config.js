angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public.passphrase', {
	      url: '/passphrase',
	      templateUrl: 'views/shared/public/passphrase/passphrase.html',
	      data : { role : "member" }
	    })
	    .state('public_provider.passphrase', {
	      url: '/passphrase',
	      templateUrl: 'views/shared/public/passphrase/passphrase.html',
	      data : { role : "hpuser" } 
	    })
	    .state('public_research.passphrase', {
	      url: '/passphrase',
	      templateUrl: 'views/shared/public/passphrase/passphrase.html',
		  data : { role : "research" }
	    })
	    .state('public_developer.passphrase', {
	      url: '/passphrase',
	      templateUrl: 'views/shared/public/passphrase/passphrase.html',
		  data : { role : "developer" }
	    })
	    .state('public_developer.passphrase_admin', {
	      url: '/passphrase_admin',
	      templateUrl: 'views/shared/public/passphrase/passphrase_admin.html',
		  data : { role : "admin" }
	    });
}]);