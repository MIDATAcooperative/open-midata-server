angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.recorddetail', {
	      url: '/record/:recordId',
	      templateUrl: 'views/shared/recorddetail/recorddetail.html' 
	    })
	    .state('provider.recorddetail', {
	      url: '/record/:recordId',
	      templateUrl: 'views/shared/recorddetail/recorddetail.html' 
	    })
	    .state('research.recorddetail', {
	      url: '/record/:recordId',
	      templateUrl: 'views/shared/recorddetail/recorddetail.html' 
	    });
});