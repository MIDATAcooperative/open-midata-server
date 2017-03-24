angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public_developer.guide', {
	      url: '/guide',
	      templateUrl: 'views/developers/public/guide/guide.html' 
	    });
}]);