'use strict';

angular.module('timeSeriesApp')
  /**
   * Service that defines fibtit-specific data and functions related to the
   * resource API documented in:
   * https://wiki.fitbit.com/display/API/Fitbit+Resource+Access+API
   */
  .service('FitbitResources', function FitbitResources() {
    var api = {};
    /**
     * Define all the possible resources obtained from the time series
     * call to the fitbit time series API:
     * https://wiki.fitbit.com/display/API/API-Get-Time-Series
     */
    api.timeSeriesResources = {
      'foods-log-caloriesIn' : {title: 'Calories intake', units : 'kcal'},
      'foods-log-water' : {title: 'Water consumption', units : 'ml'},
      'activities-calories' : {title: 'Calories burned', units : 'kcal'},
      'activities-steps' : {title: 'Steps', units : 'steps'},
      'activities-distance' : {title: 'Distance', units : 'm'},
      'activities-floors' : {title: 'Floors', units : 'floors'},
      'activities-elevation' : {title : 'Elevation', units : 'm'},
      'activities-minutesSedentary' : {title: 'Minutes sedentary',
                                       units: 'min'},
      'activities-minutesLightlyActive' : {title: 'Minutes lightly active',
                                           units: 'min'},
      'activities-minutesFairlyActive' : {title: 'Minutes fairly active',
                                          units: 'min'},
      'activities-minutesVeryActive' : {title: 'Minutes very active',
                                        units: 'min'},
      'activities-activityCalories' : {title: 'Calories burned in activities',
                                       units: 'kcal'},
      'sleep-timeInBed' : {title: 'Time in bed', units: 'min'},
      'sleep-minutesAsleep' : {title:'Minutes asleep', units: 'min'},
      'sleep-minutesAwake' : {title: 'Minutes awake', units: 'min'},
      'sleep-minutesToFallAsleep' : {title:'Minutes to fall asleep',
                                     units: 'min'},
      'sleep-efficiency' : {title: 'Sleep efficiency',
                            units: '%'},
      'body-weight' : {title: 'Weight', units: 'kg'},
      'body-bmi' : {title: 'BMI', units: ''},
      'body-fat' : {title: 'Fat percentage', units: '%'},
      'weight' : {title: 'Weight', units: 'kg'}
    };
    api.timeSeriesResourcesKeys = _.keys(api.timeSeriesResources);
    return api;
  });
