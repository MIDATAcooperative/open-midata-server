angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.studydetails', {
	      url: '/study/:studyId',
	      templateUrl: 'views/shared/studydetails/studydetails.html' 
	    })
	    .state('provider.studydetails', {
	      url: '/study/:studyId',
	      templateUrl: 'views/shared/studydetails/studydetails.html' 
	    });	    
});