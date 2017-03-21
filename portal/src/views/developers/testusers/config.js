angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('developer.testusers', {
	      url: '/testusers',
	      templateUrl: 'views/developers/testusers/testusers.html'
	    });
}]);