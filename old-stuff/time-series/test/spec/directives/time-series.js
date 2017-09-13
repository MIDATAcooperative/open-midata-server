'use strict';

describe('Directive: timeSeries', function () {

  // load the directive's module
  beforeEach(module('timeSeriesApp'));

  var element,
    scope;

  beforeEach(inject(function ($rootScope) {
    scope = $rootScope.$new();
  }));

  it('should make hidden element visible', inject(function ($compile) {
    element = angular.element('<time-series></time-series>');
    element = $compile(element)(scope);
    expect(element.text()).toBe('this is the timeSeries directive');
  }));
});
