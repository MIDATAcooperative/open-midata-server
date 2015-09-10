angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.setupquestions', {
	      url: '/help/setupquestions',
	      templateUrl: 'views/members/dashboardTiles/help/help_setup.html' 
	    });
});