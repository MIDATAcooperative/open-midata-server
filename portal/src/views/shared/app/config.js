angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.app', {
	      url: '/app/:appId',
	      templateUrl: 'views/shared/app/app.html'
	    })	    
	    .state('provider.app', {
	      url: '/app/:appId',
	      templateUrl: 'views/shared/app/app.html'
	    })
	    .state('research.app', {
	      url: '/app/:appId',
	      templateUrl: 'views/shared/app/app.html'
	    });
});