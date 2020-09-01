angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    
	    .state('admin.astudies', {
	      url: '/astudies',
	      templateUrl: 'views/admins/studies/studies.html'
	    })
	    .state('admin.astudy', {
	      url: '/astudy/:studyId',
	      templateUrl: 'views/admins/studies/study.html'
	    });
}]);