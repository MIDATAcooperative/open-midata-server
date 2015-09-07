angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.records', {
	      url: '/records',
	      templateUrl: 'views/members/records/records.html' 
	    })
	    .state('member.recordsharing', {
	      url: '/records/:selectedType/:selected',
	      templateUrl: 'views/members/records/records.html' 
	    });
});