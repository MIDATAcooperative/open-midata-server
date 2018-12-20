angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('developer.appsubscriptions', {
	      url: '/app/:appId/subscriptions',
	      templateUrl: 'views/developers/appsubscriptions/appsubscriptions.html'
	    })	    
	   .state('admin.appsubscriptions', {
		      url: '/app/:appId/subscriptions',
		      templateUrl: 'views/developers/appsubscriptions/appsubscriptions.html'
	   });
}]);