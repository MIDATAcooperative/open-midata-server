var services = angular.module('services', []);
var views = angular.module('views', ['services']);
angular.module('portal', [ 'ngCookies', 'ui.router', 'ui.bootstrap', 'services', 'views', 'config', 'ngPostMessage', 'angularUtils.directives.dirPagination'])
.config(['$stateProvider', '$urlRouterProvider', '$httpProvider', function($stateProvider, $urlRouterProvider, $httpProvider) {
   //$httpProvider.defaults.useXDomain = true;
   $httpProvider.defaults.withCredentials = true;
   //delete $httpProvider.defaults.headers.common['X-Requested-With'];
   
   $stateProvider
    .state('public', {
      url: '/portal',
      templateUrl: 'assets/nav/public.html'
    })
    .state('public_provider', {
      url: '/hc',
      templateUrl: 'assets/nav/public.html'
    })
    .state('public_research', {
      url: '/research',
      templateUrl: 'assets/nav/public.html'
    })
    .state('public_developer', {
      url: '/developer',
      templateUrl: 'assets/nav/public.html'
    })
    .state('member', {
      url: '/member',
      data : { role : 'MEMBER' },
      templateUrl: 'assets/nav/member.html'
    })
    .state('research', {
      url: '/research',
      data : { role : 'RESEARCH' },
      templateUrl: 'assets/nav/research.html'
    })
    .state('provider', {
      url: '/provider',
      data : { role : 'PROVIDER' },
      templateUrl: 'assets/nav/provider.html'
    })
    .state('developer', {
      url: '/developer',
      data : { role : 'DEVELOPER' },
      templateUrl: 'assets/nav/developer.html'
    })
    .state('admin', {
      url: '/admin',
      data : { role : 'ADMIN' },
      templateUrl: 'assets/nav/admin.html'
    });   
   
   $urlRouterProvider
   .otherwise('/portal/login');
}])
/*.run(['$state', '$cookies', '$rootScope', 'session', function($state, $cookies, $rootScope, session) {
    $rootScope.$on('$stateChangeStart', function(e, toState, toParams, fromState, fromParams) {    	
    	var sc = $cookies.get("session");
    	if (sc != null && session.storedCookie != null && sc!=session.storedCookie) {    
    		session.storedCookie = null;
    		document.location.href="/#/public/login";
    		e.preventDefault();
    	}    	
    });
}])*/;