'use strict';

angular.module('timeSeriesApp')
  /**
   * Filter that returns the points visible in the current viewport
   * after it has been modified by the brush element.
   * This also attaches the units to the points so a tooltip can be displayed
   * on mouseover.
   */
  .filter('pointsFilter', function () {
    return function(input, xScale, yScale, units){
      var newInput = input.filter(
        function(val){
          return xScale.domain()[0] <= val.datetime &&
            xScale.domain()[1] >= val.datetime;
        }).map(function(val){
        val.units = units;
        return {
          x : xScale(val.datetime),
          y : yScale(val.value),
          original : val
        };
      });
      return newInput;
    };
  });
