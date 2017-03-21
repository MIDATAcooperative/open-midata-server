angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('member.changeaddress', {
	      url: '/changeaddress',
	      templateUrl: 'views/shared/changeaddress/changeaddress.html' 
	    })
	    .state('provider.changeaddress', {
	      url: '/changepassword',
	      templateUrl: 'views/shared/changeaddress/changeaddress.html' 
	    })
	    .state('research.changeaddress', {
	      url: '/changeaddress',
	      templateUrl: 'views/shared/changeaddress/changeaddress.html' 
	    })
	    .state('admin.changeaddress', {
	      url: '/changeaddress',
	      templateUrl: 'views/shared/changeaddress/changeaddress.html' 
	    })
	    .state('developer.changeaddress', {
	      url: '/changeaddress',
	      templateUrl: 'views/shared/changeaddress/changeaddress.html' 
	    });	    
}]);