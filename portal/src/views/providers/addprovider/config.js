angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('provider.addprovider', {
	      url: '/addprovider',
	      templateUrl: 'views/providers/addprovider/addprovider.html' 
	    });
}]);