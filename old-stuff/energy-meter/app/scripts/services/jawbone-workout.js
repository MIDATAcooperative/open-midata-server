'use strict';

angular.module('energyMeterApp')
  /**
   * Service that implements all the Jawbone-specific logic and transforms the
   * data to the required format by the main controller in the visualization.
   */
  .service('JawboneWorkout', function JawboneWorkout() {
    var api = {};
    var markerDays = {
      0 : 'images/jogging-sunday.png',
      1 : 'images/jogging-monday.png',
      2 : 'images/jogging-tuesday.png',
      3 : 'images/jogging-wednesday.png',
      4 : 'images/jogging-thursday.png',
      5 : 'images/jogging-friday.png',
      6 : 'images/jogging-saturday.png'
    };

    /**
     * Preprocess jawbone records and filter out other records.
     *
     * Adds a date field as well as a type field to a record.
     * The date is given in the granularity of days (without time).
     * The type is 'workoutList'.
     * Finally, the data is a top-level node.
     */
    api.preprocessRecord = function(record) {
        var data = {},
        type = null,
        date = null;

        // capture the record's data
        if (_.has(record, 'data') && _.has(record.data, 'data') ) {
            data = record.data.data;
        }

        // set the type of the record
        if (_.has(data, 'items')) {
            if (_.isArray(data.items) && data.items.length > 0) {
                if (_.has(data.items[0], 'details') && _.has(data.items[0].details, 'intensity')) {
                    type = 'workoutList';
                }
            }
        }

        // set the date of the record (without time)
        if (type === 'workoutList') {
            if (_.has(data.items[0], 'time_created')) {
                date = new Date(data.items[0].time_created * 1000);
                // round to midnight
                date = new Date(date.getFullYear(), date.getMonth(), date.getDate());
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
     * Generate an array of marker models from an array of weekly workout data.
     */
    api.generateMarkers = function(weekData){
      var markers = _.map(weekData, function generateMarkersForDay(element){
        return _.map(element.data.items,
          function generateMarkersForWorkout(innerElement){
            return {
              longitude : innerElement.place_lon, //jshint ignore:line
              latitude : innerElement.place_lat, //jshint ignore:line
              icon : markerDays[element.date.getDay()],
              id : element.date.getDay(),
              workout : innerElement
            };
          });
      });
      return _.flatten(markers, true);
    }

    /**
     * Generate a summary object from the weekly data.
     */
    api.generateSummary = function(weekData){
      var summary = {
        totalCalories : 0,
        totalSteps : 0,
        totalTime : 0,
        caloriesPerDay : [],
        stepsPerDay : [],
        timePerDay : [],
        intensityPercents : [],
      };
      var intensityCounts = {
          1 : 0,
          2 : 0,
          3 : 0,
          4 : 0,
          5 : 0
        },
          workoutCount = 0;
      _.each(weekData, function dataPerDay(element){
        var dailyCalories = 0,
            dailySteps = 0,
            dailyTime = 0;
        _.each(element.data.items, function dataPerWorkout(innerElement){
          dailyCalories += innerElement.details.calories;
          dailySteps += innerElement.details.steps;
          dailyTime += innerElement.details.time;
          intensityCounts[innerElement.details.intensity]++;
          workoutCount++;
        });
        summary.totalCalories += dailyCalories;
        summary.totalSteps += dailySteps;
        summary.totalTime += dailyTime/60;
        summary.caloriesPerDay.push({
          dayNumber : element.date.getDay(),
          calories : dailyCalories
        });
        summary.stepsPerDay.push({
          dayNumber : element.date.getDay(),
          steps: dailySteps
        });
        summary.timePerDay.push({
          dayNumber : element.date.getDay(),
          time : dailyTime/60
        });
      });
      _.each(summary.caloriesPerDay, function percents(element){
        element.percent = element.calories*100/summary.totalCalories;
      });
      _.each(summary.stepsPerDay, function percents(element){
        element.percent = element.steps*100/summary.totalSteps;
      });
      _.each(summary.timePerDay, function percents(element){
        element.percent = element.time*100/summary.totalTime;
      });
      summary.intensityPercents = _.map(intensityCounts, function percents(val){
        return {percent : val*100/workoutCount,
                count : val};
      });
      return summary;
    }
    return api;
  });
