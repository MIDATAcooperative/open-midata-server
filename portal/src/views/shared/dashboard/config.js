angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.overview', {
	      url: '/overview',
	      templateUrl: 'views/shared/dashboard/dashboard.html',
	      dashid : 'overview'
	    })
	    .state('member.dashboard', {
	      url: '/dashboard/:dashid',
	      templateUrl: 'views/shared/dashboard/dashboard.html',	      
	    })
	    .state('provider.dashboard', {
	      url: '/dashboard',
	      templateUrl: 'views/shared/dashboard/dashboard.html',
	      dashid : 'overview'
	    })
	    .state('research.dashboard', {
	      url: '/dashboard',
	      templateUrl: 'views/shared/dashboard/dashboard.html',
	      dashid : 'overview'
	    });
});