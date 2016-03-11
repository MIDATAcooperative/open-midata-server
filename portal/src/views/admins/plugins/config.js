angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('admin.plugins', {
	      url: '/apps',
	      templateUrl: 'views/admins/plugins/plugins.html'
	    });
});