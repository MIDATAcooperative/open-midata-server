angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('admin.members', {
	      url: '/members',
	      templateUrl: 'views/admins/members/members.html'
	    });
}]);