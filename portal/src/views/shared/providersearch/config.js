angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.providersearch', {
	      url: '/providers/search?city&name',
	      templateUrl: 'views/shared/providersearch/providersearch.html' 
	    });
});