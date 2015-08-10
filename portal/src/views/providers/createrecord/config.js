angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('provider.createrecord', {
	      url: '/create/:userId/:appId',
	      templateUrl: 'views/providers/createrecord/createrecord.html'
	    });
});