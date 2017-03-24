angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.study.overview', {
	      url: '/overview',
	      templateUrl: 'views/research/studies/studyoverview/studyoverview.html'
	    });
}]);