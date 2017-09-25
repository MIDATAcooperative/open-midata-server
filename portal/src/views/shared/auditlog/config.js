angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('admin.auditlog', {
	      url: '/auditlog',
	      templateUrl: 'views/shared/auditlog/auditlog.html'
	    })
	    .state('member.auditlog', {
	      url: '/auditlog',
	      templateUrl: 'views/shared/auditlog/auditlog.html'
	    })
	    .state('research.auditlog', {
	      url: '/auditlog',
	      templateUrl: 'views/shared/auditlog/auditlog.html'
	    })
	    .state('provider.auditlog', {
	      url: '/auditlog',
	      templateUrl: 'views/shared/auditlog/auditlog.html'
	    });
}]);