'use strict';

angular.module('weightWatcherApp')
  /**
   * The BmiZones service provides an API with 3 functions:<br>
   * 1. getIntervals: Returns the BMI zones that occur between two BMI values.
   * <br>
   * 2. getZone: Returns the BMI zone corresponding to a BMI value.<br>
   * 3. getRecommendation: Returns a helpful message based on the given BMI
   * value. This is based on the health recommendations by the WHO.
   */
  .service('BmiZones', ['$filter', function BmiZones($filter) {
    var api = {};
    var breakpoints = {
        '16' : {
          name: 'Severe thinness',
          colorClass : 'weightscale-bmi-severe-thinness',
        },
        '17' : {
          name : 'Moderate thinness',
          colorClass : 'weightscale-bmi-moderate-thinness'
        },
        '18.5' : {
          name : 'Mild thinness',
          colorClass : 'weightscale-bmi-mild-thinness'
        },
        '25' : {
          name : 'Normal',
          colorClass : 'weightscale-bmi-normal'
        },
        '30' : {
          name : 'Overweight',
          colorClass : 'weightscale-bmi-overweight'
        },
        '35' : {
          name : 'Obese class I',
          colorClass : 'weightscale-bmi-obese-i'
        },
        '40' : {
          name : 'Obese class II',
          colorClass : 'weightscale-bmi-obese-ii'
        },
        '100' : {
          name : 'Obese class II',
          colorClass : 'weightscale-bmi-obese-iii'
        }
      };
    var sortedBreakpoints = _.sortBy(_.map(_.keys(breakpoints), function(val){
        return parseFloat(val);
      }), function(val){
        return val;
      });
    /**
     * Returns the BMI zones that occur between two BMI values. <br>
     * This zones are based on the WHO classification of BMI values,
     * available in: http://apps.who.int/bmi/index.jsp?introPage=intro_3.html.
     * <br>
     * The zones are in ascending order and each one is an Object with two
     * properties: interval and bmiInfo.<br>
     * interval: An Array with two float values that indicate the endpoints of
     * the zone.<br>
     * bmiInfo: An Object with two properties, name and colorClass. Name is
     * the name of the BMI range and colorClass is a CSS class defined
     * in weight-scale.css that provides color identification to the zones.<br>
     */
    api.getIntervals = function getIntervals(lowBmi, highBmi){
      var sortedBreakpointsCount = sortedBreakpoints.length,
          lowIndex = -1,
          highIndex = -1,
          i = -1;
      for(i = 0; i < sortedBreakpointsCount - 1; i++){
        if(sortedBreakpoints[i] > lowBmi) {
          lowIndex = i;
          break;
        }
      }
      for(i = sortedBreakpointsCount - 2; i >= 0; i--){
        if(sortedBreakpoints[i] < highBmi){
          highIndex = i;
          break;
        }
      }
      var result = [];
      if(lowIndex === -1){
        result.push({
          interval : [lowBmi, highBmi],
          bmiInfo : breakpoints[sortedBreakpoints[highIndex + 1].toString()]
        });
      } else if(highIndex === -1 || lowIndex === highIndex + 1){
        result.push({
          interval : [lowBmi, highBmi],
          bmiInfo : breakpoints[sortedBreakpoints[lowIndex].toString()]
        });
      } else {
        for(i = lowIndex; i <= highIndex; i++){
          if(i === lowIndex){
            result.push({
              interval : [lowBmi, sortedBreakpoints[i]],
              bmiInfo : breakpoints[sortedBreakpoints[i].toString()]
            });
          } else {
            result.push({
              interval : [sortedBreakpoints[i-1], sortedBreakpoints[i]],
              bmiInfo : breakpoints[sortedBreakpoints[i].toString()]
            });
          }
        }
        result.push({
          interval : [sortedBreakpoints[highIndex], highBmi],
          bmiInfo : breakpoints[sortedBreakpoints[highIndex + 1].toString()]
        });
      }
      return result;
    };
    /**
     * Returns the BMI zone where the given BMI value is in.<br>
     * This BMI zone Object has two properties, name and colorClass.<br>
     *  * name: The name of the BMI zone according to the WHO.<br>
     *  * colorClass: A CSS class defined in weight-scale.css for color
     *    identification of the BMI zone.
     */
    api.getZone = function getZone(bmi){
      for (var i = sortedBreakpoints.length - 2; i >= 0; i--) {
        if(bmi > sortedBreakpoints[i]){
          return breakpoints[sortedBreakpoints[i + 1].toString()];
        }
      }
      return breakpoints[sortedBreakpoints[0].toString()];
    };
    /**
     * Returns a friendly message based on the current BMI value and weight.<br>
     * The message returned depends on the usual recommendation regarding
     * normal and unhealthy BMI zones. The returned Object has a msg
     * property with the message String and a context property which is a String
     * corresponding to a Bootstrap CSS class to style the resulting text.
     */
    api.getRecommendation = function getRecommendation(bmi, weight){
      if(bmi >= 18.5 && bmi < 25){
        return {
          msg: 'Champ! Your BMI is in the recommended range. ' +
            'Keep up the good work!',
          context : 'text-success'
        };
      } else if(bmi >= 25 && bmi < 30){
        return {
          msg : 'Careful there! Your BMI is in the overweight zone, ' +
            'keep an eye on it. You are just ' +
            $filter('number')(weight - 23 * weight/bmi, 1) +
            ' kg over the ideal BMI zone.',
          context : 'text-warning'
        };
      } else if(bmi >= 30){
        return {
          msg : 'Your BMI indicates obesity, please consult with a doctor ' +
            'in order to improve this situation. ' +
            'Surely you can drop those extra grams!',
          context : 'text-danger'
        };
      } else {
        return {
          msg : 'Careful there! We recommend to gain a bit of weight. ' +
            'Your BMI is under the recommended zone, ' +
            'maybe grab an extra (veggie) burger in the next BBQ!',
          context : 'text-warning'
        };
      }
    };
    return api;
  }]);
