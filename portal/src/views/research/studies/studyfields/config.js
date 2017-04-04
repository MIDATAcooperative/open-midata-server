angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.study.fields', {
	      url: '/fields',
	      templateUrl: 'views/research/studies/studyfields/studyfields.html'
	    });
}]);