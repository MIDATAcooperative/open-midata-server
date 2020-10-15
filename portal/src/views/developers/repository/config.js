angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('developer.repository', {
	      url: '/app/:appId/repository',
	      templateUrl: 'views/developers/repository/repository.html'
	    })	    
	   .state('admin.repository', {
		      url: '/app/:appId/repository',
		      templateUrl: 'views/developers/repository/repository.html'
	   });
}]);