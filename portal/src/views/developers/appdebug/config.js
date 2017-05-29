angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('developer.appdebug', {
	      url: '/app/:appId/debug',
	      templateUrl: 'views/developers/appdebug/appdebug.html',
	      nointerceptor : true
	    })
	    .state('admin.appdebug', {
	      url: '/app/:appId/debug',
	      templateUrl: 'views/developers/appdebug/appdebug.html',
	      nointerceptor : true
	    });
}]);