'use strict';

describe('Service: mealJawboneService', function () {

  // load the service's module
  beforeEach(module('mealApp'));

  // instantiate service
  var mealJawboneService;
  beforeEach(inject(function (_mealJawboneService_) {
    mealJawboneService = _mealJawboneService_;
  }));

  it('should do something', function () {
    expect(!!mealJawboneService).toBe(true);
  });

});
