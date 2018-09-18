angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('member.overview2', {
	      url: '/me',
	      templateUrl: 'views/shared/dashboard/dashboard.html',
	      dashId : 'me'
	    })
	    .state('developer.sandbox', {
	      url: '/sandbox',
	      templateUrl: 'views/shared/dashboard/dashboard.html',
	      dashId : 'sandbox'
	    })
	    .state('member.dashboard', {
	      url: '/dashboard/:dashId',
	      templateUrl: 'views/shared/dashboard/dashboard.html',	      
	    })
	    .state('provider.dashboard', {
	      url: '/dashboard/:dashId',
	      templateUrl: 'views/shared/dashboard/dashboard.html'
	    
	    })
	    .state('research.dashboard', {
	      url: '/dashboard/:dashId',
	      templateUrl: 'views/shared/dashboard/dashboard.html'	      
	    })
	    .state('developer.dashboard', {
	      url: '/dashboard/:dashId',
	      templateUrl: 'views/shared/dashboard/dashboard.html'	     
	    });
}]);