angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.study.overview', {
	      url: '/overview',
	      templateUrl: 'views/research/studies/studyoverview/studyoverview.html'
	    })
	     .state('developer.study.overview', {
	      url: '/overview',
	      templateUrl: 'views/research/studies/studyoverview/studyoverview.html'
	    })
	     .state('admin.study.overview', {
	      url: '/overview',
	      templateUrl: 'views/research/studies/studyoverview/studyoverview.html'
	    });
}]);