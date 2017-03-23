angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    
	    .state('admin.studies', {
	      url: '/studies',
	      templateUrl: 'views/admins/studies/studies.html'
	    })
	    .state('admin.study', {
	      url: '/study/:studyId',
	      templateUrl: 'views/admins/studies/study.html'
	    });
}]);