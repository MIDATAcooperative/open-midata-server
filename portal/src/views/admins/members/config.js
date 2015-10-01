angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('admin.members', {
	      url: '/members',
	      templateUrl: 'views/admins/members/members.html'
	    });
});