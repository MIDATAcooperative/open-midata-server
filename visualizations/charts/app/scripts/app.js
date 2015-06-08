'use strict';

angular.module('chartApp', ['ngRoute', 'midata', 'chart.js'])
  /**
   * Configure a single route to the app when the authorization token is
   * passed through the URL.
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
