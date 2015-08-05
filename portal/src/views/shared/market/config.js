angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.market', {
	      url: '/market',
	      templateUrl: 'views/shared/market/market.html' 
	    })
	    .state('provider.market', {
	      url: '/market',
	      templateUrl: 'views/shared/market/market.html' 
	    })
	    .state('research.market', {
	      url: '/market',
	      templateUrl: 'views/shared/market/market.html' 
	    });
});