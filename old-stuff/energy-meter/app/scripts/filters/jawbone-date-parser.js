'use strict';

angular.module('energyMeterApp')
  /**
   * Transform a Jawbone date (i.e. formatted as yyyymmdd) into a Javascript
   * Date object.
   */
  .filter('jawboneDateParser', function () {
    return function (input) {
      var year = parseInt(input.slice(0, 4));
      var month = parseInt(input.slice(4, 6));
      var day = parseInt(input.slice(6));
      return new Date(year, month - 1, day);
    };
  });
