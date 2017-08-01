angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',	      
	    })
	    .state('admin.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',	      
	    });	    
	   
}]);