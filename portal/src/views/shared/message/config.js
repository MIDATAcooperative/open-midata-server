angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.message', {
	      url: '/message/:messageId',
	      templateUrl: 'views/shared/message/mamessage.html' 
	    })
	    .state('provider.message', {
	      url: '/message/:messageId',
	      templateUrl: 'views/shared/message/message.html' 
	    })
	    .state('research.message', {
	      url: '/message/:messageId',
	      templateUrl: 'views/shared/message/message.html' 
	    })
	    .state('admin.message', {
	      url: '/message/:messageId',
	      templateUrl: 'views/shared/message/message.html' 
	    });
});