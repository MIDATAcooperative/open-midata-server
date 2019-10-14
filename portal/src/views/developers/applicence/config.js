angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('developer.applicence', {
	      url: '/app/:appId/licence',
	      templateUrl: 'views/developers/applicence/applicence.html'
	    })	    
	   .state('admin.applicence', {
		      url: '/app/:appId/licence',
		      templateUrl: 'views/developers/applicence/applicence.html'
	   });
}]);