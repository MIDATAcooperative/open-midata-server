angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('provider.memberdetails', {
	      url: '/members/:memberId',
	      templateUrl: 'views/providers/memberdetails/memberdetails.html',
	      dashId : 'memberdetails'
	    });
});