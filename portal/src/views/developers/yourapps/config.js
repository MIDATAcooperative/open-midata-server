angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('developer.yourapps', {
	      url: '/apps',
	      templateUrl: 'views/developers/yourapps/yourapps.html'
	    })
	    .state('admin.yourapps2', {
	      url: '/appsmyself',
	      templateUrl: 'views/developers/yourapps/yourapps.html'
	    });
}]);