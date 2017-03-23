angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public.login', {
	      url: '/login',
	      templateUrl: 'views/members/public/login/login.html' 
	    });
}]);