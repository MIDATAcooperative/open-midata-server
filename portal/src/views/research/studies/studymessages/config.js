angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.study.messages', {
	      url: '/messages',
	      templateUrl: 'views/research/studies/studymessages/studymessages.html'
	    });
}]);