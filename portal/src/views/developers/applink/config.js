angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('developer.applink', {
	      url: '/app/:appId/link',
	      templateUrl: 'views/developers/applink/applink.html'
	    })	    
	   .state('admin.applink', {
		      url: '/app/:appId/applink',
		      templateUrl: 'views/developers/applink/applink.html'
	   });
}]);