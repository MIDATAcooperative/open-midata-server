angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('member.apps', {
	      url: '/apps?circleId',
	      templateUrl: 'views/shared/apps/apps.html',	      
	      dashId : 'config'
	    })
	    .state('provider.apps', {
	      url: '/apps?circleId',
	      templateUrl: 'views/shared/apps/apps.html',	      
	      dashId : 'config'
	    })
	    .state('research.apps', {
	      url: '/apps?circleId',
	      templateUrl: 'views/shared/apps/apps.html',	      
	      dashId : 'config'
	    })
	    .state('developer.apps', {
	      url: '/apps?circleId',
	      templateUrl: 'views/shared/apps/apps.html',	      
	      dashId : 'config'
	    });
}]);