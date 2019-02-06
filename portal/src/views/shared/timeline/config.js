angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('member.overview', {
	      url: '/timeline?url&params',
	      templateUrl: 'views/shared/timeline/timeline.html' 
	    })
	    .state('developer.timeline', {
	      url: '/timeline/?url&params',
	      templateUrl: 'views/shared/timeline/timeline.html' 
	    })
	    .state('admin.timeline', {
	      url: '/timeline/?url&params',
	      templateUrl: 'views/shared/timeline/timeline.html' 
	    })
	    .state('research.timeline', {
	      url: '/timeline/?url&params&user',
	      templateUrl: 'views/shared/timeline/timeline.html' 
	    })	   
	     .state('provider.timeline', {
	      url: '/timeline/?url&params',
	      templateUrl: 'views/shared/timeline/timeline.html' 
	    });
}]);