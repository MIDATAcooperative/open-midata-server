angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('research.study.overview', {
	      url: '/overview',
	      templateUrl: 'views/research/studies/studyoverview/studyoverview.html'
	    });
});