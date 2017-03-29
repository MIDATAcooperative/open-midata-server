angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('member.revconsents', {
	      url: '/revconsents',
	      templateUrl: 'views/members/revconsents/revconsents.html',	      
	      dashId : 'circles'
	    })
	    .state('provider.revconsents', {
	      url: '/revconsents',
	      templateUrl: 'views/members/revconsents/revconsents.html',	      
	      dashId : 'circles'
	    });
}]);