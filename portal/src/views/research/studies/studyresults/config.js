angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.study.results', {
	      url: '/results',
	      templateUrl: 'views/research/studies/studyresults/studyresults.html'
	    });
}]);