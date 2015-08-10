angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('research.study.participants', {
	      url: '/participants',
	      templateUrl: 'views/research/studies/studyparticipants/studyparticipants.html'
	    });
});