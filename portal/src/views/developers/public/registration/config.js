angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public_developer.registration', {
	      url: '/registration',
	      templateUrl: 'views/developers/public/registration/registration.html' 
	    });
}]);