angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('developer.appmessages', {
	      url: '/app/:appId/messages',
	      templateUrl: 'views/developers/appmessages/appmessages.html'
	    })	    
	   .state('admin.appmessages', {
		      url: '/app/:appId/messages',
		      templateUrl: 'views/developers/appmessages/appmessages.html'
	   });
}]);