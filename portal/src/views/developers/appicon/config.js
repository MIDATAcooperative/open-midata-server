angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('developer.appicon', {
	      url: '/app/:appId/icon',
	      templateUrl: 'views/developers/appicon/appicon.html'
	    })	    
	   .state('admin.appicon', {
		      url: '/app/:appId/appicon',
		      templateUrl: 'views/developers/appicon/appicon.html'
	   });
}]);