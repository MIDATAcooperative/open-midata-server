'use strict';

angular.module('weightWatcherApp')
  /**
   * Weight scale directive that renders a minimalistic rectangular
   * display that shows the current weight in kilograms of the user
   * along with the BMI zone the user is currently in. The BMI zones
   * are defined as per the WHO's classification.<br>
   * Each zone is identified by different colors and the scale shows the range
   * that occurs between the currently displayed weight and plus/minus 10 kg.
   */
  .directive('weightScale', ['BmiZones', '$window',
    function (BmiZones, $window) {
      /**
       * This function is called whenever the dimensions of the element change
       * (i.e. a resize of the window). It initializes the dimension
       * objects auxiliary according to the current width and height.<br>
       * This objects are used to render the SVG directive, it also
       * calls the update function for the scale.
       */
      function watchDimensions(scope, element){
        scope.dimensions = {
          width : element[0].clientWidth,
          height : element[0].clientHeight,
          tickWidth : 4,
          tickHeight : 5
        };
        scope.margins = {
          left: 0,
          right : 0,
          top : 0
        };
        scope.dimensions.viewHeight = scope.dimensions.height -
          scope.margins.top;
        scope.dimensions.viewWidth = scope.dimensions.width -
          scope.margins.left - scope.margins.right;
        scope.dimensions.border = {
          thickness : 2,
          x : 1,
          y : 1
        };
        scope.dimensions.border.width = scope.dimensions.viewWidth -
          scope.dimensions.border.thickness;
        scope.dimensions.border.height = scope.dimensions.viewHeight -
          scope.dimensions.border.thickness;
        watchScale(null, null, scope);
      }
      /**
       * Initialization and update function for the scale.<br>
       * This function renders the scale based on the current weight and BMI.
       * It defines the BMI zones and assigns the proper segments in the scale,
       * additionally defines the properties of the weight indicator in the
       * center of the scale. <br>
       * It also calls the update function for the axis ticks at the end.
       */
      function watchScale(newValue, oldValue, scope){
        scope.weight = parseFloat(scope.weight || 10.0);
        scope.bmi = parseFloat(scope.bmi || 10.0);
        scope.lowWeight = Math.floor(scope.weight - 10.0);
        scope.highWeight = Math.ceil(scope.weight + 10.0);
        var lowBmi = scope.lowWeight * scope.bmi / scope.weight;
        var highBmi = scope.highWeight * scope.bmi / scope.weight;
        var bmiRange = highBmi - lowBmi;
        var bmiIntervals = BmiZones.getIntervals(lowBmi, highBmi);
        scope.bmiZones = _.map(bmiIntervals, function(ele){
          return {
            width : Math.ceil(scope.dimensions.viewWidth *
              (ele.interval[1] - ele.interval[0]) / bmiRange),
            height : scope.dimensions.viewHeight,
            y : 0,
            x : Math.floor(scope.dimensions.viewWidth *
              (ele.interval[0] - lowBmi) / bmiRange),
            colorClass : ele.bmiInfo.colorClass
          };
        });
        scope.indicator = {
          x : (scope.weight - scope.lowWeight) * scope.dimensions.viewWidth /
            (scope.highWeight - scope.lowWeight) -
            scope.dimensions.tickWidth / 2,
          y : 0,
          height : scope.dimensions.viewHeight,
          width : scope.dimensions.tickWidth
        };
        watchAxis(scope);
      }
      /**
       * Initialization and update function for the ticks in the scale.<br>
       * It renders the ticks at the bottom of the scale which show the weight
       * values.
       */
      function watchAxis(scope){
        scope.ticks = _.map(_.range(scope.lowWeight + 1, scope.highWeight, 1),
          function(val, idx){
            var tick = {
              width : scope.dimensions.tickWidth,
              height : scope.dimensions.tickHeight,
              x : (idx + 1) * scope.dimensions.viewWidth /
                (scope.highWeight - scope.lowWeight) -
                scope.indicator.width / 2,
              y : scope.dimensions.viewHeight -
                scope.dimensions.border.thickness -
                scope.dimensions.tickHeight,
              text : val
            };
            tick.xText = tick.x + tick.width / 2;
            tick.yText = tick.y - 2;
            return tick;
          });
      }
      return {
        templateUrl: 'templates/weight-scale.tpl.html',
        restrict: 'E',
        scope : {
          quote : '@',
          weight : '@',
          bmi : '@'
        },
        link: function postLink(scope, element) {
          // Initialize the dimension variables, and all other elements.
          watchDimensions(scope, element);
          // Attach a listener to changes in the window (i.e. iframe)
          // whenever the window size changes, update the dimension variables
          angular.element($window).bind('resize', function(){
            watchDimensions(scope, element);
            scope.$apply();
          });
          // Attach watchers to the weight and bmi values.
          scope.$watch('weight', watchScale);
          scope.$watch('bmi', watchScale);
        }
      };
    }]);
