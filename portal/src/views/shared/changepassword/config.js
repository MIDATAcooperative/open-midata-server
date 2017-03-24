angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('member.changepassword', {
	      url: '/changepassword',
	      templateUrl: 'views/shared/changepassword/changepassword.html' 
	    })
	    .state('provider.changepassword', {
	      url: '/changepassword',
	      templateUrl: 'views/shared/changepassword/changepassword.html' 
	    })
	    .state('research.changepassword', {
	      url: '/changepassword',
	      templateUrl: 'views/shared/changepassword/changepassword.html' 
	    })
	    .state('admin.changepassword', {
	      url: '/changepassword',
	      templateUrl: 'views/shared/changepassword/changepassword.html' 
	    })
	    .state('developer.changepassword', {
	      url: '/changepassword',
	      templateUrl: 'views/shared/changepassword/changepassword.html' 
	    });	    
}]);