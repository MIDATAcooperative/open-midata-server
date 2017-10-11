angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('member.studies', {
	      url: '/studies',
	      templateUrl: 'views/members/smallstudies/smallstudies.html',	      
	      dashId : 'smallstudies'
	    });	
}]);