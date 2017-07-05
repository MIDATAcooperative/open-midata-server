angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('developer.appstats', {
	      url: '/app/:appId/stats',
	      templateUrl: 'views/developers/appstats/appstats.html'
	    })
	    .state('admin.appstats', {
	      url: '/app/:appId/stats',
	      templateUrl: 'views/developers/appstats/appstats.html'
	    });
}]);