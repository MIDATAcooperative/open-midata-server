angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.createstudy', {
	      url: '/createstudy',
	      templateUrl: 'views/research/createstudy/createstudy.html'
	    });
}]);