angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('developer.testusers', {
	      url: '/testusers',
	      templateUrl: 'views/developers/testusers/testusers.html'
	    });
});