angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('admin.address', {
	      url: '/members/:userId',
	      templateUrl: 'views/admins/address/address.html'
	    });
}]);