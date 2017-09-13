'use strict';

describe('Directive: weightScale', function () {

  // load the directive's module
  beforeEach(module('weightWatcherApp'));

  var element,
    scope;

  beforeEach(inject(function ($rootScope) {
    scope = $rootScope.$new();
  }));

  it('should make hidden element visible', inject(function ($compile) {
    element = angular.element('<weight-scale></weight-scale>');
    element = $compile(element)(scope);
    expect(element.text()).toBe('this is the weightScale directive');
  }));
});
