angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('research.study.participant', {
	      url: '/participant/:participantId',
	      templateUrl: 'views/research/studies/studyparticipant/studyparticipant.html',
	      dashId : 'studyparticipant'
	    });
});