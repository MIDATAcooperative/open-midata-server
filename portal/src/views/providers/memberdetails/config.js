angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('provider.memberdetails', {
	      url: '/patient?url&params&user',
	      templateUrl: 'views/providers/memberdetails/memberdetails.html',
	      dashId : 'memberdetails'
	    });
}]);