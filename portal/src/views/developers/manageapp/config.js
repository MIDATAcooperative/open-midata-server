angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('developer.manageapp', {
	      url: '/app/:appId',
	      templateUrl: 'views/developers/manageapp/overview.html',
	      allowDelete : false,
	      allowStudyConfig : false,
	      allowExport : false
	    })
	    .state('developer.editapp', {
	      url: '/app/:appId/edit',
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
		      templateUrl: 'views/developers/manageapp/overview.html',
		      allowDelete : true,
		      allowStudyConfig : true,
		      allowExport : true
	   })
	   .state('admin.editapp', {
		      url: '/app/:appId/edit',
		      templateUrl: 'views/developers/manageapp/manageapp.html',
		      allowDelete : true,
		      allowStudyConfig : true,
		      allowExport : true
	   })
	   .state('admin.registerapp', {
	      url: '/newapp',
	      templateUrl: 'views/developers/manageapp/manageapp.html',
	      allowDelete : false,
	      allowStudyConfig : false,
	      allowExport : false
	    });
}]);