'use strict';

describe('Service: FitbitResources', function () {

  // load the service's module
  beforeEach(module('timeSeriesApp'));

  // instantiate service
  var FitbitResources;
  beforeEach(inject(function (_FitbitResources_) {
    FitbitResources = _FitbitResources_;
  }));

  it('should do something', function () {
    expect(!!FitbitResources).toBe(true);
  });

});
