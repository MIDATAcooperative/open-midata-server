'use strict';

angular.module('clockApp', ['ngRoute', 'ui.bootstrap'])
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