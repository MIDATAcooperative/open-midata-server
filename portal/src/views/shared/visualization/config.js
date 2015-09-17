angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.visualization', {
	      url: '/visualization/:visualizationId?name&context&query',
	      templateUrl: 'views/shared/visualization/visualization.html'
	    })	    
	    .state('provider.visualization', {
	      url: '/visualization/:visualizationId?name&context&query',
	      templateUrl: 'views/shared/visualization/visualization.html'
	    })
	    .state('research.visualization', {
	      url: '/visualization/:visualizationId?name&context&query',
	      templateUrl: 'views/shared/visualization/visualization.html'
	    })
	    .state('developer.visualization', {
	      url: '/visualization/:visualizationId?name&context&query',
	      templateUrl: 'views/shared/visualization/visualization.html'
	    });
});