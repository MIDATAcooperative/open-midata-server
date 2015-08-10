angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('provider.usevisualization', {
	      url: '/usevis/:userId/:visualizationId',
	      templateUrl: 'views/providers/usevisualization/usevisualization.html'
	    });
});