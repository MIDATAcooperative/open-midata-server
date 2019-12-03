angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public.registration', {
	      url: '/registration?action&login&given&family&gender&country&birthdate&language',
	      templateUrl: 'views/members/public/registration/registration.html' 
	    })
	    .state('public.registration_new', {
	      url: '/registration2?login&given&family&gender&country&birthdate&language',
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