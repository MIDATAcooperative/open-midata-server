angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.market', {
	      url: '/market?context',
	      templateUrl: 'views/shared/market/market.html' 
	    })
	    .state('provider.market', {
	      url: '/market?context',
	      templateUrl: 'views/shared/market/market.html' 
	    })
	    .state('research.market', {
	      url: '/market?context',
	      templateUrl: 'views/shared/market/market.html' 
	    })
	     .state('developer.market', {
	      url: '/market?context',
	      templateUrl: 'views/shared/market/market.html' 
	    });
});