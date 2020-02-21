angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider	    
	   .state('admin.newmail', {
		  url: '/newmail',
		  templateUrl: 'views/admins/managemails/managemails.html',
		  allowDelete : false
	   })
	   .state('admin.managemails', {
		  url: '/mail/:mailId',
		  templateUrl: 'views/admins/managemails/managemails.html',
		  allowDelete : true
	   });
}]);