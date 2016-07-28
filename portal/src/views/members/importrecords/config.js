angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.importrecords', {
	      url: '/import/:spaceId',
	      templateUrl: 'views/members/importrecords/importrecords.html'
	    })
	    .state('developer.importrecords', {
	      url: '/import/:spaceId',
	      templateUrl: 'views/members/importrecords/importrecords.html'
	    });
});