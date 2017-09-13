'use strict';

describe('Service: MessageGenerator', function () {

  // load the service's module
  beforeEach(module('weightWatcherApp'));

  // instantiate service
  var MessageGenerator;
  beforeEach(inject(function (_MessageGenerator_) {
    MessageGenerator = _MessageGenerator_;
  }));

  it('should do something', function () {
    expect(!!MessageGenerator).toBe(true);
  });

});
