angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('research.study.codes', {
	      url: '/codes',
	      templateUrl: 'views/research/studies/codes/codes.html'
	    });
});