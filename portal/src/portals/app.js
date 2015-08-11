var services = angular.module('services', []);
var views = angular.module('views', ['services']);
angular.module('portal', [ 'ui.router', 'services', 'views' ])
.constant('apiurl', 'https://localhost:9000')
.config(function($stateProvider, $urlRouterProvider, $httpProvider) {
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
      templateUrl: 'assets/nav/member.html'
    })
    .state('research', {
      url: '/research',
      templateUrl: 'assets/nav/research.html'
    })
    .state('provider', {
      url: '/provider',
      templateUrl: 'assets/nav/provider.html'
    })
    .state('developer', {
      url: '/developer',
      templateUrl: 'assets/nav/developer.html'
    });   
   
   $urlRouterProvider
   .otherwise('/portal/login');
});