angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('admin.defineplugin', {
	      url: '/defineplugin',
	      templateUrl: 'views/admins/defineplugin/defineplugin.html'
	    });
}]);