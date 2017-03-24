angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.studies', {
	      url: '/studies',
	      templateUrl: 'views/research/studies/studies.html'
	    })
	    .state('research.study', {
	      url: '/study/:studyId',
	      templateUrl: 'views/research/studies/studynav.html'
	    });
}]);