'use strict';

angular.module('weightWatcherApp')
  .service('MessageGenerator', ['BmiZones', '$filter',
    /**
     * The MessageGenerator service returns an API object with a single function
     * generateMessages which generate "HDC tips" as described below in the
     * function's documentation.
     */
    function MessageGenerator(BmiZones, $filter) {
      var api = {};
      /**
       * Generator of "HDC tips" based on the weight data recorded up to this
       * day.<br>
       * Currently it generates 3 messages:<br>
       * 
       * 1. A message indicating if the last measurement is recent or not.
       * A measurement is not considered recent if it's older than 1 week.<br>
       * 2. A message based on the BMI value for the most recent measurement.
       * It tries to advice the user on whether to lose or gain weight based
       * on the WHO classification of BMI values.<br>
       * 3. A message that indicates the weight change compared to the last 2
       * weeks. <br>
       * 
       * These messages are returned with the following format:<br>
       *
       * {msg: 'The actual message',
       *  context: 'A bootstrap CSS class for text color'}
       */
      api.generateMessages = function generateMessages(recordList){
        var ONE_WEEK_MS = 604800000,
            TWO_WEEK_MS = 1296000000,
            messageList = [],
            latestMeasurement = recordList[0];

        // First check the last measurement datje, if it's more than a week old
        // then recommend to take a new measurement
        var latestMeasurementDate = new Date(latestMeasurement.weight[0].date);
        if(Date.now() - latestMeasurementDate > ONE_WEEK_MS){
          messageList.push({msg: 'You have not logged your weight in a while,' +
            ' please update your information to provide you with better tips.',
              context : 'text-warning'});
        } else {
          messageList.push({msg: 'Your data is up-to-date, nice job!',
              context: 'text-success'});
        }

        // Now check the BMI zone and provide advice based on it
        var bmiZoneRecommendation =
          BmiZones.getRecommendation(latestMeasurement.weight[0].bmi,
            latestMeasurement.weight[0].weight);
        messageList.push(bmiZoneRecommendation);

        // Analyze the last month of data to provide some insight on weight
        // changes.
        for (var i = recordList.length - 1; i >= 0; i--) {
          if(latestMeasurementDate - new Date(recordList[i].weight[0].date) >
              TWO_WEEK_MS){
            var weightDiff = recordList[i].weight[0].weight -
              latestMeasurement.weight[0].weight;
            if(weightDiff < 0){
              messageList.push({
                msg: 'You have gained ' + $filter('number')(weightDiff, 1) +
                  ' kg in two weeks.',
                context : 'text-primary'
              });
            } else if(weightDiff > 0){
              messageList.push({
                msg: 'You have lost ' + $filter('number')(weightDiff, 1) +
                  ' kg in two weeks.',
                context : 'text-primary'
              });
            } else {
              messageList.push({
                msg: 'Your weight has been constant in the last two weeks.',
                context : 'text-primary'
              });
            }
            break;
          }
        }
        // Shuffle the messages
        return _.shuffle(messageList);
      };
      return api;
    }]);
