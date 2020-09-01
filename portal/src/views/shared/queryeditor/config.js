angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.study.queryeditor', {
	      url: '/query',
	      templateUrl: 'views/shared/queryeditor/queryeditor.html',
	      data : { mode: 'study' }
	    })
	    .state('developer.study.queryeditor', {
	      url: '/query',
	      templateUrl: 'views/shared/queryeditor/queryeditor.html',
	      data : { mode: 'study' }
	    })
	     .state('admin.study.queryeditor', {
	      url: '/query',
	      templateUrl: 'views/shared/queryeditor/queryeditor.html',
	      data : { mode: 'study' }
	    })
	    .state('developer.appquery', {
	      url: '/app/:appId/query',
	      templateUrl: 'views/shared/queryeditor/queryeditor.html',
	      data : { mode: 'app' }
	    })
	    .state('admin.appquery', {
	      url: '/app/:appId/query',
	      templateUrl: 'views/shared/queryeditor/queryeditor.html',
	      data : { mode: 'app' }
	    });
}]);