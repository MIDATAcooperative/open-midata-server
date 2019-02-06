angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public_provider.registration', {
	      url: '/registration',
	      templateUrl: 'views/providers/public/registration/registration.html' 
	    })
	     .state('developer.provider_registration', {
	      url: '/register_provider?developer',
	      templateUrl: 'views/providers/public/registration/registration.html'
	    })
	     .state('admin.provider_registration', {
	      url: '/register_provider?developer',
	      templateUrl: 'views/providers/public/registration/registration.html'
	    });
}]);