angular.module('portal')
.config(function($stateProvider) {
	   $stateProvider
	    .state('member.usergroupsearch', {
	      url: '/usergroups/search?city&name',
	      templateUrl: 'views/shared/usergroupsearch/usergroupsearch.html' 
	    });
});