angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('research.study.actions', {
	      url: '/actions',
	      templateUrl: 'views/research/studies/studyactions/studyactions.html',
	      dashId : 'studyactions'
	    });
});