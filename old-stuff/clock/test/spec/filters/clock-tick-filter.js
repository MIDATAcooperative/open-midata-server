'use strict';

describe('Filter: clockTickFilter', function () {

  // load the filter's module
  beforeEach(module('clockApp'));

  // initialize a new instance of the filter before each test
  var clockTickFilter;
  beforeEach(inject(function ($filter) {
    clockTickFilter = $filter('clockTickFilter');
  }));

  it('should return the input prefixed with "clockTickFilter filter:"', function () {
    var text = 'angularjs';
    expect(clockTickFilter(text)).toBe('clockTickFilter filter: ' + text);
  });

});
