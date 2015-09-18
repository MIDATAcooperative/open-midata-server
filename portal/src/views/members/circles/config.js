angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.circles', {
	      url: '/circles?circleId',
	      templateUrl: 'views/members/circles/circles.html',	      
	      dashId : 'circles'
	    });
});