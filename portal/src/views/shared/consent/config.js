angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('member.newconsent', {
	      url: '/newconsent?authorize&owner&share&request&action',	      
	      templateUrl: 'views/shared/consent/newconsent.html',
	      dashId : 'circles',
	      role : 'MEMBER'
	    })
	    .state('member.editconsent', {
	      url: '/consent/:consentId',	      
	      templateUrl: 'views/shared/consent/editconsent.html',
	      dashId : 'circles'
	    })
	    .state('member.service_consent', {
	    	url: '/consentservice/:consentId?callback&action',
	        templateUrl: 'views/shared/consent/editconsent.html',
	        dashId : 'circles'
	    })
	    .state('provider.newconsent', {
	      url: '/newconsent?authorize&owner&share&request',	      
	      templateUrl: 'views/shared/consent/newconsent.html',
	      dashId : 'circles',
	      role : 'PROVIDER'
	    })
	    .state('provider.editconsent', {
	      url: '/consent/:consentId',	      
	      templateUrl: 'views/shared/consent/editconsent.html',
	      dashId : 'circles'	      
	    })
	    .state('research.editconsent', {
	      url: '/consent/:consentId',	      
	      templateUrl: 'views/shared/consent/editconsent.html',
	      dashId : 'circles'	      
	    });
}]);