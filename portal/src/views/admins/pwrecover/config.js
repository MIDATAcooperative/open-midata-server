angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('admin.pwrecover', {
	      url: '/pwrecover',
	      templateUrl: 'views/admins/pwrecover/pwrecover.html'
	    });
}]);