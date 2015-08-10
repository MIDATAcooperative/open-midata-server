angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.createrecord', {
	      url: '/createrecord/:appId',
	      templateUrl: 'views/shared/createrecord/createrecord.html'
	    })	    	    
	    .state('research.createrecord', {
	      url: '/createrecord/:appId',
	      templateUrl: 'views/shared/createrecord/createrecord.html'
	    });
});