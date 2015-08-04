angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.dashboard', {
	      url: '/member/dashboard',
	      templateUrl: 'views/shared/dashboard/dashboard.html' 
	    });
});