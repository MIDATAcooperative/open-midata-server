var services = angular.module('services', []);
var views = angular.module('views', ['services']);
angular.module('portal', [ 'ngCookies', 'ui.router', 'ui.bootstrap', 'services', 'views', 'config', 'ngPostMessage', 'angularUtils.directives.dirPagination', 'pascalprecht.translate'])
.config(['$stateProvider', '$urlRouterProvider', '$httpProvider', '$translateProvider', function($stateProvider, $urlRouterProvider, $httpProvider, $translateProvider) {
   //$httpProvider.defaults.useXDomain = true;
   $httpProvider.defaults.withCredentials = true;
   //delete $httpProvider.defaults.headers.common['X-Requested-With'];
   
       
   $translateProvider
     .useSanitizeValueStrategy('escape')
     .useLocalStorage()
     .useLoader('$translatePartialLoader', {
        urlTemplate: '/i18n/{part}_{lang}.json'
      })   
     .registerAvailableLanguageKeys(['en', 'de'], {
       'en_*': 'en',
       'de_*': 'de'
     })
     //.fallbackLanguage('en')
     .determinePreferredLanguage();
   
   $stateProvider
    .state('public', {
      url: '/portal',
      data : { locales : 'members' },
      templateUrl: 'assets/nav/public.html'
    })
    .state('public_provider', {
      url: '/hc',
      data : { locales : 'providers' },
      templateUrl: 'assets/nav/public.html'
    })
    .state('public_research', {
      url: '/research',
      data : { locales : 'researchers' },
      templateUrl: 'assets/nav/public.html'
    })
    .state('public_developer', {
      url: '/developer',
      data : { locales : 'developers' },
      templateUrl: 'assets/nav/public.html'
    })
    .state('member', {
      url: '/member',
      data : { role : 'MEMBER', locales : 'members' },
      templateUrl: 'assets/nav/member.html'
    })
    .state('research', {
      url: '/research',
      data : { role : 'RESEARCH', locales : 'researches' },
      templateUrl: 'assets/nav/research.html'
    })
    .state('provider', {
      url: '/provider',
      data : { role : 'PROVIDER', locales : 'providers' },
      templateUrl: 'assets/nav/provider.html'
    })
    .state('developer', {
      url: '/developer',
      data : { role : 'DEVELOPER', locales : 'developers' },
      templateUrl: 'assets/nav/developer.html'
    })
    .state('admin', {
      url: '/admin',
      data : { role : 'ADMIN', locales : 'admins' },
      templateUrl: 'assets/nav/admin.html'
    });   
   
   $urlRouterProvider
   .otherwise('/portal/login');
}])
.run(['$state', '$rootScope', '$translate', '$translatePartialLoader', function($state, $rootScope, $translate, $translatePartialLoader) {   
   $rootScope.$on('$translatePartialLoaderStructureChanged', function () {
      $translate.refresh();
   });
   $translatePartialLoader.addPart("shared");
}]);