'use strict';

angular.module('waterMeterApp')
  /**
   * Water glass directive that displays a water glass, represented by a
   * cyllinder in SVG, which fills up to a certain level based on the relative
   * water amount provided.
   */
  .directive('waterGlass', ['$window', function ($window) {
    function watchDimensions(scope, element){
      scope.dimensions = {
        width : element[0].clientWidth,
        height : element[0].clientHeight,
        stroke : {
          width : 2
        },
        arc : {
          ry : 10,
          rx : null
        }
      };
      scope.dimensions.arc.rx = (scope.dimensions.width -
        scope.dimensions.stroke.width)/2;
      scope.coordinates = {
        left : scope.dimensions.stroke.width/2,
        top : scope.dimensions.stroke.width/2  + scope.dimensions.arc.ry,
        right : scope.dimensions.width - scope.dimensions.stroke.width/2,
        bottom : scope.dimensions.height - scope.dimensions.arc.ry -
          scope.dimensions.stroke.width/2
      };
      scope.coordinates.center = (scope.coordinates.left +
                                  scope.coordinates.right)/2;
      scope.pathStyle = {
        'stroke-width' : scope.dimensions.stroke.width
      };
      var maxWaterLevel = 200;
      scope.waterLevel = scope.coordinates.bottom -
        parseFloat(scope.waterLevelValue)*(scope.coordinates.bottom -
            scope.coordinates.top)*0.95/maxWaterLevel;
    }
    return {
      templateUrl: 'templates/water-glass.html',
      restrict: 'E',
      scope : {
        'waterLevelValue' : '='
      },
      link: function postLink(scope, element) {
        watchDimensions(scope, element);
        // Attach a listener to changes in the window (i.e. iframe)
        // whenever the window size changes, update the dimension variables
        angular.element($window).bind('resize', function(){
          watchDimensions(scope, element);
          scope.$apply();
        });
      }
    };
  }]);
