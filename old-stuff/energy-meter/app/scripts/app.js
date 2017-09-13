'use strict';

angular
  /**
   * Define the app with a single route that is called when the authorization
   * token is passed in the URL.
   */
  .module('energyMeterApp', [
    'ngRoute',
    'google-maps'.ns()
  ])
  .config(function ($routeProvider) {
    $routeProvider
      .when('/:authToken', {
        templateUrl: 'views/main.html',
        controller: 'MainCtrl'
      })
      .otherwise({
        redirectTo: '/'
      });
  });
