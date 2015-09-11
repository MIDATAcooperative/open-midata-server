angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('provider.usevisualization', {
	      url: '/usevis/:memberId/:consentId/:visualizationId',
	      templateUrl: 'views/providers/usevisualization/usevisualization.html'
	    });
});