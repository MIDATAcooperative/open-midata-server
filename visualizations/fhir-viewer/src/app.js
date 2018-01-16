
angular.module('fhirViewer', [ 'midata', 'ui.router','ui.bootstrap', 'pascalprecht.translate' ])
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
	    .state('resource', {
	      url: '/resource?id&type&authToken',	   
	      templateUrl: 'resource.html'
	    });
	 
	 $urlRouterProvider
	 .otherwise('/resource');  
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
