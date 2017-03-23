angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public_research.login', {
	      url: '/login',
	      templateUrl: 'views/research/public/login/login.html' 
	    });
}]);