angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public_research.registration', {
	      url: '/registration',
	      templateUrl: 'views/research/public/registration/registration.html' 
	    })
	     .state('developer.research_registration', {
	      url: '/register_research?developer',
	      templateUrl: 'views/research/public/registration/registration.html'
	    })
	     .state('admin.research_registration', {
	      url: '/register_research?developer',
	      templateUrl: 'views/research/public/registration/registration.html'
	    });
}]);