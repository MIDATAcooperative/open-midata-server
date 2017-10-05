'use strict';

angular.module('clockApp')
  /**
   * Service that encapsulates the logic related to the Jawbone data format
   * and transformations to the data format used by the clock directive.
   */
  .factory('clockJawboneService', function () {
    var instance = {};

    // The fields contained in Jawbone move ticks are fixed
    instance.makeMoveField = function () {
        return [{value: 'steps', text: 'Steps', unit: 'steps'},
                {value: 'calories', text: 'Calories', unit: 'calories'},
                {value: 'activeTime', text: 'Active Time', unit: 'seconds'},
                {value: 'distance', text: 'Distance', unit: 'meters' },
                {value: 'speed', text: 'Speed', unit: 'm/s'}];
    };

    /**
     * Preprocess jawbone records and filter out other records.
     * 
     * Adds a date field as well as a type field to a record.
     * The date is given in the granularity of days (without time).
     * The type can be one of 'moveList', 'moveTicks', 'sleepList' or 
     * 'sleepTicks'.
     * Finally, the data is a top-level node.
     */
     instance.preprocessRecord = function(record) {
        var data = {},
            type = null,
            date = null;

        // remove the meta information and bring the data node to the top level
        if (_.has(record, 'data') && _.has(record.data, 'data')) {
            data = record.data.data;
        }

        // set the type of the record
        if (_.has(data, 'items')) {
            if (_.isArray(data.items) && data.items.length === 1) {
                if (_.has(data.items[0], 'type') && data.items[0].type === 'move') {
                    type = 'moveList';
                } else if (_.has(data.items[0], 'details') && _.has(data.items[0].details, 'awakenings')) {
                    type = 'sleepList';
                }
            } else if (data.items.length > 1) {
                if (_.has(data.items[0], 'active_time')) {
                    type = 'moveTicks';
                } else if (_.has(data.items[0], 'depth')) {
                    type = 'sleepTicks';
                }
            }
        }

        // set the date of the record (without time)
        if (type === 'moveList' || type === 'sleepList') {
            if (_.has(data.items[0], 'time_completed')) {
                date = new Date(data.items[0].time_completed * 1000);
                date = new Date(date.toDateString());
            }
        } else if (type === 'moveTicks' || type === 'sleepTicks') {
            if (_.has(_.last(data.items), 'time')) {
                date = new Date(_.last(data.items).time * 1000);
                date = new Date(date.toDateString());
            }
        }

        // If preprocessing was successful, return the record, otherwise null
        if (_.isEmpty(data) || !type || !date) {
            return null;
        } else {
            record.data = data;
            record.type = type;
            record.date = date;
            return record;
        }
     }

    /**
     *  This function transforms the move data into the format to be
     *  used by the visualization widget.
     *
     *  An item from move tick has the following attributes:
     *      distance: int   Distance travelled in meters
     *      active_time: int   Active time during this tick bucket, in seconds
     *      calories: int   Calories burned during this tick bucket
     *      steps: int   Steps taken during this tick bucket
     *      time: int   Timestamp for start of this tick bucket in second
     *      speed: int   Speed is calculated as distance/active_time (m/s)
     *
     *  An item from clock move data has the following attributes:
     *      distance (m)
     *      activeTime (s)
     *      calories
     *      steps
     *      speed: avaraged speed, (m/s)
     *      startTime: time stamp (s)
     *      endTime: time stamp (s)
     */
    instance.transformMoveTicksData = function (tickItems, width) {
        // Sort the ticks according to the time
        var tickItemsGrouped = [];

        tickItems.sort(function (a, b) {
            return a.time - b.time;
        });

        // Group the ticks in a width of 20 buckets (20 minutes in length)
        if (tickItems.length > 0) {
            var element = tickItems[0],
                group = {
                    distance: element.distance,
                    activeTime: element.active_time, // jshint ignore:line
                    calories: element.calories,
                    steps: element.steps,
                    speed: element.speed,
                    startTime: element.time,
                    endTime: element.time + width - 1,
                    count: 1
                };

            for (var i = 1; i < tickItems.length; i += 1) {
                element = tickItems[i];

                var timeElapsed = element.time - group.startTime;
                if (timeElapsed < width) {
                    group.distance += element.distance;
                    group.activeTime += element.active_time;//jshint ignore:line
                    group.calories += element.calories;
                    group.steps += element.steps;
                    group.speed += element.speed;
                    group.count += 1;
                } else {
                    // Average the speed
                    group.speed = group.speed / group.count;
                    group.calories = group.calories.toFixed(2);
                    group.speed = group.speed.toFixed(2);

                    tickItemsGrouped.push(group);

                    // Reset the group object
                    group = {
                        distance: element.distance,
                        activeTime: element.active_time, // jshint ignore:line
                        calories: element.calories,
                        steps: element.steps,
                        speed: element.speed,
                        startTime: element.time,
                        endTime: element.time + width - 1,
                        count: 1
                    };
                }
            }
        }

        return tickItemsGrouped;
    };

    /**
     *  This function transforms the sleep data into the format to be
     *  used by the visualization widget.
     *
     *  An item from sleep tick has the following attributes:
     *      depth: int   Sleep pahse. 1=awake, 2=light, 3=sound
     *      time: int    Timestamp when this sleep phase started in second
     *
     *  An item from sleep list has the following attributes:
     *      time_completed: int      Timestamp when this sleep was completed in second
     *
     *  An item from clock clock data has the following attributes:
     *      depth
     *      description
     *      startTime: time stamp (s)
     *      endTime: time stamp (s)
     */
    instance.transformSleepData = function(listItems, tickItems) {
        var tickItemsTransformed = [];

        tickItems.sort(function(a, b) {
            return a.time - b.time;
        });

        if (listItems.length === 1) {
            var listElement = listItems[0];

            for (var i = 1; i < tickItems.length - 1; i++) {
                var element = tickItems[i],
                    nextElement = tickItems[i + 1],
                    transformedElement = {
                        depth: element.depth,
                        description: instance.getSleepTickDescription(element.depth),
                        startTime: element.time,
                        endTime: nextElement.time
                    };

                tickItemsTransformed.push(transformedElement);
            }

            var lastElement = tickItems[tickItems.length - 1];
            tickItemsTransformed.push({
                depth: lastElement.depth,
                description: instance.getSleepTickDescription(lastElement.depth),
                startTime: lastElement.time,
                endTime: listElement.time_completed // jshint ignore:line
            });
        }

        return tickItemsTransformed;
    };

    /**
     * Retrieve the sleep tick description given a depth value.
     */
    instance.getSleepTickDescription = function(depth) {
        if (depth === 1) {
            return 'awake';
        } else if (depth === 2) {
            return 'light sleep';
        } else {
            return 'deep sleep';
        }
    };

    return instance;
});
