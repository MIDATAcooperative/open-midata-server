'use strict';

describe('Filter: jawboneDateParser', function () {

  // load the filter's module
  beforeEach(module('energyMeterApp'));

  // initialize a new instance of the filter before each test
  var jawboneDateParser;
  beforeEach(inject(function ($filter) {
    jawboneDateParser = $filter('jawboneDateParser');
  }));

  it('should return the input prefixed with "jawboneDateParser filter:"', function () {
    var text = 'angularjs';
    expect(jawboneDateParser(text)).toBe('jawboneDateParser filter: ' + text);
  });

});
