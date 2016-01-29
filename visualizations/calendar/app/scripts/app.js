'use strict';

angular.module('calendarApp', ['ngRoute', 'midata', 'ui.bootstrap', 'ui.calendar'])
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
    .when('/cal/add', {
    	templateUrl: 'views/addsources.html',
        controller: 'SourcesCtrl'
    })
    .when('/cal/display', {
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
