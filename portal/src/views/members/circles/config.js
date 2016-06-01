angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.circles', {
	      url: '/circles?circleId',
	      templateUrl: 'views/members/consents/consents.html',	      
	      dashId : 'circles'
	    })
	    .state('provider.circles', {
	      url: '/circles?circleId',
	      templateUrl: 'views/members/consents/consents.html',	      
	      dashId : 'circles'
	    });
});