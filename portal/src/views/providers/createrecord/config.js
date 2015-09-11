angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('provider.createrecord', {
	      url: '/create/:memberId/:consentId/:appId',
	      templateUrl: 'views/providers/createrecord/createrecord.html'
	    });
});