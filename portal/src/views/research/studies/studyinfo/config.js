angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.study.info', {
	      url: '/info',
	      templateUrl: 'views/research/studies/studyinfo/studyinfo.html'
	    });
}]);