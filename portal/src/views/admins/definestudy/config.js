angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('admin.definestudy', {
	      url: '/definestudy',
	      templateUrl: 'views/admins/definestudy/definestudy.html'
	    });
}]);