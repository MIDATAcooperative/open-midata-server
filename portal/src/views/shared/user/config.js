angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.user', {
	      url: '/user/:userId',
	      templateUrl: 'views/shared/user/user.html' 
	    })
	    .state('provider.user', {
	      url: '/user/:userId',
	      templateUrl: 'views/shared/user/user.html' 
	    })
	    .state('research.user', {
	      url: '/user/:userId',
	      templateUrl: 'views/shared/user/user.html' 
	    });	    
});