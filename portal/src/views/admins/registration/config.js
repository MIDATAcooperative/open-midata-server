angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('admin.registration', {
	      url: '/registration',
	      templateUrl: 'views/admins/registration/registration.html' 
	    });
});