angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('admin.auditlog', {
	      url: '/auditlog',
	      templateUrl: 'views/admins/auditlog/auditlog.html'
	    });
}]);