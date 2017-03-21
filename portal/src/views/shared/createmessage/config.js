angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('member.createmessage', {
	      url: '/createmessage',
	      templateUrl: 'views/shared/createmessage/createmessage.html'
	    })	    
	    .state('provider.createmessage', {
	      url: '/createmessage',
	      templateUrl: 'views/shared/createmessage/createmessage.html'
	    })
	    .state('research.createmessage', {
	      url: '/createmessage',
	      templateUrl: 'views/shared/createmessage/createmessage.html'
	    });
}]);