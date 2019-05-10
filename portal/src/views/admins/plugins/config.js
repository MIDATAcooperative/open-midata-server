angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('admin.yourapps', {
	      url: '/apps?creator',
	      templateUrl: 'views/admins/plugins/plugins.html'
	    });
}]);