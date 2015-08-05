angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.spaces', {
	      url: '/spaces',
	      templateUrl: 'views/members/spaces/spaces.html' 
	    });
});