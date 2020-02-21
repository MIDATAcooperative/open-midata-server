angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('admin.mails', {
	      url: '/mails',
	      templateUrl: 'views/admins/mails/mails.html'
	    });
}]);