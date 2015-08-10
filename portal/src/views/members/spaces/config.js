angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.spaces', {
	      url: '/spaces/:spaceId',
	      templateUrl: 'views/members/spaces/spaces.html' 
	    });
});