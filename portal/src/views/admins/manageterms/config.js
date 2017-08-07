angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider	    
	   .state('admin.newterms', {
		  url: '/newterms/',
		  templateUrl: 'views/admins/manageterms/manageterms.html',
		  allowDelete : false
	   });
}]);