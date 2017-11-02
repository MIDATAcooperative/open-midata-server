angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	   .state('research.study.team', {
		   url: '/team',
		   templateUrl: 'views/research/studies/studyteam/studyteam.html'
	   });
}]);