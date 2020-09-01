angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.study.info', {
	      url: '/info',
	      templateUrl: 'views/research/studies/studyinfo/studyinfo.html'
	    })
	     .state('developer.study.info', {
	      url: '/info',
	      templateUrl: 'views/research/studies/studyinfo/studyinfo.html'
	    })
	     .state('admin.study.info', {
	      url: '/info',
	      templateUrl: 'views/research/studies/studyinfo/studyinfo.html'
	    });
}]);