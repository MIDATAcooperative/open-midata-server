angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
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
	    })
	    .state('admin.user', {
	      url: '/user/:userId',
	      templateUrl: 'views/shared/user/user.html' 
	    })
	    .state('developer.user', {
	      url: '/user/:userId',
	      templateUrl: 'views/shared/user/user.html' 
	    })
	    
	    
	     .state('member.accountwipe', {
	      url: '/accountwipe',
	      templateUrl: 'views/shared/user/accountwipe.html' 
	    })
	   
	    .state('provider.accountwipe', {
	      url: '/accountwipe',
	      templateUrl: 'views/shared/user/accountwipe.html' 
	    })
	    .state('research.accountwipe', {
	      url: '/accountwipe',
	      templateUrl: 'views/shared/user/accountwipe.html' 
	    })	    
	    .state('developer.accountwipe', {
	      url: '/accountwipe',
	      templateUrl: 'views/shared/user/accountwipe.html' 
	    })
	    .state('admin.accountwipe', {
	      url: '/accountwipe',
	      templateUrl: 'views/shared/user/accountwipe.html' 
	    });	 
	       
}]);