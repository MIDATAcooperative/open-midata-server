angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.study.sharing', {
	      url: '/sharing',
	      templateUrl: 'views/research/studies/sharing/sharing.html'
	    });
}]);