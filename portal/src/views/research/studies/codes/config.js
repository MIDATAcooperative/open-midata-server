angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.study.codes', {
	      url: '/codes',
	      templateUrl: 'views/research/studies/codes/codes.html'
	    })
	    .state('developer.study.codes', {
	      url: '/codes',
	      templateUrl: 'views/research/studies/codes/codes.html'
	    })
	    .state('admin.study.codes', {
	      url: '/codes',
	      templateUrl: 'views/research/studies/codes/codes.html'
	    });
}]);