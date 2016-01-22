angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.changepassphrase', {
	      url: '/changepassphrase',
	      templateUrl: 'views/shared/changepassphrase/changepassphrase.html' 
	    })
	    .state('provider.changepassphrase', {
	      url: '/changepassphrase',
	      templateUrl: 'views/shared/changepassphrase/changepassphrase.html' 
	    })
	    .state('research.changepassphrase', {
	      url: '/changepassphrase',
	      templateUrl: 'views/shared/changepassphrase/changepassphrase.html' 
	    });	    
});