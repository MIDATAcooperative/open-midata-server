var services = angular.module('services', []);
var views = angular.module('views', ['services']);
angular.module('portal', [ 'ngCookies', 'ui.router', 'ui.bootstrap', 'services', 'views', 'config', 'ngPostMessage', 'angularUtils.directives.dirPagination', 'pascalprecht.translate', 'ngSanitize'])
.config(['$stateProvider', '$urlRouterProvider', '$httpProvider', '$translateProvider', 'ENV', '$locationProvider', function($stateProvider, $urlRouterProvider, $httpProvider, $translateProvider, ENV, $locationProvider) {
   //$httpProvider.defaults.useXDomain = true;
   //$httpProvider.defaults.withCredentials = true;
   //delete $httpProvider.defaults.headers.common['X-Requested-With'];
   $httpProvider.interceptors.push('SessionInterceptor');
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
      data : { role : 'RESEARCH', locales : 'researchers' },
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
   
   $rootScope.greeting = {};
   $rootScope.$on('$stateChangeStart', function(event, toState, toParams, fromState, fromParams, options){ 
       $rootScope.greeting = {};
   });
   
}])
.config(['$compileProvider', function ($compileProvider) {
  $compileProvider.debugInfoEnabled(false);
  $compileProvider.commentDirectivesEnabled(false);
  $compileProvider.cssClassDirectivesEnabled(false);  
}])
.config(['paginationTemplateProvider', function(paginationTemplateProvider) {
    paginationTemplateProvider.setPath('assets/directives/dirPagination.tpl.html');
}])
.service('SessionInterceptor', ["$injector", "$q" , function($injector, $q) {
    var service = this;    
    service.responseError = function(response) {    	
    	var $state = $injector.get("$state");
    	if ($state.current.nointerceptor) return $q.reject(response); 
        if (response.status === 401) {
        	if ($state.includes("provider") || $state.includes("public_provider")) $state.go("public_provider.login");
			else if ($state.includes("research") || $state.includes("public_research")) $state.go("public_research.login");
			else if ($state.includes("admin") || $state.includes("developer") || $state.includes("public_developer")) $state.go("public_developer.login");
			else $state.go("public.login");
            
        } else if (response.status === 403) {
        	if (response.data && (response.data.requiredSubUserRole || response.data.requiredFeature)) {
        		var p = $state.current.name.split(".")[0];
        		console.log($state.current);
        		console.log(p);
        		$state.go(p+".upgrade", { role : response.data.requiredSubUserRole, feature : response.data.requiredFeature });
        	}
        }
        return $q.reject(response);
    };
}]);