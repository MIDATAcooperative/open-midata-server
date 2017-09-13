'use strict';

angular.module('clockApp')
    /**
     * Filter out any hour that is not a multiple of 3.
     */
    .filter('clockTickFilter', function () {
        return function (input) {
            return _.filter(input, function (value) {
                return parseInt(value.hour, 10) % 3 === 0;
            });
        };
    });