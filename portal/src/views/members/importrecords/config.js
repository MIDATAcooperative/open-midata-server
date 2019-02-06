angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('member.importrecords', {
	      url: '/import/:spaceId',
	      templateUrl: 'views/members/importrecords/importrecords.html'
	    })
	    .state('developer.importrecords', {
	      url: '/import/:spaceId',
	      templateUrl: 'views/members/importrecords/importrecords.html'
	    })
	    .state('admin.importrecords', {
	      url: '/import/:spaceId',
	      templateUrl: 'views/members/importrecords/importrecords.html'
	    });
}]);