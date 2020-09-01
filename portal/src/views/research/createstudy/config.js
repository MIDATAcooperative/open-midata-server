angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.createstudy', {
	      url: '/createstudy',
	      templateUrl: 'views/research/createstudy/createstudy.html'
	    })
	    .state('research.study.description', {
	      url: '/description',
	      templateUrl: 'views/research/createstudy/createstudy.html'	    
	    })
	    .state('developer.createstudy', {
	      url: '/createstudy',
	      templateUrl: 'views/research/createstudy/createstudy.html'
	    })
	    .state('developer.study.description', {
	      url: '/description',
	      templateUrl: 'views/research/createstudy/createstudy.html'	    
	    })
	    .state('admin.createstudy', {
	      url: '/createstudy',
	      templateUrl: 'views/research/createstudy/createstudy.html'
	    })
	    .state('admin.study.description', {
	      url: '/description',
	      templateUrl: 'views/research/createstudy/createstudy.html'	    
	    })
	    ;
}]);