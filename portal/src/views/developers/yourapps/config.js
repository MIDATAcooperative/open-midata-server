angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('developer.yourapps', {
	      url: '/apps',
	      templateUrl: 'views/developers/yourapps/yourapps.html'
	    });
});