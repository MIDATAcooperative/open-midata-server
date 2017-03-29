angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('member.newconsent', {
	      url: '/newconsent?authorize&owner&share',	      
	      templateUrl: 'views/shared/consent/newconsent.html',
	      dashId : 'circles',
	      role : 'MEMBER'
	    })
	    .state('member.editconsent', {
	      url: '/consent/:consentId',	      
	      templateUrl: 'views/shared/consent/editconsent.html',
	      dashId : 'circles'
	    })
	    .state('provider.newconsent', {
	      url: '/newconsent?authorize&owner&share',	      
	      templateUrl: 'views/shared/consent/newconsent.html',
	      dashId : 'circles',
	      role : 'PROVIDER'
	    })
	    .state('provider.editconsent', {
	      url: '/consent/:consentId',	      
	      templateUrl: 'views/shared/consent/editconsent.html',
	      dashId : 'circles'	      
	    });
}]);