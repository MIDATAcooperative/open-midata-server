angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('provider.usergroups', {
	      url: '/teams',
	      templateUrl: 'views/providers/usergroups/usergroups.html'
	    });
});