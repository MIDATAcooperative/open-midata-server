angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider	    
	   .state('admin.newlicence', {
		  url: '/newlicence',
		  templateUrl: 'views/admins/addlicence/addlicence.html',	
		  allowDelete : false
	   })
	   .state('admin.addlicence', {
		  url: '/licence/:licenceId',
		  templateUrl: 'views/admins/addlicence/addlicence.html',
		  allowDelete : true
	   });	   
}]);