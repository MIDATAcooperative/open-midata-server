/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

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
   
   //This is a workaround that prevents angular translate using a different language than reported by $translate.use() 
   $translate("-1");
}])
.config(['$compileProvider', function ($compileProvider) {
  $compileProvider.debugInfoEnabled(false);
  $compileProvider.commentDirectivesEnabled(false);
  $compileProvider.cssClassDirectivesEnabled(false);  
}]);
