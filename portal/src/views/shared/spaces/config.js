angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.spaces', {
	      url: '/spaces/:spaceId',
	      templateUrl: 'views/shared/spaces/spaces.html' 
	    })
	    .state('developer.spaces', {
	      url: '/spaces/:spaceId',
	      templateUrl: 'views/shared/spaces/spaces.html' 
	    })
	    .state('research.spaces', {
	      url: '/spaces/:spaceId',
	      templateUrl: 'views/shared/spaces/spaces.html' 
	    })
	     .state('provider.spaces', {
	      url: '/spaces/:spaceId',
	      templateUrl: 'views/shared/spaces/spaces.html' 
	    });
});