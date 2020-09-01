angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.studies', {
	      url: '/studies',
	      templateUrl: 'views/research/studies/studies.html'
	    })
	    .state('research.study', {
	      url: '/study/:studyId',
	      templateUrl: 'views/research/studies/studynav.html',
	      allowExecution : true
	    })
	    .state('developer.studies', {
	      url: '/studies',
	      templateUrl: 'views/research/studies/studies.html'
	    })
	    .state('developer.study', {
	      url: '/study/:studyId',
	      templateUrl: 'views/research/studies/studynav_simple.html'
	    })
	    .state('admin.studies', {
	      url: '/studies',
	      templateUrl: 'views/research/studies/studies.html'
	    })
	    .state('admin.study', {
	      url: '/study/:studyId',
	      templateUrl: 'views/research/studies/studynav_simple.html'
	    });
}]);