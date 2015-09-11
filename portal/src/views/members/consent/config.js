angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.newconsent', {
	      url: '/newconsent?authorize',	      
	      templateUrl: 'views/members/consent/newconsent.html',
	      dashId : 'circles'
	    });
});