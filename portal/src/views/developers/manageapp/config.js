angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('developer.manageapp', {
	      url: '/app/:appId',
	      templateUrl: 'views/developers/manageapp/manageapp.html'
	    })
	    .state('developer.registerapp', {
	      url: '/newapp',
	      templateUrl: 'views/developers/manageapp/manageapp.html'
	    })
	   .state('admin.manageapp', {
		      url: '/app/:appId',
		      templateUrl: 'views/developers/manageapp/manageapp.html'
	   });
});