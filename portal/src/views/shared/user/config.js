angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('member.user', {
	      url: '/user/:userId',
	      templateUrl: 'views/shared/user/user.html' 
	    })
	    .state('member.upgrade', {
	      url: '/upgrade/:role',
	      templateUrl: 'views/shared/user/upgrade.html' 
	    })
	    .state('provider.user', {
	      url: '/user/:userId',
	      templateUrl: 'views/shared/user/user.html' 
	    })
	    .state('research.user', {
	      url: '/user/:userId',
	      templateUrl: 'views/shared/user/user.html' 
	    })
	    .state('admin.user', {
	      url: '/user/:userId',
	      templateUrl: 'views/shared/user/user.html' 
	    })
	    .state('developer.user', {
	      url: '/user/:userId',
	      templateUrl: 'views/shared/user/user.html' 
	    });	    
}]);