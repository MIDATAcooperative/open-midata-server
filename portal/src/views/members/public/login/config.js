angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public.login', {
	      url: '/login?action&login&role',
	      templateUrl: 'views/members/public/login/login.html' 
	    });
}]);