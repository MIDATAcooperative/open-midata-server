angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('provider.createpatientrecord', {
	      url: '/create/:memberId/:consentId/:appId',
	      templateUrl: 'views/providers/createrecord/createrecord.html'
	    });
});