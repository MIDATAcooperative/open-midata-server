angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public.service', {
	      url: '/service?app&consent&login&callback',
	      templateUrl: 'views/shared/public/service/service.html' 
	    })
	    .state('goplugin', {
	      url: '/apps/:pluginName?login',
	      templateUrl: 'views/shared/public/service/service.html' 
	    })
	    .state('member.service2', {
	      url: '/service2?app&consent&login&callback&action',
	      templateUrl: 'views/shared/public/service/empty.html' 
	    })
	    .state('member.serviceleave', {
	      url: '/serviceleave?app&callback',
	      templateUrl: 'views/shared/public/service/serviceleave.html' 
	    });
}]);