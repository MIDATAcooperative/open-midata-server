angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.studydetails', {
	      url: '/study/:studyId',
	      templateUrl: 'views/shared/studydetails/studydetails.html',
	      dashId : 'studydetails'
	    })
	    .state('provider.studydetails', {
	      url: '/study/:studyId',
	      templateUrl: 'views/shared/studydetails/studydetails.html',
	      dashId : 'studydetails'
	    });	    
});