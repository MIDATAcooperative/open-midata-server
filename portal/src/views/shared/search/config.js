angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.search', {
	      url: '/search?query',
	      templateUrl: 'views/shared/search/search.html' 
	    })
	    .state('provider.search', {
	      url: '/search?query',
	      templateUrl: 'views/shared/search/search.html' 
	    })
	    .state('research.search', {
	      url: '/search?query',
	      templateUrl: 'views/shared/search/search.html' 
	    });
});