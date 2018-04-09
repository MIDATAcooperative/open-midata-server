var services = angular.module('services', []);
var views = angular.module('views', ['services']);
angular.module('portal', [ 'ngCookies', 'ui.router', 'services', 'views', 'config', 'pascalprecht.translate', 'ngSanitize'])
.config(['$stateProvider', '$urlRouterProvider', '$httpProvider', '$translateProvider', 'ENV', '$locationProvider', function($stateProvider, $urlRouterProvider, $httpProvider, $translateProvider, ENV, $locationProvider) {
   //$httpProvider.defaults.useXDomain = true;
   //$httpProvider.defaults.withCredentials = true;
   //delete $httpProvider.defaults.headers.common['X-Requested-With'];
	$locationProvider.hashPrefix('');

   $translateProvider
     .useSanitizeValueStrategy('escape')
     .useLocalStorage()
     .useLoader('$translatePartialLoader', {
        urlTemplate: '/i18n/{part}_{lang}.json'
      })   
     .registerAvailableLanguageKeys(['en', 'de', 'fr', 'it'], {
       'en_*': 'en',
       'de_*': 'de',
       'fr_*': 'fr',
       'it_*': 'it'
     })
     .fallbackLanguage('en')
     .determinePreferredLanguage();
   
   $stateProvider
    .state('public', {
      url: '/portal',
      data : { locales : 'members' },
      templateUrl: 'assets/nav/oauthnav.html'
    });   

    if (["demo", "localhost", "test"].indexOf(ENV.instance) >= 0) {
      $urlRouterProvider.otherwise('/portal/info');
    } else {
      $urlRouterProvider.otherwise('/portal/login');
    }
}])
.run(['$state', '$rootScope', '$translate', '$translatePartialLoader', function($state, $rootScope, $translate, $translatePartialLoader) {   
   $rootScope.$on('$translatePartialLoaderStructureChanged', function () {
      $translate.refresh();
   });
   $rootScope.currentDate = new Date();
   $translatePartialLoader.addPart("shared");
}])
.config(['$compileProvider', function ($compileProvider) {
  $compileProvider.debugInfoEnabled(false);
  $compileProvider.commentDirectivesEnabled(false);
  $compileProvider.cssClassDirectivesEnabled(false);  
}]);
