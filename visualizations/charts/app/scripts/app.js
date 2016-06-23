'use strict';

angular.module('chartApp', ['ngRoute', 'midata', 'chart.js', 'ui.bootstrap', 'pascalprecht.translate'])
  /**
   * Configure a single route to the app when the authorization token is
   * passed through the URL.
   */
  .config(['$routeProvider', '$translateProvider', function ($routeProvider, $translateProvider) {
  
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
  
    $routeProvider
    .when('/:authToken', {
      templateUrl: 'views/main.html',
      controller: 'MainCtrl'
    })
    .when('/preview/:authToken', {
            templateUrl: 'views/preview.html',
            controller: 'MainCtrl'
    })
    .otherwise({
      redirectTo: '/'
    });
  }])
.run(['$translate', 'midataPortal', function($translate, midataPortal) {
 	console.log("Language: "+midataPortal.language);
	$translate.use(midataPortal.language);	   	  
}]);