angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('admin.usagestats', {
	      url: '/usagestats?appId&date',
	      templateUrl: 'views/admins/usagestats/usagestats.html'
	    });
}]);