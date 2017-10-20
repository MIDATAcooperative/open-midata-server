angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.addresearcher', {
	      url: '/addresearcher',
	      templateUrl: 'views/research/addresearcher/addresearcher.html' 
	    });
}]);