'use strict';

describe('Service: clockUtilsService', function () {

  // load the service's module
  beforeEach(module('clockApp'));

  // instantiate service
  var clockUtilsService;
  beforeEach(inject(function (_clockUtilsService_) {
    clockUtilsService = _clockUtilsService_;
  }));

  it('should do something', function () {
    expect(!!clockUtilsService).toBe(true);
  });

});
