
angular.module('fhirDebug', [ 'midata', 'ui.router','ui.bootstrap', 'pascalprecht.translate' ])
.config(['$stateProvider', '$urlRouterProvider', '$translateProvider', function($stateProvider, $urlRouterProvider, $translateProvider) {	    
    
	$translateProvider
	.useSanitizeValueStrategy('escape')	   	    
	.registerAvailableLanguageKeys(['en', 'de', 'it', 'fr'], {
	  'en_*': 'en',
	  'de_*': 'de',
	  'fr_*': 'fr',
	  'it_*': 'it',
	})
	.translations('en', en)
	.translations('de', de)
	.translations('it', it)
	.translations('fr', fr)
	.fallbackLanguage('en');
	
	 $stateProvider
	    .state('list', {
	      url: '/list?authToken',	   
	      templateUrl: 'list.html'
	    })
	    .state('welcome', {
	      url: '/welcome?authToken',	   
	      templateUrl: 'welcome.html'
	    })
	    .state('resourcepick', {
	      url: '/resourcepick?authToken',	   
	      templateUrl: 'resourcepick.html'
	    })
	    .state('resources', {
	      url: '/resources?authToken',	   
	      templateUrl: 'resources.html'
	    })
	    .state('query', {
	      url: '/query?authToken&type',	   
	      templateUrl: 'query.html'
	    })
	    .state('results', {
	      url: '/results?authToken&query&type',	   
	      templateUrl: 'results.html'
	    })	 
	    .state('resource', {
	      url: '/resource?id&type&authToken',	   
	      templateUrl: 'resource.html'
	    });
	 
	 $urlRouterProvider
	 .otherwise('/welcome');  
}])
.run(['$translate', '$location', 'midataPortal', 'midataServer', function($translate, $location, midataPortal, midataServer) {
	console.log("Language: "+midataPortal.language);
    console.log($location.search());
	$translate.use(midataPortal.language);	
    midataPortal.autoresize();
				
	midataServer.authToken = $location.search().authToken;
	//var params = $location.search();
    
}])
;
