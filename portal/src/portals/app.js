var services = angular.module('services', []);

angular.module('portal', [ 'ui.router', 'services' ])
.constant('apiurl', 'https://localhost:9000')
.config(function($stateProvider, $urlRouterProvider) {
   $stateProvider
    .state('public', {
      url: '/portal',
      templateUrl: 'common/html/public.html'
    })
    .state('public_hc', {
      url: '/hc',
      templateUrl: 'common/html/public.html'
    })
    .state('public_research', {
      url: '/research',
      templateUrl: 'common/html/public.html'
    })
    .state('public_developer', {
      url: '/developer',
      templateUrl: 'common/html/public.html'
    })
    .state('member', {
      url: '/member',
      templateUrl: 'common/html/member.html'
    });    
});