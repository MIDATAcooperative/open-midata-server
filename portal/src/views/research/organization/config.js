angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.organization', {
	      url: '/organization',
	      templateUrl: 'views/research/organization/organization.html'
	    });
}]);