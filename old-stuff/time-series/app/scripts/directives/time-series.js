'use strict';

/**
 * The time series directive implements a line chart with data points and a
 * brush element that allows zoom-in-and-out over the horizontal axis.
 */
angular.module('timeSeriesApp').directive('timeSeries', ['$filter', '$window',
  function($filter, $window){
    // Memory heavy objects are stored outside the scope and just in the
    // function closure for efficiency.
    var d3Helpers = {};

    /**
     * Define all basic dimension elements needed throughout the directive
     * e.g. width, margins, height of content, etc...
     */
    function initializeDimensions(scope){
      scope.dimensions = {
        width : null,
        height : null,
        axis : {
          x : {
            title : {
              height : 20,
            },
            height : 25,
          },
          y : {
            title : {
              width : 30,
            },
            width : 50,
          },
        },
        margins : {
          top : 30,
          right : 30,
          bottom : null,
          left : null,
        },
        content : {
          height : null,
          width : null,
        },
        context : {
          height : null,
          y : null
        },
        focus : {
          height : null,
        }
      };
    }

    /**
     * Update the necessary dimension variables when a change occurs in the
     * containing element, e.g. a window resize.
     */
    function watchDimensions(scope, element){
      // This element expands to the available space.
      scope.dimensions.width = element[0].clientWidth;
      scope.dimensions.height = element[0].clientHeight;
      // The margin bottom must be enough to hold the X axis of the context
      // graph and the title of the X axis.
      scope.dimensions.margins.bottom = scope.dimensions.axis.x.title.height +
        scope.dimensions.axis.x.height;
      // The margin to the left must hold the Y axis for the focus graph
      // and the title of the Y axis.
      scope.dimensions.margins.left = scope.dimensions.axis.y.title.width +
        scope.dimensions.axis.y.width;
      // The content height is the height that can be used for the
      // focus and context graphs without axii.
      scope.dimensions.content.height = scope.dimensions.height -
        scope.dimensions.margins.bottom - scope.dimensions.margins.top -
          scope.dimensions.axis.x.height;
      // The content height is divided 60/40 for focus and context respectively.
      scope.dimensions.context.height = 0.4*scope.dimensions.content.height;
      scope.dimensions.focus.height = 0.6*scope.dimensions.content.height;
      // The content width is the width that can be used for the graphs
      // without the axis and title.
      scope.dimensions.content.width = scope.dimensions.width -
        scope.dimensions.margins.left - scope.dimensions.margins.right;
      // The context y coordinate indicates where the context graph starts.
      scope.dimensions.context.y = scope.dimensions.margins.top +
        scope.dimensions.focus.height + scope.dimensions.axis.x.height;

      // An update in the dimensions require updating the D3 helpers
      updateD3Helpers(scope);
    }

    /**
     * Initialize the D3 helpers which build the different axis and lines
     * in the scope.
     */
    function initializeD3Helpers(scope, element){
      d3Helpers.element = d3.select(element[0]);
      d3Helpers.scales = {
        focus : {
          x : null,
          y : null,
        },
        context : {
          x : null,
          y : null,
        },
      };
      d3Helpers.axii = {
        x : d3.svg.axis().orient('bottom')
          .ticks(5).outerTickSize(2).innerTickSize(10),
        y : d3.svg.axis().orient('left')
          .ticks(5).outerTickSize(2).innerTickSize(10)
      };
      d3Helpers.generators = {
        focus : {
          line : d3.svg.line().interpolate('cardinal')
            .x(function getScaleDatetime(d){
              return d3Helpers.scales.focus.x(d.datetime);
            })
            .y(function getScaledValue(d){
              return d3Helpers.scales.focus.y(d.value);
            }),
          point : [
            d3.svg.symbol().size(32).type('circle'),
            d3.svg.symbol().size(32).type('cross'),
            d3.svg.symbol().size(32).type('diamond'),
            d3.svg.symbol().size(32).type('square'),
            d3.svg.symbol().size(32).type('triangle-down')
          ]
        },
        context : {
          area : d3.svg.area().interpolate('cardinal')
            .x(function getScaledDatetime(d){
              return d3Helpers.scales.focus.x(d.datetime);
            })
            .y0(function getAreaBase(){
              return d3Helpers.scales.context.y(0);
            })
            .y1(function getAreaHeight(d){
              return d3Helpers.scales.context.y(d.value);
            }),
          brush : d3.svg.brush()
            .on('brush', function onBrush(){
              d3Helpers.scales.focus.x.domain(
                d3Helpers.generators.context.brush.empty() ?
                  d3Helpers.scales.context.x.domain() :
                  d3Helpers.generators.context.brush.extent());
              d3Helpers.element.select('.time-series-x.time-series-axis.' +
                'time-series-focus').call(d3Helpers.axii.x);
              generateLinePaths(scope);
              generatePoints(scope);
              // This apply is necessary because this is not an angular event
              // but a D3 one.
              scope.$apply();
            })
        }
      };
      scope.tooltip = {x : 0, y : 0};
    }

    /**
     * Update the scales and axis D3 objects when the dimensions change.
     */
    function updateD3Helpers(scope){
      d3Helpers.scales.focus.x = d3.time.scale()
        .range([0, scope.dimensions.content.width]);
      d3Helpers.scales.focus.y = d3.scale.linear()
        .range([scope.dimensions.focus.height, 0]);
      d3Helpers.scales.context.x = d3.time.scale()
        .range([0, scope.dimensions.content.width]);
      d3Helpers.scales.context.y = d3.scale.linear()
        .range([scope.dimensions.context.height, 0]);
      d3Helpers.axii.x.scale(d3Helpers.scales.focus.x);
      d3Helpers.axii.y.scale(d3Helpers.scales.focus.y);
      d3Helpers.generators.context.brush.x(d3Helpers.scales.context.x);
      watchData(null, null, scope);
    }

    /**
     * Generate the line paths to draw the different data objects in the
     * focus viewport.
     */
    function generateLinePaths(scope){
      scope.dataLines = _.map(scope.data, function getDataLine(ele){
          return d3Helpers.generators.focus.line(ele.data);
        });
    }

    /**
     * Generate the area paths to draw the different data objects in the
     * context viewport.
     */
    function generateAreaPaths(scope){
      scope.dataAreas = _.map(scope.data, function getDataArea(ele){
          return d3Helpers.generators.context.area(ele.data);
        });
    }

    /**
     * Generate the sets of points to display in the graph.
     */
    function generatePoints(scope){
      scope.pointSets = _.map(scope.data, function getPointSet(ele, idx){
        return {
          points : $filter('pointsFilter')(ele.data,
            d3Helpers.scales.focus.x,
            d3Helpers.scales.focus.y,
            ele.units),
          symbol : d3Helpers.generators.focus.point[idx % 5](),
          colorClass : 'time-series-focus-point-' + idx % 5
        };
      });
    }

    /**
     * Respond to data changes by drawing again the axii, lines, area and
     * brush.
     */
    function watchData(newData, oldData, scope){
      if (scope.data) {
        _.each(scope.data, function sortData(ele){
          ele.data = _.sortBy(ele.data, function getDate(val){
            return val.datetime;
          });
        });
        var maxDate = d3.max(_.map(scope.data, function getMaxDate(ele){
          return ele.data[ele.data.length - 1].datetime;
        }));
        var minDate = d3.min(_.map(scope.data, function getMinDate(ele){
            return ele.data[0].datetime;
          }));
        var maxVal = d3.max(_.map(scope.data, function getMaxVal(ele){
          return _.max(ele.data, function getVal(val){
            return val.value;
          }).value;
        }));
        d3Helpers.scales.focus.x.domain([minDate, maxDate]);
        d3Helpers.scales.focus.y.domain([0, 1.1*maxVal]);
        d3Helpers.scales.context.x.domain(d3Helpers.scales.focus.x.domain());
        d3Helpers.scales.context.y.domain(d3Helpers.scales.focus.y.domain());
        d3Helpers.element.select(
            '.time-series-x.time-series-axis.time-series-focus').call(
            d3Helpers.axii.x);
        d3Helpers.element.select(
            '.time-series-x.time-series-axis.time-series-context').call(
            d3Helpers.axii.x);
        d3Helpers.element.select('.time-series-y.time-series-axis').call(
            d3Helpers.axii.y);
        generateLinePaths(scope);
        generateAreaPaths(scope);
        generatePoints(scope);
        d3Helpers.element.select('.time-series-x.time-series-brush').call(
          d3Helpers.generators.context.brush).selectAll('rect').attr('height',
          scope.dimensions.context.height).style('fill-opacity', '.125');

      }
    }

    /**
     * Function that returns a scope-aware function that displays a
     * tooltip with the value of the point that triggered the mouseenter
     * event.
     */
    function showTooltip(scope){
      return function showTooltipScoped(element){
        scope.tooltip.show = true;
        scope.tooltip.x = element.x;
        scope.tooltip.y = element.y - 5;
        scope.tooltip.value = element.original.value + ' ' +
          element.original.units;
      };
    }

    /**
     * Function that returns a scope-aware function that hides the current
     * tooltip, if any is present.
     */
    function hideTooltip(scope){
      return function hideTooltipScoped(){
        scope.tooltip.show = false;
      };
    }

    return {
      restrict : 'E',
      templateUrl : 'templates/time-series.tpl.html',
      scope : {
        data : '=',
        yTitle : '@',
        xTitle : '@axisXTitle',
      },
      link : function postLink(scope, element){
        // Initialize the directive objects
        initializeDimensions(scope);
        initializeD3Helpers(scope, element);
        // Run a first update which sets all dependent objects to theeir
        // correct values,
        watchDimensions(scope, element);
        // Attach a listener to changes in the window (i.e. iframe)
        // whenever the window size changes, update the dimension variables
        angular.element($window).bind('resize', function(){
          watchDimensions(scope, element);
          scope.$apply();
        });
        // Setup watchers for the data
        scope.$watch('data', watchData);
        scope.$watchCollection('data', watchData);

        scope.showTooltip = showTooltip(scope);
        scope.hideTooltip = hideTooltip(scope);
      }
    };
  } ]);
