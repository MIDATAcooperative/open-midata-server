angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('member.circles', {
	      url: '/circles?circleId',
	      templateUrl: 'views/members/consents/consents.html',	      
	      dashId : 'circles',
	      types : ['CIRCLE','HEALTHCARE']
	    })
	    .state('provider.circles', {
	      url: '/circles?circleId',
	      templateUrl: 'views/members/consents/consents.html',	      
	      dashId : 'circles'
	    })
	    .state('research.circles', {
	      url: '/circles?circleId',
	      templateUrl: 'views/members/consents/consents.html',	      
	      dashId : 'circles'
	    });
}]);