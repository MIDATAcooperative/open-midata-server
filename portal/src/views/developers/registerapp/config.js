angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('developer.registerapp', {
	      url: '/circles',
	      templateUrl: 'views/developers/registerapp/registerapp.html'
	    });
});