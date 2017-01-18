angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('provider.editusergroup', {
	      url: '/teams/:groupId',
	      templateUrl: 'views/providers/editusergroup/editusergroup.html',
	      dashId : 'usergroup'
	    })
	    .state('provider.newusergroup', {
	      url: '/newteam',
	      templateUrl: 'views/providers/editusergroup/newusergroup.html'
	    });
});