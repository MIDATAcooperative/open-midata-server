'use strict';

describe('Service: BmiZones', function () {

  // load the service's module
  beforeEach(module('weightWatcherApp'));

  // instantiate service
  var BmiZones;
  beforeEach(inject(function (_BmiZones_) {
    BmiZones = _BmiZones_;
  }));

  it('should do something', function () {
    expect(!!BmiZones).toBe(true);
  });

});
