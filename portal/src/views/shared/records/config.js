angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.records', {
	      url: '/records',
	      templateUrl: 'views/shared/records/records.html' 
	    })
	    .state('member.delete_records', {
	      url: '/records-delete',
	      templateUrl: 'views/shared/records/records.html',
	      allowDelete : true,
	    })
	    .state('member.recordsharing', {
	      url: '/records/:selectedType/:selected',
	      templateUrl: 'views/shared/records/records.html' 
	    })
	    .state('provider.recordsharing', {
	      url: '/records/:selectedType/:selected',
	      templateUrl: 'views/shared/records/records.html' 
	    })
	    .state('developer.delete_records', {
	      url: '/records-delete',
		  templateUrl: 'views/shared/records/records.html',
		  allowDelete : true,
		})
	    .state('developer.records', {
	      url: '/records',
	      templateUrl: 'views/shared/records/records.html' 
	    })
	    .state('developer.recordsharing', {
	      url: '/records/:selectedType/:selected',
	      templateUrl: 'views/shared/records/records.html' 
	    })
	    .state('research.records', {
	      url: '/records',
	      templateUrl: 'views/shared/records/records.html',
	      role : 'research'
	    });
});