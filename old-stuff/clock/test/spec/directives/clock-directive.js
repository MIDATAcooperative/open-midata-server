'use strict';

describe('Directive: clockDirective', function () {

  // load the directive's module
  beforeEach(module('clockApp'));

  var element,
    scope;

  beforeEach(inject(function ($rootScope) {
    scope = $rootScope.$new();
  }));

  it('should make hidden element visible', inject(function ($compile) {
    element = angular.element('<clock-directive></clock-directive>');
    element = $compile(element)(scope);
    expect(element.text()).toBe('this is the clockDirective directive');
  }));
});
