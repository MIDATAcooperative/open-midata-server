angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('member.servicekeys', {
	      url: '/servicekeys',
	      templateUrl: 'views/shared/apikeys/apikeys.html'
        })
        .state('provider.servicekeys', {
            url: '/servicekeys',
            templateUrl: 'views/shared/apikeys/apikeys.html'
        })
        .state('research.servicekeys', {
            url: '/servicekeys?studyId',
            templateUrl: 'views/shared/apikeys/apikeys.html'
        })
        .state('developer.servicekeys', {
            url: '/servicekeys',
            templateUrl: 'views/shared/apikeys/apikeys.html'
        })
        .state('admin.servicekeys', {
            url: '/servicekeys',
            templateUrl: 'views/shared/apikeys/apikeys.html'
        });
	    
}]);