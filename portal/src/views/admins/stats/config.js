angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('admin.stats', {
	      url: '/stats',
	      templateUrl: 'views/admins/stats/stats.html'
	    });
}]);