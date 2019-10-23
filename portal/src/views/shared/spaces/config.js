angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('member.spaces', {
	      url: '/spaces?spaceId&url&params&app&action',
	      templateUrl: 'views/shared/spaces/spaces.html' 
	    })
	    .state('developer.spaces', {
	      url: '/spaces?spaceId&url&params',
	      templateUrl: 'views/shared/spaces/spaces.html' 
	    })
	    .state('admin.spaces', {
	      url: '/spaces?spaceId&url&params',
	      templateUrl: 'views/shared/spaces/spaces.html' 
	    })
	    .state('research.spaces', {
	      url: '/spaces?spaceId&url&params&user&study&app&action',
	      templateUrl: 'views/shared/spaces/spaces.html' 
	    })
	    .state('research.study.spaces', {
	      url: '/spaces?spaceId&url&params&user&study',
	      templateUrl: 'views/shared/spaces/spaces.html' 
	    })
	     .state('provider.spaces', {
	      url: '/spaces?spaceId&url&params&user&app&action',
	      templateUrl: 'views/shared/spaces/spaces.html' 
	    });
}]);