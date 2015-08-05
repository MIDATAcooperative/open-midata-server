angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('providers.memberdetails', {
	      url: '/members/:memberId',
	      templateUrl: 'views/providers/memberdetails/memberdetails.html',
	      dashid : 'memberdetails'
	    });
});