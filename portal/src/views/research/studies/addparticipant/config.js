angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.study.addparticipant', {
	      url: '/addparticipant',
	      templateUrl: 'views/research/studies/addparticipant/addparticipant.html' 
	    });
}]);