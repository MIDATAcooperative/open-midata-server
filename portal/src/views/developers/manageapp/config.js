angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('developer.manageapp', {
	      url: '/app/:appId',
	      templateUrl: 'views/developers/manageapp/manageapp.html',
	      allowDelete : false,
	      allowStudyConfig : false,
	      allowExport : false
	    })
	    .state('developer.registerapp', {
	      url: '/newapp',
	      templateUrl: 'views/developers/manageapp/manageapp.html',
	      allowDelete : false,
	      allowStudyConfig : false,
	      allowExport : false
	    })
	   .state('admin.manageapp', {
		      url: '/app/:appId',
		      templateUrl: 'views/developers/manageapp/manageapp.html',
		      allowDelete : true,
		      allowStudyConfig : true,
		      allowExport : true
	   });
}]);