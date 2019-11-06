angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public.registration', {
	      url: '/registration?action&login',
	      templateUrl: 'views/members/public/registration/registration.html' 
	    })
	    .state('public.registration_new', {
	      url: '/registration2',
	      templateUrl: 'views/members/public/registration/registration_new.html' 
	    })
	    .state('developer.member_registration', {
	      url: '/register_member?developer',
	      templateUrl: 'views/members/public/registration/registration.html'
	    })
	    .state('admin.member_registration', {
	      url: '/register_member?developer',
	      templateUrl: 'views/members/public/registration/registration.html'
	    });
}]);