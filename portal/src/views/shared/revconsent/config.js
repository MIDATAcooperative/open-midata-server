angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider	 
	    .state('member.showconsent', {
	      url: '/revconsent/:consentId',	      
	      templateUrl: 'views/shared/consent/editconsent.html',
	      dashId : 'circles'
	    })	 
	    .state('provider.showconsent', {
	      url: '/revconsent/:consentId',	      
	      templateUrl: 'views/shared/consent/editconsent.html',
	      dashId : 'circles'
	    });
}]);