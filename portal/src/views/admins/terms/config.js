angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('admin.viewterms', {
	      url: '/viewterms',
	      templateUrl: 'views/admins/terms/terms.html'
	    });
}]);