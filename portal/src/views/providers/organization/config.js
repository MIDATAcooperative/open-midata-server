angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('provider.organization', {
	      url: '/organization',
	      templateUrl: 'views/providers/organization/organization.html'
	    });
}]);