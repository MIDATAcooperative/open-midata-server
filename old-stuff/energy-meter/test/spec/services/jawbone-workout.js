'use strict';

describe('Service: JawboneWorkout', function () {

  // load the service's module
  beforeEach(module('energyMeterApp'));

  // instantiate service
  var JawboneWorkout;
  beforeEach(inject(function (_JawboneWorkout_) {
    JawboneWorkout = _JawboneWorkout_;
  }));

  it('should do something', function () {
    expect(!!JawboneWorkout).toBe(true);
  });

});
