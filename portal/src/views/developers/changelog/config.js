angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public_developer.changelog', {
	      url: '/changelog',
	      templateUrl: 'views/developers/changelog/changelog_public.html' 
	    })
	   .state('developer.changelog', {
		      url: '/changelog',
		      templateUrl: 'views/developers/changelog/changelog.html' 
		 });
}]);