angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('provider.addpatient', {
	      url: '/addpatient?email',
	      templateUrl: 'views/providers/addpatient/addpatient.html' 
	    });
}]);