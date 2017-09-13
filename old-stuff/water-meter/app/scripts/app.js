'use strict';

angular
  .module('waterMeterApp', [
    'ngRoute',
    'ngAnimate'
  ])
  /**
   * Configure the app with a single route for the path which reads the
   * authorization token from the URL.
   */
  .config(function ($routeProvider) {
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
  });
