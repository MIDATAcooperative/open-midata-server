angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('admin.content', {
	      url: '/content',
	      templateUrl: 'views/admins/content/content.html'
	    });
});