'use strict';

describe('Service: clockJawboneService', function () {

  // load the service's module
  beforeEach(module('clockApp'));

  // instantiate service
  var clockJawboneService;
  beforeEach(inject(function (_clockJawboneService_) {
    clockJawboneService = _clockJawboneService_;
  }));

  it('should do something', function () {
    expect(!!clockJawboneService).toBe(true);
  });

});
