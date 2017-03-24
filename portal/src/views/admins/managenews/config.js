angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider	    
	   .state('admin.newnews', {
		  url: '/newnews/',
		  templateUrl: 'views/admins/managenews/managenews.html',
		  allowDelete : false
	   })
	   .state('admin.managenews', {
		  url: '/news/:newsId',
		  templateUrl: 'views/admins/managenews/managenews.html',
		  allowDelete : true
	   });
}]);