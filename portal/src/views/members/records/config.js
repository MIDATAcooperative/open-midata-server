angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.records', {
	      url: '/records',
	      templateUrl: 'views/members/records/records.html' 
	    });
});