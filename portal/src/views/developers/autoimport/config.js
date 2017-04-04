angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('developer.autoimport', {
	      url: '/app/:appId/autoimport',
	      templateUrl: 'views/developers/autoimport/autoimport.html'
	    });
}]);