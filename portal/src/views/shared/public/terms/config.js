angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',
	      termsRole : "member"
	    })
	    .state('public_provider.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',
	      termsRole : "provider"
	    })
	    .state('public_research.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',
	      termsRole : "research"
	    })
	    .state('public_developer.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',
	      termsRole : "developer"
	    })
	    .state('member.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',
	      termsRole : "member"	      
	    })
	    .state('developer.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',
	      termsRole : "developer"	      
	    })
	    .state('admin.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',
	      termsRole : "admin"	      
	    })
	    .state('research.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',
	      termsRole : "research"	      
	    });	    
	   
}]);