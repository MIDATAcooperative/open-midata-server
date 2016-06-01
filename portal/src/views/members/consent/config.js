angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.newconsent', {
	      url: '/newconsent?authorize',	      
	      templateUrl: 'views/members/consent/newconsent.html',
	      dashId : 'circles'
	    })
	    .state('member.editconsent', {
	      url: '/consent/:consentId',	      
	      templateUrl: 'views/members/consent/editconsent.html',
	      dashId : 'circles'
	    })
	    .state('provider.newconsent', {
	      url: '/newconsent?authorize',	      
	      templateUrl: 'views/members/consent/newconsent.html',
	      dashId : 'circles'
	    })
	    .state('provider.editconsent', {
	      url: '/consent/:consentId',	      
	      templateUrl: 'views/members/consent/editconsent.html',
	      dashId : 'circles'
	    });
});