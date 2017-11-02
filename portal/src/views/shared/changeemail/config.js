angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('member.changeemail', {
	      url: '/changeemail',
	      templateUrl: 'views/shared/changeemail/changeemail.html' 
	    })
	    .state('provider.changeemail', {
	      url: '/changeemail',
	      templateUrl: 'views/shared/changeemail/changeemail.html' 
	    })
	    .state('research.changeemail', {
	      url: '/changeemail',
	      templateUrl: 'views/shared/changeemail/changeemail.html' 
	    })
	    .state('admin.changeemail', {
	      url: '/changeemail?userId',
	      templateUrl: 'views/shared/changeemail/changeemail.html' 
	    })
	    .state('developer.changeemail', {
	      url: '/changeemail',
	      templateUrl: 'views/shared/changeemail/changeemail.html' 
	    });	    
}]);