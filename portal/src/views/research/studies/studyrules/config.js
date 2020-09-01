angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.study.rules', {
	      url: '/rules',
	      mode : 'study',
	      templateUrl: 'views/research/studies/studyrules/studyrules.html'
	    })
	     .state('developer.study.rules', {
	      url: '/rules',
	      mode : 'study',
	      templateUrl: 'views/research/studies/studyrules/studyrules.html'
	    })
	     .state('admin.study.rules', {
	      url: '/rules',
	      mode : 'study',
	      templateUrl: 'views/research/studies/studyrules/studyrules.html'
	    });
}]);