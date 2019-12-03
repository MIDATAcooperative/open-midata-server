angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.study.rules', {
	      url: '/rules',
	      mode : 'study',
	      templateUrl: 'views/research/studies/studyrules/studyrules.html'
	    });
}]);