angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public.registration', {
	      url: '/registration?action&login&given&family&gender&country&birthdate&language&joincode',
	      templateUrl: 'views/members/public/registration/registration.html' 
	    })
	    .state('public.registration_new', {
	      url: '/registration2?action&language&email&login&client_id&redirect_uri&state&code_challenge&code_challenge_method&device_id&role&given&family&gender&country&birthdate&joincode',
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