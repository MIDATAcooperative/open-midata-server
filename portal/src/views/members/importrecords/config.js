angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.importrecords', {
	      url: '/import/:appId',
	      templateUrl: 'views/members/importrecords/importrecords.html'
	    });
});