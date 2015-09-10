angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.overview', {
	      url: '/me',
	      templateUrl: 'views/shared/dashboard/dashboard.html',
	      dashId : 'me'
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
	    });
});