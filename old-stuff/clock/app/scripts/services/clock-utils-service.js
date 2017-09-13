'use strict';

angular.module('clockApp')
    /**
     * Provides various utilities to transform the data to clock specific
     * elements and values.
     */
    .factory('clockUtilsService', function () {
        var instance = {};

        /**
         * Degrees to radian conversion.
         */
        instance.degree2Rad = function degree2Rad(degree) {
            return degree * Math.PI / 180;
        };

        /**
         * Radians to degrees conversion.
         */
        instance.rad2Degree = function rad2Degree(rad) {
            return  rad * 180 / Math.PI;
        };

        /**
         * Convert an hour,minute pair to an angle in the clock.
         */
        instance.time2Angle = function time2Angle(hour, minute) {
            var angle = 360 * hour / 24;
            angle = angle + (360 / 24) * (minute / 60);

            return angle;
        };

        /**
         * Produce a D3 arc given radius and angle parameters.
         */
        instance.makeArc = function makeArc(innerRadius, outerRadius,
                                            startAngle, endAngle) {
            return d3.svg.arc()
                .innerRadius(innerRadius)
                .outerRadius(outerRadius)
                .startAngle(startAngle)
                .endAngle(endAngle);
        };

        /**
         * Make a clock tick object given an hour.
         */
        instance.makeClockTick = function makeClockTick(hour) {
            return {
                angle : hour > 12 ? instance.time2Angle(hour - 12, 0) :
                                    instance.time2Angle(hour + 12, 0),
                hour : hour
            };
        };
        return instance;
    });
