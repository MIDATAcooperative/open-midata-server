angular.module('surveys', [ 'midata', 'ui.router','ui.bootstrap', 'pascalprecht.translate', 'chart.js' ])
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
	    .state('edititem', {
	      url: '/edititem?questionnaire&item&parent&idx&authToken',	   
	      templateUrl: 'edititem.html'
	    })
	    .state('editsurvey', {
	      url: '/editsurvey?questionnaire&authToken',	   
	      templateUrl: 'editsurvey.html'
	    })
	    .state('pick', {
	      url: '/pick?authToken',	    
	      templateUrl: 'pick.html'
	    })
	    .state('previewsurvey', {
	      url: '/previewsurvey?questionnaire&authToken',	   
	      templateUrl: 'previewsurvey.html'
	    })
	    .state('results', {
	      url: '/results?questionnaire&authToken',	   
	      templateUrl: 'results.html'
	    });
	 
	 $urlRouterProvider
	 .otherwise('/pick');  
}])
.run(['$translate', '$location', 'midataPortal', 'midataServer', function($translate, $location, midataPortal, midataServer) {    
	$translate.use(midataPortal.language);	
    midataPortal.autoresize();				
	midataServer.authToken = $location.search().authToken;    
}]);
