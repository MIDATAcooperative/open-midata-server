angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.messages', {
	      url: '/messages',
	      templateUrl: 'views/shared/messages/messages.html' 
	    })
	    .state('provider.messages', {
	      url: '/messages',
	      templateUrl: 'views/shared/messages/messages.html' 
	    })
	    .state('research.messages', {
	      url: '/messages',
	      templateUrl: 'views/shared/messages/messages.html' 
	    })
	    .state('developer.messages', {
	      url: '/messages',
	      templateUrl: 'views/shared/messages/messages.html' 
	    });
});