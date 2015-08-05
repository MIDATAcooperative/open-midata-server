angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.circles', {
	      url: '/circles',
	      templateUrl: 'views/members/circles/circles.html',
	      dashid : 'circles'
	    });
});