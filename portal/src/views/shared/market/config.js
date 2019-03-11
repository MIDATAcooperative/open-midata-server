angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('member.market', {
	      url: '/market?context&tag',
	      templateUrl: 'views/shared/market/market.html' 
	    })
	    .state('provider.market', {
	      url: '/market?context&tag&user',
	      templateUrl: 'views/shared/market/market.html' 
	    })
	    .state('research.market', {
	      url: '/market?context&tag&study&user',
	      templateUrl: 'views/shared/market/market.html' 
	    })
	     .state('developer.market', {
	      url: '/market?context&tag',
	      templateUrl: 'views/shared/market/market.html' 
	    })
	    .state('admin.market', {
	      url: '/market?context&tag',
	      templateUrl: 'views/shared/market/market.html' 
	    });
}]);