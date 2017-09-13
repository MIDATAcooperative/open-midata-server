'use strict';

describe('Filter: pointsFilter', function () {

  // load the filter's module
  beforeEach(module('timeSeriesApp'));

  // initialize a new instance of the filter before each test
  var pointsFilter;
  beforeEach(inject(function ($filter) {
    pointsFilter = $filter('pointsFilter');
  }));

  it('should return the input prefixed with "pointsFilter filter:"', function () {
    var text = 'angularjs';
    expect(pointsFilter(text)).toBe('pointsFilter filter: ' + text);
  });

});
