angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.study.actions', {
	      url: '/actions',
	      templateUrl: 'views/research/studies/studyactions/studyactions.html',
	      dashId : 'studyactions',
	      allowPersonalApps : true,
	    })
	    .state('developer.study.actions', {
	      url: '/actions',
	      templateUrl: 'views/research/studies/studyactions/studyactions.html',
	      dashId : 'studyactions'
	    })
	    .state('admin.study.actions', {
	      url: '/actions',
	      templateUrl: 'views/research/studies/studyactions/studyactions.html',
	      dashId : 'studyactions'
	    });
}]);