angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('admin.licenses', {
	      url: '/licenses',
	      templateUrl: 'views/admins/licenses/licenses.html'
	    });
}]);