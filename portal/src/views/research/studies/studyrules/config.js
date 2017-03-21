angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.study.rules', {
	      url: '/rules',
	      templateUrl: 'views/research/studies/studyrules/studyrules.html'
	    });
}]);