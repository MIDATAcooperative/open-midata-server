angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('research.study.records', {
	      url: '/records',
	      templateUrl: 'views/research/studies/studyrecords/studyrecords.html'
	    });
});