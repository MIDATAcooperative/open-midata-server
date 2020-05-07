angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public.unsubscribe', {
	      url: '/unsubscribe',
	      templateUrl: 'views/shared/public/unsubscribe/unsubscribe.html',
	      data : { role : "member" }
	    });
}]);